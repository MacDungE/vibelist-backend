package org.example.vibelist.domain.playlist.provider;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionAnalysis;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionFeatureProfile;
import org.example.vibelist.domain.playlist.es.builder.ESQueryBuilder;
import org.example.vibelist.domain.playlist.es.document.AudioFeatureEsDocument;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrackQueryProvider {

    private final ElasticsearchClient client;

    // 구체적 audio feature 기반 검색
    public List<TrackRsDto> recommendByAnalysis(EmotionAnalysis analysis, int size) {
        Query query = ESQueryBuilder.build(analysis);
        return searchTracks(query, size);
    }

    // valence, energy 기반 검색
    public List<TrackRsDto> recommendByProfile(EmotionFeatureProfile profile, int size) {
        Query query = ESQueryBuilder.build(profile);
        return searchTracks(query, size);
    }

    // es에 검색 실행 / 파싱
    private List<TrackRsDto> searchTracks(Query query, int size) {
        SearchRequest request = SearchRequest.of(s -> s
                .index("audio_feature_index")
                .query(query)
                .size(size)
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
            throw new GlobalException(ResponseCode.ES_SEARCH_FAILED);
        }
    }
}
