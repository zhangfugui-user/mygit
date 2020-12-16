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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ## 代拨服务标记一个IP下线
 * <p>
 * 接口形式
 * <p>
 * * bool saveOfflineState(String clientMAC);
 * <p>
 * 关键过程
 * <p>
 * * 1、向 redis-list "dhcp.offline" 中push这个 MAC地址
 * * 2、根据这个MAC地址构造一个HASH键， "dhcp.info.C62B0A3516CF"
 * * 向这个key删除一个 IP 信息， field 名称为 "internalip"
 * * 向这个key保存一个 state（offline）信息， field 名称为 "state"，值为 "5"
 */

@ExtendWith(SpringExtension.class) //导入spring测试框架[2]
@SpringBootTest  //提供spring依赖注入
@DisplayName("Test saveOfflineState")
class RedisUtilSaveOfflineStateTest {

    @Autowired
    private RedisConnectionFactory factory;

    @Autowired
    private RedisUtil redisUtil;

    String clientMAC = "C62B0A3516CF";

    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {

        var template = new StringRedisTemplate(factory);
        List<String> range = template.boundListOps("dhcp.offline").range(0, -1);
        System.out.println("dhcp.offline: " + range);
        String internalip = (String)template.boundHashOps("dhcp.info.C62B0A3516CF").get("internalip");
        System.out.println("internalip: " + internalip);
        String state = (String) template.boundHashOps("dhcp.info.C62B0A3516CF").get("state");
        System.out.println("state: " + state);

    }

    @Test
    void saveOfflineState() {

        var template = new StringRedisTemplate(factory);
        template.boundHashOps("dhcp.info.C62B0A3516CF").put("internalip","127.0.0.3");

        boolean b = redisUtil.saveOfflineState(clientMAC);
        System.out.println(b);

    }
}