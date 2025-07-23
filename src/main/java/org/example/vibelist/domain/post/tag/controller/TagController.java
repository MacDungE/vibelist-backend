package org.example.vibelist.domain.post.tag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.post.tag.dto.TagDTO;
import org.example.vibelist.domain.post.tag.service.TagService;
import org.example.vibelist.global.response.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/tag")
@RequiredArgsConstructor
@Tag(name = "Tag", description = "태그 검색·자동완성 API")
public class TagController {

    private final TagService tagService;

    /**
     * 태그 자동완성
     *
     * @param q     검색어(프리픽스)
     * @param limit 최대 반환 개수 (기본 10)
     * @return 태그 DTO 리스트
     */
    @Operation(
            summary     = "태그 자동완성",
            description = "입력한 문자열(초성·키워드)을 기반으로 태그를 추천합니다."
    )
    @GetMapping("/suggest")
    public ResponseEntity<RsData<List<TagDTO>>> suggest(
            @Parameter(description = "검색어(초성 또는 키워드)", example = "아")
            @RequestParam String q,
            @Parameter(description = "반환 개수 (default = 10)", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        RsData<List<TagDTO>> result = tagService.autoComplete(q, limit);
        return ResponseEntity.ok(result);
    }
}