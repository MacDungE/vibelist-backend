package org.example.vibelist.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.vibelist.domain.auth.entity.Auth;
import org.example.vibelist.global.constants.Role;
import org.example.vibelist.global.jpa.entity.BaseTime;

@Entity
@Table(name="user")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@NamedEntityGraph(
    name = "User.withProfile",
    attributeNodes = @NamedAttributeNode("userProfile")
)
public class User extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, length = 60)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile userProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Auth auth;

    /**
     * 사용자명 업데이트
     */
    public void updateUsername(String newUsername) {
        this.username = newUsername;
    }
}
