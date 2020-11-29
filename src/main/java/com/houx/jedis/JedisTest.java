package com.houx.jedis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @Author: HouX
 * @Date: 2020/11/29
 * @Description:
 */
public class JedisTest {

    public void testJedis() {
        Jedis jedis = testPool().getResource();
        int i = 0;// 记录操作次数
        try {
            long start = System.currentTimeMillis();// 开始毫秒数
            while (true) {
                long end = System.currentTimeMillis();
                if (end - start >= 1000) {// 当大于等于1000毫秒（相当于1秒）时，结束操作
                    break;
                }
                i++;
                jedis.set("test" + i, i + "");
            }
        } finally {// 关闭连接
            jedis.close();
        }
        System.out.println("redis每秒操作：" + i + "次");// 打印1秒内对Redis的操作次数
    }

    //使用Redis连接池
    private JedisPool testPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        //最大空闲数
        poolConfig.setMaxIdle(50);
        //最大连接数
        poolConfig.setMaxTotal(100);
        //最大等待毫秒数
        poolConfig.setMaxWaitMillis(20000);
        //使用配置创建连接池
        JedisPool pool = new JedisPool(poolConfig, "localhost");
        //从连接池中获取单个连接
        Jedis jedis = pool.getResource();
        //如果需要密码
        //jedis.auth("password");
        return pool;


    }
}
