package org.example;

import org.example.新老系统.Step1;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class Step1Test {
    @Resource
    private Step1 step1;
    @Test
    void test1() {
        step1.find();;
    }
}
