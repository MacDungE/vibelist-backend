package org.example.vibelist.domain.comment.dto;

import lombok.Getter;
import org.example.vibelist.global.user.entity.User;

@Getter
public class CommentCreateDto {
    private Long postId;
    private String content;
    private User user;
    private Long parentId;
}
