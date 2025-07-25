package org.example.vibelist.domain.post.like.pool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 좋아요/취소 이벤트 (Redis 버퍼용)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeEvent implements Serializable {
    private Long postId;
    private Long userId;
    private Action action; // LIKE or UNLIKE
    private long timestamp;

    public enum Action {
        LIKE, UNLIKE
    }
} 