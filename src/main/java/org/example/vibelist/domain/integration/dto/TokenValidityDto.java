package org.example.vibelist.domain.integration.dto;

import lombok.*;

/**
 * 토큰 유효성 확인 응답 DTO
 * {isValid: boolean} 형식으로 반환
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidityDto {
    /** 토큰 유효성 여부 */
    private Boolean isValid;
}