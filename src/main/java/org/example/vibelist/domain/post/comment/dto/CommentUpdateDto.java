package org.example.vibelist.domain.post.comment.dto;

import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 수정 요청 DTO")
@Getter
public class CommentUpdateDto {
    @Schema(description = "댓글 내용", example = "수정된 댓글 내용입니다.", required = true)
    private String content;
}
