package org.example.vibelist.domain.playlist.redis.pool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendPoolService {

    private final RedisTemplate<String, Object> redisTemplate;

    // pool 저장 (TTL: 캐싱 시간)
    public void savePool(String key, List<TrackRsDto> pool, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, pool, ttl, unit);
        log.info("🆕 Pool 저장: key={}, size={}, TTL={} {}", key, pool.size(), ttl, unit);

    }

    // pool 조회
    @SuppressWarnings("unchecked")
    public List<TrackRsDto> getPool(String key) {
        List<TrackRsDto> pool = (List<TrackRsDto>) redisTemplate.opsForValue().get(key);
        if (pool != null && !pool.isEmpty()) {
            log.info("🚀 Pool HIT: key={}, size={}", key, pool.size());
        } else {
            log.info("❌ Pool MISS: key={}", key);
        }
        return pool;
//        return null; // es 직접 검색과 비교용
    }

    // pool 삭제
    public void deletePool(String key) {
        redisTemplate.delete(key);
    }
}
