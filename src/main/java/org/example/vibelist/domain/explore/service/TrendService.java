package org.example.vibelist.domain.explore.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.explore.dto.TrendResponse;
import org.example.vibelist.domain.explore.entity.PostTrend;
import org.example.vibelist.domain.explore.entity.TrendSnapshot;
import org.example.vibelist.domain.explore.es.PostDocument;
import org.example.vibelist.domain.explore.es.PostESQueryBuilder;
import org.example.vibelist.domain.explore.repository.PostTrendRepository;
import org.example.vibelist.domain.explore.repository.TrendSnapshotRepository;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendService {

    private final ExploreService exploreService;
    private final ElasticsearchOperations operations;
    private final PostTrendRepository postTrendRepository;
    private final TrendSnapshotRepository trendSnapshotRepository;

    private static final int TOP_N_TRENDS = 50; // 스냅샷으로 저장할 상위 게시글 수
    private static final int TREND_API_LIMIT = 10; // API로 가져올 기본 개수

    /**
     * 매 시간마다 Elasticsearch에서 최근 게시글을 조회하여 트렌드 스코어를 계산하고 RDB에 저장합니다.
     * (예: 매시 0분 0초에 실행)
     */
//    @Scheduled(cron = "0 0 * * * ?") // 매 시간 0분에 실행
    @Scheduled(cron = "0 0/30 * * * ?")
    @Transactional
    public void captureAndSaveTrends() {
        log.info("Starting trend capturing and saving process at {}", LocalDateTime.now());

        // 1. 새로운 스냅샷(IN_PROGRESS) 생성
        TrendSnapshot snapshot = trendSnapshotRepository.save(
                TrendSnapshot.builder()
                        .snapshotTime(LocalDateTime.now())
                        .status(TrendSnapshot.SnapshotStatus.IN_PROGRESS)
                        .build()
        );

        try {
            // 2. 이전 트렌드 데이터 조회
            Map<Long, PostTrend> previousMap = getPreviousTrendsMap();

            // 3. 지난 24시간 내 활성 게시글 조회 & 스코어링
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            LocalDateTime updateSince = LocalDateTime.now().minusHours(72);
            List<SearchHit<PostDocument>> hits = findScoredAndSortedActivePosts(since,updateSince, TOP_N_TRENDS);

            // 4. DTO → 엔티티 변환 및 저장
            List<PostTrend> trends = hits.stream()
                    .map(hit -> buildPostTrend(hit, snapshot, previousMap, hits))
                    .collect(Collectors.toList());
            postTrendRepository.saveAll(trends);

            // 5. 스냅샷 완료 처리
            snapshot.complete();
            trendSnapshotRepository.save(snapshot);

            log.info("Finished trend capturing. Saved {} trends (snapshot ID={}).",
                    trends.size(), snapshot.getId());
        } catch (Exception e) {
            log.error("Error during trend capturing and saving process", e);
            snapshot.fail();
            trendSnapshotRepository.save(snapshot);
            throw new GlobalException(ResponseCode.TREND_CAPTURE_FAILED, "트렌드 스냅샷 생성 실패: " + e.getMessage());
        }
    }

    private PostTrend buildPostTrend(
            SearchHit<PostDocument> hit,
            TrendSnapshot snapshot,
            Map<Long, PostTrend> previousMap,
            List<SearchHit<PostDocument>> allHits
    ) {
        PostDocument doc = hit.getContent();
        double score = hit.getScore();
        int rank = allHits.indexOf(hit) + 1;

        PostTrend prev = previousMap.get(doc.getId());
        Integer prevRank = prev != null ? prev.getRank() : null;
        int change = (prev != null) ? (prev.getRank() - rank) : 0;

        PostTrend.TrendStatus status;
        if (prev == null) {
            status = PostTrend.TrendStatus.NEW;
        } else if (change > 0) {
            status = PostTrend.TrendStatus.UP;
        } else if (change < 0) {
            status = PostTrend.TrendStatus.DOWN;
        } else {
            status = PostTrend.TrendStatus.SAME;
        }

        return PostTrend.builder()
                .snapshot(snapshot)
                .postId(doc.getId())
                .score(score)
                .rank(rank)
                .previousRank(prevRank)
                .trendStatus(status)
                .rankChange(change)
                .postContent(doc.getContent())
                .userName(doc.getUserName())
                .userProfileName(doc.getUserProfileName())
                .snapshotTime(snapshot.getSnapshotTime())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TrendResponse> getTopTrends(int limit) {
        if (limit <= 0) {
            limit = TREND_API_LIMIT;
        }
        Optional<TrendSnapshot> opt = trendSnapshotRepository
                .findFirstByStatusOrderBySnapshotTimeDesc(TrendSnapshot.SnapshotStatus.COMPLETED);
        if (opt.isEmpty()) {
            log.warn("No completed trend snapshot found. Returning empty list.");
            return Collections.emptyList();
        }

        Long snapshotId = opt.get().getId();
        List<PostTrend> stored = postTrendRepository.findBySnapshot_IdOrderByScoreDesc(snapshotId);

        return stored.stream()
                .limit(limit)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private Map<Long, PostTrend> getPreviousTrendsMap() {
        Optional<TrendSnapshot> opt = trendSnapshotRepository
                .findFirstByStatusOrderBySnapshotTimeDesc(TrendSnapshot.SnapshotStatus.COMPLETED);
        return opt.map(snap -> postTrendRepository
                        .findBySnapshot_IdOrderByScoreDesc(snap.getId())
                        .stream()
                        .collect(Collectors.toMap(PostTrend::getPostId, Function.identity()))
                )
                .orElse(Collections.emptyMap());
    }

    private List<SearchHit<PostDocument>> findScoredAndSortedActivePosts(
            LocalDateTime since,
            LocalDateTime updateSince,
            int size
    ) {
        // ES 쿼리 생성
        Query activeQuery = PostESQueryBuilder.buildActivePostsSince(since, updateSince);
        List<SortOptions> sort = PostESQueryBuilder.buildScoreSortOptions();

        NativeQuery nq = NativeQuery.builder()
                .withQuery(activeQuery)
                .withSort(sort)
                .withPageable(Pageable.ofSize(size))
                .build();

        log.info(nq.toString());

        SearchHits<PostDocument> hits = operations.search(nq, PostDocument.class);

        return hits.getSearchHits();
    }

    private TrendResponse toDto(PostTrend pt) {
        return new TrendResponse(
                pt.getPostId(),
                pt.getScore(),
                pt.getRank(),
                pt.getPreviousRank(),
                pt.getTrendStatus(),
                pt.getRankChange(),
                pt.getPostContent(),
                pt.getUserName(),
                pt.getUserProfileName(),
                pt.getSnapshotTime()
        );
    }
}