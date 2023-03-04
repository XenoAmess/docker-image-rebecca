package com.xenoamess.docker.image.rebecca;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.xenoamess.docker.image.rebecca.encode.FrontSearcher;
import com.xenoamess.docker.image.rebecca.pojo.FrontSearchResultPojo;
import org.junit.jupiter.api.Test;

public class FrontSearcherTest {

    @Test
    public void test() {
        Map<String, FrontSearchResultPojo> hashToCount = FrontSearcher.frontSearch(
                "src/test/resources/0.tar"
        );
        System.out.println( hashToCount );
        Set<String> set = new LinkedHashSet<>();
        for (Map.Entry<String, FrontSearchResultPojo> entry : hashToCount.entrySet()) {
            String[] arr = entry.getKey().split( "\\." );
            if (arr.length == 1) {
                continue;
            }
            set.add( arr[arr.length - 1] );
        }
        for (String string : set) {
            System.out.println( string );
        }
    }

    @Test
    public void testNoFilter() {
        Map<String, FrontSearchResultPojo> hashToCount = FrontSearcher.frontSearch(
                "src/test/resources/0.tar"
        );
        System.out.println( hashToCount );
        for (Map.Entry<String, FrontSearchResultPojo> entry : hashToCount.entrySet()) {
            System.out.println( entry.getKey() );
            System.out.println( entry.getKey().length() );
        }
    }

}
