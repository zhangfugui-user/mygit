package com.zc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Component
public class RedisUtil {

    private final Logger logger = LoggerFactory.getLogger(RedisUtil.class);

    // 在 getClientIP() 方法中的第一步 向 redis-list "task.wait" 中push一个值（IP/MAC等都可以）
    // 此为 PUSH 进去的任意的 IP 值
    private final String ip = "127.0.0.6";

    // 在 saveOnlineState() 方法中需要向按指定方式拼接的MAC地址构造的Hahs键中保存一个 state（online）信息， field 名称为 "state"，值为 "4"
    private final String state1 = "4";

    // 在 saveOfflineState() 方法中方法中需要向按指定方式拼接的MAC地址构造的Hahs键中保存一个 state（offline）信息， field 名称为 "state"，值为 "5"
    private final String state2 = "5";

    // 判断一个地址是否是 mac 的正则
    private final String trueMacAddress = "^[A-Fa-f0-9]{12}$";

    // 判断一个地址是否是 IP 的正则
    private final String trueIP = "^((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}$";

    @Autowired
    private RedisConnectionFactory factory;

    /*
     * 1、向 redis-list "task.wait" 中push一个值（IP/MAC等都可以）
     * 2、从redis-list "dhcp.ready" 中pop一个值
     * 建议使用 BRPOP ， 会被阻塞，直到有适当的值返回
     * 返回值是一个字符串形式的MAC地址，类似 "C62B0A351778"
     * 3、根据这个MAC地址构造一个HASH键， "dhcp.info.C62B0A3516CF"
     * 获取这个键下面的所有信息，重点是 IP 信息， field 名称为 "clientip"
     * 4、向 redis-list "dhcp.inuse" 中push这个 MAC地址
     */
    public String getClientIP() {
        var template = new StringRedisTemplate(factory);

        template.boundListOps("task.wait").leftPush(ip);

        //list集合 第一个元素为key值，第二个元素为弹出的元素值;当超时返回[null]
        List<Object> result = template.executePipelined(new RedisCallback<Object>() {
            // 表示定义字段可以为空
            @Nullable
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                //队列没有元素会阻塞操作，直到队列获取新的元素或超时
                //等待时间为0的话一直阻塞等待，知道有合适的值出现
                return connection.bLPop(2, "dhcp.ready".getBytes());
            }
        }, new StringRedisSerializer());

        if (result.get(0) != null) {
            ArrayList infoList = (ArrayList) result.get(0);
            String mac = (String) infoList.get(1);
            if(mac.matches(trueMacAddress)) {
                String hashKey = "dhcp.info." + mac;
                String clientip = (String) template.boundHashOps(hashKey).get("clientip");
                if(clientip.matches(trueIP)) {
                    template.boundListOps("dhcp.inuse").leftPush((String) (infoList.get(1)));
                    return clientip;
                }else {
                    logger.info("getClientIP() 方法中拿到的IP可能不是一个正确的ip值");
                    return null;
                }
            }else {
                logger.info("getClientIP() 方法从 dhcp.ready 键中pop到的值不是一个正确的mac地址");
                return null;
            }
        } else {
            logger.info("getClientIP() 方法未能从 dhcp.ready 键中pop到值，");
            return null;
        }

    }

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
    public boolean saveOnlineState(String clientMAC, String internalip) {

        if(clientMAC.length() == 12 && clientMAC.matches(trueMacAddress)) {
            var template = new StringRedisTemplate(factory);
            template.boundListOps("dhcp.online").leftPush(clientMAC);
            String hashKey = "dhcp.info." + clientMAC;
            if(internalip.matches(trueIP)) {
                template.boundHashOps(hashKey).put("internalip",internalip);
                template.boundHashOps(hashKey).put("state",state1);
                return true;
            }else {
                logger.info("mac地址为 " + clientMAC + " 的saveOnlineState()方法ip参数可能不是一个对的ip");
                return false;
            }
        }else {
            logger.info("该mac地址 "+ clientMAC +" 的长度可能存在问题，不是一个mac地址");
            return false;
        }
    }

    /**
     * ## 代拨服务标记一个IP下线
     *
     * 接口形式
     *
     * * bool saveOfflineState(String clientMAC);
     *
     * 关键过程
     *
     * * 1、向 redis-list "dhcp.offline" 中push这个 MAC地址
     * * 2、根据这个MAC地址构造一个HASH键， "dhcp.info.C62B0A3516CF"
     *   * 向这个key删除一个 IP 信息， field 名称为 "internalip"
     *   * 向这个key保存一个 state（offline）信息， field 名称为 "state"，值为 "5"
     */
    public boolean saveOfflineState(String clientMAC) {

        if(clientMAC.length() == 12 && clientMAC.matches(trueMacAddress)) {
            var template = new StringRedisTemplate(factory);
            template.boundListOps("dhcp.offline").leftPush(clientMAC);
            String hashKey = "dhcp.info." + clientMAC;
            Long delete = template.boundHashOps(hashKey).delete("internalip");
            if(delete == 1) {
                // 删除成功
                template.boundHashOps(hashKey).put("state", state2);
                return true;
            }else {
                logger.info("mac为 " + clientMAC +" 在执行saveOfflineState()方法时 未成功删除internalip或不存在hashKey键值internalip");
                return false;
            }
        }else {
            logger.info("该mac " + clientMAC + " 可能不是一个正确的mac地址 saveOfflineStateh() 方法执行失败");
            return false;
        }

    }

}
