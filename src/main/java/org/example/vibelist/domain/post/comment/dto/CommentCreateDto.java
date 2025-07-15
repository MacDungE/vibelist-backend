package org.example.vibelist.domain.post.comment.dto;

import lombok.Getter;

@Getter
public class CommentCreateDto {
    private Long postId;
    private String content;
    private Long parentId;
}
