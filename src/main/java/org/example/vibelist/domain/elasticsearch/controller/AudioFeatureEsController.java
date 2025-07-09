package org.example.vibelist.domain.elasticsearch.controller;

import lombok.RequiredArgsConstructor;

import org.example.vibelist.domain.elasticsearch.service.AudioFeatureEsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("v1/elasticsearch")
@RequiredArgsConstructor
public class AudioFeatureEsController {
    private final AudioFeatureEsService audioFeatureEsService;
    @GetMapping("/insert")
    /*
    Rds에 저장되어 있는 Audio-feature 데이터들을 ElasticSearch로 옮기는 메소드.
     */
    public ResponseEntity<?> rdsToEs() {
        audioFeatureEsService.insert();
        return ResponseEntity.ok().body("RdsToEs_pure success");
    }

}
