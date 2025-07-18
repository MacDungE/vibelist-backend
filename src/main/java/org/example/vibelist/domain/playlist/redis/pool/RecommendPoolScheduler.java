package org.example.vibelist.domain.playlist.redis.pool;

import jakarta.annotation.PostConstruct;
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

import java.util.List;
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

    @Scheduled(cron = "0 0 * * * *") // 매 시 정각마다 캐싱
    public void refreshAllEmotionPools() {
        log.info("⏰ Pool 스케줄러 실행");
        for (EmotionType emotion : EmotionType.values()) {
            String key = "recommendPool:" + emotion;
            EmotionFeatureProfile profile = profileManager.getProfile(emotion);
            Set<TrackRsDto> pool = poolProvider.createPool(emotion, profile, 1000);
            poolService.savePool(key, pool, 65, TimeUnit.MINUTES);
            log.info("🔁 Pool 새로 갱신: key={}, size={}", key, pool.size());
        }
    }
}
