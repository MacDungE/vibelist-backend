package org.example.vibelist.domain.playlist.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.emotion.EmotionFeatureProfile;
import org.example.vibelist.domain.playlist.emotion.EmotionModeType;
import org.example.vibelist.domain.playlist.emotion.EmotionProfileManager;
import org.example.vibelist.domain.playlist.emotion.EmotionType;
import org.example.vibelist.domain.playlist.es.document.AudioFeatureEsDocument;
import org.example.vibelist.domain.playlist.es.builder.ESQueryBuilder;
import org.example.vibelist.domain.playlist.repository.TrackRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendService {

    // 감정 분류 및 전이 → 검색 범위 계산 → Elasticsearch 쿼리 실행을 수행하는 추천 서비스
    // 추천 결과를 트랙 리스트로 반환

    private final TrackRepository trackRepository;
    private final ElasticsearchClient client;
    private final EmotionProfileManager profileRepository;


    public List<TrackRsDto> recommend(double userValence, double userEnergy, EmotionModeType mode) {
        log.info("🎯 추천 요청 수신 - valence: {}, energy: {}, mode: {}", userValence, userEnergy, mode);

        EmotionType emotion = profileRepository.classify(userValence, userEnergy);
        log.info("🧠 분류된 감정: {}", emotion);

        EmotionType transitioned = profileRepository.getTransition(emotion, mode);
        log.info("🔁 전이된 감정: {}", transitioned);

        EmotionFeatureProfile profile = profileRepository.getProfile(transitioned);
        log.info("📊 검색 범위 - valence: {} ~ {}, energy: {} ~ {}",
                profile.getValence().getMin(), profile.getValence().getMax(),
                profile.getEnergy().getMin(), profile.getEnergy().getMax());

        Query emotionQuery = ESQueryBuilder.build(profile);
        log.info("🔍 Elasticsearch 쿼리 생성 완료");

        SearchRequest request = SearchRequest.of(s -> s
                .index("audio_feature_index")
                .query(emotionQuery)
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
                    .map(TrackRsDto::from)  // ES document -> DTO
                    .collect(Collectors.toList());

//            List<String> spotifyIds = response.hits()
//                    .hits()
//                    .stream()
//                    .map(hit -> hit.source().getSpotifyId())
//                    .collect(Collectors.toList());
//
//            List<Track> tracks = trackRepository.findAllBySpotifyIdIn(spotifyIds);
//            log.info("🎶 최종 추천 트랙 수: {}", tracks.size());
//
//            return tracks.stream()
//                    .map(TrackRsDto::from)
//                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("❌ Elasticsearch 검색 실패", e);
            throw new RuntimeException("Failed to search Elasticsearch", e);
        }
    }


}

