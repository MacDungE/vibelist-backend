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

ㅎ
    public /**
     * Retrieves the current {@link Authentication} object from the Spring Security context.
     *
     * @return the current authentication, or {@code null} if no authentication is present
     */
    static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static CustomUserDetails getCurrentUserDetails() {
        return (CustomUserDetails) getAuthentication().getPrincipal();
    }
    /**
     * Retrieves the ID of the currently authenticated user.
     *
     * Supports principals of type {@code CustomUserDetails} or {@code Long}. Throws {@code IllegalArgumentException} if the user is unauthenticated or the principal type is unsupported.
     *
     * @return the authenticated user's ID
     * @throws IllegalArgumentException if the user is unauthenticated or the principal type is not supported
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
     * Extracts the user ID from the provided Authentication object.
     *
     * Supports principals of type CustomUserDetails or Long. Throws IllegalArgumentException if the authentication is null or the principal type is unsupported.
     *
     * @param authentication the Authentication object from which to extract the user ID
     * @return the user ID associated with the authentication
     * @throws IllegalArgumentException if authentication is null or the principal type is unsupported
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