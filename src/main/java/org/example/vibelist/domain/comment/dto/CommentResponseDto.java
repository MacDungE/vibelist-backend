package org.example.vibelist.domain.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponseDto {
    private Long id;
    private String content;
    private String username;
    private Long parentId;
    private LocalDateTime createdAt;
    private List<CommentResponseDto> children;
}
