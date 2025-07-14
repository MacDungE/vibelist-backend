package org.example.vibelist.domain.post.service;


import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.post.dto.PlaylistDetailResponse;
import org.example.vibelist.domain.post.dto.PostCreateRequest;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.dto.PostUpdateRequest;
import org.example.vibelist.domain.post.entity.Playlist;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.repository.PostRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public Long createPost(Long userId, PostCreateRequest dto) {


        String spotifyUrl="";

        // 2) 총 트랙 수·총 길이 계산
        int totalTracks     = dto.getTracks().size();
        int totalLengthSec = dto.getTracks()          // List<TrackRsDto>
                .stream()
                .mapToInt(TrackRsDto::getDurationMs)  // ① 밀리초 합산
                .sum() / 1_000;                       // ② 초 단위로 변환

        Playlist playlist = Playlist.builder()
                .spotifyUrl(spotifyUrl)
                .totalTracks(totalTracks)
                .totalLengthSec(totalLengthSec)
                .tracks(dto.getTracks())
                .build();

        /* ─── ② Post 생성 & 저장(Cascade.ALL) ───────────── */

        Post post = Post.builder()
                .userId(userId)
                .content(dto.getContent())
                .isPublic(dto.getIsPublic())
                .playlist(playlist)      // 1:1 연결
                .build();

        postRepository.save(post);               // Playlist 가 함께 INSERT
        return post.getId();
    }

    @Transactional
    public void updatePost(Long userId, PostUpdateRequest dto) {

        Post post = postRepository.findByIdAndDeletedAtIsNull(dto.getId())
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getUserId().equals(userId))
            throw new RuntimeException("Post id mismatch");

        post.edit(dto.getContent(),dto.getIsPublic());

    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getUserId().equals(userId))
            throw new RuntimeException("Post id mismatch");
        post.markDeleted();
    }

    public PostDetailResponse getPostDetail(Long postId, Long viewerId) {

        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

        /* 비공개 게시글이면 작성자만 허용 */
        if (!post.getIsPublic() && !post.getUserId().equals(viewerId)) {
            throw new AccessDeniedException("열람 권한이 없습니다.");
        }

        post.addViewCnt();

        return toDto(post);
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

        return new PostDetailResponse(
                post.getId(),
                post.getUserId(),
                post.getContent(),
                post.getIsPublic(),
                post.getLikeCnt(),
                post.getViewCnt(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                playlistDto
        );
    }

}
