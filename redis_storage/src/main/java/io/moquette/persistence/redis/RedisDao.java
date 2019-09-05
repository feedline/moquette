package io.moquette.persistence.redis;

import java.time.Duration;
import java.util.Properties;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.ObjectUtils;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;

import redis.clients.jedis.JedisPoolConfig;

public class RedisDao<T> {
    private static final Logger logger = LoggerFactory.getLogger(RedisDao.class);

    protected RedisTemplate<String, T> redisTemplate = new RedisTemplate<>();

    private JedisConnectionFactory connectionFactory;

    /** 是否准备好 **/
    private boolean ready = false;

    public void connect(Properties config) {
        // 如果已经存在，则销毁
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }

        lettuce(config);

        ready = true;
    }

    private void lettuce(Properties config) {
        String host = config.getProperty(RedisConstant.HOST);
        String port = config.getProperty(RedisConstant.PORT);
        String password = config.getProperty(RedisConstant.PASSWORD);
        String database = config.getProperty(RedisConstant.DATABASE);


        /* ========= basic ========= */
        final RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(Integer.parseInt(port));
        configuration.setDatabase(Integer.parseInt(database));
        if (!ObjectUtils.isEmpty(password)) {
            final RedisPassword redisPassword = RedisPassword.of(password);
            configuration.setPassword(redisPassword);
        }

        /* ========= connection pool ========= */
        final GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxTotal(200);
        genericObjectPoolConfig.setMinIdle(500);
        genericObjectPoolConfig.setMaxIdle(1000);
        genericObjectPoolConfig.setMaxWaitMillis(500);

        /* ========= lettuce pool ========= */
        final LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        builder.poolConfig(genericObjectPoolConfig);
        builder.commandTimeout(Duration.ofSeconds(1000));
        final LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(configuration, builder.build());
        connectionFactory.afterPropertiesSet();
        logger.info("Command redis database select index: {}", configuration.getDatabase());

        createRedisTemplate(connectionFactory);

        logger.info("redis config set! ip:" + host + ", port:" + port + ", password:" + password + ", database:"
                + database);
    }

    private RedisTemplate createRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // set redis serializer
        final FastJson2JsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJson2JsonRedisSerializer<>(Object.class);
        // build redisTemplate
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        final RedisSerializer<?> stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);// key serialization
        redisTemplate.setValueSerializer(fastJsonRedisSerializer);// serialization serialization
        redisTemplate.setHashKeySerializer(fastJsonRedisSerializer);// Hash key serialization
//        redisTemplate.setHashValueSerializer(fastJsonRedisSerializer);// Hash value serialization
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }


    /**
     * 清理redis相关资源
     * 
     */
    public void close() {
        connectionFactory.destroy();
    }

    /**
     * 获取
     * 
     * @return
     */
    public RedisTemplate<String, T> getTemplate() {

        if (ready) {
            return redisTemplate;
        }

        return null;
    }

    /**
     * 获取字符串操作对象
     * 
     * @return
     */
    public <V> ValueOperations<String, V> opsForValue() {
        return (ValueOperations<String,V>)redisTemplate.opsForValue();
    }

    /**
     * 获取列表操作对象
     * 
     * @return
     */
    public ListOperations<String, T> opsForList() {
        return redisTemplate.opsForList();
    }

    /**
     * 获取hash操作对象
     * 
     * @return
     */
    public <K, V> HashOperations<String, K, V> opsForHash() {
        return redisTemplate.opsForHash();
    }

    /**
     * 获取Set操作对象
     * 
     * @return
     */
    public <V> SetOperations<String, V> opsForSet() {
        return (SetOperations<String, V>)redisTemplate.opsForSet();
    }

    /**
     * 获取ZSet操作对象
     * 
     * @return
     */
    public ZSetOperations<String, T> opsForZSet() {
        return redisTemplate.opsForZSet();
    }

}
