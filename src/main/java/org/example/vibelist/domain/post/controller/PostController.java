package org.example.vibelist.domain.post.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.post.dto.PostCreateRequest;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.example.vibelist.domain.post.dto.PostUpdateRequest;
import org.example.vibelist.domain.post.service.PostService;
import org.example.vibelist.global.security.core.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.MediaType;

@Slf4j
@RestController
@RequestMapping("/v1/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 + 플레이리스트 생성", description = "트랙 리스트를 포함한 게시글을 작성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "생성된 게시글 ID", content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Long createPost(@RequestBody @Valid PostCreateRequest request,
                           @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetail) {

        return postService.createPost(1L,request);

    }

    @Operation(summary = "게시글 상세 조회", description = "게시글과 연결된 플레이리스트 및 트랙 정보를 반환합니다.")
    @GetMapping("/{id}")
    public PostDetailResponse getPostDetail(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails userDetail) {

        return postService.getPostDetail(id, 1L);
    }

    @Operation(summary = "게시글 수정", description = "게시글 내용과 공개 여부를 수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "수정 성공"),
        @ApiResponse(responseCode = "403", description = "수정 권한 없음", content = @Content)
    })
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePost(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetail,
                           @RequestBody @Valid PostUpdateRequest request) {

        postService.updatePost(1L, request);
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 소프트 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "403", description = "삭제 권한 없음", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id,
                           @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetail) {

        postService.deletePost(1L, id);
    }


}
