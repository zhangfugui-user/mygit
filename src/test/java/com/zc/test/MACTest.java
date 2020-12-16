package com.zc.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MACTest {

    @Test
    public void test01() {
        String s = "C62B0A3516CF";
        if(s.length() == 12) {
            System.out.printf("长度为12位");
        }
    }

}
