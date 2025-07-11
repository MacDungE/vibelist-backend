package org.example.vibelist.global.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.global.auth.service.AuthService;
import org.example.vibelist.global.user.dto.CreateUserRequest;
import org.example.vibelist.global.user.dto.UpdateUserProfileRequest;
import org.example.vibelist.global.user.dto.UserDto;
import org.example.vibelist.global.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserService userService;

    // 사용자 생성
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            UserDto createdUser = userService.createUserWithProfile(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 현재 인증된 사용자 정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            UserDto userDto = userService.findUserDtoById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            return ResponseEntity.ok(userDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 사용자 정보 조회 (관리자용)
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserInfo(@PathVariable Long userId) {
        try {
            UserDto userDto = userService.findUserDtoById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            return ResponseEntity.ok(userDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 현재 사용자 프로필 업데이트
    @PutMapping("/me/profile")
    public ResponseEntity<?> updateCurrentUserProfile(@RequestBody UpdateUserProfileRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            UserDto updatedUser = userService.updateUserProfile(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 사용자 프로필 업데이트 (관리자용)
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateUserProfile(@PathVariable Long userId, @RequestBody UpdateUserProfileRequest request) {
        try {
            UserDto updatedUser = userService.updateUserProfile(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 사용자 목록 조회 (관리자용)
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // 사용자 검색
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String name) {
        List<UserDto> users = userService.searchUsersByName(name);
        return ResponseEntity.ok(users);
    }

    // 현재 사용자 삭제
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = (Long) authentication.getPrincipal();
            
            authService.deleteUser(userId);
            return ResponseEntity.ok("사용자가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 사용자 삭제 (관리자용)
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            authService.deleteUser(userId);
            return ResponseEntity.ok("사용자가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 