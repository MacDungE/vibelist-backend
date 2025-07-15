package org.example.vibelist.domain.post.like.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.post.like.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /* ----------------------------------------------------
       Post Like (base path: /posts/{postId}/likes)
    ---------------------------------------------------- */

    @Operation(summary = "포스트 좋아요 토글",
            security = @SecurityRequirement(name = "access-cookie"))
    @PostMapping("/posts/{postId}/likes")
    public ResponseEntity<LikeStatusRes> togglePost(@PathVariable Long postId) {
        boolean liked = likeService.togglePostLike(postId, currentUserId());
        return ResponseEntity.ok(new LikeStatusRes(liked));
    }

    @Operation(summary = "포스트 좋아요 개수")
    @GetMapping("/posts/{postId}/likes/count")
    public ResponseEntity<LikeCountRes> postCount(@PathVariable Long postId) {
        long cnt = likeService.countPostLikes(postId);
        return ResponseEntity.ok(new LikeCountRes(cnt));
    }

    @Operation(summary = "내가 눌렀는지 (포스트)",
            security = @SecurityRequirement(name = "access-cookie"))
    @GetMapping("/posts/{postId}/likes/me")
    public ResponseEntity<LikeStatusRes> postMe(@PathVariable Long postId) {
        boolean liked = likeService.userLikedPost(postId, currentUserId());
        return ResponseEntity.ok(new LikeStatusRes(liked));
    }

    /* ----------------------------------------------------
       Comment Like (base path: /comments/{commentId}/likes)
    ---------------------------------------------------- */

    @Operation(summary = "댓글 좋아요 토글",
            security = @SecurityRequirement(name = "access-cookie"))
    @PostMapping("/comments/{commentId}/likes")
    public ResponseEntity<LikeStatusRes> toggleComment(@PathVariable Long commentId) {
        boolean liked = likeService.toggleCommentLike(commentId, currentUserId());
        return ResponseEntity.ok(new LikeStatusRes(liked));
    }

    @Operation(summary = "댓글 좋아요 개수")
    @GetMapping("/comments/{commentId}/likes/count")
    public ResponseEntity<LikeCountRes> commentCount(@PathVariable Long commentId) {
        long cnt = likeService.countCommentLikes(commentId);
        return ResponseEntity.ok(new LikeCountRes(cnt));
    }

    @Operation(summary = "내가 눌렀는지 (댓글)",
            security = @SecurityRequirement(name = "access-cookie"))
    @GetMapping("/comments/{commentId}/likes/me")
    public ResponseEntity<LikeStatusRes> commentMe(@PathVariable Long commentId) {
        boolean liked = likeService.userLikedComment(commentId, currentUserId());
        return ResponseEntity.ok(new LikeStatusRes(liked));
    }

    /* ---------------------------------------------------- */

    private long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();   // 커스텀 Principal → id
    }

    /* 응답 DTO */
    @Data @AllArgsConstructor
    static class LikeStatusRes { private boolean liked; }
    @Data @AllArgsConstructor
    static class LikeCountRes  { private long likeCount; }
}