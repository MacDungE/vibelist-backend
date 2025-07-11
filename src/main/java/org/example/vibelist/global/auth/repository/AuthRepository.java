package org.example.vibelist.global.auth.repository;

import org.example.vibelist.global.auth.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {
    
    /**
     * 소셜 제공자와 제공자 사용자 ID로 소셜 계정 조회
     */
    Optional<Auth> findByProviderAndProviderUserId(String provider, String providerUserId);
    
    /**
     * 소셜 제공자와 제공자 이메일로 소셜 계정 조회
     */
    Optional<Auth> findByProviderAndProviderEmail(String provider, String providerEmail);
    
    /**
     * 사용자 ID로 소셜 계정 목록 조회
     */
    List<Auth> findByUserId(Long userId);
    
    /**
     * 소셜 제공자별 소셜 계정 목록 조회
     */
    List<Auth> findByProvider(String provider);
    
    /**
     * 특정 소셜 제공자 계정 존재 여부 확인
     */
    boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
    
    /**
     * 사용자 ID와 소셜 제공자로 소셜 계정 조회
     */
    Optional<Auth> findByUserIdAndProvider(Long userId, String provider);
    
    /**
     * 사용자 ID로 일반 로그인 계정 조회 (provider가 null인 경우)
     */
    Optional<Auth> findByUserIdAndProviderIsNull(Long userId);
} 