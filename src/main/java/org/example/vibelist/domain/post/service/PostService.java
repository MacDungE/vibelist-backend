package org.example.vibelist.domain.post.service;


import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.explore.service.ExploreService;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.SpotifyPlaylistDto;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.service.PlaylistService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.NoSuchElementException;
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

    public RsData<PostDetailResponse> getPostDetail(Long postId, Long viewerId) {
        try {
            Post post = postRepository.findDetailById(postId)
                    .orElseThrow(() -> new GlobalException(ResponseCode.POST_NOT_FOUND, "postId=" + postId + "인 게시글을 찾을 수 없습니다."));
            if (!post.getIsPublic() && !post.getUser().getId().equals(viewerId)) {
                throw new GlobalException(ResponseCode.POST_FORBIDDEN, "비공개 게시글 접근 권한 없음 - viewerId=" + viewerId + ", postId=" + postId);
            }
            post.addViewCnt();
            try {
                exploreService.saveToES(toDto(post));
            } catch (Exception e) {
                log.error("[POST_004] Elasticsearch 반영 실패 - postId: {}, error: {}", post.getId(), e.getMessage());
            }
            return RsData.success(ResponseCode.POST_CREATED, "게시글 상세 조회 성공", toDto(post));
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
