package com.houx.reids.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @Author: HouX
 * @Date: 2020/12/2
 * @Description:
 */
public class RedisMessageListener implements MessageListener {

    private RedisTemplate redisTemplate;

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        //获取消息
        byte[] body = message.getBody();
        //使用值序列转化器转换
        String msgBody = (String) getRedisTemplate().getValueSerializer().deserialize(body);
        System.err.println(msgBody);
        //获取Channel
        byte[] channel = message.getChannel();
        //使用字符序列化器转换
        String channelStr = (String) getRedisTemplate().getStringSerializer().deserialize(channel);
        System.err.println(channelStr);
        //渠道名称转换
        String byteStr = new String(bytes);
        System.err.println(byteStr);
    }
}
