package org.example.vibelist.domain.integration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.integration.dto.IntegrationTokenResponse;
import org.example.vibelist.domain.integration.dto.IntegrationStatusDto;
import org.example.vibelist.domain.integration.dto.TokenValidityDto;
import org.example.vibelist.domain.integration.entity.IntegrationTokenInfo;
import org.example.vibelist.domain.integration.service.IntegrationTokenInfoService;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.security.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 외부 서비스 연동 관리 컨트롤러
 * Spotify, Google, Apple Music 등 다양한 서비스의 토큰을 관리하는 REST API
 */
@RestController
@RequestMapping("/v1/integrations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "외부 서비스 연동", description = "Spotify, Google Music 등 외부 서비스 연동 관리 API")
public class IntegrationController {

    private final IntegrationTokenInfoService integrationTokenInfoService;

    @Operation(summary = "연동 상태 조회", description = "현재 사용자의 모든 외부 서비스 연동 상태를 조회합니다.")
    @GetMapping("/status")
    @PreAuthorize("authenticated")
    public ResponseEntity<List<IntegrationStatusDto>> getCurrentUserIntegrationStatus() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }
        List<IntegrationStatusDto> result = integrationTokenInfoService.getUserIntegrationStatus(userId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "제공자별 토큰 정보 조회", description = "특정 제공자의 토큰 정보를 조회합니다.")
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{provider}")
    public ResponseEntity<IntegrationTokenResponse> getProviderIntegration(
            @Parameter(description = "서비스 제공자 (spotify, google 등)") @PathVariable String provider) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        IntegrationTokenInfo tokenInfo = integrationTokenInfoService.getActiveTokenInfo(userId, provider)
                .orElseThrow(() -> new GlobalException(ResponseCode.INTEGRATION_NOT_FOUND, "provider='" + provider + "'에 대한 연동 정보를 찾을 수 없습니다."));

        IntegrationTokenResponse response = convertToTokenResponse(tokenInfo);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "유효한 토큰 정보 조회", description = "특정 제공자의 토큰 유효성을 확인합니다.")
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{provider}/valid")
    @PreAuthorize("authenticated")
    public ResponseEntity<TokenValidityDto> getValidProviderIntegration(
            @Parameter(description = "서비스 제공자 (spotify, google 등)") @PathVariable String provider) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        boolean isValid = integrationTokenInfoService.hasValidToken(userId, provider);
        
        TokenValidityDto response = TokenValidityDto.builder()
                .isValid(isValid)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "연동 해제", description = "특정 제공자의 연동을 해제하고 토큰을 비활성화합니다.")
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/{provider}")
    public ResponseEntity<RsData<Void>> disconnectProvider(
            @Parameter(description = "연동 해제할 서비스 제공자") @PathVariable String provider) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        integrationTokenInfoService.deactivateToken(userId, provider);

        return ResponseEntity.ok(RsData.success(ResponseCode.INTEGRATION_DISCONNECTED, null));
    }

    @Operation(summary = "모든 연동 해제", description = "모든 외부 서비스 연동을 해제하고 토큰을 비활성화합니다.")
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/all")
    public ResponseEntity<RsData<Map<String, Object>>> disconnectAllProviders() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        List<IntegrationTokenInfo> activeTokens = integrationTokenInfoService.getUserActiveTokens(userId).getData();

        for (IntegrationTokenInfo token : activeTokens) {
            integrationTokenInfoService.deactivateToken(userId, token.getProvider());
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("disconnectedCount", activeTokens.size());

        return ResponseEntity.ok(RsData.success(ResponseCode.INTEGRATION_DISCONNECTED_ALL, responseData));
    }

    @Operation(summary = "토큰 존재 여부 확인", description = "특정 제공자의 토큰 존재 여부를 확인합니다.")
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{provider}/exists")
    public ResponseEntity<RsData<Map<String, Object>>> checkProviderIntegrationExists(
            @Parameter(description = "확인할 서비스 제공자") @PathVariable String provider) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        boolean exists = integrationTokenInfoService.hasToken(userId, provider);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("provider", provider.toUpperCase());
        responseData.put("exists", exists);

        return ResponseEntity.ok(RsData.success(ResponseCode.INTEGRATION_TOKEN_CHECK, responseData));
    }

    @Operation(summary = "연동된 제공자 목록", description = "연동된 제공자 목록을 간단한 형태로 조회합니다.")
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/providers")
    public ResponseEntity<RsData<Map<String, Object>>> getConnectedProviders() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        List<IntegrationTokenInfo> activeTokens = integrationTokenInfoService.getUserActiveTokens(userId).getData();

        List<String> providers = activeTokens.stream()
                .map(IntegrationTokenInfo::getProvider)
                .collect(Collectors.toList());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("providers", providers);
        responseData.put("count", providers.size());

        return ResponseEntity.ok(RsData.success(ResponseCode.INTEGRATION_PROVIDERS_LIST, responseData));
    }

    @Operation(summary = "권한별 연동 조회", description = "특정 권한(scope)을 가진 연동을 조회합니다.")
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/by-scope")
    public ResponseEntity<RsData<Map<String, Object>>> getIntegrationsByScope(
            @Parameter(description = "조회할 권한 스코프") @RequestParam String scope) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        List<IntegrationTokenInfo> activeTokens = integrationTokenInfoService.getUserActiveTokens(userId).getData();

        // scope를 포함하는 토큰들 필터링
        List<IntegrationTokenInfo> matchingTokens = activeTokens.stream()
                .filter(token -> token.getScope() != null && token.getScope().contains(scope))
                .collect(Collectors.toList());

        List<IntegrationTokenResponse> responses = matchingTokens.stream()
                .map(this::convertToTokenResponse)
                .collect(Collectors.toList());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("scope", scope);
        responseData.put("integrations", responses);
        responseData.put("count", responses.size());

        return ResponseEntity.ok(RsData.success(ResponseCode.INTEGRATION_BY_SCOPE, responseData));
    }


    private IntegrationTokenResponse convertToTokenResponse(IntegrationTokenInfo tokenInfo) {
        // 민감한 정보는 제외하고 메타데이터만 포함
        Map<String, Object> additionalInfo = new HashMap<>();
        if (tokenInfo.getTokenResponse() != null) {
            // 민감하지 않은 정보만 포함 (토큰 값 제외)
            tokenInfo.getTokenResponse().entrySet().stream()
                    .filter(entry -> !isSecretField(entry.getKey()))
                    .forEach(entry -> additionalInfo.put(entry.getKey(), entry.getValue()));
        }

        return IntegrationTokenResponse.builder()
                .id(tokenInfo.getId())
                .provider(tokenInfo.getProvider())
                .tokenType(tokenInfo.getTokenType())
                .expiresIn(tokenInfo.getExpiresIn())
                .scope(tokenInfo.getScope())
                .tokenIssuedAt(tokenInfo.getTokenIssuedAt())
                .tokenExpiresAt(tokenInfo.getTokenExpiresAt())
                .isActive(tokenInfo.getIsActive())
                .isValid(tokenInfo.isValid())
                .isExpired(tokenInfo.isExpired())
                .additionalInfo(additionalInfo)
                .createdAt(tokenInfo.getCreatedAt())
                .updatedAt(tokenInfo.getUpdatedAt())
                .build();
    }

    @Operation(summary = "스포티파이 연동 시작", description = "인증된 사용자가 스포티파이 연동을 시작합니다. 스포티파이 OAuth2 인증 URL을 반환합니다.")
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/spotify/connect")
    public ResponseEntity<RsData<Map<String, Object>>> connectSpotify() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        log.info("[INTEGRATION] 스포티파이 연동 시작 - userId: {}", userId);

        // 이미 연동되어 있는지 확인
        if (integrationTokenInfoService.getValidTokenInfo(userId, "SPOTIFY").isPresent()) {
            log.warn("[INTEGRATION] 이미 스포티파이가 연동되어 있음 - userId: {}", userId);
            throw new GlobalException(ResponseCode.INTEGRATION_ALREADY_CONNECTED, "이미 스포티파이가 연동되어 있습니다.");
        }

        // 간소화된 방식: URL 파라미터로 userId 전달
        String spotifyAuthUrl = "/oauth2/authorization/spotify?integration_user_id=" + userId;

        log.info("[INTEGRATION] 스포티파이 OAuth2 URL 생성 - userId: {}, url: {}", userId, spotifyAuthUrl);

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("redirectUrl", spotifyAuthUrl);

        return ResponseEntity.ok(RsData.success(ResponseCode.INTEGRATION_SPOTIFY_CONNECT_SUCCESS, responseData));
    }

    @Operation(summary = "스포티파이 토큰 디버그 정보", description = "스포티파이 토큰이 정상적으로 저장되었는지 확인합니다. (개발용)")
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/spotify/token-debug")
    public ResponseEntity<RsData<Map<String, Object>>> getSpotifyTokenDebugInfo() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }

        log.info("[INTEGRATION_DEBUG] 스포티파이 토큰 디버그 정보 조회 - userId: {}", userId);

        IntegrationTokenInfo spotifyToken = integrationTokenInfoService.getActiveTokenInfo(userId, "SPOTIFY")
                .orElseThrow(() -> new GlobalException(ResponseCode.INTEGRATION_TOKEN_NOT_FOUND, "스포티파이 토큰 정보를 찾을 수 없습니다. 스포티파이 연동을 먼저 진행해주세요."));


        // 디버그 정보 생성 (민감한 토큰 값은 제외)
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("provider", spotifyToken.getProvider());
        debugInfo.put("isActive", spotifyToken.getIsActive());
        debugInfo.put("isValid", spotifyToken.isValid());
        debugInfo.put("isExpired", spotifyToken.isExpired());
        debugInfo.put("hasAccessToken", spotifyToken.getAccessToken() != null);
        debugInfo.put("hasRefreshToken", spotifyToken.getRefreshToken() != null);
        debugInfo.put("tokenType", spotifyToken.getTokenType());
        debugInfo.put("expiresIn", spotifyToken.getExpiresIn());
        debugInfo.put("scope", spotifyToken.getScope());
        debugInfo.put("tokenIssuedAt", spotifyToken.getTokenIssuedAt());
        debugInfo.put("tokenExpiresAt", spotifyToken.getTokenExpiresAt());
        debugInfo.put("createdAt", spotifyToken.getCreatedAt());
        debugInfo.put("updatedAt", spotifyToken.getUpdatedAt());
        debugInfo.put("tokenResponseKeys", spotifyToken.getTokenResponse() != null ?
                spotifyToken.getTokenResponse().keySet() : null);

        log.info("[INTEGRATION_DEBUG] 스포티파이 토큰 디버그 정보: {}", debugInfo);

        return ResponseEntity.ok(RsData.success(ResponseCode.INTEGRATION_SPOTIFY_DEBUG_SUCCESS, debugInfo));
    }

    private boolean isSecretField(String fieldName) {
        String lowerFieldName = fieldName.toLowerCase();
        return lowerFieldName.contains("token") ||
               lowerFieldName.contains("secret") ||
               lowerFieldName.contains("key") ||
               lowerFieldName.contains("password");
    }
} 