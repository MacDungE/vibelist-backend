package org.example.vibelist.domain.batch.elasticsearch.controller;

import lombok.RequiredArgsConstructor;

import org.example.vibelist.domain.batch.elasticsearch.service.EsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("v1/elasticsearch")
@RequiredArgsConstructor
public class EsController {
    private final EsService esService;
    @GetMapping("/insert")
    /*
    Rds에 저장되어 있는 Audio-feature 데이터들을 ElasticSearch로 옮기는 메소드.
     */
    public ResponseEntity<?> rdsToEs() {
        esService.executeTestInsert();
        //audioFeatureEsService.executeBatchInsert(); 모든 데이터 삽입시
        return ResponseEntity.ok().body("insertion success");
    }

}
