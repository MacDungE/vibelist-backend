package org.example.vibelist.domain.integration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.integration.entity.IntegrationTokenInfo;
import org.example.vibelist.domain.integration.repository.IntegrationTokenInfoRepository;
import org.example.vibelist.domain.oauth2.dto.TokenInfo;
import org.example.vibelist.domain.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 외부 서비스 연동을 위한 토큰 정보 관리 서비스
 * Spotify, Google, Apple Music 등 다양한 서비스의 토큰을 통합 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationTokenInfoService {

    private final IntegrationTokenInfoRepository tokenInfoRepository;
    private final ObjectMapper objectMapper;

    /**
     * 토큰 정보 저장 (신규 생성 또는 업데이트) - TokenInfo 기반
     */
    @Transactional
    public IntegrationTokenInfo saveOrUpdateTokenInfo(User user, String provider, TokenInfo tokenInfo) {
        log.info("[INTEGRATION_TOKEN] 토큰 정보 저장/업데이트 - userId: {}, provider: {}", user.getId(), provider);

        Optional<IntegrationTokenInfo> existingTokenOpt = tokenInfoRepository.findByUserIdAndProvider(user.getId(), provider);

        IntegrationTokenInfo integrationToken;
        if (existingTokenOpt.isPresent()) {
            // 기존 토큰 정보 업데이트
            integrationToken = existingTokenOpt.get();
            integrationToken.updateTokenInfo(
                tokenInfo.getAccessToken(), 
                tokenInfo.getRefreshToken(), 
                tokenInfo.getTokenType(), 
                tokenInfo.getExpiresIn(), 
                tokenInfo.getScope()
            );
            if (tokenInfo.getAdditionalParameters() != null) {
                updateTokenResponse(integrationToken, tokenInfo.getAdditionalParameters());
            }
            log.info("[INTEGRATION_TOKEN] 기존 토큰 정보 업데이트 - tokenId: {}", integrationToken.getId());
        } else {
            // 새로운 토큰 정보 생성
            integrationToken = IntegrationTokenInfo.builder()
                    .user(user)
                    .provider(provider.toUpperCase())
                    .accessToken(tokenInfo.getAccessToken())
                    .refreshToken(tokenInfo.getRefreshToken())
                    .tokenType(tokenInfo.getTokenType())
                    .expiresIn(tokenInfo.getExpiresIn())
                    .scope(tokenInfo.getScope())
                    .tokenResponse(tokenInfo.getAdditionalParameters())
                    .isActive(true)
                    .build();
            
            // 발급 시간과 만료 시간 설정을 위해 업데이트 메서드 호출
            integrationToken.updateTokenInfo(
                tokenInfo.getAccessToken(), 
                tokenInfo.getRefreshToken(), 
                tokenInfo.getTokenType(), 
                tokenInfo.getExpiresIn(), 
                tokenInfo.getScope()
            );
            log.info("[INTEGRATION_TOKEN] 새로운 토큰 정보 생성 - provider: {}", provider);
        }

        IntegrationTokenInfo savedToken = tokenInfoRepository.save(integrationToken);
        log.info("[INTEGRATION_TOKEN] 토큰 정보 저장 완료 - tokenId: {}, isValid: {}", 
                savedToken.getId(), savedToken.isValid());

        return savedToken;
    }
    
    /**
     * 토큰 정보 저장 (레거시 메서드 - 하위 호환성 유지)
     */
    @Transactional
    public IntegrationTokenInfo saveOrUpdateTokenInfo(User user, String provider, 
                                                String accessToken, String refreshToken, 
                                                String tokenType, Integer expiresIn, String scope) {
        TokenInfo tokenInfo = TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .scope(scope)
                .build();
                
        return saveOrUpdateTokenInfo(user, provider, tokenInfo);
    }

    /**
     * 토큰 정보 저장 (tokenResponse 포함 - 레거시 메서드)
     */
    @Transactional
    public IntegrationTokenInfo saveOrUpdateTokenInfo(User user, String provider, 
                                                String accessToken, String refreshToken, 
                                                String tokenType, Integer expiresIn, String scope,
                                                Map<String, Object> tokenResponse) {
        TokenInfo tokenInfo = TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .expiresIn(expiresIn)
                .scope(scope)
                .additionalParameters(tokenResponse)
                .build();
                
        return saveOrUpdateTokenInfo(user, provider, tokenInfo);
    }

    /**
     * Access Token만 업데이트
     */
    @Transactional
    public Optional<IntegrationTokenInfo> updateAccessToken(Long userId, String provider, 
                                                      String accessToken, Integer expiresIn) {
        log.info("[INTEGRATION_TOKEN] Access Token 업데이트 - userId: {}, provider: {}", userId, provider);

        Optional<IntegrationTokenInfo> tokenOpt = tokenInfoRepository.findByUserIdAndProvider(userId, provider);
        
        if (tokenOpt.isPresent()) {
            IntegrationTokenInfo tokenInfo = tokenOpt.get();
            tokenInfo.updateAccessToken(accessToken, expiresIn);
            IntegrationTokenInfo savedToken = tokenInfoRepository.save(tokenInfo);
            log.info("[INTEGRATION_TOKEN] Access Token 업데이트 완료 - tokenId: {}", savedToken.getId());
            return Optional.of(savedToken);
        } else {
            log.warn("[INTEGRATION_TOKEN] 토큰 정보를 찾을 수 없음 - userId: {}, provider: {}", userId, provider);
            return Optional.empty();
        }
    }

    /**
     * 유효한 토큰 정보 조회
     */
    @Transactional(readOnly = true)
    public Optional<IntegrationTokenInfo> getValidTokenInfo(Long userId, String provider) {
        return tokenInfoRepository.findValidTokenByUserIdAndProvider(userId, provider.toUpperCase());
    }

    /**
     * 토큰 정보 조회 (활성 상태만)
     */
    @Transactional(readOnly = true)
    public Optional<IntegrationTokenInfo> getActiveTokenInfo(Long userId, String provider) {
        return tokenInfoRepository.findActiveTokenByUserIdAndProvider(userId, provider.toUpperCase());
    }

    /**
     * 사용자의 모든 활성 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> getUserActiveTokens(Long userId) {
        return tokenInfoRepository.findActiveTokensByUserId(userId);
    }

    /**
     * 토큰 비활성화
     */
    @Transactional
    public void deactivateToken(Long userId, String provider) {
        log.info("[INTEGRATION_TOKEN] 토큰 비활성화 - userId: {}, provider: {}", userId, provider);

        Optional<IntegrationTokenInfo> tokenOpt = tokenInfoRepository.findByUserIdAndProvider(userId, provider.toUpperCase());
        
        if (tokenOpt.isPresent()) {
            IntegrationTokenInfo tokenInfo = tokenOpt.get();
            tokenInfo.deactivate();
            tokenInfoRepository.save(tokenInfo);
            log.info("[INTEGRATION_TOKEN] 토큰 비활성화 완료 - tokenId: {}", tokenInfo.getId());
        } else {
            log.warn("[INTEGRATION_TOKEN] 비활성화할 토큰을 찾을 수 없음 - userId: {}, provider: {}", userId, provider);
        }
    }

    /**
     * 만료된 토큰들 정리
     */
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("[INTEGRATION_TOKEN] 만료된 토큰 정리 시작");

        List<IntegrationTokenInfo> expiredTokens = tokenInfoRepository.findExpiredTokens();
        
        for (IntegrationTokenInfo token : expiredTokens) {
            if (Boolean.TRUE.equals(token.getIsActive())) {
                token.deactivate();
                log.debug("[INTEGRATION_TOKEN] 만료된 토큰 비활성화 - tokenId: {}, provider: {}", 
                        token.getId(), token.getProvider());
            }
        }
        
        if (!expiredTokens.isEmpty()) {
            tokenInfoRepository.saveAll(expiredTokens);
            log.info("[INTEGRATION_TOKEN] 만료된 토큰 정리 완료 - 처리된 토큰 수: {}", expiredTokens.size());
        } else {
            log.info("[INTEGRATION_TOKEN] 정리할 만료된 토큰이 없습니다");
        }
    }

    /**
     * 토큰 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasToken(Long userId, String provider) {
        return tokenInfoRepository.existsByUserIdAndProvider(userId, provider.toUpperCase());
    }

    /**
     * tokenResponse 업데이트
     */
    @Transactional
    public void updateTokenResponse(Long userId, String provider, Map<String, Object> tokenResponse) {
        log.info("[INTEGRATION_TOKEN] tokenResponse 업데이트 - userId: {}, provider: {}", userId, provider);

        Optional<IntegrationTokenInfo> tokenOpt = tokenInfoRepository.findByUserIdAndProvider(userId, provider.toUpperCase());
        
        if (tokenOpt.isPresent()) {
            IntegrationTokenInfo tokenInfo = tokenOpt.get();
            updateTokenResponse(tokenInfo, tokenResponse);
            tokenInfoRepository.save(tokenInfo);
            log.info("[INTEGRATION_TOKEN] tokenResponse 업데이트 완료 - tokenId: {}", tokenInfo.getId());
        } else {
            log.warn("[INTEGRATION_TOKEN] tokenResponse 업데이트할 토큰을 찾을 수 없음 - userId: {}, provider: {}", userId, provider);
        }
    }

    /**
     * tokenResponse에서 특정 값 조회
     */
    @Transactional(readOnly = true)
    public Optional<Object> getTokenResponseValue(Long userId, String provider, String key) {
        Optional<IntegrationTokenInfo> tokenOpt = tokenInfoRepository.findByUserIdAndProvider(userId, provider.toUpperCase());
        
        if (tokenOpt.isPresent() && tokenOpt.get().getTokenResponse() != null) {
            return Optional.ofNullable(tokenOpt.get().getTokenResponse().get(key));
        }
        
        return Optional.empty();
    }

    /**
     * tokenResponse 전체 조회
     */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getTokenResponse(Long userId, String provider) {
        Optional<IntegrationTokenInfo> tokenOpt = tokenInfoRepository.findByUserIdAndProvider(userId, provider.toUpperCase());
        
        if (tokenOpt.isPresent()) {
            return Optional.ofNullable(tokenOpt.get().getTokenResponse());
        }
        
        return Optional.empty();
    }

    /**
     * tokenResponse 필터링으로 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findTokensByResponseFilter(Map<String, Object> filter) {
        try {
            String jsonFilter = objectMapper.writeValueAsString(filter);
            return tokenInfoRepository.findByTokenResponseFilter(jsonFilter);
        } catch (Exception e) {
            log.error("[INTEGRATION_TOKEN] JSON 필터 변환 실패", e);
            return List.of();
        }
    }

    /**
     * 사용자별 tokenResponse 필터링으로 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findUserTokensByResponseFilter(Long userId, Map<String, Object> filter) {
        try {
            String jsonFilter = objectMapper.writeValueAsString(filter);
            return tokenInfoRepository.findByUserIdAndTokenResponseFilter(userId, jsonFilter);
        } catch (Exception e) {
            log.error("[INTEGRATION_TOKEN] 사용자별 JSON 필터 변환 실패 - userId: {}", userId, e);
            return List.of();
        }
    }

    /**
     * Provider별 tokenResponse 필터링으로 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findProviderTokensByResponseFilter(String provider, Map<String, Object> filter) {
        try {
            String jsonFilter = objectMapper.writeValueAsString(filter);
            return tokenInfoRepository.findByProviderAndTokenResponseFilter(provider.toUpperCase(), jsonFilter);
        } catch (Exception e) {
            log.error("[INTEGRATION_TOKEN] Provider별 JSON 필터 변환 실패 - provider: {}", provider, e);
            return List.of();
        }
    }

    /**
     * tokenResponse에서 특정 키가 존재하는 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findTokensByResponseKeyExists(String key) {
        return tokenInfoRepository.findByTokenResponseKeyExists(key);
    }

    /**
     * tokenResponse에서 특정 키-값 쌍을 가진 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findTokensByResponseKeyValue(String key, String value) {
        return tokenInfoRepository.findByTokenResponseKeyValue(key, value);
    }

    /**
     * 사용자의 특정 Provider 토큰에서 키 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasTokenResponseKey(Long userId, String provider, String key) {
        return tokenInfoRepository.findByUserIdAndProviderAndTokenResponseKeyExists(userId, provider.toUpperCase(), key).isPresent();
    }

    /**
     * 특정 scope를 가진 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findTokensByScope(String scope) {
        return tokenInfoRepository.findByTokenResponseKeyValue("scope", scope);
    }

    /**
     * 사용자별 특정 scope를 가진 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findUserTokensByScope(Long userId, String scope) {
        Map<String, Object> filter = Map.of("scope", scope);
        return findUserTokensByResponseFilter(userId, filter);
    }

    /**
     * 특정 권한(permission)을 가진 토큰 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findTokensByPermission(String permission) {
        // scope에 특정 권한이 포함된 토큰 조회 (LIKE 검색)
        return tokenInfoRepository.findByTokenResponseKeyValue("scope", "%" + permission + "%");
    }

    /**
     * 복잡한 JSON 경로로 토큰 조회 (중첩 JSON 지원)
     * @param jsonPath PostgreSQL JSON 경로 형식 (예: '{"user", "profile", "email"}')
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenInfo> findTokensByJsonPath(String jsonPath) {
        return tokenInfoRepository.findByTokenResponsePathExists(jsonPath);
    }

    /**
     * 내부 헬퍼 메서드: tokenResponse 업데이트
     */
    private void updateTokenResponse(IntegrationTokenInfo tokenInfo, Map<String, Object> tokenResponse) {
        if (tokenResponse != null) {
            // 기존 tokenResponse가 있으면 병합, 없으면 새로 설정
            if (tokenInfo.getTokenResponse() != null) {
                tokenInfo.getTokenResponse().putAll(tokenResponse);
            } else {
                tokenInfo.updateTokenResponse(tokenResponse);
            }
        }
    }
} 