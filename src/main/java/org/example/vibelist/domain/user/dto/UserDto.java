package org.example.vibelist.domain.user.dto;

import lombok.*;
import org.example.vibelist.global.constants.Role;

import java.time.LocalDateTime;

/**
 * 사용자 정보 응답 DTO
 * 사용자 프로필 조회 시 반환되는 사용자 정보를 포함합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    /** 사용자 고유 ID */
    private Long userId;
    
    /** 사용자명 */
    private String username;
    
    /** 사용자 역할 (USER, ADMIN 등) */
    private Role role;
    
    /** 사용자 이메일 주소 */
    private String email;
    
    /** 사용자 실명 */
    private String name;
    
    /** 사용자 전화번호 */
    private String phone;
    
    /** 사용자 프로필 이미지 URL */
    private String avatarUrl;
    
    /** 사용자 자기소개 */
    private String bio;
    
    /** 계정 생성 시간 */
    private LocalDateTime createdAt;
    
    /** 계정 정보 수정 시간 */
    private LocalDateTime updatedAt;
} 