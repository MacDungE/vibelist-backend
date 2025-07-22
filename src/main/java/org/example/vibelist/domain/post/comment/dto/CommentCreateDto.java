package org.example.vibelist.domain.post.comment.dto;

import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 생성 요청 DTO")
@Getter
public class CommentCreateDto {
    @Schema(description = "댓글 내용", example = "좋은 글 감사합니다!", required = true)
    private String content;
    @Schema(description = "게시글 ID", example = "123", required = true)
    private Long postId;
    @Schema(description = "부모 댓글 ID(대댓글일 때만)", example = "456", required = false)
    private Long parentId;
}
