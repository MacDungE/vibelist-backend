package org.example.vibelist.domain.explore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.explore.dto.TrendResponse;
import org.example.vibelist.domain.explore.service.ExploreService;
import org.example.vibelist.domain.explore.service.TrendService;
import org.example.vibelist.domain.post.dto.PostDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.vibelist.global.response.RsData;

import java.util.List;

@Tag(name = "Explore", description = "검색 및 트렌드 피드 API")
@RestController
@RequestMapping("/v1/explore")
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;
    private final TrendService trendService;

    @Operation(summary = "게시글 검색", description = "키워드를 기반으로 게시글을 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<RsData<?>> search(
            @Parameter(description = "검색 키워드", required = true)
            @RequestParam("q") String keyword,
            @Parameter(hidden = true) Pageable pageable) {
        RsData<?> result = exploreService.search(keyword, pageable);
        return ResponseEntity.status(result.isSuccess() ? 200 : 400).body(result);
    }

    @Operation(summary = "피드 조회", description = "추천 게시글 피드를 조회합니다.")
    @GetMapping("/feed")
    public ResponseEntity<RsData<?>> feed(@Parameter(hidden = true) Pageable pageable) {
        RsData<?> result = exploreService.feed(pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "트렌드 조회", description = "현재 인기 있는 트렌드를 조회합니다.")
    @GetMapping("/trend")
    public ResponseEntity<List<TrendResponse>> getTrends(
            @Parameter(description = "최대 조회 개수", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        List<TrendResponse> trends = trendService.getTopTrends(limit);
        return ResponseEntity.ok(trends);
    }
}