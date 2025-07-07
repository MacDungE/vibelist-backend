package org.example.vibelist.domain.batch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.audiofeature.service.AudioFeatureBatchService;
import org.example.vibelist.domain.track.service.TrackBatchService;
import org.example.vibelist.domain.youtube.service.YoutubeBatchService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchSequenceRunner implements ApplicationRunner {

    private final TrackBatchService trackBatchService;
    private final YoutubeBatchService youtubeBatchService;
    private final AudioFeatureBatchService audioFeatureBatchService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("▶️ 전체 배치 실행 시작");

        audioFeatureBatchService.executeBatch();
        trackBatchService.executeBatch();
        youtubeBatchService.executeBatch();

        log.info("🏁 전체 배치 실행 종료");
    }
}
