package org.example.vibelist.domain.recommend.controller;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.recommend.dto.RecommendRqDto;
import org.example.vibelist.domain.recommend.dto.TrackRsDto;
import org.example.vibelist.domain.recommend.service.RecommendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/recommend")
@RequiredArgsConstructor
public class RecommendController {
    // 감정 기반 트랙 추천 API의 HTTP 요청을 처리하는 컨트롤러
    // RecommendService를 호출해 추천 결과를 반환

    private final RecommendService recommendService;
    @PostMapping
    public ResponseEntity<List<TrackRsDto>> recommend(@RequestBody RecommendRqDto request) {
        List<TrackRsDto> result = recommendService.recommend(request.getUserValence(), request.getUserEnergy(), request.getMode());


        return ResponseEntity.ok(result);
    }


}
