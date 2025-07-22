package org.example.vibelist.domain.playlist.pool;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionFeatureProfile;
import org.example.vibelist.domain.playlist.emotion.type.EmotionType;
import org.example.vibelist.domain.playlist.es.builder.ESQueryBuilder;
import org.example.vibelist.domain.playlist.es.document.AudioFeatureEsDocument;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmotionPoolProvider {
    private final ElasticsearchClient client;

    /**
     * 감정+모드+pool 크기 기준 pool 생성 (ES 쿼리→결과→DTO 리스트)
     */
    public Set<TrackRsDto> createPool(EmotionType emotion, EmotionFeatureProfile profile, int poolSize) {
        log.info("🔨 Pool 생성 요청: emotion={}, poolSize={}", emotion, poolSize);
        Query emotionQuery = ESQueryBuilder.build(profile);
        Set<TrackRsDto> pool = new HashSet<>(searchTracks(emotionQuery, poolSize));
        log.info("✅ Pool 생성 완료: emotion={}, pool size={}", emotion, pool.size());
        return pool;
    }

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
