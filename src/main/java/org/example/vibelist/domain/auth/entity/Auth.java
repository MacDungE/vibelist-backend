package org.example.vibelist.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.vibelist.global.jpa.entity.BaseTime;
import org.example.vibelist.domain.user.entity.User;

@Entity
@Table(name = "auth",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerUserId"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auth extends BaseTime {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String provider;  // null이면 일반 로그인, SocialProviderConstants 상수들이면 소셜 로그인

    private String providerUserId;    // 소셜 로그인시에만 사용
    private String providerEmail;     // 소셜 로그인시에만 사용
    
    // 소셜 로그인 토큰 정보 (소셜 로그인시에만 사용)
    // 일반 로그인은 JWT 토큰을 메모리에서만 관리하므로 이 필드들은 소셜 로그인 전용
    @Column(columnDefinition = "text", nullable = true)
    private String refreshTokenEnc;   // 암호화된 소셜 refresh token

    private String tokenType;         // "Bearer" 등

    /**
     * 일반 로그인용 생성자
     */
    public Auth(User user, String refreshToken, String tokenType) {
        this.user = user;
        this.provider = null; // 일반 로그인
        this.refreshTokenEnc = refreshToken;
        this.tokenType = tokenType;
    }

    /**
     * 소셜 로그인용 생성자
     */
    public Auth(User user, String provider, String providerUserId, 
                     String providerEmail, String refreshToken) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.refreshTokenEnc = refreshToken;
        this.tokenType = "Bearer";
    }

    /**
     * Refresh Token 업데이트
     */
    public void updateRefreshToken(String refreshToken) {
        this.refreshTokenEnc = refreshToken;
    }

    /**
     * 일반 로그인 여부 확인
     */
    public boolean isRegularLogin() {
        return this.provider == null;
    }

    /**
     * 소셜 로그인 여부 확인
     */
    public boolean isSocialLogin() {
        return this.provider != null;
    }
} 