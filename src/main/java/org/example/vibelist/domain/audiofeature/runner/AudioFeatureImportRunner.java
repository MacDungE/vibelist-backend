package org.example.vibelist.domain.audiofeature.runner;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.audiofeature.service.AudioFeatureBatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
* AudioFeature Batch 작업만 실행
**/
@Component
@RequiredArgsConstructor
@Profile("audioFeature-batch")
public class AudioFeatureImportRunner implements CommandLineRunner {

    private final AudioFeatureBatchService audioFeatureBatchService;

    @Override
    public void run(String... args) throws Exception {
        audioFeatureBatchService.executeBatch();
    }
}
