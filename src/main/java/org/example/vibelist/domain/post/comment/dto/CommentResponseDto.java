package org.example.vibelist.domain.post.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CommentResponseDto {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private String userProfileName;
    private Long parentId;
    private LocalDateTime createdAt;
    private List<CommentResponseDto> children;
    private int likeCount;
}
