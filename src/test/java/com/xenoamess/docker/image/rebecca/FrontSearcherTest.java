package com.xenoamess.docker.image.rebecca;

import com.xenoamess.docker.image.rebecca.encode.FrontSearcher;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FrontSearcherTest {

    @Test
    public void test() {
        Map<String, Integer> hashToCount = FrontSearcher.frontSearch(
                "src/test/resources/0.tar"
        );
        System.out.println(hashToCount);
        Set<String> set = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : hashToCount.entrySet()) {
            String[] arr = entry.getKey().split("\\.");
            if (arr.length == 1) {
                continue;
            }
            set.add(arr[arr.length - 1]);
        }
        for (String string : set) {
            System.out.println(string);
        }
    }

}