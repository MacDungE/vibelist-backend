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
import org.example.vibelist.global.aop.UserActivityLog;
import org.example.vibelist.global.response.GlobalException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.example.vibelist.global.response.RsData;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.example.vibelist.global.security.core.CustomUserDetails;
import org.example.vibelist.global.response.ResponseCode;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /* ----------------------------------------------------
       Post Like (base path: /posts/{postId}/likes)
    ---------------------------------------------------- */

    @Operation(summary = "포스트 좋아요 토글")
    @PostMapping("/post/{postId}/likes")
    @UserActivityLog(action = "TOGGLE_LIKE_POST")//AOP 전달
    public ResponseEntity<RsData<?>> togglePost(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        Long userId = userDetail.getId();
        RsData<Boolean> result = likeService.togglePostLike(postId, userId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "포스트 좋아요 개수")
    @GetMapping("/post/{postId}/likes/count")
    public ResponseEntity<LikeCountRes> postCount(@PathVariable Long postId) {
        long cnt = likeService.countPostLikes(postId);
        return ResponseEntity.ok(new LikeCountRes(cnt));
    }

    @Operation(summary = "내가 눌렀는지 (포스트)",
            security = @SecurityRequirement(name = "access-cookie"))
    @GetMapping("/post/{postId}/likes/me")
    public ResponseEntity<LikeStatusRes> postMe(@PathVariable Long postId, @AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        boolean liked = likeService.userLikedPost(postId, userDetail.getId());
        return ResponseEntity.ok(new LikeStatusRes(liked));
    }

    /* ----------------------------------------------------
       Comment Like (base path: /comments/{commentId}/likes)
    ---------------------------------------------------- */

    @Operation(summary = "댓글 좋아요 토글",
            security = @SecurityRequirement(name = "access-cookie"))
    @PostMapping("/comment/{commentId}/likes")
    @UserActivityLog(action = "TOGGLE_LIKE_COMMENT") //AOP 전달 
    public ResponseEntity<LikeStatusRes> toggleComment(@PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        boolean liked = likeService.toggleCommentLike(commentId, userDetail.getId());
        return ResponseEntity.ok(new LikeStatusRes(liked));
    }

    @Operation(summary = "댓글 좋아요 개수")
    @GetMapping("/comment/{commentId}/likes/count")
    public ResponseEntity<LikeCountRes> commentCount(@PathVariable Long commentId) {
        long cnt = likeService.countCommentLikes(commentId);
        return ResponseEntity.ok(new LikeCountRes(cnt));
    }

    @Operation(summary = "내가 눌렀는지 (댓글)",
            security = @SecurityRequirement(name = "access-cookie"))
    @GetMapping("/comment/{commentId}/likes/me")
    public ResponseEntity<LikeStatusRes> commentMe(@PathVariable Long commentId, @AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        boolean liked = likeService.userLikedComment(commentId, userDetail.getId());
        return ResponseEntity.ok(new LikeStatusRes(liked));
    }

    /* ---------------------------------------------------- */

    /* 응답 DTO */
    @Data @AllArgsConstructor
    static class LikeStatusRes { private boolean liked; }
    @Data @AllArgsConstructor
    static class LikeCountRes  { private long likeCount; }
}