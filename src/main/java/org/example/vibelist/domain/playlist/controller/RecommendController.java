package org.example.vibelist.domain.playlist.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.playlist.dto.RecommendRqDto;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.service.RecommendService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;

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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추천 결과", content = @Content(schema = @Schema(implementation = TrackRsDto.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터", content = @Content)
    })
    public ResponseEntity<List<TrackRsDto>> recommend(@RequestBody @Valid RecommendRqDto request) throws JsonProcessingException {
        return ResponseEntity.ok(recommendService.recommend(request));
    }

}
