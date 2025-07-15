package org.example.vibelist.domain.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.comment.dto.CommentCreateDto;
import org.example.vibelist.domain.comment.dto.CommentResponseDto;
import org.example.vibelist.domain.comment.dto.CommentUpdateDto;
import org.example.vibelist.domain.comment.service.CommentService;
import org.example.vibelist.global.exception.CustomException;
import org.example.vibelist.global.exception.ErrorCode;
import org.example.vibelist.global.security.core.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/comments")
@RequiredArgsConstructor
@Tag(name = "댓글 관리", description = "댓글 API")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 생성", description = "새로운 댓글을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "댓글 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CommentCreateDto dto, CustomUserDetails details) {
        commentService.create(dto, 1L);//details.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "댓글 조회", description = "현재 게시글의 댓글을 정렬 기준에 따라 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 조회 성공"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 정렬 기준")
    })
    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getAll(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        List<CommentResponseDto> comments = commentService.getSortedComments(postId, sort);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
            @ApiResponse(responseCode = "403", description = "댓글 작성자가 아님"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody CommentUpdateDto dto, CustomUserDetails details) {
        commentService.update(id, dto, 1L);//details.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "댓글 작성자가 아님"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, CustomUserDetails details) {
        commentService.delete(id, 1L);//details.getId());
        return ResponseEntity.noContent().build();
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
