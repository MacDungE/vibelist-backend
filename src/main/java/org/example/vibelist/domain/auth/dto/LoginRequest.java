package org.example.vibelist.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    /** 사용자명 */
    private String username;
    
    /** 비밀번호 */
    private String password;
}