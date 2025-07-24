package org.example.vibelist.domain.user.dto;

import lombok.*;

/**
 * 사용자 프로필 업데이트 요청 DTO
 * 사용자가 자신의 프로필 정보를 수정할 때 사용됩니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    /** 사용자명 (unique) */
    private String username;
    
    /** 사용자 실명 */
    private String name;
    
    /** 사용자 전화번호 */
    private String phone;
    
    /** 사용자 프로필 이미지 URL */
    private String avatarUrl;
    
    /** 사용자 자기소개 */
    private String bio;
} 