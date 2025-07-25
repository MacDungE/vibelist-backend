package org.example.vibelist.domain.post.like.pool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.like.entity.PostLike;
import org.example.vibelist.domain.post.like.repository.PostLikeRepository;
import org.example.vibelist.domain.post.repository.PostRepository;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeEventPoolBatchScheduler {
    private final LikeEventPoolBufferProvider bufferProvider;
    private final PostLikeRepository postLikeRepo;
    private final PostRepository postRepo;
    private final UserRepository userRepo;

    /**
     * 3분마다 파티션별로 좋아요 이벤트를 일괄 DB 반영
     */
    @Scheduled(cron = "0 */3 * * * ?")
    @Transactional
    public void batchApplyLikeEvents() {
        int partitionCount = bufferProvider.getPartitionCount();
        for (int partition = 0; partition < partitionCount; partition++) {
            List<LikeEvent> events = bufferProvider.getEvents(partition);
            if (events.isEmpty()) continue;
            log.info("[LikeBatch] Partition {}: {} events to process", partition, events.size());
            // postId-userId별 최신 이벤트만 남김
            Map<String, LikeEvent> latest = new HashMap<>();
            for (LikeEvent e : events) {
                String key = e.getPostId() + ":" + e.getUserId();
                LikeEvent prev = latest.get(key);
                if (prev == null || e.getTimestamp() > prev.getTimestamp()) {
                    latest.put(key, e);
                }
            }
            // 최신 이벤트만 DB에 반영
            for (LikeEvent e : latest.values()) {
                try {
                    if (e.getAction() == LikeEvent.Action.LIKE) {
                        if (!postLikeRepo.existsByPostIdAndUserId(e.getPostId(), e.getUserId())) {
                            Post post = postRepo.findById(e.getPostId()).orElse(null);
                            if (post != null) {
                                PostLike like = PostLike.create(userRepo.getReferenceById(e.getUserId()), post);
                                postLikeRepo.save(like);
                                post.incLike();
                            }
                        }
                    } else if (e.getAction() == LikeEvent.Action.UNLIKE) {
                        if (postLikeRepo.existsByPostIdAndUserId(e.getPostId(), e.getUserId())) {
                            postLikeRepo.deleteByPostIdAndUserId(e.getPostId(), e.getUserId());
                            postRepo.findById(e.getPostId()).ifPresent(Post::decLike);
                        }
                    }
                } catch (Exception ex) {
                    log.error("[LikeBatch] Failed to apply event: {}", e, ex);
                }
            }
            // 처리 후 버퍼 삭제
            bufferProvider.clearBuffer(partition);
        }
    }
} 