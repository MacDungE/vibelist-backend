package org.example.vibelist.domain.explore.es;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class PostESQueryBuilder {

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private PostESQueryBuilder() { }

    //    /* ---------- 1. 키워드 검색 ---------- */
    public static Query search(String keyword) {
        return Query.of(q -> q.bool(b -> b
                /* MUST 절 – multi_match 하나만 넣어 간결하게 */
                .must(m -> m.multiMatch(mm -> mm
                        .query(keyword)
                        .fields(
                                "content",
                                "tagsAnalyzed",
                                "playlist.tracks.title",
                                "playlist.tracks.artist",
                                "playlist.tracks.album"
                        )
                        .type(TextQueryType.BestFields)   // ← 기본값과 동일 – 한 field 최고스코어만
                        .operator(Operator.And)           // ← 모든 토큰 포함 (느슨하게 = Or)
                        .fuzziness("AUTO")                // ← 오타 허용
                ))

                /* FILTER 절 – 공개 글만 */
                .filter(f -> f.term(t -> t
                        .field("isPublic")
                        .value(true)
                ))
        ));
    }


    /* ---------- 2. 공개 피드 + 시간 감쇠 정렬 ---------- */
    public static Query feed() {
        return Query.of(q -> q.functionScore(fs -> fs
                .query(Query.of(q2 -> q2.bool(b -> b
                        .filter(f -> f.term(t -> t.field("isPublic").value(true)))
                        .filter(f -> f.bool(fb -> fb.mustNot(mn -> mn.exists(e -> e.field("deletedAt")))))
                )))
                .functions(f -> f.scriptScore(ss -> ss
                        .script(s -> s
                                .source("""
                                    double likes = doc['likeCnt'].size() > 0 ? doc['likeCnt'].value : 0;
                                    double views = doc['viewCnt'].size() > 0 ? doc['viewCnt'].value : 0;
                                    double score = likes * 2 + views;
                                    double hours = (params.now - doc['createdAt'].value.toInstant().toEpochMilli()) / 3600000.0;
                                    return score / Math.sqrt(hours + 2);
                                """)
                                .params("now", JsonData.of(System.currentTimeMillis()))
                        )
                ))
                .boostMode(FunctionBoostMode.Replace)
        ));
    }

    public static List<SortOptions> sortByUpdatedAtDesc() {
        return List.of(
                SortOptions.of(s -> s
                        .field(f -> f.field("createdAt").order(SortOrder.Desc)))
        );
    }

    public static Query buildActivePostsSince(LocalDateTime since, LocalDateTime updatedSince) {
        String isoCreated = since.atOffset(ZoneOffset.UTC).format(ISO_DATE_TIME_FORMATTER);
        String isoUpdated = updatedSince.atOffset(ZoneOffset.UTC).format(ISO_DATE_TIME_FORMATTER);

        return Query.of(q -> q.bool(b -> b
                // ① deletedAt 없어야 함
                .filter(f -> f.bool(fb -> fb.mustNot(mn -> mn.exists(e -> e.field("deletedAt")))))

                // ② 공개된 글만
                .filter(f -> f.term(t -> t.field("isPublic").value(true)))

                // ③ createdAt ≥ since OR updatedAt ≥ updatedSince
                .filter(f -> f.bool(b2 -> b2
                        .should(s -> s.range(r -> r
                                .untyped(u -> u.field("createdAt").gte(JsonData.of(isoCreated)))
                        ))
                        .should(s -> s.range(r -> r
                                .untyped(u -> u.field("updatedAt").gte(JsonData.of(isoUpdated)))
                        ))
                        .minimumShouldMatch("1")  // 둘 중 하나만 만족하면 OK
                ))
        ));
    }

    public static Query buildScoredAndSortedActivePosts(LocalDateTime since) {
        String iso = since.atOffset(ZoneOffset.UTC).format(ISO_DATE_TIME_FORMATTER);

        Query baseQuery = Query.of(q -> q.bool(b -> b
                .must(m -> m.matchAll(ma -> ma))
                .filter(f -> f.bool(fb -> fb.mustNot(mn -> mn.exists(e -> e.field("deletedAt")))))
                .filter(f -> f.bool(b2 -> b2
                        .should(s -> s.range(r -> r.untyped(u -> u.field("createdAt").gte(JsonData.of(iso)))))
                        .should(s -> s.range(r -> r.untyped(u -> u.field("updatedAt").gte(JsonData.of(iso)))))
                        .minimumShouldMatch("1")
                ))
        ));

        return Query.of(q -> q.functionScore(fs -> fs
                .query(baseQuery)
                .functions(f -> f.fieldValueFactor(fvf -> fvf
                        .field("likeCnt")
                        .factor(2.0)
                        .missing(0.0)
                ).weight(1.0))
                .functions(f -> f.fieldValueFactor(fvf -> fvf
                        .field("viewCnt")
                        .factor(1.0)
                        .missing(0.0)
                ).weight(1.0))
                .scoreMode(FunctionScoreMode.Sum)
        ));
    }

    public static List<SortOptions> buildScoreSortOptions() {
        return Collections.singletonList(
                SortOptions.of(so -> so
                        .score(s -> s.order(SortOrder.Desc))
                )
        );
    }
}
