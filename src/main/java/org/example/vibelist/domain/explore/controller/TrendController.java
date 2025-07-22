package org.example.vibelist.domain.explore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.explore.service.TrendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 💡 트렌드 스냅샷을 수동으로 생성-저장하기 위한 컨트롤러.
 *    (권한 체크는 필요에 따라 @PreAuthorize 등으로 감싸 주세요)
 */
@RestController
@RequestMapping("/v1/explore/trend")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Explore – 트렌드 관리", description = "트렌드 스냅샷(TrendSnapshot) 수동 생성 API")
public class TrendController {

    private final TrendService trendService;

    /** POST /v1/explore/trend/rebuild  */
    @PostMapping("/rebuild")
    @Operation(
            summary = "트렌드 스냅샷 강제 생성",
            description = """
            스케줄러를 기다리지 않고 즉시 트렌드 데이터를 갱신합니다.
            > **주의**: 운영 환경에서는 관리자 권한(예: `ROLE_ADMIN`) 또는 API Key 등으로 보호하세요.
            """
    )
    public ResponseEntity<String> rebuildTrends() {
        log.info("[ADMIN] Trend rebuild API called");
        trendService.captureAndSaveTrends();
        return ResponseEntity.ok("Trend snapshot captured successfully.");
    }
}