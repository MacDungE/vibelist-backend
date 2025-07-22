package org.example.vibelist.domain.post.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.post.dto.PostCreateRequest;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.dto.PostUpdateRequest;
import org.example.vibelist.domain.post.service.PostService;
import org.example.vibelist.global.aop.UserActivityLog;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.security.core.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/post")
@SecurityRequirement(name = "bearer-key")
@SecurityRequirement(name = "access-cookie")
@SecurityRequirement(name = "cookie-auth")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 + 플레이리스트 생성", description = "트랙 리스트를 포함한 게시글을 작성합니다.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @UserActivityLog(action = "CREATE_POST")//AOP 전달
    public ResponseEntity<RsData<Long>> createPost(@RequestBody @Valid PostCreateRequest request,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED);
        Long userId = userDetail.getId();
        RsData<Long> result = postService.createPost(userId, request);
        return ResponseEntity.status(result.isSuccess() ? 201 : 400).body(result);
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글과 연결된 플레이리스트 및 트랙 정보를 반환합니다.")
    @GetMapping("/{id}")
    @UserActivityLog(action="VIEW_POST")//AOP전달
    public ResponseEntity<RsData<?>> getPostDetail(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED);
        Long userId = userDetail.getId();
        RsData<?> result = postService.getPostDetail(id, userId);
        return ResponseEntity.status(result.isSuccess() ? 200 : 404).body(result);
    }

    @Operation(summary = "게시글 수정", description = "게시글 내용과 공개 여부를 수정합니다.")
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @UserActivityLog(action="EDIT_POST")//AOP전달
    public ResponseEntity<RsData<?>> updatePost(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetail,
                           @RequestBody @Valid PostUpdateRequest request) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED);
        Long userId = userDetail.getId();
        RsData<?> result = postService.updatePost(userId, request);
        return ResponseEntity.status(result.isSuccess() ? 204 : 403).body(result);
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 소프트 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @UserActivityLog(action="DELETE_POST")//AOP전달
    public ResponseEntity<RsData<?>> deletePost(@PathVariable Long id,
                           @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED);
        Long userId = userDetail.getId();
        RsData<?> result = postService.deletePost(userId, id);
        return ResponseEntity.status(result.isSuccess() ? 204 : 403).body(result);
    }


    @Operation(summary = "사용자가 좋아요한 게시글 목록 조회", description = "현재 인증된 사용자가 좋아요한 게시글 목록을 조회합니다.")
    @GetMapping(value = "/likes", produces = MediaType.APPLICATION_JSON_VALUE)
    @UserActivityLog(action="VIEW_LIKES_POST")//AOP전달
    public ResponseEntity<RsData<?>> getLikedPostsByUser(@AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED);
        Long userId = userDetail.getId();
        RsData<?> result = postService.getLikedPostsByUser(userId);
        return ResponseEntity.ok(result);
    }
}
