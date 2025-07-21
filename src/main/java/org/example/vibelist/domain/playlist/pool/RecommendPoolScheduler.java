package org.example.vibelist.domain.playlist.pool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionFeatureProfile;
import org.example.vibelist.domain.playlist.emotion.profile.EmotionProfileManager;
import org.example.vibelist.domain.playlist.emotion.type.EmotionType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendPoolScheduler {

    private final RecommendPoolService poolService;
    private final EmotionPoolProvider poolProvider;
    private final EmotionProfileManager profileManager;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        refreshAllEmotionPools();
    }

    @Scheduled(cron = "0 0/30 * * * *") // 30분마다
    public void refreshAllEmotionPools() {
        log.info("⏰ Pool 스케줄러 실행");

//        감정 pool 갱신 지연 시간 (1분) <- 조절 가능
//        int delayMinutes = 1;
//        int idx = 0;
        for (EmotionType emotion : EmotionType.values()) {
            String key = "recommendPool:" + emotion;
            EmotionFeatureProfile profile = profileManager.getProfile(emotion);
            Set<TrackRsDto> pool = poolProvider.createPool(emotion, profile, 5000);
            poolService.savePool(key, pool, 35, TimeUnit.MINUTES);
            log.info("🔁 Pool 새로 갱신: key={}, size={}", key, pool.size());

//            // 마지막 pool 아니면 딜레이
//            if (++idx < EmotionType.values().length) {
//                try {
//                    Thread.sleep(delayMinutes * 60 * 1000L); // 1 분 대기
//                } catch (InterruptedException e) {
//                    log.error("Pool 갱신 딜레이 중 오류 발생", e);
//                    Thread.currentThread().interrupt(); // 인터럽트 상태 복원
//                }
//            }
        }
    }
}
