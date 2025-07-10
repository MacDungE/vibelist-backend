package org.example.vibelist.global.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.vibelist.global.jpa.entity.BaseTime;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseTime {
    

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name= "user_id")
    private User user;

    private String email;
    private String name;
    private String phone;
    private String avatarUrl;
    private String bio;
    private String locale = "ko";
}
