package org.example.vibelist.global.integration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.global.integration.dto.IntegrationStatusResponse;
import org.example.vibelist.global.integration.dto.IntegrationTokenResponse;
import org.example.vibelist.global.integration.dto.RefreshTokenRequest;
import org.example.vibelist.global.integration.entity.IntegrationTokenInfo;
import org.example.vibelist.global.integration.service.IntegrationTokenInfoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "연동 상태 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/status")
    public ResponseEntity<?> getCurrentUserIntegrationStatus() {
        try {
            Long userId = getCurrentUserId();
            
            List<IntegrationTokenInfo> activeTokens = integrationTokenInfoService.getUserActiveTokens(userId);
            
            List<IntegrationStatusResponse.IntegrationSummary> summaries = activeTokens.stream()
                    .map(this::convertToIntegrationSummary)
                    .collect(Collectors.toList());
            
            long expiredCount = activeTokens.stream()
                    .mapToLong(token -> token.isExpired() ? 1 : 0)
                    .sum();
            
            IntegrationStatusResponse response = IntegrationStatusResponse.builder()
                    .userId(userId)
                    .totalIntegrations(activeTokens.size())
                    .activeIntegrations((int) (activeTokens.size() - expiredCount))
                    .expiredIntegrations((int) expiredCount)
                    .integrations(summaries)
                    .statusCheckedAt(LocalDateTime.now())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "제공자별 토큰 정보 조회", description = "특정 제공자의 토큰 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "토큰을 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{provider}")
    public ResponseEntity<?> getProviderIntegration(
            @Parameter(description = "서비스 제공자 (spotify, google 등)") @PathVariable String provider) {
        try {
            Long userId = getCurrentUserId();
            
            Optional<IntegrationTokenInfo> tokenInfoOpt = integrationTokenInfoService.getActiveTokenInfo(userId, provider);
            
            if (tokenInfoOpt.isPresent()) {
                IntegrationTokenResponse response = convertToTokenResponse(tokenInfoOpt.get());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "유효한 토큰 정보 조회", description = "특정 제공자의 유효한 토큰 정보를 조회합니다. (만료되지 않은 토큰만)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "유효한 토큰 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "유효한 토큰을 찾을 수 없음"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{provider}/valid")
    public ResponseEntity<?> getValidProviderIntegration(
            @Parameter(description = "서비스 제공자 (spotify, google 등)") @PathVariable String provider) {
        try {
            Long userId = getCurrentUserId();
            
            Optional<IntegrationTokenInfo> tokenInfoOpt = integrationTokenInfoService.getValidTokenInfo(userId, provider);
            
            if (tokenInfoOpt.isPresent()) {
                IntegrationTokenResponse response = convertToTokenResponse(tokenInfoOpt.get());
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", provider + " 서비스의 유효한 토큰이 없습니다.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "연동 해제", description = "특정 제공자의 연동을 해제하고 토큰을 비활성화합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "연동 해제 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/{provider}")
    public ResponseEntity<?> disconnectProvider(
            @Parameter(description = "연동 해제할 서비스 제공자") @PathVariable String provider) {
        try {
            Long userId = getCurrentUserId();
            
            integrationTokenInfoService.deactivateToken(userId, provider);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", provider + " 서비스 연동이 해제되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "모든 연동 해제", description = "모든 외부 서비스 연동을 해제하고 토큰을 비활성화합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "모든 연동 해제 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "bearer-key")
    @DeleteMapping("/all")
    public ResponseEntity<?> disconnectAllProviders() {
        try {
            Long userId = getCurrentUserId();
            
            List<IntegrationTokenInfo> activeTokens = integrationTokenInfoService.getUserActiveTokens(userId);
            
            for (IntegrationTokenInfo token : activeTokens) {
                integrationTokenInfoService.deactivateToken(userId, token.getProvider());
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "모든 서비스 연동이 해제되었습니다.");
            response.put("disconnectedCount", String.valueOf(activeTokens.size()));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "토큰 존재 여부 확인", description = "특정 제공자의 토큰 존재 여부를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 존재 여부 확인 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/{provider}/exists")
    public ResponseEntity<?> checkProviderIntegrationExists(
            @Parameter(description = "확인할 서비스 제공자") @PathVariable String provider) {
        try {
            Long userId = getCurrentUserId();
            
            boolean exists = integrationTokenInfoService.hasToken(userId, provider);
            
            Map<String, Object> response = new HashMap<>();
            response.put("provider", provider.toUpperCase());
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "연동된 제공자 목록", description = "연동된 제공자 목록을 간단한 형태로 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "제공자 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/providers")
    public ResponseEntity<?> getConnectedProviders() {
        try {
            Long userId = getCurrentUserId();
            
            List<IntegrationTokenInfo> activeTokens = integrationTokenInfoService.getUserActiveTokens(userId);
            
            List<String> providers = activeTokens.stream()
                    .map(IntegrationTokenInfo::getProvider)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("providers", providers);
            response.put("count", providers.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(summary = "권한별 연동 조회", description = "특정 권한(scope)을 가진 연동을 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "권한별 연동 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "bearer-key")
    @GetMapping("/by-scope")
    public ResponseEntity<?> getIntegrationsByScope(
            @Parameter(description = "조회할 권한 스코프") @RequestParam String scope) {
        try {
            Long userId = getCurrentUserId();
            
            List<IntegrationTokenInfo> activeTokens = integrationTokenInfoService.getUserActiveTokens(userId);
            
            // scope를 포함하는 토큰들 필터링
            List<IntegrationTokenInfo> matchingTokens = activeTokens.stream()
                    .filter(token -> token.getScope() != null && token.getScope().contains(scope))
                    .collect(Collectors.toList());
            
            List<IntegrationTokenResponse> responses = matchingTokens.stream()
                    .map(this::convertToTokenResponse)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("scope", scope);
            response.put("integrations", responses);
            response.put("count", responses.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * 현재 사용자 ID 추출
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Long)) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }
        return (Long) authentication.getPrincipal();
    }

    /**
     * IntegrationTokenInfo를 IntegrationTokenResponse로 변환
     */
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

    /**
     * IntegrationTokenInfo를 IntegrationSummary로 변환
     */
    private IntegrationStatusResponse.IntegrationSummary convertToIntegrationSummary(IntegrationTokenInfo tokenInfo) {
        String status = "INACTIVE";
        if (Boolean.TRUE.equals(tokenInfo.getIsActive())) {
            status = tokenInfo.isExpired() ? "EXPIRED" : "ACTIVE";
        }
        
        return IntegrationStatusResponse.IntegrationSummary.builder()
                .provider(tokenInfo.getProvider())
                .status(status)
                .isValid(tokenInfo.isValid())
                .lastUpdated(tokenInfo.getUpdatedAt())
                .expiresAt(tokenInfo.getTokenExpiresAt())
                .scope(tokenInfo.getScope())
                .build();
    }

    /**
     * 민감한 필드 여부 확인
     */
    private boolean isSecretField(String fieldName) {
        String lowerFieldName = fieldName.toLowerCase();
        return lowerFieldName.contains("token") || 
               lowerFieldName.contains("secret") || 
               lowerFieldName.contains("key") ||
               lowerFieldName.contains("password");
    }
} 