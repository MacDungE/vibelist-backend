package org.example.vibelist.domain.playlist.controller;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.playlist.dto.RecommendRqDto;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.service.RecommendService;
import org.example.vibelist.global.aop.UserActivityLog;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

import org.example.vibelist.global.response.RsData;

import java.util.List;

@RestController
@RequestMapping("/v1/recommend")
@RequiredArgsConstructor
@Validated
public class RecommendController {
    // 감정 기반 트랙 추천 API의 HTTP 요청을 처리하는 컨트롤러
    // RecommendService를 호출해 추천 결과를 반환

    private final RecommendService recommendService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "감정 기반 트랙 추천",
            description = "사용자의 감정 정보를 바탕으로 트랙을 추천합니다. valence/energy 직접 입력하거나 자연어 감정 설명을 입력할 수 있습니다. (둘 중 하나만 입력해도 추천이 동작합니다.)"
    )
    @UserActivityLog(action = "RECOMMEND_PLAYLIST")
    public ResponseEntity<RsData<?>> recommend(@RequestBody @Valid RecommendRqDto request) {
        RsData<?> result = recommendService.recommend(request);
        return ResponseEntity.status(result.isSuccess() ? 200 : 400).body(result);
    }

}
