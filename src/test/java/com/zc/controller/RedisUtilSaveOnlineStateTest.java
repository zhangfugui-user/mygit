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
    ## 代拨服务标记一个IP上线

    接口形式

    * bool saveOnlineState(String clientMAC, String internalip);

    关键过程

    * 1、向 redis-list "dhcp.online" 中push这个 MAC地址
    * 2、根据这个MAC地址构造一个HASH键， "dhcp.info.C62B0A3516CF"
      * 向这个key保存一个 IP 信息， field 名称为 "internalip"
      * 向这个key保存一个 state（online）信息， field 名称为 "state"，值为 "4"
     */

@ExtendWith(SpringExtension.class) //导入spring测试框架[2]
@SpringBootTest  //提供spring依赖注入
@DisplayName("Test saveOnlineState")
class RedisUtilSaveOnlineStateTest {

    @Autowired
    private RedisConnectionFactory factory;

    @Autowired
    private RedisUtil redisUtil;

    String clientMAC = "C62B0A3516CF";
    String internalip = "12345";

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {

        var template = new StringRedisTemplate(factory);
        List<String> range = template.boundListOps("dhcp.online").range(0, -1);
        System.out.println("dhcp.online: " + range);
        String ip = (String)template.boundHashOps("dhcp.info.C62B0A3516CF").get("internalip");
        System.out.println("ip: " + ip);
        String state = (String)template.boundHashOps("dhcp.info.C62B0A3516CF").get("state");
        System.out.println("state: " + state);

    }

    @Test
    void saveOnlineState() {

        boolean b = redisUtil.saveOnlineState(clientMAC, internalip);
        System.out.println("执行结果： " + b);

    }
}