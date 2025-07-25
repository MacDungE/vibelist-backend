package org.example.vibelist.domain.post.like.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.vibelist.domain.explore.service.ExploreService;
import org.example.vibelist.domain.post.comment.entity.Comment;
import org.example.vibelist.domain.post.comment.repository.CommentRepository;
import org.example.vibelist.domain.post.dto.PlaylistDetailResponse;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.entity.Playlist;

import org.example.vibelist.domain.post.like.pool.LikeEvent;
import org.example.vibelist.domain.post.like.pool.LikeEventPoolBufferProvider;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.like.entity.CommentLike;
import org.example.vibelist.domain.post.like.entity.PostLike;
import org.example.vibelist.domain.post.like.repository.CommentLikeRepository;
import org.example.vibelist.domain.post.like.repository.PostLikeRepository;
import org.example.vibelist.domain.post.repository.PostRepository;
import org.example.vibelist.domain.post.service.PostService;
import org.example.vibelist.domain.post.tag.entity.Tag;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.RsData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final PostLikeRepository postLikeRepo;
    private final CommentLikeRepository commentLikeRepo;
    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final UserRepository userRepo;
    private final ExploreService exploreService;
    private final LikeEventPoolBufferProvider likeEventPoolBufferProvider;
    /* ---------------- Post ---------------- */

    @Transactional
    public RsData<Boolean> togglePostLike(Long postId, Long userId) {
        try {
            boolean liked = postLikeRepo.existsByPostIdAndUserId(postId, userId);
            LikeEvent.Action action = liked ? LikeEvent.Action.UNLIKE : LikeEvent.Action.LIKE;
            LikeEvent event = new LikeEvent(postId, userId, action, System.currentTimeMillis());
            likeEventPoolBufferProvider.addEvent(event);
            // DB에는 바로 반영하지 않음, 즉시 성공 응답
            return RsData.success(liked ? ResponseCode.LIKE_CANCELLED : ResponseCode.LIKE_SUCCESS, !liked);
        } catch (Exception e) {
            log.info("[LIKE_500] 게시글 좋아요 토글 실패 - postId: {}, userId: {}, error: {}", postId, userId, e.getMessage());
            throw new GlobalException(ResponseCode.LIKE_INTERNAL_ERROR, "게시글 좋아요 토글 실패 - postId=" + postId + ", userId=" + userId + ", error=" + e.getMessage());
        }
    }

    public long countPostLikes(Long postId) {
        // Redis 버퍼에 해당 postId의 최신 이벤트들 반영
        List<LikeEvent> buffer = likeEventPoolBufferProvider.getEvents(likeEventPoolBufferProvider.getPartition(postId));
        // userId별 최신 이벤트만 남김
        java.util.Map<Long, LikeEvent> latest = new java.util.HashMap<>();
        for (LikeEvent e : buffer) {
            if (e.getPostId().equals(postId)) {
                LikeEvent prev = latest.get(e.getUserId());
                if (prev == null || e.getTimestamp() > prev.getTimestamp()) {
                    latest.put(e.getUserId(), e);
                }
            }
        }
        long dbCount = postLikeRepo.countByPostId(postId);
        // Redis 버퍼의 최신 상태 반영
        long delta = latest.values().stream().mapToInt(e -> e.getAction() == LikeEvent.Action.LIKE ? 1 : -1).sum();
        return dbCount + delta;
    }

    public boolean userLikedPost(Long postId, Long userId) {
        // Redis 버퍼에 최신 이벤트가 있으면 그 결과 우선
        LikeEvent latest = likeEventPoolBufferProvider.getLatestEventForUser(postId, userId);
        if (latest != null) {
            return latest.getAction() == LikeEvent.Action.LIKE;
        }
        // 없으면 DB로 fallback
        return postLikeRepo.existsByPostIdAndUserId(postId, userId);
    }

    public List<Post> getPostsByUserId(Long userId) {
        return postLikeRepo.findLikedPostsByUserId(userId);
    }

    /* ---------------- Comment ---------------- */

    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        try {
            boolean liked = commentLikeRepo.existsByCommentIdAndUserId(commentId, userId);
            LikeEvent.Action action = liked ? LikeEvent.Action.UNLIKE : LikeEvent.Action.LIKE;
            LikeEvent event = new LikeEvent(commentId, userId, action, System.currentTimeMillis());
            likeEventPoolBufferProvider.addEvent(event);
            // DB에는 바로 반영하지 않음, 즉시 성공 응답
            return !liked;
        } catch (Exception e) {
            log.info("[COMMENT_LIKE_500] 댓글 좋아요 토글 실패 - commentId: {}, userId: {}, error: {}", commentId, userId, e.getMessage());
            throw new GlobalException(ResponseCode.COMMENT_LIKE_INTERNAL_ERROR, "댓글 좋아요 토글 실패 - commentId=" + commentId + ", userId=" + userId + ", error=" + e.getMessage());
        }
    }

    public long countCommentLikes(Long commentId) {
        // Redis 버퍼에 해당 commentId의 최신 이벤트들 반영
        List<LikeEvent> buffer = likeEventPoolBufferProvider.getEvents(likeEventPoolBufferProvider.getPartition(commentId));
        java.util.Map<Long, LikeEvent> latest = new java.util.HashMap<>();
        for (LikeEvent e : buffer) {
            if (e.getPostId().equals(commentId)) {
                LikeEvent prev = latest.get(e.getUserId());
                if (prev == null || e.getTimestamp() > prev.getTimestamp()) {
                    latest.put(e.getUserId(), e);
                }
            }
        }
        long dbCount = commentLikeRepo.countByCommentId(commentId);
        long delta = latest.values().stream().mapToInt(e -> e.getAction() == LikeEvent.Action.LIKE ? 1 : -1).sum();
        return dbCount + delta;
    }

    public boolean userLikedComment(Long commentId, Long userId) {
        // Redis 버퍼에 최신 이벤트가 있으면 그 결과 우선
        LikeEvent latest = likeEventPoolBufferProvider.getLatestEventForUser(commentId, userId);
        if (latest != null) {
            return latest.getAction() == LikeEvent.Action.LIKE;
        }
        // 없으면 DB로 fallback
        return commentLikeRepo.existsByCommentIdAndUserId(commentId, userId);
    }

    /* ---------------- User Data Deletion ---------------- */

    @Transactional
    public RsData<Void> deleteAllPostLikesByUserId(Long userId) {
        try {
            // 1. 해당 사용자의 모든 포스트 좋아요 조회
            List<PostLike> userPostLikes = postLikeRepo.findByUserId(userId);

            if (userPostLikes.isEmpty()) {
                log.info("[LIKE_601] 삭제할 포스트 좋아요가 없음 - userId: {}", userId);
                return RsData.success(ResponseCode.POSTLIKE_DELETE, null);
            }

            log.info("[LIKE_602] 사용자 포스트 좋아요 일괄 삭제 시작 - userId: {}, likeCount: {}", userId, userPostLikes.size());

            // 2. 각 좋아요를 삭제하면서 포스트의 좋아요 카운트도 감소
            int successCount = 0;
            int failCount = 0;

            for (PostLike postLike : userPostLikes) {
                try {
                    // 포스트의 좋아요 수 감소
                    Post post = postLike.getPost();
                    post.decLike();

                    // ES 업데이트 시도
                    try {
                        exploreService.saveToES(toDto(post));
                    } catch (Exception e) {
                        log.error("[LIKE_603] ES 업데이트 실패 - postId: {}, error: {}", post.getId(), e.getMessage());
                    }

                    // 좋아요 삭제
                    postLikeRepo.delete(postLike);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    log.error("[LIKE_604] 포스트 좋아요 삭제 실패 - postLikeId: {}, userId: {}, error: {}",
                            postLike.getId(), userId, e.getMessage());
                }
            }

            log.info("[LIKE_605] 사용자 포스트 좋아요 일괄 삭제 완료 - userId: {}, 성공: {}, 실패: {}",
                    userId, successCount, failCount);

            return RsData.success(ResponseCode.POSTLIKE_DELETE, null);

        } catch (Exception e) {
            log.error("[LIKE_500] 사용자 포스트 좋아요 일괄 삭제 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR,
                    "사용자 포스트 좋아요 삭제 중 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    @Transactional
    public RsData<Void> deleteAllCommentLikesByUserId(Long userId) {
        try {
            // 1. 해당 사용자의 모든 댓글 좋아요 조회
            List<CommentLike> userCommentLikes = commentLikeRepo.findByUserId(userId);

            if (userCommentLikes.isEmpty()) {
                log.info("[LIKE_606] 삭제할 댓글 좋아요가 없음 - userId: {}", userId);
                return RsData.success(ResponseCode.COMMENT_LIKE_DELETE, null);
            }

            log.info("[LIKE_607] 사용자 댓글 좋아요 일괄 삭제 시작 - userId: {}, likeCount: {}", userId, userCommentLikes.size());

            // 2. 각 좋아요를 삭제하면서 댓글의 좋아요 카운트도 감소
            int successCount = 0;
            int failCount = 0;

            for (CommentLike commentLike : userCommentLikes) {
                try {
                    // 댓글의 좋아요 수 감소
                    Comment comment = commentLike.getComment();
                    comment.decLike();

                    // 좋아요 삭제
                    commentLikeRepo.delete(commentLike);
                    successCount++;

                } catch (Exception e) {
                    failCount++;
                    log.error("[LIKE_608] 댓글 좋아요 삭제 실패 - commentLikeId: {}, userId: {}, error: {}",
                            commentLike.getId(), userId, e.getMessage());
                }
            }

            log.info("[LIKE_609] 사용자 댓글 좋아요 일괄 삭제 완료 - userId: {}, 성공: {}, 실패: {}",
                    userId, successCount, failCount);

            return RsData.success(ResponseCode.COMMENT_LIKE_DELETE, null);

        } catch (Exception e) {
            log.error("[LIKE_500] 사용자 댓글 좋아요 일괄 삭제 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR,
                    "사용자 댓글 좋아요 삭제 중 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    @Transactional
    public RsData<Void> deleteAllLikesByUserId(Long userId) {
        try {
            log.info("[LIKE_610] 사용자 모든 좋아요 일괄 삭제 시작 - userId: {}", userId);

            // 포스트 좋아요 삭제
            deleteAllPostLikesByUserId(userId);

            // 댓글 좋아요 삭제
            deleteAllCommentLikesByUserId(userId);

            log.info("[LIKE_611] 사용자 모든 좋아요 일괄 삭제 완료 - userId: {}", userId);

            return RsData.success(ResponseCode.POSTLIKE_DELETE, null);

        } catch (Exception e) {
            log.error("[LIKE_500] 사용자 모든 좋아요 일괄 삭제 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR,
                    "사용자 모든 좋아요 삭제 중 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    private PostDetailResponse toDto(Post post) {
        Playlist pl = post.getPlaylist();

        PlaylistDetailResponse playlistDto = new PlaylistDetailResponse(
                pl.getId(),
                pl.getSpotifyUrl(),
                pl.getTotalTracks(),
                pl.getTotalLengthSec(),
                pl.getTracks()                   // List<TrackRsDto>
        );
        List<String> tags = post.getTags()          // Set<Tag>
                .stream()
                .map(Tag::getName)                  // Tag → String
                .toList();                          // Java 16+ (Java 21에서도 OK)

        return new PostDetailResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getUserProfile().getName(),
                post.getContent(),
                tags,
                post.getIsPublic(),
                post.getLikeCnt(),
                post.getViewCnt(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                playlistDto
        );
    }

    public Page<Post> getPostsByUserIdPageable(Long userId, Long viewerId, Pageable pageable) {
        try {
            // 본인이 조회하는 경우: 모든 좋아요한 게시글 조회 (공개/비공개 모두)
            if (userId.equals(viewerId)) {
                return postLikeRepo.findLikedPostsByUserIdPageable(userId, pageable);
            }
            // 타인이 조회하는 경우: 공개 게시글만 조회
            else {
                return postLikeRepo.findPublicLikedPostsByUserIdPageable(userId, pageable);
            }
        } catch (Exception e) {
            log.error("[LIKE_612] 사용자가 좋아요한 게시글 페이지 조회 실패 - userId: {}, viewerId: {}, error: {}",
                    userId, viewerId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR,
                    "좋아요한 게시글 조회 중 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }
    public Page<Post> getPostsByUsernamePageable(String username, String viewerUsername, Pageable pageable) {
        try {
            // 본인이 조회하는 경우: 모든 좋아요한 게시글 조회 (공개/비공개 모두)
            if (username.equals(viewerUsername)) {
                return postLikeRepo.findLikedPostsByUsernamePageable(username, pageable);
            }
            // 타인이 조회하는 경우: 공개 게시글만 조회
            else {
                return postLikeRepo.findPublicLikedPostsByUsernamePageable(username, pageable);
            }
        } catch (Exception e) {
            log.error("[LIKE_612] 사용자가 좋아요한 게시글 페이지 조회 실패 - userId: {}, viewerId: {}, error: {}",
                    username, viewerUsername, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR,
                    "좋아요한 게시글 조회 중 오류 - userId=" + username + ", error=" + e.getMessage());
        }
    }
}