package com.houx.main;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisListCommands;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands.Range;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author: HouX
 * @Date: 2020/11/30
 * @Description:
 */
public class Main {

    static String file = "applicationContext.xml";
    static ApplicationContext context = new ClassPathXmlApplicationContext(file);
    static RedisTemplate redisTemplate = context.getBean(RedisTemplate.class);

    public static void main(String[] args) {
        // testJedis();
        // testString();
        // testCal();
        // testRedisHash();
        // testList();
        // testBList();
        // testSet();
        // testZset();
        testTransaction();
    }

    public static void testJedis() {
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
    private static JedisPool testPool() {
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

    public static void testString() {

        //赋值
        redisTemplate.opsForValue().set("key1", "value1");
        redisTemplate.opsForValue().set("key2", "value2");
        //通过key获取值
        String value1 = (String) redisTemplate.opsForValue().get("key1");
        System.out.println(value1);
        //通过key删除值
        redisTemplate.delete("key1");
        //求长度
        Long length = redisTemplate.opsForValue().size("key2");
        System.out.println(length);
        //设置新值并返回旧值
        String oldValue2 = (String) redisTemplate.opsForValue().getAndSet("key2", "new_value2");
        System.out.println(oldValue2);
        //通过key获取值
        String value2 = (String) redisTemplate.opsForValue().get("key2");
        System.out.println(value2);
        //求子串
        String rangeValue2 = redisTemplate.opsForValue().get("key2", 0, 3);
        System.out.println(rangeValue2);
        //追加字符串到末尾，返回新串长度
        int newLen = redisTemplate.opsForValue().append("key2", "_app");
        System.out.println(newLen);
        String appendValue2 = (String) redisTemplate.opsForValue().get("key2");
        System.out.println(appendValue2);


    }

    //测试Redis运算
    public static void testCal() {
        redisTemplate.opsForValue().set("1", "9");
        printCurrValue(redisTemplate, "i");
        redisTemplate.opsForValue().increment("i", 1);
        printCurrValue(redisTemplate, "i");
        redisTemplate.getConnectionFactory().getConnection().decr(redisTemplate.getKeySerializer().serialize("i"));
        printCurrValue(redisTemplate, "i");
        redisTemplate.getConnectionFactory().getConnection().decrBy(redisTemplate.getKeySerializer().serialize("1"), 6);
        printCurrValue(redisTemplate, "i");
        redisTemplate.opsForValue().increment("i", 2.3);
        printCurrValue(redisTemplate, "i");
    }

    //使用Spring操作hash结构
    public static void testRedisHash() {
        String key = "hash";
        Map<String, String> map = new HashMap<String, String>();
        map.put("f1", "val1");
        map.put("f2", "val2");
        //相当于hmset命令
        redisTemplate.opsForHash().putAll(key, map);
        //相当于hset命令
        redisTemplate.opsForHash().put(key, "f3", "6");
        printValueForHash(redisTemplate, key, "f3");
        //相当于hexists key filed命令
        boolean exists = redisTemplate.opsForHash().hasKey(key, "f3");
        System.out.println(exists);
        //相当于hgetall命令
        Map keyValueMap = redisTemplate.opsForHash().entries(key);
        //相当于hincrby命令
        redisTemplate.opsForHash().increment(key, "f3", 2);
        printValueForHash(redisTemplate, key, "f3");
        //相当于hincrbyfloat命令
        redisTemplate.opsForHash().increment(key, "f3", 0.88);
        printValueForHash(redisTemplate, key, "f3");
        //相当于hvals命令
        List valueList = redisTemplate.opsForHash().values(key);
        //相当于hkeys命令
        Set keyList = redisTemplate.opsForHash().keys(key);
        List<String> fieldList = new ArrayList<String>();
        fieldList.add("f1");
        fieldList.add("f2");
        //相当于hmget命令
        List valueList2 = redisTemplate.opsForHash().multiGet(key, keyList);
        //相当于hsetnx命令
        boolean success = redisTemplate.opsForHash().putIfAbsent(key, "f4", "val4");
        System.out.println(success);
        //相当于hdel命令
        Long result = redisTemplate.opsForHash().delete(key, "f1", "f2");
        System.out.println(result);
    }

    //操作链表list
    public static void testList() {
        try {
            //删除链表，可以反复测试
            redisTemplate.delete("list");
            //把node3插入链表list
            redisTemplate.opsForList().leftPush("list", "node3");
            List<String> nodeList = new ArrayList<String>();
            for (int i = 2; i >= 1; i--) {
                nodeList.add("node" + i);
            }
            //相当于lpush把多个价值从左插入链表
            redisTemplate.opsForList().leftPushAll("list", nodeList);
            //从右边插入一个节点
            redisTemplate.opsForList().rightPush("list", "node4");
            //获取下标为0的节点
            String node1 = (String) redisTemplate.opsForList().index("list", 0);
            //获取链表长度
            long size = redisTemplate.opsForList().size("list");
            //从左边弹出一个节点
            String lpop = (String) redisTemplate.opsForList().leftPop("list");
            //从右边弹出一个节点
            String rpop = (String) redisTemplate.opsForList().rightPop("list");
            //注意，需要使用更为底层的命令才能操作linsert命令
            //使用linsert命令在node2前插入一个节点
            redisTemplate.getConnectionFactory().getConnection().lInsert("list".getBytes("utf-8"),
                    RedisListCommands.Position.AFTER, "node2".getBytes("utf-8"),
                    "after_node".getBytes("utf-8"));
            //判断list是否存在,如果存在则从左边插入head节点
            redisTemplate.opsForList().leftPushIfPresent("list", "head");
            //判断list是否存在,如果存在则从右边插入head节点
            redisTemplate.opsForList().rightPushIfPresent("list", "end");
            //从左到右，或者下标从0到10的节点元素
            List valueList = redisTemplate.opsForList().range("list", 0, 10);
            nodeList.clear();
            for (int i = 1; i <= 3; i++) {
                nodeList.add("node");
            }
            //在链表左边插入三个值为node的节点
            redisTemplate.opsForList().leftPushAll("list", nodeList);
            //从左到右删除至多三个node节点
            redisTemplate.opsForList().remove("list", 3, "node");
            //给链表下标为0的节点设置新值
            redisTemplate.opsForList().set("list", 0, "new_head_value");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        printList(redisTemplate, "list");
    }

    //Spring对Redis阻塞命令的操作
    public static void testBList() {
        //清空数据,可以重复测试
        redisTemplate.delete("list1");
        redisTemplate.delete("list2");
        //初始化链表list1
        List<String> nodeList = new ArrayList<String>();
        for (int i = 1; i <= 5; i++) {
            nodeList.add("node" + i);
        }
        redisTemplate.opsForList().leftPushAll("list1", nodeList);
        //Spring使用参数超时时间作为阻塞命令区分，等价于blpop命令，并且可以设置时间参数
        redisTemplate.opsForList().leftPop("list1", 1, TimeUnit.SECONDS);
        //Spring使用参数超时时间作为阻塞命令区分，等价于brpop命令，并且可以设置时间参数
        redisTemplate.opsForList().rightPop("list1", 1, TimeUnit.SECONDS);
        nodeList.clear();
        //初始化链表list2
        for (int i = 1; i <= 3; i++) {
            nodeList.add("data" + i);
        }
        redisTemplate.opsForList().leftPushAll("list2", nodeList);
        //相当于rpoplpush命令，弹出list1最右边的节点，插入到list2最左边
        redisTemplate.opsForList().rightPopAndLeftPush("list1", "list2");
        //相当于brpoplpush命令，注意在Spring中使用超时参数区分
        redisTemplate.opsForList().rightPopAndLeftPush("list1", "list2", 1, TimeUnit.SECONDS);
        //打印
        printList(redisTemplate, "list1");
        printList(redisTemplate, "list2");
    }

    public static void testSet() {
        Set set = null;
        //将元素加入列表
        redisTemplate.boundSetOps("set1").add("v1", "v2", "v3", "v4", "v5", "v6");
        redisTemplate.boundSetOps("set2").add("v0", "v2", "v4", "v6", "v8");
        //求集合长度
        redisTemplate.opsForSet().size("set1");
        //求差集
        set = redisTemplate.opsForSet().difference("set1", "set2");
        //求并集
        set = redisTemplate.opsForSet().intersect("set1", "set2");
        //判断是否集合中的元素
        boolean exists = redisTemplate.opsForSet().isMember("set1", "v1");
        //获取集合所有元素
        set = redisTemplate.opsForSet().members("set1");
        //从集合中随机弹出一个元素
        String val = (String) redisTemplate.opsForSet().pop("set1");
        //随机获取一个集合的元素
        val = (String) redisTemplate.opsForSet().randomMember("set1");
        //随机获取2个集合的元素
        List list = redisTemplate.opsForSet().randomMembers("set1", 2L);
        //删除一个集合的元素，参数可以是多个
        redisTemplate.opsForSet().remove("set1", "v1");
        //求两个集合的并集
        redisTemplate.opsForSet().union("set1", "set2");
        //求两个集合的差集，并保存到集合diff_set中
        redisTemplate.opsForSet().differenceAndStore("set1", "set2", "diff_set");
        //求两个集合的交集，并保存到集合inter_set中
        redisTemplate.opsForSet().intersectAndStore("set1", "set2", "inter_set");
        //求两个集合的并集，并保存到集合union_set中
        redisTemplate.opsForSet().unionAndStore("set1", "set2", "union_set");
    }

    public static void testZset() {
        //Spring提供接口TypedTuple操作有序集合
        Set<TypedTuple> set1 = new HashSet<TypedTuple>();
        Set<TypedTuple> set2 = new HashSet<TypedTuple>();
        int j = 9;
        for (int i = 1; i <= 9; i++) {
            j--;
            //计算分数和值
            Double score1 = Double.valueOf(i);
            String value1 = "x" + i;
            Double score2 = Double.valueOf(j);
            String value2 = j % 2 == 1 ? "y" + j : "x" + j;
            //使用Spring提供的默认TypeTuple————DefaultTypedTuple
            TypedTuple typedTuple1 = new DefaultTypedTuple(value1, score1);
            set1.add(typedTuple1);
            TypedTuple typedTuple2 = new DefaultTypedTuple(value2, score2);
            set2.add(typedTuple2);
        }
        //将元素插入有序集合zset1
        redisTemplate.opsForZSet().add("zset1", set1);
        redisTemplate.opsForZSet().add("zset2", set2);
        //统计总数
        Long size = null;
        size = redisTemplate.opsForZSet().zCard("set1");
        // 计分数为score，那么下面的方法就是求3<=score<=6的元素
        size = redisTemplate.opsForZSet().count("zset1", 3, 6);
        Set set = null;
        // 从下标一开始截取5个元素，但是不返回分数,每一个元素是String
        set = redisTemplate.opsForZSet().range("zset1", 1, 5);
        printSet(set);
        // 截取集合所有元素，并且对集合按分数排序，并返回分数,每一个元素是TypedTuple
        set = redisTemplate.opsForZSet().rangeWithScores("zset1", 0, -1);
        printTypedTuple(set);
        // 将zset1和zset2两个集合的交集放入集合inter_zset
        size = redisTemplate.opsForZSet().intersectAndStore("zset1", "zset2", "inter_zset");
        // 区间
        Range range = Range.range();
        range.lt("x8");// 小于
        range.gt("x1");// 大于
        set = redisTemplate.opsForZSet().rangeByLex("zset1", range);
        printSet(set);
        range.lte("x8");// 小于等于
        range.gte("x1");// 大于等于
        set = redisTemplate.opsForZSet().rangeByLex("zset1", range);
        printSet(set);
        // 限制返回个数
        Limit limit = Limit.limit();
        // 限制返回个数
        limit.count(4);
        // 限制从第五个开始截取
        limit.offset(5);
        // 求区间内的元素，并限制返回4条
        set = redisTemplate.opsForZSet().rangeByLex("zset1", range, limit);
        printSet(set);
        // 求排行，排名第1返回0，第2返回1
        Long rank = redisTemplate.opsForZSet().rank("zset1", "x4");
        System.err.println("rank = " + rank);
        // 删除元素，返回删除个数
        size = redisTemplate.opsForZSet().remove("zset1", "x5", "x6");
        System.err.println("delete = " + size);
        // 按照排行删除从0开始算起，这里将删除排名第2和第3的元素
        size = redisTemplate.opsForZSet().removeRange("zset2", 1, 2);
        // 获取所有集合的元素和分数，以-1代表全部元素
        set = redisTemplate.opsForZSet().rangeWithScores("zset2", 0, -1);
        printTypedTuple(set);
        // 删除指定的元素
        size = redisTemplate.opsForZSet().remove("zset2", "y5", "y3");
        System.err.println(size);
        // 给集合中的一个元素的分数加上11
        Double dbl = redisTemplate.opsForZSet().incrementScore("zset1", "x1", 11);
        redisTemplate.opsForZSet().removeRangeByScore("zset1", 1, 2);
        set = redisTemplate.opsForZSet().reverseRangeWithScores("zset2", 1, 10);
        printTypedTuple(set);
    }

    public static void printTypedTuple(Set<TypedTuple> set) {
        if (set != null && set.isEmpty()) {
            return;
        }
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            TypedTuple val = (TypedTuple) iterator.next();
            System.err.print("{value = " + val.getValue() + ", score = " + val.getScore() + "}\n");
        }
    }

    public static void printSet(Set set) {
        if (set != null && set.isEmpty()) {
            return;
        }
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Object val = iterator.next();
            System.out.print(val + "\t");
        }
        System.out.println();
    }

    public static void printList(RedisTemplate redisTemplate, String key) {
        //链表长度
        Long size = redisTemplate.opsForList().size(key);
        //获取整个链表的值
        List valueList = redisTemplate.opsForList().range(key, 0, size);
        System.out.println(valueList);
    }

    public static void printValueForHash(RedisTemplate redisTemplate, String key, String field) {
        //相当于hget命令
        Object value = redisTemplate.opsForHash().get(key, field);
        System.out.println(value);
    }

    //打印当前的值
    private static void printCurrValue(RedisTemplate redisTemplate, String key) {
        String i = (String) redisTemplate.opsForValue().get(key);
        System.out.println(i);
    }

    //在Spring中使用Redis事务命令
    public static void testTransaction() {
        SessionCallback callBack = (SessionCallback) (RedisOperations ops) -> {
            ops.multi();
            ops.boundValueOps("key1").set("value1");
            // 注意由于命令只是进入队列，而没有被执行，所以此处采用get命令，而value却返回为null
            String value = (String) ops.boundValueOps("key1").get();
            System.out.println("事务执行过程中，命令入队列，而没有被执行，所以value为空：value=" + value);
            // 此时list会保存之前进入队列的所有命令的结果
            List list = ops.exec();// 执行事务
            // 事务结束后，获取value1
            value = (String) redisTemplate.opsForValue().get("key1");
            return value;
        };
        // 执行Redis的命令
        String value = (String) redisTemplate.execute(callBack);
        System.out.println(value);
    }



}
