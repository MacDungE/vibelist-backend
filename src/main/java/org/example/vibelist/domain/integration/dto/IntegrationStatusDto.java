package org.example.vibelist.domain.integration.dto;

import lombok.*;

/**
 * 외부 서비스 연동 상태 DTO
 * {provider: string, isIntegration: boolean} 형식으로 반환
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationStatusDto {
    /** 서비스 제공자 (spotify, google, kakao 등) */
    private String provider;
    
    /** 연동 여부 */
    private Boolean isIntegration;
}