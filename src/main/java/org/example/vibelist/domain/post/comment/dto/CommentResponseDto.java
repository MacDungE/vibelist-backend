package org.example.vibelist.domain.post.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Builder
@Schema(description = "댓글 응답 DTO")
public class CommentResponseDto {
    @Schema(description = "댓글 ID", example = "1001", required = true)
    private Long id;
    @Schema(description = "댓글 내용", example = "좋은 글 감사합니다!", required = true)
    private String content;
    @Schema(description = "작성자 ID", example = "1", required = true)
    private Long userId;
    @Schema(description = "작성자명", example = "vibelist_user", required = true)
    private String username;
    private String userProfileName;
    private Long parentId;
    private LocalDateTime createdAt;
    private List<CommentResponseDto> children;
    private int likeCount;
}
