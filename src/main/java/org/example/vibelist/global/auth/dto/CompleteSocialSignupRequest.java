package org.example.vibelist.global.auth.dto;

import lombok.*;

/**
 * 소셜 회원가입 완료 요청 DTO
 * 소셜 로그인 후 추가 정보 입력을 통해 회원가입을 완료할 때 사용됩니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSocialSignupRequest {
    /** 사용자가 설정할 사용자명 */
    private String username;
    
    /** 소셜 로그인 제공자 (예: GOOGLE, KAKAO, NAVER) */
    private String provider;
    
    /** 소셜 제공자로부터 받은 사용자 ID */
    private String userId;
} 