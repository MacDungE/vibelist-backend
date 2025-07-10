package org.example.vibelist.domain.recommend.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.emotion.*;
import org.example.vibelist.domain.recommend.builder.ESQueryBuilder;
import org.example.vibelist.domain.recommend.dto.*;
import org.example.vibelist.domain.track.client.SpotifyApiClient;
import org.example.vibelist.domain.track.entity.Track;
import org.example.vibelist.domain.track.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendService {

    // 감정 분류 및 전이 → 검색 범위 계산 → Elasticsearch 쿼리 실행을 수행하는 추천 서비스
    // 추천 결과를 트랙 리스트로 반환

    private final EmotionMapper emotionMapper;
    private final TrackRepository trackRepository;
    private final ElasticsearchClient client;
    private final EmotionClassifier emotionClassifier;

    public final SpotifyApiClient spotifyApiClient ; // spotify 등록을 위해

    public List<TrackRsDto> recommend(double userValence, double userEnergy, EmotionModeType mode) {
        log.info("🎯 추천 요청 수신 - valence: {}, energy: {}, mode: {}", userValence, userEnergy, mode);

        EmotionType emotion = emotionClassifier.classify(userValence, userEnergy);
        log.info("🧠 분류된 감정: {}", emotion);

        EmotionType transitioned = EmotionTransitionMap.getNext(emotion, mode);
        log.info("🔁 전이된 감정: {}", transitioned);

        EmotionFeatureProfile profile = emotionMapper.map(emotion, mode);
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

            List<String> spotifyIds = response.hits()
                    .hits()
                    .stream()
                    .map(hit -> hit.source().getSpotifyId())
                    .collect(Collectors.toList());

            List<Track> tracks = trackRepository.findAllBySpotifyIdIn(spotifyIds);
            log.info("🎶 최종 추천 트랙 수: {}", tracks.size());

            return tracks.stream()
                    .map(TrackRsDto::from)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            log.error("❌ Elasticsearch 검색 실패", e);
            throw new RuntimeException("Failed to search Elasticsearch", e);
        }
    }

    @Transactional
    public void registerSpotify(List<TrackRsDto> results){
        //Spotify에 playlist 생성하기
        String user_id = "Sung1";
        String url = "https://api.spotify.com/v1/users/"+ user_id+"/playlists";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(spotifyApiClient.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 바디
        Map<String, Object> body = new HashMap<>();
        body.put("name", "New Playlist");
        body.put("description", "New playlist description");
        body.put("public", false);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }
}
