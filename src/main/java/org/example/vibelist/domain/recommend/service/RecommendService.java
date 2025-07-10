package org.example.vibelist.domain.recommend.service;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.recommend.builder.ESQueryBuilder;
import org.example.vibelist.domain.recommend.dto.*;
import org.example.vibelist.domain.recommend.emotion.EmotionMapper;
import org.example.vibelist.domain.track.entity.Track;
import org.example.vibelist.domain.track.repository.TrackRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class RecommendService {

    private final EmotionMapper emotionMapper;

    private final TrackRepository trackRepository;


    private final ElasticsearchClient client;


    public List<TrackRsDto> recommend(EmotionType emotion, EmotionMode mode) {
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
}
