package org.example.vibelist.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT 토큰 응답 DTO
 * 인증 성공 시 발급되는 액세스 토큰 정보를 포함합니다.
 * 클라이언트가 Authorization 헤더에 사용할 수 있는 형태로 제공됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    /** 토큰 타입 - 항상 "Bearer" */
    private String tokenType;
    
    /** JWT 액세스 토큰 */
    private String accessToken;
    
    /**
     * 기본 생성자에서 tokenType을 "Bearer"로 설정
     */
    public TokenResponse(String accessToken) {
        this.tokenType = "Bearer";
        this.accessToken = accessToken;
    }
    
    /**
     * Builder 패턴 사용 시 tokenType이 설정되지 않으면 기본값 "Bearer" 설정
     */
    public static class TokenResponseBuilder {
        public TokenResponse build() {
            if (this.tokenType == null) {
                this.tokenType = "Bearer";
            }
            return new TokenResponse(this.tokenType, this.accessToken);
        }
    }
} 