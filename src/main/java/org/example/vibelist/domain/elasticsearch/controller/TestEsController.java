package org.example.vibelist.domain.elasticsearch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.elasticsearch.dto.TestEsDoc;
import org.example.vibelist.domain.elasticsearch.service.TestEsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/elasticsearch")
@RequiredArgsConstructor
@Slf4j
public class TestEsController {
    private final TestEsService  testEsService;
    @PostMapping("insert")
    ResponseEntity<?> insert(@RequestBody List<TestEsDoc> testEsDocs) {
        for(TestEsDoc testEsDoc : testEsDocs) {
            log.info(testEsDoc.toString());
            log.info("insert test es doc: {}", testEsDoc);
            testEsService.insert(testEsDoc);
        }
        return ResponseEntity.ok().build();
    }


}
