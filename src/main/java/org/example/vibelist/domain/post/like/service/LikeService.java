package org.example.vibelist.domain.post.like.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.explore.service.ExploreService;
import org.example.vibelist.domain.post.comment.entity.Comment;
import org.example.vibelist.domain.post.comment.repository.CommentRepository;
import org.example.vibelist.domain.post.dto.PlaylistDetailResponse;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.entity.Playlist;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.post.like.entity.CommentLike;
import org.example.vibelist.domain.post.like.entity.PostLike;
import org.example.vibelist.domain.post.like.repository.CommentLikeRepository;
import org.example.vibelist.domain.post.like.repository.PostLikeRepository;
import org.example.vibelist.domain.post.repository.PostRepository;
import org.example.vibelist.domain.post.service.PostService;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final PostLikeRepository postLikeRepo;
    private final CommentLikeRepository commentLikeRepo;
    private final PostRepository postRepo;
    private final CommentRepository commentRepo;
    private final UserRepository userRepo;
    private final ExploreService exploreService;
    /* ---------------- Post ---------------- */

    @Transactional
    public boolean togglePostLike(Long postId, Long userId) {
        if (postLikeRepo.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepo.deleteByPostIdAndUserId(postId, userId);   // hard-delete

            postRepo.findById(postId).ifPresent(Post::decLike); //post entity에 반영

            return false;
        }
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found " + postId));
        PostLike like = PostLike.create(userRepo.getReferenceById(userId), post);
        postLikeRepo.save(like);

        post.incLike(); //post entity에 반영

        // 💡 게시글 좋아요 수 변경 후 Elasticsearch에 직접 업데이트
        // PostService의 public toDto 메서드를 사용하여 PostDetailResponse로 변환
        exploreService.saveToES(toDto(post));

        return true;
    }

    public long countPostLikes(Long postId) {
        return postLikeRepo.countByPostId(postId);
    }

    public boolean userLikedPost(Long postId, Long userId) {
        return postLikeRepo.existsByPostIdAndUserId(postId, userId);
    }

    public List<Post> getPostsByUserId(Long userId) {
        return postLikeRepo.findPostsByUserId(userId);
    }

    /* ---------------- Comment ---------------- */

    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        if (commentLikeRepo.existsByCommentIdAndUserId(commentId, userId)) {
            commentLikeRepo.deleteByCommentIdAndUserId(commentId, userId);


            commentRepo.findById(commentId).ifPresent(Comment::decLike); //comment entity에 반영

            return false;
        }
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found " + commentId));
        CommentLike like = CommentLike.create(userRepo.getReferenceById(userId), comment);
        commentLikeRepo.save(like);
        comment.incLike();//comment entity에 반영
        return true;
    }

    public long countCommentLikes(Long commentId) {
        return commentLikeRepo.countByCommentId(commentId);
    }

    public boolean userLikedComment(Long commentId, Long userId) {
        return commentLikeRepo.existsByCommentIdAndUserId(commentId, userId);
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