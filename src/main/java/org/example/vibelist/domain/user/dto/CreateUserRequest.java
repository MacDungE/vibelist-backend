package org.example.vibelist.domain.user.dto;

import lombok.*;
import org.example.vibelist.global.constants.Role;

/**
 * 사용자 생성 요청 DTO
 * 새로운 사용자 계정을 생성할 때 필요한 정보를 포함합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    /** 사용자명 (고유해야 함) */
    private String username;
    
    /** 사용자 비밀번호 */
    private String password;
    
    /** 사용자 이메일 주소 */
    private String email;
    
    /** 사용자 실명 */
    private String name;
    
    /** 사용자 전화번호 */
    private String phone;
    
    /** 사용자 역할 (기본값: USER) */
    private Role role = Role.USER; // 기본값 설정
} 