package org.example.vibelist.domain.post.service;


import lombok.RequiredArgsConstructor;
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
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final PlaylistService playlistService;
    private final UserRepository userRepository;
    private final LikeService likeService;
    @Transactional
    public Long createPost(Long userId, PostCreateRequest dto) {
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        List<TrackRsDto> tracks = dto.getTracks();//track 정보 받아오기
        SpotifyPlaylistDto responseDto= new SpotifyPlaylistDto();
        try {
            responseDto = playlistService.createPlaylist(userId,tracks);
        }
        catch (Exception e) {
            log.info("Spotify api 호출 중 에러가 발생했습니다.");
        }
        String spotifyUrl= responseDto.getSpotifyId();

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
                .user(user)
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
        if (!post.getUser().getId().equals(userId))
            throw new RuntimeException("Post id mismatch");

        post.edit(dto.getContent(),dto.getIsPublic());

    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getUser().getId().equals(userId))
            throw new RuntimeException("Post id mismatch");
        post.markDeleted();
    }

    public PostDetailResponse getPostDetail(Long postId, Long viewerId) {

        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글이 존재하지 않습니다."));

        /* 비공개 게시글이면 작성자만 허용 */
        if (!post.getIsPublic() && !post.getUser().getId().equals(viewerId)) {
            throw new AccessDeniedException("열람 권한이 없습니다.");
        }

        post.addViewCnt();

        return toDto(post);
    }

    public List<PostDetailResponse> getLikedPostsByUser(Long userId) {
        List<Post> posts = likeService.getPostsByUserId(userId);
        return posts.stream()
                .map(this::toDto)
                .toList();
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
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getUserProfile().getName(),
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
