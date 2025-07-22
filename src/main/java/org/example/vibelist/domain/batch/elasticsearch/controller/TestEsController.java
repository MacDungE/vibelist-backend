package org.example.vibelist.domain.batch.elasticsearch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.batch.elasticsearch.dto.TestEsDoc;
import org.example.vibelist.domain.batch.elasticsearch.service.TestEsService;
import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.response.ResponseCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("v1/elasticsearch")
@RequiredArgsConstructor
@Slf4j
public class TestEsController {
    private final TestEsService  testEsService;
    @Operation(summary = "테스트 ES 도큐먼트 삽입", description = "테스트용 ES 도큐먼트 삽입 API")
    @PostMapping("insert")
    public ResponseEntity<RsData<?>> insert(@RequestBody List<TestEsDoc> testEsDocs) {
        try {
            for(TestEsDoc testEsDoc : testEsDocs) {
                log.info(testEsDoc.toString());
                log.info("insert test es doc: {}", testEsDoc);
                testEsService.insert(testEsDoc);
            }
            return ResponseEntity.ok(RsData.success(ResponseCode.BATCH_SUCCESS, "insert success"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(RsData.fail(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }


}
