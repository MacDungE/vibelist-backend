package org.example.vibelist.global.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 사용자 인증 상태를 나타내는 응답 DTO
 * 소셜 로그인 상태 확인 시 사용되며, 인증 여부와 사용자 정보를 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusResponse {
    /** 사용자 인증 여부 */
    private boolean authenticated;
    
    /** 소셜 로그인 제공자 (예: GOOGLE, KAKAO, NAVER) */
    private String provider;
    
    /** 사용자 이메일 주소 */
    private String email;
    
    /** 사용자 이름 */
    private String name;
    
    /**
     * 미인증 상태의 응답을 생성하는 정적 메서드
     * @return 인증되지 않은 상태의 StatusResponse 객체
     */
    public static StatusResponse unauthenticated() {
        return StatusResponse.builder()
                .authenticated(false)
                .build();
    }
    
    /**
     * 인증된 상태의 응답을 생성하는 정적 메서드
     * @param provider 소셜 로그인 제공자
     * @param email 사용자 이메일
     * @param name 사용자 이름
     * @return 인증된 상태의 StatusResponse 객체
     */
    public static StatusResponse authenticated(String provider, String email, String name) {
        return StatusResponse.builder()
                .authenticated(true)
                .provider(provider)
                .email(email)
                .name(name)
                .build();
    }
} 