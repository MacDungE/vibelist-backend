package org.example.vibelist.global.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT 토큰 응답 DTO
 * 인증 성공 시 발급되는 액세스 토큰과 관련 정보를 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {
    /** JWT 액세스 토큰 */
    private String accessToken;
    
    /** 토큰 타입 (일반적으로 "Bearer") */
    private String tokenType;
    
    /** 토큰 만료 시간 (초 단위) */
    private Long expiresIn;
    
    /** 사용자 ID */
    private Long userId;
    
    /** 사용자명 */
    private String username;
    
    /** 사용자 역할 (예: USER, ADMIN) */
    private String role;
} 