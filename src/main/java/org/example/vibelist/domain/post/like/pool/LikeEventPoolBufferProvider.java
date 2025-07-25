package org.example.vibelist.domain.post.like.pool;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.global.redis.RedisUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 파티션별 좋아요 이벤트 버퍼 Provider (Redis)
 */
@Component
@RequiredArgsConstructor
public class LikeEventPoolBufferProvider {
    private static final int PARTITION_COUNT = 10;
    private static final String BUFFER_KEY_PREFIX = "like:buffer:";

    private final RedisUtil redisUtil;

    // 파티션 계산
    public int getPartition(Long postId) {
        return (int) (postId % PARTITION_COUNT);
    }

    // 버퍼에 이벤트 추가
    public void addEvent(LikeEvent event) {
        int partition = getPartition(event.getPostId());
        String key = BUFFER_KEY_PREFIX + partition;
        List<LikeEvent> buffer = (List<LikeEvent>) redisUtil.get(key);
        if (buffer == null) buffer = new ArrayList<>();
        buffer.add(event);
        redisUtil.set(key, buffer);
    }

    // 파티션별 버퍼 조회
    @SuppressWarnings("unchecked")
    public List<LikeEvent> getEvents(int partition) {
        String key = BUFFER_KEY_PREFIX + partition;
        List<LikeEvent> buffer = (List<LikeEvent>) redisUtil.get(key);
        return buffer != null ? buffer : new ArrayList<>();
    }

    // 파티션별 버퍼 삭제
    public void clearBuffer(int partition) {
        String key = BUFFER_KEY_PREFIX + partition;
        redisUtil.delete(key);
    }

    // 전체 파티션 개수 반환
    public int getPartitionCount() {
        return PARTITION_COUNT;
    }

    // 특정 postId/userId의 최신 이벤트 조회
    public LikeEvent getLatestEventForUser(Long postId, Long userId) {
        int partition = getPartition(postId);
        List<LikeEvent> buffer = getEvents(partition);
        LikeEvent latest = null;
        for (LikeEvent e : buffer) {
            if (e.getPostId().equals(postId) && e.getUserId().equals(userId)) {
                if (latest == null || e.getTimestamp() > latest.getTimestamp()) {
                    latest = e;
                }
            }
        }
        return latest;
    }
} 