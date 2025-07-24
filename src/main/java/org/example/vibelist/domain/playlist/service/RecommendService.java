package org.example.vibelist.domain.playlist.service;

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
import org.example.vibelist.domain.playlist.provider.TrackQueryProvider;
import org.example.vibelist.domain.playlist.pool.RecommendPoolService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import java.util.stream.Stream;

import org.example.vibelist.global.response.RsData;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.GlobalException;


@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendService {

    // 감정 분류 및 전이 → 검색 범위 계산 → Elasticsearch 쿼리 실행을 수행하는 추천 서비스
    // 1. 좌표 기반: (valence, energy)를 감정으로 매핑 -> 매핑된 감정 pool에서 가져오기 -> 추천 결과 반환 (List<TrackRsDto>)
    // 2. 텍스트 기반: llm이 반환한 audio feature -> es 검색(fallback: 감정별 pool에서 가져오기) -> 추천 결과 반환 (List<TrackRsDto>)

    private final RecommendPoolService poolService;
    private final TrackQueryProvider queryProvider;
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

        List<TrackRsDto> result = queryProvider.recommendByAnalysis(analysis, 20);

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
        long start = System.currentTimeMillis();

        EmotionType transitioned = profileManager.getTransition(emotion, mode);
        log.info("🔁 전이된 감정: {}", transitioned);

        EmotionFeatureProfile profile = profileManager.getProfile(transitioned);
        log.info("📊 검색 범위 - valence: {} ~ {}, energy: {} ~ {}",
                profile.getValence().getMin(), profile.getValence().getMax(),
                profile.getEnergy().getMin(), profile.getEnergy().getMax());

        String key = "recommendPool:" + transitioned;

        List<TrackRsDto> randTracks = poolService.recommendFromPool(key, 20);

        // ES 직접 검색: fallback
        if (randTracks == null || randTracks.isEmpty()) {
            log.info("❌ Pool MISS - ES 직접 검색만 수행 (pool 저장 안함): key={}", key);
            List<TrackRsDto> result = queryProvider.recommendByProfile(profile, 20); // 20곡 직접 ES에서 가져옴
            long end = System.currentTimeMillis();
            log.info("🎯 추천 결과 반환: 분기=ES직접검색, 곡수={}, 시간={}ms", result.size(), (end - start));
            return result;
        }

        long end = System.currentTimeMillis();
        log.info("🎯 추천 결과 반환: 분기=캐시, 곡수={}, 시간={}ms", randTracks.size(), (end - start));
        return randTracks;
    }

    // es 기반 추천 <- 성능 비교 테스트용(k6)
//    public List<TrackRsDto> recommendByEs(RecommendRqDto request) throws JsonProcessingException {
//        log.info("🎯 recommendDirect 호출 - request: {}", request);
//        log.info("🧭 좌표 기반 추천 - valence: {}, energy: {}, mode: {}", request.getUserValence(), request.getUserEnergy(), request.getMode());
//        EmotionType emotion = profileManager.classify(request.getUserValence(), request.getUserEnergy());
//        log.info("🧠 분류된 감정: {}", emotion);
//        EmotionType transitioned = profileManager.getTransition(emotion, request.getMode());
//        log.info("🔁 전이된 감정: {}", transitioned);
//        EmotionFeatureProfile profile = profileManager.getProfile(transitioned);
//        log.info("📊 검색 범위 - valence: {} ~ {}, energy: {} ~ {}",
//                profile.getValence().getMin(), profile.getValence().getMax(),
//                profile.getEnergy().getMin(), profile.getEnergy().getMax());
//        List<TrackRsDto> result = queryProvider.recommendByProfile(profile, 20);
//        log.info("🎵 추천 결과 반환 - 분기=좌표, 곡수={}", result.size());
//        return result;
//    }

}
