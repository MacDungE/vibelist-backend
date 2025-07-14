package org.example.vibelist.domain.integration.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 외부 서비스 연동 토큰 정보 응답 DTO
 * 클라이언트에게 토큰 정보 조회 시 반환되는 데이터를 포함합니다.
 * (보안상 민감한 토큰 값은 제외하고 메타데이터만 포함)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationTokenResponse {
    /** 토큰 정보 ID */
    private Long id;
    
    /** 외부 서비스 제공자 (SPOTIFY, GOOGLE, KAKAO 등) */
    private String provider;
    
    /** 토큰 타입 (Bearer) */
    private String tokenType;
    
    /** 토큰 만료 시간 (초) */
    private Integer expiresIn;
    
    /** 권한 범위 */
    private String scope;
    
    /** 토큰 발급 시간 */
    private LocalDateTime tokenIssuedAt;
    
    /** 토큰 만료 시간 */
    private LocalDateTime tokenExpiresAt;
    
    /** 토큰 활성 상태 */
    private Boolean isActive;
    
    /** 토큰 유효성 (활성 상태이고 만료되지 않음) */
    private Boolean isValid;
    
    /** 토큰 만료 여부 */
    private Boolean isExpired;
    
    /** 추가 토큰 정보 (민감하지 않은 메타데이터만) */
    private Map<String, Object> additionalInfo;
    
    /** 토큰 생성 시간 */
    private LocalDateTime createdAt;
    
    /** 토큰 수정 시간 */
    private LocalDateTime updatedAt;
} 