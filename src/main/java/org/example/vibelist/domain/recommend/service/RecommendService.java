package org.example.vibelist.domain.recommend.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
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
public class RecommendService {

    private final EmotionMapper emotionMapper;

    private final TrackRepository trackRepository;

    private final ElasticsearchClient client;

    private final EmotionClassifier emotionClassifier;

    public final SpotifyApiClient spotifyApiClient ; // spotify 등록을 위해

    public List<TrackRsDto> recommend(double userValence, double userEnergy, EmotionModeType mode) {
        EmotionType emotion = emotionClassifier.classify(userValence, userEnergy);
        EmotionFeatureProfile profile = emotionMapper.map(emotion, mode);
        Query emotionQuery = ESQueryBuilder.build(profile);


        SearchRequest request = SearchRequest.of(s -> s
                .index("audio_feature_index") // 검색할 인덱스 이름
                .query(emotionQuery) // 위에서 생성한 emotionQuery를 사용
                .size(20) // 가져올 문서의 최대 개수
                .sort(sort -> sort
                        // _score를 기준으로 정렬합니다.
                        // function_score 쿼리에서 random_score가 _score를 무작위로 변경했으므로,
                        // _score로 정렬하면 무작위 결과를 얻을 수 있습니다.
                        .score(scoreSort -> scoreSort.order(SortOrder.Desc)) // 점수가 높은(무작위로 부여된) 순서로 정렬
                )
        );

        try {
            SearchResponse<AudioFeatureEsDocument> response = client.search(request, AudioFeatureEsDocument.class);

            List<String> spotifyIds = response.hits()
                    .hits()
                    .stream()
                    .map(hit -> hit.source().getSpotifyId())
                    .collect(Collectors.toList());

            List<Track> tracks = trackRepository.findAllBySpotifyIdIn(spotifyIds);

            return tracks.stream()
                    .map(TrackRsDto::from)
                    .collect(Collectors.toList());

        } catch (IOException e) {
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
