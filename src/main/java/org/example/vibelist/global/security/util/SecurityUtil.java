package org.example.vibelist.global.security.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.vibelist.global.security.core.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 보안 관련 유틸리티 클래스
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtil {


    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static CustomUserDetails getCurrentUserDetails() {
        return (CustomUserDetails) getAuthentication().getPrincipal();
    }
    /**
     * 현재 인증된 사용자의 ID를 가져옵니다.
     * Authentication 객체의 Principal이 CustomUserDetails 또는 Long 타입인 경우를 처리합니다.
     *
     * @return 사용자 ID
     * @throws IllegalArgumentException 인증되지 않은 사용자이거나 Principal 타입이 지원되지 않는 경우
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityUtil.getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        } else if (authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        } else {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
    }

    /**
     * 인증 객체에서 사용자 ID를 추출합니다.
     *
     * @param authentication 인증 객체
     * @return 사용자 ID
     * @throws IllegalArgumentException Principal 타입이 지원되지 않는 경우
     */
    public static Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("인증 객체가 null입니다.");
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        } else if (authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        } else {
            throw new IllegalArgumentException("지원되지 않는 Principal 타입입니다: " +
                    (authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null"));
        }
    }
} 