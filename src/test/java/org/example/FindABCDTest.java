package org.example;

import org.example.分类.FindABCD;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
@SpringBootTest
public class FindABCDTest {
    @Resource
    private FindABCD findABCD;
    @Test
    void test(){
        findABCD.doFindABDC();
    }
}
