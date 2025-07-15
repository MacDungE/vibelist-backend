package org.example.vibelist.domain.post.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.global.jpa.entity.BaseEntity;
import org.example.vibelist.global.jpa.entity.BaseTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "posts")
//@EntityListeners(AuditingEntityListener.class)
public class Post extends BaseTime {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "like_cnt", nullable = false)
    private Long likeCnt = 0L;

    @Column(name = "view_cnt", nullable = false)
    private Long viewCnt = 0L;

//    @CreatedDate
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @LastModifiedDate
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToOne(cascade = CascadeType.ALL, fetch = LAZY)
    @JoinColumn(name="playlist_id")
    private Playlist playlist;


    public void edit(String content, Boolean isPublic){
        this.content = content;
        this.isPublic = isPublic;
    }

    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    public void adjustLikeCnt(long L){
        this.likeCnt += L;
    }

    public void addViewCnt(){
        this.viewCnt ++;
    }

    public void incLike()  { this.likeCnt++; }
    public void decLike()  { this.likeCnt--; }


    @Builder
    private Post(User user,
                 String content,
                 Boolean isPublic,
                 Playlist playlist
                 ) {
        this.user       = user;
        this.content    = content;
        this.isPublic   = isPublic;
        this.playlist  = playlist;
    }
}
