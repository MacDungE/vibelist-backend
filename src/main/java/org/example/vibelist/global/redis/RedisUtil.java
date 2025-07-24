package org.example.vibelist.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
/**
 * Redis 유틸리티 클래스
 * - Redis에 값 저장, 조회, 삭제, 카운터 증가 등의 기능을 제공합니다.
 * - 필요하면 사용하고, 각 도메인별 사용성 떨어지면 삭제하면 됩니다.
 */
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis에 값 저장 - TTL 설정 가능
    public void set(String key, Object value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    // Redis에 값 저장 - TTL 미지정
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // Redis에서 값 조회
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Redis에서 값 삭제
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // 카운터 증가
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    // TTL 설정
    public Boolean expire(String key, long ttl, TimeUnit unit) {
        return redisTemplate.expire(key, ttl, unit);
    }
}
