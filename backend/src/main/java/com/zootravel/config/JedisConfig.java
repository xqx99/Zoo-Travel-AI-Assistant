package com.zootravel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

/**
 * Jedis 连接（向量库 RedisEmbeddingStore 与向量库幂等检查共用）
 */
@Configuration
public class JedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(host, port);
    }
}
