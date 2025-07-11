package org.example.vibelist.global.integration.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.vibelist.global.jpa.entity.BaseTime;
import org.example.vibelist.global.user.entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 외부 서비스 연동 토큰 정보를 저장하는 엔티티
 * CustomAuthorizationCodeTokenResponseClient에서 받아온 토큰 정보를 저장
 * 
 * 외부 서비스(Spotify, Google, Apple Music 등) 연동을 위한 토큰 정보 관리
 */
@Entity
@Table(name = "integration_token_info",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "provider"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationTokenInfo extends BaseTime {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String provider; // SPOTIFY, GOOGLE, KAKAO, APPLE, YOUTUBE 등

    @Column(columnDefinition = "text", nullable = true)
    private String accessToken; // 암호화된 Access Token

    @Column(columnDefinition = "text", nullable = true)
    private String refreshToken; // 암호화된 Refresh Token

    // Map 타입으로 매핑 (자동 직렬화/역직렬화)
    @Column(name = "token_response", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> tokenResponse; // 토큰 응답 정보

    @Column(length = 20)
    private String tokenType; // Bearer

    @Column
    private Integer expiresIn; // 토큰 만료 시간 (초)

    @Column(columnDefinition = "text", nullable = true)
    private String scope; // 권한 범위

    @Column
    private LocalDateTime tokenIssuedAt; // 토큰 발급 시간

    @Column
    private LocalDateTime tokenExpiresAt; // 토큰 만료 시간

    @Column
    private Boolean isActive; // 토큰 활성 상태

    /**
     * Access Token 업데이트
     */
    public void updateAccessToken(String accessToken, Integer expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.tokenIssuedAt = LocalDateTime.now();
        this.tokenExpiresAt = expiresIn != null ? 
            LocalDateTime.now().plusSeconds(expiresIn) : null;
    }

    /**
     * Refresh Token 업데이트
     */
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * 전체 토큰 정보 업데이트
     */
    public void updateTokenInfo(String accessToken, String refreshToken, 
                               String tokenType, Integer expiresIn, String scope) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.scope = scope;
        this.tokenIssuedAt = LocalDateTime.now();
        this.tokenExpiresAt = expiresIn != null ? 
            LocalDateTime.now().plusSeconds(expiresIn) : null;
        this.isActive = true;
    }

    /**
     * 토큰 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isExpired() {
        return tokenExpiresAt != null && LocalDateTime.now().isAfter(tokenExpiresAt);
    }

    /**
     * 토큰 유효성 확인 (활성 상태이고 만료되지 않음)
     */
    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && !isExpired();
    }

    /**
     * tokenResponse 업데이트
     */
    public void updateTokenResponse(Map<String, Object> tokenResponse) {
        this.tokenResponse = tokenResponse;
    }

    /**
     * tokenResponse에 값 추가
     */
    public void addTokenResponseValue(String key, Object value) {
        if (this.tokenResponse == null) {
            this.tokenResponse = new java.util.HashMap<>();
        }
        this.tokenResponse.put(key, value);
    }
} 