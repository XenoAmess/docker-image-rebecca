package com.xenoamess.docker.image.rebecca;

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
//        System.out.println(MAP.entrySet().stream().max(
//                new Comparator<Map.Entry<String, Integer>>() {
//                    @Override
//                    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
//                        return o1.getValue() - o2.getValue();
//                    }
//                }
//        ).get());
//        for (Map.Entry<String, Integer> entry : MAP.entrySet()) {
//            if (entry.getValue() == 1) {
//                MAP.remove(entry.getKey());
//            }
//        }
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