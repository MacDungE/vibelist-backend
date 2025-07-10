package org.example.vibelist.domain.recommend.controller;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.recommend.dto.RecommendRqDto;
import org.example.vibelist.domain.recommend.dto.TrackRsDto;
import org.example.vibelist.domain.recommend.service.RecommendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @PostMapping
    public ResponseEntity<List<TrackRsDto>> recommend(@RequestBody RecommendRqDto request) {
        List<TrackRsDto> result = recommendService.recommend(request.getEmotion(), request.getMode());
        return ResponseEntity.ok(result);
    }


}
