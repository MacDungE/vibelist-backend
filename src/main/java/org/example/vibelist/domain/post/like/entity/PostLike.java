package org.example.vibelist.domain.post.like.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.vibelist.domain.post.entity.Post;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.global.jpa.entity.BaseTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "post_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_like_user_post",
                columnNames = {"user_id", "post_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PostLike extends BaseTime {

    // ------------------------- 연관관계 ------------------------- //
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_like_post"))
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_like_user"))
    private User user;


    // ------------------------- 팩토리 메서드 ------------------------- //
    public static PostLike create(User user, Post post) {
        return PostLike.builder()
                .user(user)
                .post(post)
                .build();
    }
}
