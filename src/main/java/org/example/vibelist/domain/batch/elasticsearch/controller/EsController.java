package org.example.vibelist.domain.batch.elasticsearch.controller;

import lombok.RequiredArgsConstructor;

import org.example.vibelist.domain.batch.elasticsearch.service.EsService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.response.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;

@Controller
@RequestMapping("v1/elasticsearch")
@RequiredArgsConstructor
public class EsController {
    private final EsService esService;
    @Operation(summary = "RDS to ES 데이터 이관", description = "Rds에 저장되어 있는 Audio-feature 데이터들을 ElasticSearch로 옮기는 메소드.")
    @GetMapping("/insert")
    public ResponseEntity<RsData<?>> rdsToEs() {
        try {
            esService.executeTestInsert();
            return ResponseEntity.ok(RsData.success(ResponseCode.BATCH_SUCCESS, "insertion success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(RsData.fail(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

}
