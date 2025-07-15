package org.example.vibelist.domain.integration.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 외부 서비스 연동 상태 응답 DTO
 * 사용자의 전체 연동 상태를 확인할 때 반환되는 데이터를 포함합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationStatusResponse {
    /** 사용자 ID */
    private Long userId;
    
    /** 연동된 서비스 총 개수 */
    private Integer totalIntegrations;
    
    /** 활성화된 연동 개수 */
    private Integer activeIntegrations;
    
    /** 만료된 연동 개수 */
    private Integer expiredIntegrations;
    
    /** 연동된 서비스 목록 */
    private List<IntegrationSummary> integrations;
    
    /** 상태 조회 시간 */
    private LocalDateTime statusCheckedAt;
    
    /**
     * 연동 서비스 요약 정보
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntegrationSummary {
        /** 서비스 제공자 */
        private String provider;
        
        /** 연동 상태 (ACTIVE, EXPIRED, INACTIVE) */
        private String status;
        
        /** 토큰 유효성 */
        private Boolean isValid;
        
        /** 마지막 토큰 갱신 시간 */
        private LocalDateTime lastUpdated;
        
        /** 토큰 만료 시간 */
        private LocalDateTime expiresAt;
        
        /** 권한 범위 */
        private String scope;
    }
} 