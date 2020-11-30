package com.houx.main;

import javafx.application.Application;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @Author: HouX
 * @Date: 2020/11/30
 * @Description:
 */
public class Main {
    public static void main(String[] args) {
        //testString();
        testCal();
    }

    public static void testString(){
        String file = "applicationContext.xml";
        ApplicationContext context = new ClassPathXmlApplicationContext(file);
        RedisTemplate redisTemplate = context.getBean(RedisTemplate.class);
        //赋值
        redisTemplate.opsForValue().set("key1","value1");
        redisTemplate.opsForValue().set("key2","value2");
        //通过key获取值
        String value1 = (String) redisTemplate.opsForValue().get("key1");
        System.out.println(value1);
        //通过key删除值
        redisTemplate.delete("key1");
        //求长度
        Long length = redisTemplate.opsForValue().size("key2");
        System.out.println(length);
        //设置新值并返回旧值
        String oldValue2 = (String)redisTemplate.opsForValue().getAndSet("key2","new_value2");
        System.out.println(oldValue2);
        //通过key获取值
        String value2 = (String) redisTemplate.opsForValue().get("key2");
        System.out.println(value2);
        //求子串
        String  rangeValue2 = redisTemplate.opsForValue().get("key2",0,3);
        System.out.println(rangeValue2);
        //追加字符串到末尾，返回新串长度
        int newLen = redisTemplate.opsForValue().append("key2","_app");
        System.out.println(newLen);
        String appendValue2 = (String) redisTemplate.opsForValue().get("key2");
        System.out.println(appendValue2);


    }

    //测试Redis运算
    public static void testCal(){
        String file = "applicationContext.xml";
        ApplicationContext context = new ClassPathXmlApplicationContext(file);
        RedisTemplate redisTemplate = context.getBean(RedisTemplate.class);
        redisTemplate.opsForValue().set("1","9");
        printCurrValue(redisTemplate,"i");
        redisTemplate.opsForValue().increment("i",1);
        printCurrValue(redisTemplate,"i");
        redisTemplate.getConnectionFactory().getConnection().decr(redisTemplate.getKeySerializer().serialize("i"));
        printCurrValue(redisTemplate,"i");
        redisTemplate.getConnectionFactory().getConnection().decrBy(redisTemplate.getKeySerializer().serialize("1"),6);
        printCurrValue(redisTemplate,"i");
        redisTemplate.opsForValue().increment("i",2.3);
        printCurrValue(redisTemplate,"i");
    }

    /**
     * 打印当前的值
     * @param redisTemplate
     * @param key
     */
    private static void printCurrValue(RedisTemplate redisTemplate, String key) {
        String i = (String) redisTemplate.opsForValue().get(key);
        System.out.println(i);
    }


}
