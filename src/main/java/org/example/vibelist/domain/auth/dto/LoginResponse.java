package org.example.vibelist.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 * 사용자 로그인 성공 시 반환되는 토큰 정보를 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /** 액세스 토큰과 관련 정보를 담은 객체 */
    private TokenResponse tokenResponse;
    
    /** 리프레시 토큰 (토큰 갱신 시 사용) */
    private String refreshToken;
} 