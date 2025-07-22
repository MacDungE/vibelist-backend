package org.example.vibelist.domain.playlist.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.RecommendRqDto;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.emotion.llm.EmotionTextManager;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionAnalysis;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionFeatureProfile;
import org.example.vibelist.domain.playlist.emotion.type.EmotionModeType;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionProfileManager;
import org.example.vibelist.domain.playlist.emotion.type.EmotionType;
import org.example.vibelist.domain.playlist.es.document.AudioFeatureEsDocument;
import org.example.vibelist.domain.playlist.es.builder.ESQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;

import java.io.IOException;
import java.util.stream.Stream;

import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.GlobalException;


@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendService {

    // 감정 분류 및 전이 → 검색 범위 계산 → Elasticsearch 쿼리 실행을 수행하는 추천 서비스
    // 추천 결과를 트랙 리스트로 반환

    private final ElasticsearchClient client;
    private final EmotionProfileManager profileManager;
    private final EmotionTextManager textManager;

    // 입력값 구분
    public RsData<List<TrackRsDto>> recommend(RecommendRqDto request) {
        try {
            if (request.getText() != null && !request.getText().isBlank()) {
                return RsData.success(ResponseCode.RECOMMEND_SUCCESS, recommendByText(request.getText(), request.getMode()));
            } else if (request.getUserValence() != null && request.getUserEnergy() != null) {
                return RsData.success(ResponseCode.RECOMMEND_SUCCESS, recommendByCoordinate(request.getUserValence(), request.getUserEnergy(), request.getMode()));
            } else {
                throw new GlobalException(ResponseCode.RECOMMEND_INVALID_INPUT, "추천 입력값이 잘못되었습니다. text, userValence, userEnergy 중 하나는 반드시 입력되어야 합니다.");
            }
        } catch (GlobalException ce) {
            throw ce;
        } catch (Exception e) {
            throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR, "추천 처리 중 오류: " + e.getMessage());
        }
    }

    // valence, energy -> 감정 매핑
    public List<TrackRsDto> recommendByCoordinate(double userValence, double userEnergy, EmotionModeType mode) {
        log.info("🎯 좌표 기반 추천 요청 수신 - valence: {}, energy: {}, mode: {}", userValence, userEnergy, mode);
        EmotionType emotion = profileManager.classify(userValence, userEnergy);
        log.info("🧠 분류된 감정: {}", emotion);
        return recommendByEmotionType(emotion, mode);
    }

    // 자연어 -> 감정 매핑
    public List<TrackRsDto> recommendByText(String userText, EmotionModeType mode) throws JsonProcessingException {
        log.info("🎯 텍스트 기반 추천 요청 수신 - text: \"{}\", mode: {}", userText, mode);

        EmotionAnalysis analysis = textManager.getEmotionAnalysis(userText, mode);
        log.info("📊 LLM 기반 검색 범위: {}", analysis);

        Query llmQuery = ESQueryBuilder.build(analysis);
        log.info("🔍 Elasticsearch 쿼리 생성 완료");

        List<TrackRsDto> result = searchTracks(llmQuery);

        // 검색 결과 너무 적으면 fallback
        if (result.size() < 10) {
            log.info("🔁 Fallback - 감정타입 기반 검색 진행: {}", analysis.getEmotionType());
            List<TrackRsDto> fallback = recommendByEmotionType(EmotionType.valueOf(analysis.getEmotionType()), mode);

            // 합치기(중복 제거)
            result = Stream.concat(result.stream(), fallback.stream())
                    .distinct()
                    .limit(20)
                    .collect(Collectors.toList());

            log.info("🔁 Fallback 결과 사이즈: {}", result.size());
        }

        return result;
    }

    // 감정 -> 플레이리스트 추천
    public List<TrackRsDto> recommendByEmotionType(EmotionType emotion, EmotionModeType mode) {
        EmotionType transitioned = profileManager.getTransition(emotion, mode);
        log.info("🔁 전이된 감정: {}", transitioned);

        EmotionFeatureProfile profile = profileManager.getProfile(transitioned);
        log.info("📊 검색 범위 - valence: {} ~ {}, energy: {} ~ {}",
                profile.getValence().getMin(), profile.getValence().getMax(),
                profile.getEnergy().getMin(), profile.getEnergy().getMax());

        Query emotionQuery = ESQueryBuilder.build(profile);
        log.info("🔍 Elasticsearch 쿼리 생성 완료");

        return searchTracks(emotionQuery);
        }

    // **공통화된 ES 검색/변환 메서드**
    private List<TrackRsDto> searchTracks(Query query) {
        SearchRequest request = SearchRequest.of(s -> s
                .index("audio_feature_index")
                .query(query)
                .size(20)
                .sort(sort -> sort
                        .score(scoreSort -> scoreSort.order(SortOrder.Desc))
                )
        );

        try {
            SearchResponse<AudioFeatureEsDocument> response = client.search(request, AudioFeatureEsDocument.class);
            log.info("📦 검색 결과 수신 - 총 {}개", response.hits().hits().size());

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .map(TrackRsDto::from)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("❌ Elasticsearch 검색 실패", e);
            throw new GlobalException(ResponseCode.ES_SEARCH_FAILED, "Elasticsearch 검색 실패: " + e.getMessage());
        }
    }

}
