package org.example.vibelist.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.auth.entity.Auth;
import org.example.vibelist.domain.auth.repository.AuthRepository;
import org.example.vibelist.global.constants.Role;

import org.example.vibelist.domain.user.dto.CreateUserRequest;
import org.example.vibelist.domain.user.dto.SocialAccountResponse;
import org.example.vibelist.domain.user.dto.UpdateUserProfileRequest;
import org.example.vibelist.domain.user.dto.UserDto;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.entity.UserProfile;
import org.example.vibelist.domain.user.repository.UserProfileRepository;
import org.example.vibelist.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.RsData;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthRepository authRepository;

    // User 관련 메소드 - 개선된 버전
    public Optional<User> findUserById(Long id) {
        return userRepository.findByIdWithProfile(id);
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsernameWithProfile(username);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public List<User> findUsersByRole(Role role) {
        return userRepository.findByRoleWithProfile(role);
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Builder 패턴을 사용한 User 생성 및 저장
     */
    @Transactional
    public User createUser(String username, String password, Role role) {
        User user = User.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // UserProfile 관련 메소드
    public Optional<UserProfile> findUserProfileById(Long userId) {
        return userProfileRepository.findById(userId);
    }

    public Optional<UserProfile> findUserProfileByEmail(String email) {
        return userProfileRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userProfileRepository.existsByEmail(email);
    }

    public Optional<UserProfile> findUserProfileByPhone(String phone) {
        return userProfileRepository.findByPhone(phone);
    }

    public boolean existsByPhone(String phone) {
        return userProfileRepository.existsByPhone(phone);
    }

    public List<UserProfile> findUserProfilesByName(String name) {
        return userProfileRepository.findByNameContaining(name);
    }

    @Transactional
    public UserProfile saveUserProfile(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    /**
     * Builder 패턴을 사용한 UserProfile 생성 및 저장
     */
    @Transactional
    public UserProfile createUserProfile(User user, String email, String name, String phone) {
        UserProfile userProfile = UserProfile.builder()
                .user(user)
                .email(email)
                .name(name)
                .phone(phone)
                .build();
        return userProfileRepository.save(userProfile);
    }

    @Transactional
    public void deleteUserProfile(Long userId) {
        userProfileRepository.deleteById(userId);
    }

    // Auth 관련 메소드 (소셜 계정 정보 조회용)
    public List<Auth> findAuthsByUserId(Long userId) {
        return authRepository.findByUserId(userId);
    }

    public Optional<Auth> findAuthByUserIdAndProvider(Long userId, String provider) {
        return authRepository.findByUserIdAndProvider(userId, provider);
    }

    // 복합 조회 메소드 - 개선된 버전
    public Optional<User> findUserWithProfile(Long userId) {
        return userRepository.findByIdWithProfile(userId);
    }

    // DTO 변환 메서드들
    public UserDto convertToUserDto(User user, UserProfile profile) {
        return UserDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .email(profile != null ? profile.getEmail() : null)
                .name(profile != null ? profile.getName() : null)
                .phone(profile != null ? profile.getPhone() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .bio(profile != null ? profile.getBio() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public SocialAccountResponse convertToSocialAccountResponse(Auth auth) {
        return SocialAccountResponse.builder()
                .id(auth.getId())
                .provider(auth.getProvider())
                .providerUserId(auth.getProviderUserId())
                .providerEmail(auth.getProviderEmail())
                .createdAt(auth.getCreatedAt())
                .build();
    }

    // DTO 기반 서비스 메서드들 - 개선된 버전
    public Optional<UserDto> findUserDtoById(Long userId) {
        Optional<User> user = userRepository.findByIdWithProfile(userId);
        if (user.isPresent()) {
            UserProfile profile = user.get().getUserProfile();
            return Optional.of(convertToUserDto(user.get(), profile));
        }
        return Optional.empty();
    }

    public List<UserDto> searchUsersByName(String name) {
        List<UserProfile> profiles = userProfileRepository.findByNameContaining(name);
        return profiles.stream()
                .map(profile -> {
                    User user = profile.getUser();
                    return convertToUserDto(user, profile);
                })
                .collect(Collectors.toList());
    }

    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAllWithProfile();
        return users.stream()
                .map(user -> convertToUserDto(user, user.getUserProfile()))
                .collect(Collectors.toList());
    }

    public List<SocialAccountResponse> findUserSocialAccounts(Long userId) {
        List<Auth> socialAccounts = authRepository.findByUserId(userId);
        return socialAccounts.stream()
                .map(this::convertToSocialAccountResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RsData<UserDto> getUser(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ResponseCode.USER_NOT_FOUND, "userId=" + userId + "인 사용자를 찾을 수 없습니다."));
            return RsData.success(ResponseCode.USER_FOUND, convertToUserDto(user, user.getUserProfile()));
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "사용자 조회 중 오류: " + e.getMessage());
        }
    }

    @Transactional
    public RsData<UserDto> createUserWithProfile(CreateUserRequest request) {
        try {
            if (existsByUsername(request.getUsername())) {
                throw new GlobalException(ResponseCode.USER_ALREADY_EXISTS, "username='" + request.getUsername() + "'은 이미 존재합니다.");
            }
            if (existsByEmail(request.getEmail())) {
                throw new GlobalException(ResponseCode.USER_ALREADY_EXISTS, "email='" + request.getEmail() + "'은 이미 존재합니다.");
            }
            User user = createUser(request.getUsername(), request.getPassword(), request.getRole());
            UserProfile profile = createUserProfile(user, request.getEmail(), request.getName(), request.getPhone());
            return RsData.success(ResponseCode.USER_CREATED, convertToUserDto(user, profile));
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "사용자 생성 중 오류: " + e.getMessage());
        }
    }

    @Transactional
    public UserDto updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        Optional<User> userOpt = userRepository.findByIdWithProfile(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UserProfile profile = user.getUserProfile();
            
            // username 업데이트 로직 (중복 체크 포함)
            if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new GlobalException(ResponseCode.USER_ALREADY_EXISTS, 
                        "username='" + request.getUsername() + "'은 이미 존재합니다.");
                }
                user.updateUsername(request.getUsername());
            }
            
            // 프로필 업데이트 로직
            if (request.getName() != null) {
                profile.setName(request.getName());
            }
            if (request.getPhone() != null) {
                profile.setPhone(request.getPhone());
            }
            if (request.getAvatarUrl() != null) {
                profile.setAvatarUrl(request.getAvatarUrl());
            }
            if (request.getBio() != null) {
                profile.setBio(request.getBio());
            }
            
            User updatedUser = userRepository.save(user);
            UserProfile updatedProfile = userProfileRepository.save(profile);
            return convertToUserDto(updatedUser, updatedProfile);
        }
        throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId);
    }

    // 새로운 개선된 메서드들
    /**
     * 사용자명으로 UserDto 조회 (Profile 포함)
     */
    public Optional<UserDto> findUserDtoByUsername(String username) {
        Optional<User> user = userRepository.findByUsernameWithProfile(username);
        if (user.isPresent()) {
            UserProfile profile = user.get().getUserProfile();
            return Optional.of(convertToUserDto(user.get(), profile));
        }
        return Optional.empty();
    }

    /**
     * 역할별 UserDto 목록 조회 (Profile 포함)
     */
    public List<UserDto> findUserDtosByRole(Role role) {
        List<User> users = userRepository.findByRoleWithProfile(role);
        return users.stream()
                .map(user -> convertToUserDto(user, user.getUserProfile()))
                .collect(Collectors.toList());
    }

    /**
     * 사용자와 프로필을 함께 조회하여 UserDto로 반환
     */
    public Optional<UserDto> findUserWithProfileAsDto(Long userId) {
        return findUserDtoById(userId);
    }

    /**
     * 모든 사용자를 UserDto 목록으로 조회 (Profile 포함)
     */
    public List<UserDto> getAllUserDtos() {
        return getAllUsers();
    }
}
