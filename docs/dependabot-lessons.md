# Dependabot 优化经验总结

本文档记录在本仓库 (`XenoAmess/docker-image-rebecca`) 进行 Dependabot 自动合并
流水线改造过程中**真实踩到**的问题、原因和解决方式,以及沉淀下来的经验。

> 适用读者:本仓库维护者,以及任何想给一个 Maven + GitHub Actions 项目
> 接入 Dependabot 自动合并的工程师。

---

## TL;DR

Dependabot 自动合并**不是**写一个 `auto-merge.yml` 就完事的。它至少需要**六件事同时
对齐**,少任何一件,失败都是**静默**的(看起来跑成功了,实际啥也没发生)。
本仓库修复前就是一个"看起来在工作、其实从来没合并过"的典型例子。

| # | 必须存在的部件 | 本仓库修复前状态 |
|---|---|---|
| 1 | `.github/workflows/auto-merge.yml` | 有,但 `if` 写错、token 错、策略不完整 |
| 2 | `.github/workflows/build.yml` 触发 `pull_request` | ❌ 只触发 `push` |
| 3 | 仓库 `allow_auto_merge = true` | ❌ `false`(默认关闭) |
| 4 | PAT secret(`repo` + `workflow` 权限) | ✅ `MYTOKEN` 已存在 |
| 5 | 分支保护 + 必需 CI 检查 | ❌ 无保护,任意 push 可直入 |
| 6 | `.github/dependabot.yml` | 有,但 daily + limit=100 + 无分组 |

