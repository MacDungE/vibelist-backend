package org.example.vibelist.domain.batch.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.batch.service.AudioFeatureBatchService;
import org.example.vibelist.domain.batch.service.EsBatchService;
import org.example.vibelist.domain.batch.service.TrackBatchService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/*
* 세 가지 BatchService 한 번에 실행
* */
@Profile("batch")
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchSequenceRunner implements ApplicationRunner {

    private final AudioFeatureBatchService audioFeatureBatchService;
    private final TrackBatchService trackBatchService;
    private final EsBatchService esBatchService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("▶️ 전체 배치 실행 시작");

        //audioFeatureBatchService.executeBatch();
        //trackBatchService.executeBatch();
        esBatchService.executeBatch();

        log.info("🏁 전체 배치 실행 종료");
    }
}
