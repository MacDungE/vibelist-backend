package org.example.vibelist.global.integration.dto;

import lombok.*;

/**
 * 토큰 갱신 요청 DTO
 * 외부 서비스의 토큰을 갱신할 때 사용됩니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    /** 갱신할 서비스 제공자 (필수) */
    private String provider;
    
    /** 강제 갱신 여부 (선택사항) */
    private Boolean forceRefresh;
} 