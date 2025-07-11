package org.example.vibelist.global.integration.repository;

import org.example.vibelist.global.integration.entity.IntegrationTokenInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 외부 서비스 연동을 위한 토큰 정보 Repository
 * Spotify, Google, Apple Music 등 다양한 서비스의 토큰을 관리
 */
@Repository
public interface IntegrationTokenInfoRepository extends JpaRepository<IntegrationTokenInfo, Long> {

    /**
     * 사용자 ID와 Provider로 토큰 정보 조회
     */
    Optional<IntegrationTokenInfo> findByUserIdAndProvider(Long userId, String provider);

    /**
     * 사용자 ID로 모든 토큰 정보 조회
     */
    List<IntegrationTokenInfo> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Provider로 모든 토큰 정보 조회
     */
    List<IntegrationTokenInfo> findByProviderOrderByCreatedAtDesc(String provider);

    /**
     * 활성 상태인 토큰 정보만 조회
     */
    @Query("SELECT t FROM IntegrationTokenInfo t WHERE t.user.id = :userId AND t.provider = :provider AND t.isActive = true")
    Optional<IntegrationTokenInfo> findActiveTokenByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") String provider);

    /**
     * 만료되지 않은 활성 토큰 정보 조회
     */
    @Query("SELECT t FROM IntegrationTokenInfo t WHERE t.user.id = :userId AND t.provider = :provider AND t.isActive = true AND (t.tokenExpiresAt IS NULL OR t.tokenExpiresAt > CURRENT_TIMESTAMP)")
    Optional<IntegrationTokenInfo> findValidTokenByUserIdAndProvider(@Param("userId") Long userId, @Param("provider") String provider);

    /**
     * 사용자의 모든 활성 토큰 조회
     */
    @Query("SELECT t FROM IntegrationTokenInfo t WHERE t.user.id = :userId AND t.isActive = true ORDER BY t.createdAt DESC")
    List<IntegrationTokenInfo> findActiveTokensByUserId(@Param("userId") Long userId);

    /**
     * 만료된 토큰들 조회
     */
    @Query("SELECT t FROM IntegrationTokenInfo t WHERE t.tokenExpiresAt IS NOT NULL AND t.tokenExpiresAt < CURRENT_TIMESTAMP")
    List<IntegrationTokenInfo> findExpiredTokens();

    /**
     * 토큰 존재 여부 확인
     */
    boolean existsByUserIdAndProvider(Long userId, String provider);

    /**
     * Provider별 활성 토큰 수 조회
     */
    @Query("SELECT COUNT(t) FROM IntegrationTokenInfo t WHERE t.provider = :provider AND t.isActive = true")
    long countActiveTokensByProvider(@Param("provider") String provider);

    /**
     * 사용자별 활성 토큰 수 조회
     */
    @Query("SELECT COUNT(t) FROM IntegrationTokenInfo t WHERE t.user.id = :userId AND t.isActive = true")
    long countActiveTokensByUserId(@Param("userId") Long userId);

    /**
     * tokenResponse에서 특정 키-값 쌍을 가진 토큰 조회
     * @param jsonFilter JSON 필터 (예: '{"scope": "user-read-private"}')
     */
    @Query(value = "SELECT * FROM integration_token_info WHERE token_response @> :filter::jsonb", nativeQuery = true)
    List<IntegrationTokenInfo> findByTokenResponseFilter(@Param("filter") String jsonFilter);

    /**
     * 사용자별 tokenResponse 필터링 조회
     */
    @Query(value = "SELECT * FROM integration_token_info WHERE user_id = :userId AND token_response @> :filter::jsonb", nativeQuery = true)
    List<IntegrationTokenInfo> findByUserIdAndTokenResponseFilter(@Param("userId") Long userId, @Param("filter") String jsonFilter);

    /**
     * Provider별 tokenResponse 필터링 조회
     */
    @Query(value = "SELECT * FROM integration_token_info WHERE provider = :provider AND token_response @> :filter::jsonb", nativeQuery = true)
    List<IntegrationTokenInfo> findByProviderAndTokenResponseFilter(@Param("provider") String provider, @Param("filter") String jsonFilter);

    /**
     * tokenResponse에서 특정 키가 존재하는 토큰 조회
     */
    @Query(value = "SELECT * FROM integration_token_info WHERE token_response ?| ARRAY[:key]", nativeQuery = true)
    List<IntegrationTokenInfo> findByTokenResponseKeyExists(@Param("key") String key);

    /**
     * tokenResponse에서 특정 키의 값이 특정 값과 같은 토큰 조회
     */
    @Query(value = "SELECT * FROM integration_token_info WHERE token_response ->> :key = :value", nativeQuery = true)
    List<IntegrationTokenInfo> findByTokenResponseKeyValue(@Param("key") String key, @Param("value") String value);

    /**
     * 사용자별 tokenResponse 키 존재 여부 확인
     */
    @Query(value = "SELECT * FROM integration_token_info WHERE user_id = :userId AND provider = :provider AND token_response ?| ARRAY[:key]", nativeQuery = true)
    Optional<IntegrationTokenInfo> findByUserIdAndProviderAndTokenResponseKeyExists(@Param("userId") Long userId, @Param("provider") String provider, @Param("key") String key);

    /**
     * tokenResponse에서 특정 경로의 값 조회 (중첩 JSON 지원)
     */
    @Query(value = "SELECT * FROM integration_token_info WHERE token_response #> :path IS NOT NULL", nativeQuery = true)
    List<IntegrationTokenInfo> findByTokenResponsePathExists(@Param("path") String path);

    /**
     * tokenResponse에서 배열 요소 포함 여부 확인
     */
    @Query(value = "SELECT * FROM integration_token_info WHERE token_response -> :key @> :arrayElement::jsonb", nativeQuery = true)
    List<IntegrationTokenInfo> findByTokenResponseArrayContains(@Param("key") String key, @Param("arrayElement") String arrayElement);
} 