修复后:推送 → 约 1 分钟内,dependabot 自己关闭了旧的 3 个零散 PR,创建了 2 个
分组 PR(#349 actions 三合一、#350 maven-minor-and-patch),全部自动合并。

---

## 一、本仓库修复前后对比

### 1. `.github/dependabot.yml`

| 项目 | 修复前 | 修复后 |
|---|---|---|
| 调度 | `interval: daily` | `interval: weekly`, 周一 04:00 Asia/Shanghai |
| open-PR 上限 | `100` | maven `10`, github-actions `5` |
| commit 前缀 | 无 | `build(deps)` / `build(deps-dev)` |
| labels | 无 | `dependencies` + `java` / `github-actions` |
| 分组 | 无 | maven minor+patch 一组、maven major 一组、actions 一组 |

**为什么这样配**:

- **daily + limit=100** 是噪音放大器:每天最多 100 个 PR 同时存在,
  永远清不完。weekly + 5–10 是"周一集中处理、其余时间安静"的节奏。
- 分组把"每天一个小 PR"变成"周一一次一组合并",PR 数量从 ~30/月降到 ~4/月,
  CI 触发次数同步下降。

### 2. `.github/workflows/auto-merge.yml`

**关键修复(按重要性排序)**:

1. **`if` 同时匹配新旧 Dependabot 登录名**
   ```yaml
   if: |
     github.event.pull_request.user.login == 'dependabot[bot]' ||
     github.event.pull_request.user.login == 'app/dependabot'
   ```
   - `app/dependabot` 是 GitHub 新版 Dependabot(2024+)
   - `dependabot[bot]` 是旧版
   - 本仓库所有现行 PR 都是 `app/dependabot`,原 `if: github.actor == 'dependabot[bot]'` **永远不命中**

2. **`gh pr merge --auto` 使用 `MYTOKEN`(PAT),不是 `GITHUB_TOKEN`**
   ```yaml
   env:
     GH_TOKEN: ${{ secrets.MYTOKEN }}
   ```
   - 任何修改 `.github/workflows/*.yml` 的 PR(基本上 100% 的 `dependabot/github_actions/*` PR)
     都会触发 GitHub 限制:`GITHUB_TOKEN` 没有 `workflows` 权限,无法启用 auto-merge。
   - `dependabot/fetch-metadata` 步骤是只读,可以继续用 `GITHUB_TOKEN`。
   - 失败信息长这样:
     `Pull request refusing to allow a GitHub App to create or update workflow '.github/workflows/xxx.yml'`

3. **GitHub Actions 的 `semver-major` 也自动合并**
   ```bash
   [[ "$TYPE" == "version-update:semver-major" ]] && [[ "$REF" == dependabot/github_actions/* ]]
   ```
   - GitHub Actions 的 major 版本基本就是 Node runtime 升级,几乎不会破坏构建。
   - 用 head ref 前缀(`dependabot/github_actions/*`)作为生态判断器,因为
     `update-type` 本身无法区分 ecosystem。
   - Maven major 留给人工审核,因为 Java API 变更可能破坏构建
     (本仓库 checkstyle 9.3→13.6.0 的 CI 就是这么挂的)。

4. **先 approve 再 enable auto-merge**
   - 让 PR 在合并前至少有个 approve 记录,审计更友好。

### 3. `.github/workflows/build.yml`

```yaml
on:
  push:
    branches: [ master ]   # ← 限定到 master,避免重复跑
  pull_request:             # ← 新增,这是 auto-merge 能"等 CI 通过"的前提
```

- 只加 `pull_request:` 不限定 `push:`,会导致 dependabot 每次推 PR 头分支都触发两次
  (push + pull_request),3 OS × 3 JDK 矩阵 = 一次 PR 浪费 9 × 2 = 18 个 job。
- 限定 `push: branches: [ master ]` 之后,master 直推只跑 push,PR 头分支只跑 pull_request,完全正交。

### 4. 仓库设置

```bash
gh api -X PATCH repos/XenoAmess/docker-image-rebecca -f allow_auto_merge=true
```

- `allow_auto_merge` 是**仓库级别**设置,跟分支保护**独立**,**默认是 false**。
- 不开它,`gh pr merge --auto` 会**静默 422**:`Auto merge is not allowed for this repository`。
- 修复前最后一次 `auto-merge.yml` 的运行日志里 `Enable auto-merge for Dependabot PRs: skipped`
  就是这条原因留下的痕迹。

---

## 二、修复后踩到的额外问题

### 1.5 第二轮优化(2025) — 去掉 `groups:` 与 `include: scope`

修复后第一版用了 `groups: patterns: ["*"]` 把每个生态的所有升级合成一个 PR,看起来 PR 数量少了,实际踩到更糟的几个坑:

1. **巨型 PR 无法定位失败**(Pitfall 13)。标题形如
   `build(deps)(deps): bump the maven-major group with 1 update`,
   失败时日志无法指向具体哪个依赖。 比如 PR #352
   (`build(deps)(deps): bump com.puppycrawl.tools:checkstyle from 9.3 to 13.7.0 in the maven-major group`)
   就是这套配置产出的。
2. **Maven-major 分组把不该合的 major 留在人类手里,但被绑成了"巨型 major PR"**。
   自动合并策略本来就是"maven major 留人工审",一旦所有 major 被绑成一个 PR,
   人类就要一次性审 N 个破坏性升级,几乎不可能逐个判断。
3. **`include: "scope"` 跟 `prefix:` 在 rebase 后会叠成双重前缀**
   (`build(deps)(deps): ...`),纯 cosmetic 但噪声大。

修法(本仓库当前配置):

```yaml
# .github/dependabot.yml — 关键变更点
-      include: "scope"      # 删除,避免 (deps)(deps) 双前缀
+      prefix: "ci"          # github-actions 用 ci,跟 maven 的 build(deps) 区分
       labels: [...]
-      groups:               # 整段删除,回退到 "每依赖每周期一个 PR"
-        maven-minor-and-patch:
-          patterns: ["*"]
-          update-types: [minor, patch]
-        ...
```

回退到 `groups:` 之前的行为后:
- 每个依赖每周最多一个 PR,  diff 足够小,  review / revert / bisect 都隔离
- PR 计数上升, 但 prefix + labels 已经把整条流编得很整齐, 不会失控
- 标题回到 `build(deps): bump <dep> from X to Y`,  `ci: bump actions/... from X to Y`

#### 必须手动处理的孤儿 PR

config 改了之后, 已开的 grouped PR 不会自动消失; dependabot 也不会自动把它们拆开。
对 PR #352 这种"被 mvn major 策略拒收"、又是老 grouped 配置留下的 PR:

```bash
gh pr close <N> --delete-branch=false   # 关掉,下次 weekly 让 dependabot 开新的 per-dep PR
```

下次 weekly(周一 04:00 Asia/Shanghai)开始, dependabot 会按新配置生成 1 PR / 依赖。



### 2.1 推送时被 OAuth scope 拒绝

```
! [remote rejected] master -> master (refusing to allow an OAuth App
to create or update workflow `.github/workflows/auto-merge.yml` without
`workflow` scope)
```

- `gh auth` 拿到的 `gho_*` token 只有 `repo`、`read:org` 等,**没有 `workflow` 权限**,
  而修改 `.github/workflows/*.yml` 必须有。
- 最简单的解法:**把 remote 从 HTTPS 切到 SSH**,走本地 SSH key,绕开 OAuth 限制:
  ```bash
  git remote set-url origin git@github.com:XenoAmess/docker-image-rebecca.git
  ```
- `gh` CLI 的 protocol 已经是 `ssh`(在 `gh auth status` 里能看到),说明这台机器的
  SSH key 已经配置好,直接切就行。

### 2.2 commit 标题出现重复前缀

PR #345 重新 rebase 后,标题变成:

```
build(deps)(deps): bump com.puppycrawl.tools:checkstyle from 9.3 to 13.6.0
```

- 原 PR 的标题来自依赖更新前生成的 `build(deps):`,rebase 时 dependabot 又把
  新的 `prefix: build(deps)` 套了上去,产生 `build(deps)(deps)`。
- 这是 **cosmetic** 问题,不影响功能。如果想清掉,close + reopen 这个 PR
  就会用新的 prefix 重生成标题。

### 2.3 fork 依赖 `commons-compress:1.23.x.1`

`pom.xml` 里的:

```xml
<dependency>
    <groupId>com.xenoamess.fork.org.apache.commons</groupId>
    <artifactId>commons-compress</artifactId>
    <version>1.23.x.1</version>
</dependency>
```

这个 **x-range 版本号** 在 Dependabot 的 semver 分类里识别不太干净。
如果后续需要,可以把固定版本换成普通 semver (`1.23.0`),让 dependabot 能正确
判断 major/minor/patch。

---

## 三、通用 Pitfall 速查

这是从真实失败里提炼的"失败原因表",遇到问题按表对照:

| 现象 | 真因 | 修复 |
|---|---|---|
| Major 版本 PR 永远不合并 | auto-merge 只匹配 minor/patch | 在条件链里加 `semver-major` + 用 head ref 前缀区分 ecosystem |
| Auto-merge 跳过 CI 直接合并 | `build.yml` 没触发 `pull_request` | 加 `pull_request:` 到 `on:`,并配 branch protection 必需 CI |
| `mergeStateStatus: BLOCKED`,但 CI 是绿的 | 必需 CI 名写错了(没带矩阵维度) | 用 GraphQL 查真实 `isRequired` 状态,看 `build (ubuntu-latest, 17, false)` 这种全名 |
| 一次 PR 跑两次 CI | `on: [push, pull_request]` 都触发 | `push` 限定 `branches: [master]` |
| `GITHUB_TOKEN` 在 workflow PR 上 422 | token 没 `workflow` 权限 | `gh pr merge` 步骤切到 PAT (`secrets.MYTOKEN`) |
| `gh pr merge --auto` 返回 422 | 仓库 `allow_auto_merge = false` | `gh api -X PATCH ... -f allow_auto_merge=true` |
| 一堆 PR 卡在 `BEHIND` | 多 PR 抢着合并,`strict: true` 强制 rebase 后才能合 | 批量 `@dependabot rebase`,或把 `strict` 改成 `false` |
| `auto-merge.yml` 步骤 success 但 PR 没合 | 上面六条之一 | 看 step 结论:`skipped` 通常是 `allow_auto_merge`,`failure` 通常是 token |

---

## 四、修改 workflow 后的旧 PR 处理

workflow 文件改了之后,**已经开着的 dependabot PR 不会自动跑新逻辑**,因为它们没有新 commit。
要让它们走新管线,有两种方法:

1. **批量 rebase**(force-push,会短暂重置 auto-merge):
   ```bash
   gh pr list --state open --json number,author \
     --jq '.[] | select(.author.login == "app/dependabot" or .author.login == "dependabot[bot]") | .number' | \
     xargs -I {} gh pr comment {} --body "@dependabot rebase"
   ```
2. **close + reopen**(不动分支内容,只重发 `pull_request` 事件):
   ```bash
   gh pr close 345 --delete-branch=false
   gh pr reopen 345
   ```

方法 2 适合"只想触发 workflow、不想动分支内容"的场景。本仓库处理 PR #345
(checkstyle maven-major,按策略**不**自动合并)时,两种方法效果一样 —— 反正
最终都不会被合并,只是同步到最新 master。

---

## 五、验证清单(改完必跑)

不要相信"看起来对"。改完后**至少**核对这 5 件事:

1. `gh api repos/<owner>/<repo> --jq '.allow_auto_merge'` 返回 `true`
2. 打开一个 dependabot PR,Checks 页能看到 `build (ubuntu-latest, 17, false)` 这类矩阵维度的 check
3. 用 GraphQL 查一次 `isRequired: true`,确认 branch protection 真的把这些 check 标为必需
4. 让 dependabot 跑一次(等到下周一,或手动 `@dependabot rebase`),观察:
   - patch/minor PR 在 CI 绿后 ~30 秒自动合并
   - actions major PR 同上
   - maven major PR **不**自动合并,留在那等人工
5. Actions tab 里一次 PR 的 CI 跑数 == 矩阵大小(本仓库是 9),**不是** 18

---

## 六、常用命令速查

```bash
# 仓库级 auto-merge 开关
gh api repos/XenoAmess/docker-image-rebecca --jq '.allow_auto_merge'
gh api -X PATCH repos/XenoAmess/docker-image-rebecca -f allow_auto_merge=true

# 必需 CI 名查询(找 isRequired)
gh api graphql -F query='
query {
  repository(owner: "XenoAmess", name: "docker-image-rebecca") {
    pullRequest(number: 349) {
      statusCheckRollup {
        contexts(first: 20) {
          nodes {
            ... on CheckRun { name isRequired(pullRequestNumber: 349) }
          }
        }
      }
    }
  }
}'

# 批量 rebase 所有 dependabot PR
gh pr list --state open --json number,author \
  --jq '.[] | select(.author.login == "app/dependabot" or .author.login == "dependabot[bot]") | .number' \
  | xargs -I {} gh pr comment {} --body "@dependabot rebase"

# 看最近一次 auto-merge 跑的具体步骤
gh run list --workflow=auto-merge.yml --limit 1 --json databaseId --jq '.[0].databaseId' \
  | xargs -I {} gh api repos/XenoAmess/docker-image-rebecca/actions/runs/{}/jobs \
  | python3 -c "import sys,json;[print(s['name'], s['conclusion']) for j in json.load(sys.stdin)['jobs'] for s in j['steps']]"
```

---

## 七、本仓库自动合并策略(留档)

| update-type        | ecosystem github-actions | ecosystem maven |
|--------------------|:------------------------:|:---------------:|
| semver-patch       | ✅ 自动合并              | ✅ 自动合并     |
| semver-minor       | ✅ 自动合并              | ✅ 自动合并     |
| semver-major       | ✅ 自动合并              | ❌ 人工审核     |
| digest / indirect  | ❌ 人工审核              | ❌ 人工审核     |

如果将来要调整,改 `.github/workflows/auto-merge.yml` 里 `Check if auto-merge is applicable`
那一步的 if 链即可。
