package com.zc.test;

import com.zc.controller.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
public class RedisTest {

    // redis 模板
    @Autowired
    private RedisUtil redisUtil;

    @Test
    public void redisTest1() {

        System.out.println(redisUtil.getClientIP());

    }

    @Test
    public void redisTest2() {

       redisUtil.saveOnlineState("C62B0A351778", "127.0.0.1");

    }

    @Test
    public void redisTest3() {

        redisUtil.saveOfflineState("C62B0A3516CF");

    }

}
