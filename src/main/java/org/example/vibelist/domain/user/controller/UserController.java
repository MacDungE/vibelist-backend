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
import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.security.util.SecurityUtil;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.example.vibelist.global.security.core.CustomUserDetails;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "사용자 관리", description = "사용자 정보 및 프로필 관리 API")
public class UserController {
    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "사용자 생성", description = "새로운 사용자와 프로필을 생성합니다.")
    @PostMapping
    public ResponseEntity<RsData<?>> createUser(@RequestBody CreateUserRequest request) {
        RsData<?> result = userService.createUserWithProfile(request);
        return ResponseEntity.status(result.isSuccess() ? 201 : 400).body(result);
    }

    @Operation(summary = "현재 사용자 정보 조회", description = "현재 인증된 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserInfo(@AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        UserDto userDto = userService.findUserDtoById(userDetail.getId())
                .orElseThrow(() -> new GlobalException(ResponseCode.USER_NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));
        return ResponseEntity.ok(userDto);
    }

    @Operation(summary = "사용자 정보 조회", description = "특정 사용자의 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<RsData<?>> getUser(@PathVariable Long userId) {
        RsData<?> result = userService.getUser(userId);
        return ResponseEntity.status(result.isSuccess() ? 200 : 404).body(result);
    }

    @Operation(summary = "현재 사용자 프로필 업데이트", description = "현재 인증된 사용자의 프로필을 업데이트합니다.")
    @SecurityRequirement(name = "bearer-key")
    @PutMapping("/me/profile")
    public ResponseEntity<?> updateCurrentUserProfile(@AuthenticationPrincipal CustomUserDetails userDetail, @RequestBody UpdateUserProfileRequest request) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        UserDto updatedUser = userService.updateUserProfile(userDetail.getId(), request);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(summary = "사용자 프로필 업데이트", description = "특정 사용자의 프로필을 업데이트합니다. (관리자용)")
    @SecurityRequirement(name = "bearer-key")
    @PutMapping("/{userId}/profile")
    public ResponseEntity<?> updateUserProfile(
            @Parameter(description = "업데이트할 사용자 ID") @PathVariable Long userId, 
            @RequestBody UpdateUserProfileRequest request) {
        try {
            UserDto updatedUser = userService.updateUserProfile(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            throw new GlobalException(ResponseCode.USER_NOT_FOUND, e.getMessage());
        }
    }

    @Operation(summary = "사용자 목록 조회", description = "모든 사용자의 목록을 조회합니다. (관리자용)")
    @SecurityRequirement(name = "bearer-key")
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "사용자 검색", description = "이름으로 사용자를 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @Parameter(description = "검색할 사용자 이름") @RequestParam String name) {
        List<UserDto> users = userService.searchUsersByName(name);
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "현재 사용자 삭제", description = "현재 인증된 사용자를 삭제합니다.")
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetail) {
        if (userDetail == null) throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        authService.deleteUser(userDetail.getId());
        return ResponseEntity.ok("사용자가 삭제되었습니다.");
    }

    @Operation(summary = "사용자 삭제", description = "특정 사용자를 삭제합니다. (관리자용)")
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @Parameter(description = "삭제할 사용자 ID") @PathVariable Long userId) {
        try {
            authService.deleteUser(userId);
            return ResponseEntity.ok("사용자가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            throw new GlobalException(ResponseCode.USER_NOT_FOUND, e.getMessage());
        }
    }
} 