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
import org.example.vibelist.global.exception.CustomException;
import org.example.vibelist.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    /* ---------------- Post ---------------- */

    @Transactional
    public boolean togglePostLike(Long postId, Long userId) {
        try {
            if (postLikeRepo.existsByPostIdAndUserId(postId, userId)) {
                postLikeRepo.deleteByPostIdAndUserId(postId, userId);   // hard-delete
                postRepo.findById(postId).ifPresent(Post::decLike); //post entity에 반영
                return false;
            }
            Post post = postRepo.findById(postId)
                    .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
            PostLike like = PostLike.create(userRepo.getReferenceById(userId), post);
            postLikeRepo.save(like);
            post.incLike(); //post entity에 반영
            exploreService.saveToES(toDto(post));
            return true;
        } catch (Exception e) {
            log.info("[POST_001] 게시글 좋아요 토글 실패 - postId: {}, userId: {}, error: {}", postId, userId, e.getMessage());
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }
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