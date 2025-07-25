package org.example.vibelist.domain.post.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostCacheProvider {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String POST_CACHE_KEY = "post:";
    private static final Duration TTL = Duration.ofMinutes(30);

    public PostDetailResponse get(Long postId) {
        try {
            String key = POST_CACHE_KEY + postId;
            Object cachedObject = redisTemplate.opsForValue().get(key);

            if (cachedObject == null) {
                return null;
            }

            // LinkedHashMap을 PostDetailResponse로 안전하게 변환
            PostDetailResponse cachedPost = objectMapper.convertValue(cachedObject, PostDetailResponse.class);

            if (cachedPost != null) {
                log.info("캐시에서 포스트 조회: postId={}", postId);
            }

            return cachedPost;
        } catch (Exception e) {
            log.error("캐시 조회 실패: postId={}, error={}", postId, e.getMessage());
            return null;
        }
    }

    public void set(Long postId, PostDetailResponse post) {
        try {
            String key = POST_CACHE_KEY + postId;
            redisTemplate.opsForValue().set(key, post, TTL);
            log.info("캐시에 포스트 저장: postId={}", postId);
        } catch (Exception e) {
            log.error("캐시 저장 실패: postId={}, error={}", postId, e.getMessage());
        }
    }

    public void delete(Long postId) {
        try {
            String key = POST_CACHE_KEY + postId;
            redisTemplate.delete(key);
            log.info("캐시에서 포스트 삭제: postId={}", postId);
        } catch (Exception e) {
            log.error("캐시 삭제 실패: postId={}, error={}", postId, e.getMessage());
        }
    }
}
