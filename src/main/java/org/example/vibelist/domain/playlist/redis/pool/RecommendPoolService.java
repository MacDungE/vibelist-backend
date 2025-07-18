package org.example.vibelist.domain.playlist.redis.pool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendPoolService {

    private final RedisTemplate<String, Object> redisTemplate;

    // pool 저장 (TTL: 캐싱 시간)
    public void savePool(String key, Set<TrackRsDto> pool, long ttl, TimeUnit unit) {
        if (!pool.isEmpty()) {
            // Set 전체를 개별 원소로 Redis Set에 저장
            redisTemplate.opsForSet().add(key, pool.toArray());
            redisTemplate.expire(key, ttl, unit);
            log.info("🆕 Pool 저장: key={}, size={}, TTL={} {}", key, pool.size(), ttl, unit);
        }
    }

    // pool 조회
    @SuppressWarnings("unchecked")
    public List<TrackRsDto> recommendFromPool(String key, int count) {
        Set<Object> randTracks = redisTemplate.opsForSet().distinctRandomMembers(key, count);
        if (randTracks != null && !randTracks.isEmpty()) {
            log.info("🚀 Pool HIT: key={}, size={}", key, randTracks.size());
            return randTracks.stream().map(o -> (TrackRsDto) o).toList();
        } else {
            log.info("❌ Pool MISS: key={}", key);
            return  null;
        }
    }

    // pool 삭제
    public void deletePool(String key) {
        redisTemplate.delete(key);
    }
}
