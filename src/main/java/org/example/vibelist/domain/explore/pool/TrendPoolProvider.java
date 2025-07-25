package org.example.vibelist.domain.explore.pool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.explore.dto.TrendResponse;
import org.example.vibelist.global.redis.RedisUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrendPoolProvider {
    private final RedisUtil redisUtil;
    private static final String DEFAULT_POOL_KEY = "trend:pool";
    private static final long DEFAULT_TTL = 60 * 65; // 1시간 5분

    // Pool 저장 (기본키)
    public void savePool(List<TrendResponse> pool) {
        savePool(DEFAULT_POOL_KEY, pool, DEFAULT_TTL, TimeUnit.SECONDS);
    }

    // Pool 저장 (커스텀 키/TTL)
    public void savePool(String key, List<TrendResponse> pool, long ttl, TimeUnit unit) {
        redisUtil.set(key, pool, ttl, unit);
    }

    // Pool 조회 (기본키)
    @SuppressWarnings("unchecked")
    public List<TrendResponse> getPool() {
        return (List<TrendResponse>) redisUtil.get(DEFAULT_POOL_KEY);
    }

    // Pool 조회 (커스텀 키)
    @SuppressWarnings("unchecked")
    public List<TrendResponse> getPool(String key) {
        return (List<TrendResponse>) redisUtil.get(key);
    }

    // Pool 삭제
    public void deletePool() {
        redisUtil.delete(DEFAULT_POOL_KEY);
    }
    public void deletePool(String key) {
        redisUtil.delete(key);
    }

    // Pool 업데이트 (있으면 덮어씀)
    public void updatePool(List<TrendResponse> pool) {
        savePool(pool);
    }
    public void updatePool(String key, List<TrendResponse> pool, long ttl, TimeUnit unit) {
        savePool(key, pool, ttl, unit);
    }
}
