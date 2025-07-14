package org.example.vibelist.domain.comment.dto;

import lombok.Getter;

@Getter
public class CommentCreateDto {
    private Long postId;
    private String content;
    private String username;
    private Long parentId;
}
