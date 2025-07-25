package org.example.vibelist.domain.post.service;


import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.explore.service.ExploreService;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.SpotifyPlaylistDto;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.service.PlaylistService;
import org.example.vibelist.domain.post.cache.PostCacheService;
import org.example.vibelist.domain.post.dto.PlaylistDetailResponse;
import org.example.vibelist.domain.post.dto.PostCreateRequest;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.dto.PostUpdateRequest;
import org.example.vibelist.domain.post.entity.Playlist;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.like.service.LikeService;
import org.example.vibelist.domain.post.repository.PostRepository;
import org.example.vibelist.domain.post.tag.entity.Tag;
import org.example.vibelist.domain.post.tag.service.TagService;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.GlobalException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PlaylistService playlistService;
    private final UserRepository userRepository;
    private final LikeService likeService;
    private final ExploreService exploreService;
    private final TagService tagService;

    // 캐시 전용 서비스
    private final PostCacheService postCacheService;

    @Transactional
    public RsData<Long> createPost(Long userId, PostCreateRequest dto) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ResponseCode.USER_NOT_FOUND, "userId=" + userId + "인 사용자를 찾을 수 없습니다."));
            List<TrackRsDto> tracks = dto.getTracks();
            SpotifyPlaylistDto responseDto;
            try {
                responseDto = playlistService.createPlaylist(userId, tracks).getData();
            } catch (Exception e) {
                log.error("[POST_502] 플레이리스트 생성 실패 - userId: {}, error: {}", userId, e.getMessage());
                throw new GlobalException(ResponseCode.PLAYLIST_CREATE_FAIL, "플레이리스트 생성 실패 - userId=" + userId + ", error=" + e.getMessage());
            }
            String spotifyUrl = responseDto.getSpotifyId();
            int totalTracks = tracks.size();
            int totalLengthSec = tracks.stream().mapToInt(TrackRsDto::getDurationMs).sum() / 1_000;
            Playlist playlist = Playlist.builder()
                    .spotifyUrl(spotifyUrl)
                    .totalTracks(totalTracks)
                    .totalLengthSec(totalLengthSec)
                    .tracks(tracks)
                    .build();
            Set<Tag> tags = dto.getTags().stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(tagService::getOrCreate)
                    .collect(Collectors.toSet());
            Post post = Post.builder()
                    .user(user)
                    .content(dto.getContent())
                    .tags(tags)
                    .isPublic(dto.getIsPublic())
                    .playlist(playlist)
                    .build();
            try {
                postRepository.save(post);
            } catch (Exception e) {
                log.error("[POST_503] 게시글 저장 실패 - userId: {}, error: {}", userId, e.getMessage());
                throw new GlobalException(ResponseCode.POST_SAVE_FAIL, "게시글 저장 실패 - userId=" + userId + ", error=" + e.getMessage());
            }
            try {
                PostDetailResponse postDetailResponse = toDto(post);
                exploreService.saveToES(postDetailResponse);
            } catch (Exception e) {
                log.error("[POST_504] Elasticsearch 저장 실패 - postId: {}, error: {}", post.getId(), e.getMessage());
            }
            return RsData.success(ResponseCode.POST_CREATED, post.getId());
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[SYS_500] 알 수 없는 게시글 생성 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "게시글 생성 중 알 수 없는 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    @Transactional
    public RsData<Void> updatePost(Long userId, PostUpdateRequest dto) {
        try {
            Post post = postRepository.findByIdAndDeletedAtIsNull(dto.getId())
                    .orElseThrow(() -> new GlobalException(ResponseCode.POST_NOT_FOUND, "postId=" + dto.getId() + "인 게시글을 찾을 수 없습니다."));
            if (!post.getUser().getId().equals(userId))
                throw new GlobalException(ResponseCode.POST_FORBIDDEN, "게시글 수정 권한 없음 - userId=" + userId + ", postId=" + dto.getId());
            Set<Tag> tags = dto.getTags().stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(tagService::getOrCreate)
                    .collect(Collectors.toSet());
            post.edit(dto.getContent(), tags, dto.getIsPublic());

            // 캐시 무효화
            postCacheService.deleteFromCache(dto.getId());

            try {
                PostDetailResponse postDetailResponse = toDto(post);
                exploreService.saveToES(postDetailResponse);
            } catch (Exception e) {
                log.error("[POST_004] Elasticsearch 반영 실패 - postId: {}, error: {}", post.getId(), e.getMessage());
            }
            return RsData.success(ResponseCode.POST_UPDATED, null);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[POST_999] 알 수 없는 게시글 수정 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "게시글 수정 중 알 수 없는 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    @Transactional
    public RsData<Void> deletePost(Long userId, Long postId) {
        try {
            Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                    .orElseThrow(() -> new GlobalException(ResponseCode.POST_NOT_FOUND, "postId=" + postId + "인 게시글을 찾을 수 없습니다."));
            if (!post.getUser().getId().equals(userId))
                throw new GlobalException(ResponseCode.POST_FORBIDDEN, "게시글 삭제 권한 없음 - userId=" + userId + ", postId=" + postId);
            post.markDeleted();

            // 캐시 무효화
            postCacheService.deleteFromCache(postId);

            try {
                exploreService.deleteFromES(postId);
            } catch (Exception e) {
                log.error("[POST_005] Elasticsearch 삭제 실패 - postId: {}, error: {}", postId, e.getMessage());
            }
            return RsData.success(ResponseCode.POST_DELETED, null);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[POST_999] 알 수 없는 게시글 삭제 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "게시글 삭제 중 알 수 없는 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    @Transactional
    public RsData<Void> softDeleteAllPostsByUserIdBulk(Long userId) {
        try {
            // 1. ES 삭제를 위해 먼저 포스트 ID들 조회
            List<Long> postIds = postRepository.findPostIdsByUserIdAndDeletedAtIsNull(userId);

            if (postIds.isEmpty()) {
                log.info("[POST_006] 삭제할 포스트가 없음 - userId: {}", userId);
                return RsData.success(ResponseCode.POST_DELETED, null);
            }

            // 2. 벌크 소프트 삭제
            int deletedCount = postRepository.softDeleteByUserId(userId);

            // 3. 캐시에서 일괄 삭제
            for (Long postId : postIds) {
                postCacheService.deleteFromCache(postId);
            }

            // 4. ES에서 일괄 삭제
            int esFailCount = 0;
            for (Long postId : postIds) {
                try {
                    exploreService.deleteFromES(postId);
                } catch (Exception e) {
                    esFailCount++;
                    log.error("[POST_008] Elasticsearch 삭제 실패 - postId: {}, error: {}", postId, e.getMessage());
                }
            }

            log.info("[POST_010] 사용자 포스트 벌크 삭제 완료 - userId: {}, 삭제된 포스트: {}, ES실패: {}",
                    userId, deletedCount, esFailCount);

            return RsData.success(ResponseCode.POST_DELETED, null);

        } catch (Exception e) {
            log.error("[POST_999] 사용자 포스트 벌크 삭제 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR,
                    "사용자 포스트 삭제 중 알 수 없는 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }

    public RsData<PostDetailResponse> getPostDetail(Long postId, Long viewerId) {
        try {
            // 캐시에서 권한 체크와 함께 조회 시도
            PostDetailResponse cachedPost = postCacheService.getWithPermissionCheck(postId, viewerId);
            if (cachedPost != null) {
                // 조회수 증가 (캐시 히트 시에도)
                Post post = postRepository.findById(postId).orElse(null);
                if (post != null && post.getDeletedAt() == null) {
                    post.addViewCnt();
                }

                log.info("캐시에서 게시글 반환: postId={}, isPublic={}", postId, cachedPost.isPublic());
                return RsData.success(ResponseCode.POST_CREATED, "게시글 상세 조회 성공", cachedPost);
            }

            // 캐시 미스 또는 권한 없음 - DB에서 조회
            Post post = postRepository.findDetailById(postId)
                    .orElseThrow(() -> new GlobalException(ResponseCode.POST_NOT_FOUND, "postId=" + postId + "인 게시글을 찾을 수 없습니다."));

            // 비공개 게시글 접근 권한 체크
            if (!post.getIsPublic()) {
                if (viewerId == null) {
                    throw new GlobalException(ResponseCode.POST_FORBIDDEN, "비공개 게시글은 로그인이 필요합니다 - postId=" + postId);
                }
                if (!post.getUser().getId().equals(viewerId)) {
                    throw new GlobalException(ResponseCode.POST_FORBIDDEN, "비공개 게시글 접근 권한 없음 - viewerId=" + viewerId + ", postId=" + postId);
                }
            }

            // 조회수 증가
            post.addViewCnt();

            // Elasticsearch 업데이트
            try {
                exploreService.saveToES(toDto(post));
            } catch (Exception e) {
                log.error("[POST_004] Elasticsearch 반영 실패 - postId: {}, error: {}", post.getId(), e.getMessage());
            }

            PostDetailResponse responseDto = toDto(post);

            // 캐시에 저장 (공개 게시글만)
            postCacheService.saveToCache(postId, responseDto);

            return RsData.success(ResponseCode.POST_CREATED, "게시글 상세 조회 성공", responseDto);

        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[POST_999] 알 수 없는 게시글 상세 조회 오류 - postId: {}, error: {}", postId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "게시글 상세 조회 중 알 수 없는 오류 - postId=" + postId + ", error=" + e.getMessage());
        }
    }

    public RsData<List<PostDetailResponse>> getLikedPostsByUser(Long userId) {
        try {
            List<Post> posts = likeService.getPostsByUserId(userId);
            List<PostDetailResponse> result = posts.stream().map(this::toDto).toList();
            return RsData.success(ResponseCode.POST_CREATED, "좋아요한 게시글 목록 조회 성공", result);
        } catch (Exception e) {
            log.error("[POST_999] 좋아요한 게시글 목록 조회 오류 - userId: {}, error: {}", userId, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "좋아요한 게시글 목록 조회 중 오류 - userId=" + userId + ", error=" + e.getMessage());
        }
    }
    public RsData<Page<PostDetailResponse>> getLikedPostsByUser(String username, String viewerUsername, Pageable pageable) {
        try {
            // 프로필 소유자 확인
            User profileOwner = userRepository.findByUsername(username)
                    .orElseThrow(() -> new GlobalException(ResponseCode.USER_NOT_FOUND, "username=" + username + "인 사용자를 찾을 수 없습니다."));

            // 좋아요한 게시글 페이지 조회
            Page<Post> posts = likeService.getPostsByUsernamePageable(username, viewerUsername, pageable);
            Page<PostDetailResponse> result = posts.map(this::toDto);

            return RsData.success(ResponseCode.POST_CREATED, "좋아요한 게시글 목록 조회 성공", result);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[POST_999] 좋아요한 게시글 목록 조회 오류 - username: {}, error: {}", username, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "좋아요한 게시글 목록 조회 중 오류 - userId=" + username + ", error=" + e.getMessage());
        }
    }

    public RsData<Page<PostDetailResponse>> getPostsByUser(String username, String viewerUsername, Pageable pageable) {
        try {
            // 프로필 소유자 확인
            User profileOwner = userRepository.findByUsername(username)
                    .orElseThrow(() -> new GlobalException(ResponseCode.USER_NOT_FOUND, "username=" + username + "인 사용자를 찾을 수 없습니다."));

            Page<Post> posts;

            // 본인 프로필인 경우: 모든 게시글 조회
            if (username.equals(viewerUsername)) {
                posts = postRepository.findByUsernameAndDeletedAtIsNull(username, pageable);
            }
            // 타인 프로필인 경우: 공개 게시글만 조회
            else {
                posts = postRepository.findByUsernameAndIsPublicTrueAndDeletedAtIsNull(username, pageable);
            }

            Page<PostDetailResponse> result = posts.map(this::toDto);

            return RsData.success(ResponseCode.POST_CREATED, "작성한 게시글 목록 조회 성공", result);
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[POST_999] 작성한 게시글 목록 조회 오류 - username: {}, error: {}", username, e.getMessage());
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "작성한 게시글 목록 조회 중 오류 - username=" + username + ", error=" + e.getMessage());
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

}
