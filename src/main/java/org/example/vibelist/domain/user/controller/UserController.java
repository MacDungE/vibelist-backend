package org.example.vibelist.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.auth.service.AuthService;
import org.example.vibelist.domain.user.dto.CreateUserRequest;
import org.example.vibelist.domain.user.dto.UpdateUserProfileRequest;
import org.example.vibelist.domain.user.dto.UserDto;
import org.example.vibelist.domain.user.service.UserService;
import org.example.vibelist.global.security.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자 관리", description = "사용자 정보 및 프로필 관리 API")
public class UserController {
    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "사용자 생성", description = "새로운 사용자와 프로필을 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "사용자 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (중복된 사용자명 등)")
    })
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            UserDto createdUser = userService.createUserWithProfile(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "현재 사용자 정보 조회", description = "현재 인증된 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            
            UserDto userDto = userService.findUserDtoById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            return ResponseEntity.ok(userDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "사용자 정보 조회", description = "특정 사용자의 정보를 조회합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserInfo(
            @Parameter(description = "조회할 사용자 ID") @PathVariable Long userId) {
        try {
            UserDto userDto = userService.findUserDtoById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
            return ResponseEntity.ok(userDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "현재 사용자 프로필 업데이트", description = "현재 인증된 사용자의 프로필을 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 업데이트 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearer-key")
    @PutMapping("/me/profile")
    public ResponseEntity<?> updateCurrentUserProfile(@RequestBody UpdateUserProfileRequest request) {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            
            UserDto updatedUser = userService.updateUserProfile(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "사용자 프로필 업데이트", description = "특정 사용자의 프로필을 업데이트합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 업데이트 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearer-key")
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateUserProfile(
            @Parameter(description = "업데이트할 사용자 ID") @PathVariable Long userId, 
            @RequestBody UpdateUserProfileRequest request) {
        try {
            UserDto updatedUser = userService.updateUserProfile(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "사용자 목록 조회", description = "모든 사용자의 목록을 조회합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 목록 조회 성공")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "사용자 검색", description = "이름으로 사용자를 검색합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 검색 성공")
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @Parameter(description = "검색할 사용자 이름") @RequestParam String name) {
        List<UserDto> users = userService.searchUsersByName(name);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "현재 사용자 삭제", description = "현재 인증된 사용자를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUser() {
        try {
            Long userId = SecurityUtil.getCurrentUserId();
            
            authService.deleteUser(userId);
            return ResponseEntity.ok("사용자가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "사용자 삭제", description = "특정 사용자를 삭제합니다. (관리자용)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "사용자 삭제 성공"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @Parameter(description = "삭제할 사용자 ID") @PathVariable Long userId) {
        try {
            authService.deleteUser(userId);
            return ResponseEntity.ok("사용자가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 