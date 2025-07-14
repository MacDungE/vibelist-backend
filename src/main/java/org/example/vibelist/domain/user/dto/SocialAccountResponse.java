package org.example.vibelist.domain.user.dto;

import lombok.*;


import java.time.LocalDateTime;

/**
 * 소셜 계정 정보 응답 DTO
 * 사용자가 연동한 소셜 계정 정보를 조회할 때 반환됩니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccountResponse {
    /** 소셜 계정 연동 정보의 고유 ID */
    private Long id;
    
    /** 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER 등) */
    private String provider;
    
    /** 소셜 제공자에서 발급한 사용자 ID */
    private String providerUserId;
    
    /** 소셜 제공자에서 제공하는 이메일 주소 */
    private String providerEmail;
    
    /** 소셜 계정 연동 시간 */
    private LocalDateTime createdAt;
} 