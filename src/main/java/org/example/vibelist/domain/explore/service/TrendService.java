package org.example.vibelist.domain.explore.service;

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
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
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
    @Scheduled(cron = "0 0 * * * ?") // 매 시간 0분에 실행
    @Transactional
    public void captureAndSaveTrends() {
        log.info("Starting trend capturing and saving process at {}", LocalDateTime.now());

        TrendSnapshot currentSnapshot = null;
        try {
            // 1. 새로운 스냅샷 생성 및 상태 IN_PROGRESS로 저장
            currentSnapshot = TrendSnapshot.builder()
                    .snapshotTime(LocalDateTime.now())
                    .status(TrendSnapshot.SnapshotStatus.IN_PROGRESS)
                    .build();
            currentSnapshot = trendSnapshotRepository.save(currentSnapshot);
            final TrendSnapshot finalCurrentSnapshot = currentSnapshot; // 람다에서 사용하기 위해 final 변수 선언

            // 2. 이전 스냅샷의 트렌드 데이터 조회 (순위 비교를 위함)
            Map<Long, PostTrend> previousTrendsMap = getPreviousTrendsMap();

            // 3. 최근 24시간 이내에 생성되거나 업데이트된 활성 게시글 조회 (Elasticsearch에서 스코어링 및 정렬)
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
            List<SearchHit<PostDocument>> scoredAndSortedPosts =
                    findScoredAndSortedActivePosts(twentyFourHoursAgo, TOP_N_TRENDS);

            // 4. 상위 N개 게시글을 PostTrend 엔티티로 변환하여 RDB에 저장
            LocalDateTime snapshotTime = currentSnapshot.getSnapshotTime();
            List<PostTrend> postTrendsToSave = scoredAndSortedPosts.stream()
                    .map(hit -> {
                        PostDocument doc = hit.getContent();
                        Double score = (double) hit.getScore();

                        // 이전 스냅샷 데이터 조회
                        PostTrend previousTrend = previousTrendsMap.get(doc.getId());

                        // 순위 및 트렌드 상태 계산
                        int currentRank = scoredAndSortedPosts.indexOf(hit) + 1; // 1부터 시작하는 순위
                        PostTrend.TrendStatus trendStatus;
                        Integer previousRank = null;
                        Integer rankChange = 0;

                        if (previousTrend != null) {
                            previousRank = previousTrend.getRank();
                            rankChange = previousRank - currentRank; // 이전 순위 - 현재 순위
                            if (rankChange > 0) {
                                trendStatus = PostTrend.TrendStatus.UP;
                            } else if (rankChange < 0) {
                                trendStatus = PostTrend.TrendStatus.DOWN;
                            } else {
                                trendStatus = PostTrend.TrendStatus.SAME;
                            }
                        } else {
                            trendStatus = PostTrend.TrendStatus.NEW; // 이전 스냅샷에 없으면 신규 진입
                            rankChange = 0; // 신규 진입 시 순위 변화는 0으로 표시
                        }

                        return PostTrend.builder()
                                .snapshot(finalCurrentSnapshot) // 💡 TrendSnapshot 엔티티 참조로 변경
                                .postId(doc.getId())
                                .score(score)
                                .rank(currentRank)
                                .previousRank(previousRank)
                                .trendStatus(trendStatus)
                                .rankChange(rankChange)
                                .postContent(doc.getContent())
                                .userName(doc.getUserName())
                                .userProfileName(doc.getUserProfileName())
                                .snapshotTime(snapshotTime)
                                .build();
                    })
                    .collect(Collectors.toList());

            postTrendRepository.saveAll(postTrendsToSave);

            // 5. 스냅샷 상태를 COMPLETED로 업데이트
            currentSnapshot.complete();
            trendSnapshotRepository.save(currentSnapshot);

            log.info("Finished trend capturing and saving process. Saved {} trends for snapshot ID: {}", postTrendsToSave.size(), finalCurrentSnapshot.getId());

        } catch (Exception e) {
            log.error("Error during trend capturing and saving process", e);
            if (currentSnapshot != null) {
                currentSnapshot.fail(); // 스냅샷 상태를 FAILED로 업데이트
                trendSnapshotRepository.save(currentSnapshot);
            }
            throw new RuntimeException("Failed to capture and save trends", e);
        }
    }

    /**
     * RDB에 저장된 가장 최신의 트렌드 게시글 목록을 조회합니다.
     * 스케줄링 중에도 항상 최신 완료된 스냅샷의 데이터를 반환합니다.
     * @param limit 조회할 게시글의 최대 개수
     * @return 트렌드 게시글 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<TrendResponse> getTopTrends(int limit) {
        if (limit <= 0) {
            limit = TREND_API_LIMIT; // 기본값 설정
        }

        // 1. 가장 최신의 COMPLETED 상태 스냅샷 조회
        Optional<TrendSnapshot> latestCompletedSnapshot =
                trendSnapshotRepository.findFirstByStatusOrderBySnapshotTimeDesc(TrendSnapshot.SnapshotStatus.COMPLETED);

        if (latestCompletedSnapshot.isEmpty()) {
            log.warn("No completed trend snapshot found. Returning empty list.");
            return Collections.emptyList(); // 완료된 스냅샷이 없으면 빈 리스트 반환
        }

        Long snapshotId = latestCompletedSnapshot.get().getId();
        log.debug("Found latest completed snapshot ID: {}", snapshotId);

        // 2. 해당 스냅샷 ID에 해당하는 트렌드 게시글 목록 조회
        // 💡 ManyToOne 관계로 변경됨에 따라 findBySnapshot_IdOrderByScoreDesc로 변경
        List<PostTrend> topTrends = postTrendRepository.findBySnapshot_IdOrderByScoreDesc(snapshotId);

        return topTrends.stream()
                .limit(limit) // API limit 적용
                .map(this::toDto)
                .toList();
    }

    /**
     * 가장 최신의 완료된 스냅샷의 트렌드 데이터를 Map<postId, PostTrend> 형태로 가져옵니다.
     * @return postId를 키로 하는 PostTrend 맵
     */
    private Map<Long, PostTrend> getPreviousTrendsMap() {
        Optional<TrendSnapshot> previousSnapshot =
                trendSnapshotRepository.findFirstByStatusOrderBySnapshotTimeDesc(TrendSnapshot.SnapshotStatus.COMPLETED);

        if (previousSnapshot.isPresent()) {
            // 💡 ManyToOne 관계로 변경됨에 따라 findBySnapshot_IdOrderByScoreDesc로 변경
            List<PostTrend> previousTrends = postTrendRepository.findBySnapshot_IdOrderByScoreDesc(previousSnapshot.get().getId());
            return previousTrends.stream()
                    .collect(Collectors.toMap(PostTrend::getPostId, Function.identity()));
        }
        return Collections.emptyMap();
    }

    /**
     * 특정 시간 범위 내의 활성 게시글 중 스코어링되어 정렬된 상위 N개의 문서를 Elasticsearch에서 조회합니다.
     * 이 메서드는 트렌드 스케줄러에서 사용됩니다.
     * @param since 조회 시작 시간 (예: 24시간 전)
     * @param size 가져올 문서의 최대 개수 (TOP_N_TRENDS)
     * @return 스코어링되고 정렬된 SearchHit<PostDocument> 리스트 (스코어 정보 포함)
     */
    private List<SearchHit<PostDocument>> findScoredAndSortedActivePosts(LocalDateTime since, int size) {
        Query queryBody = PostESQueryBuilder.buildScoredAndSortedActivePosts(since);
        List<co.elastic.clients.elasticsearch._types.SortOptions> sortOptions = PostESQueryBuilder.buildScoreSortOptions();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(_qb -> (co.elastic.clients.util.ObjectBuilder<Query>) queryBody)
                .withSort(sortOptions) // 스코어 기준 정렬 적용
                .withPageable(Pageable.ofSize(size)) // 상위 N개만 가져오도록 제한
                .build();

        SearchHits<PostDocument> hits = operations.search(nativeQuery, PostDocument.class);
        return hits.getSearchHits(); // SearchHit 리스트를 반환하여 _score를 사용할 수 있도록 함
    }

    private TrendResponse toDto(PostTrend postTrend) {
        return new TrendResponse(
                postTrend.getPostId(),
                postTrend.getScore(),
                postTrend.getRank(),
                postTrend.getPreviousRank(),
                postTrend.getTrendStatus(),
                postTrend.getRankChange(),
                postTrend.getPostContent(),
                postTrend.getUserName(),
                postTrend.getUserProfileName(),
                postTrend.getSnapshotTime()
        );
    }


}
