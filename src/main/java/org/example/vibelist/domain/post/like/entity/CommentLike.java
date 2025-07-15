package org.example.vibelist.domain.post.like.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.vibelist.domain.post.comment.entity.Comment;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.global.jpa.entity.BaseTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 중간 테이블(유저 ↔ 댓글) ― "좋아요" 엔티티
 *  ▸ 한 사용자가 같은 댓글에 여러 번 좋아요를 찍지 못하도록
 *    (user_id, comment_id) 복합 Unique 제약을 둡니다.
 *  ▸ createdAt / updatedAt 은 스프링 데이터 JPA Auditing 으로 자동 관리됩니다.
 */
@Entity
@Table(
        name = "comment_likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_comment_like_user_comment",
                columnNames = {"user_id", "comment_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CommentLike extends BaseTime {

    // ------------------------- 연관관계 ------------------------- //
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_like_comment"))
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_comment_like_user"))
    private User user;


    // ------------------------- 팩토리 메서드 ------------------------- //
    public static CommentLike create(User user, Comment comment) {
        return CommentLike.builder()
                .user(user)
                .comment(comment)
                .build();
    }
}
