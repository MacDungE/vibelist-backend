package org.example.vibelist.domain.post.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.post.comment.dto.CommentCreateDto;
import org.example.vibelist.domain.post.comment.dto.CommentResponseDto;
import org.example.vibelist.domain.post.comment.dto.CommentUpdateDto;
import org.example.vibelist.domain.post.comment.service.CommentService;
import org.example.vibelist.global.security.core.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.RsData;

@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
@Tag(name = "댓글 관리", description = "댓글 API")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 생성", description = "새로운 댓글을 등록합니다.")
    @PostMapping
    public ResponseEntity<RsData<?>> create(@RequestBody CommentCreateDto dto, @AuthenticationPrincipal CustomUserDetails details) {
        if (details == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }
        Long userId = details.getId();
        RsData<?> result = commentService.create(dto, userId);
        return ResponseEntity.status(result.isSuccess() ? 201 : 400).body(result);
    }

    @Operation(summary = "댓글 조회", description = "현재 게시글의 댓글을 정렬 기준에 따라 조회합니다.")
    @GetMapping
    public ResponseEntity<RsData<List<CommentResponseDto>>> getAll(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        RsData<List<CommentResponseDto>> result = commentService.getSortedComments(postId, sort);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<RsData<?>> update(@PathVariable Long id, @RequestBody CommentUpdateDto dto, @AuthenticationPrincipal CustomUserDetails details) {
        if (details == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }
        Long userId = details.getId();
        RsData<?> result = commentService.update(id, dto, userId);
        return ResponseEntity.status(result.isSuccess() ? 200 : 403).body(result);
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<RsData<Void>> delete(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails details) {
        if (details == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }
        Long userId = details.getId();
        RsData<Void> result = commentService.delete(id, userId);
        return ResponseEntity.status(result.isSuccess() ? 204 : 403).body(result);
    }
// 댓글 좋아요 / 취소
//    @PostMapping("/{id}/like")
//    public ResponseEntity<Void> like(@PathVariable Long id) {
//        return ResponseEntity.ok().build();
//    }
//
//    @DeleteMapping("/{id}/like")
//    public ResponseEntity<Void> unlike(@PathVariable Long id) {
//        return ResponseEntity.ok().build();
//    }

}
