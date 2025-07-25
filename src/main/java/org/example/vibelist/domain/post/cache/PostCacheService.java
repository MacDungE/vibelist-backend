package org.example.vibelist.domain.post.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.like.service.LikeService;
import org.springframework.stereotype.Service;

/**
 * 게시글 캐시 관련 로직을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService {

    private final PostCacheProvider postCacheProvider;
    private final LikeService likeService;

    /**
     * 캐시에서 게시글 조회 (실시간 좋아요 수 반영)
     */
    public PostDetailResponse getFromCache(Long postId) {
        try {
            PostDetailResponse cachedPost = postCacheProvider.get(postId);

            if (cachedPost == null) {
                return null; // 캐시 미스
            }

            // 실시간 좋아요 수 확인 및 반영
            long currentLikeCount = likeService.countPostLikes(postId);

            if (currentLikeCount != cachedPost.likeCnt()) {
                // 좋아요 수가 변경된 경우 업데이트된 DTO 생성
                return new PostDetailResponse(
                    cachedPost.id(), cachedPost.userId(), cachedPost.userName(),
                    cachedPost.userProfileName(), cachedPost.content(), cachedPost.tags(),
                    cachedPost.isPublic(), currentLikeCount, // 실시간 좋아요 수
                    cachedPost.viewCnt(), cachedPost.createdAt(), cachedPost.updatedAt(),
                    cachedPost.playlist()
                );
            }

            return cachedPost;
        } catch (Exception e) {
            log.error("캐시 조회 중 오류 발생: postId={}, error={}", postId, e.getMessage());
            return null; // fallback to DB
        }
    }

    /**
     * 캐시에 게시글 저장 (공개 게시글만)
     */
    public void saveToCache(Long postId, PostDetailResponse post) {
        if (post.isPublic()) { // 공개 게시글만 캐시
            postCacheProvider.set(postId, post);
        }
    }

    /**
     * 캐시에서 게시글 삭제
     */
    public void deleteFromCache(Long postId) {
        postCacheProvider.delete(postId);
    }

    /**
     * 권한 체크 후 캐시 처리
     */
    public PostDetailResponse getWithPermissionCheck(Long postId, Long viewerId) {
        PostDetailResponse cachedPost = getFromCache(postId);

        if (cachedPost == null) {
            return null;
        }

        // 공개 게시글이면 바로 반환
        if (cachedPost.isPublic()) {
            return cachedPost;
        }

        // 비공개 게시글 권한 체크
        if (viewerId != null && cachedPost.userId().equals(viewerId)) {
            return cachedPost; // 작성자는 접근 가능
        }

        // 권한 없으면 캐시 삭제 후 null 반환
        deleteFromCache(postId);
        return null;
    }
}

