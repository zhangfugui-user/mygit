package com.zc.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * 1、向 redis-list "task.wait" 中push一个值（IP/MAC等都可以）
 * 2、从redis-list "dhcp.ready" 中pop一个值
 * 建议使用 BRPOP ， 会被阻塞，直到有适当的值返回
 * 返回值是一个字符串形式的MAC地址，类似 "C62B0A351778"
 * 3、根据这个MAC地址构造一个HASH键， "dhcp.info.C62B0A3516CF"
 * 获取这个键下面的所有信息，重点是 IP 信息， field 名称为 "clientip"
 * 4、向 redis-list "dhcp.inuse" 中push这个 MAC地址
 */

@ExtendWith(SpringExtension.class) //导入spring测试框架[2]
@SpringBootTest  //提供spring依赖注入
@DisplayName("Test saveOfflineState")
class RedisUtilGetClientIPTest {

    @Autowired
    private RedisConnectionFactory factory;

    @Autowired
    private RedisUtil redisUtil;



    @AfterEach
    void tearDown() {

        var template = new StringRedisTemplate(factory);
        List<String> range = template.boundListOps("task.wait").range(0, -1);
        System.out.println("task.wait: " + range);

        String clientip = (String) template.boundHashOps("dhcp.info.C62B0A3516CF").get("clientip");
        System.out.println("clientip:" + clientip);

        List<String> range1 = template.boundListOps("dhcp.inuse").range(0, -1);
        System.out.println("dhcp.inuse: " + range1);

    }

    @Test
    void getClientIP() {

        var template = new StringRedisTemplate(factory);

        template.boundListOps("dhcp.ready").leftPush("C62B0A35177");

        template.boundHashOps("dhcp.info.C62B0A351778").put("clientip", "127");

        String clientIP = redisUtil.getClientIP();
        System.out.println("clientIP: " + clientIP);

    }
}