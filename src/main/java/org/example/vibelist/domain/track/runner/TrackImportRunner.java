package org.example.vibelist.domain.track.runner;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.track.service.TrackBatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Spotify Batch만 별도 실행하는 클래스
 **/
@Component
@RequiredArgsConstructor
@Profile("spotify-batch") // "spotify-batch" profile만 실행
public class TrackImportRunner implements CommandLineRunner {

    private final TrackBatchService trackBatchService;

    @Override
    public void run(String... args) throws Exception {
        trackBatchService.executeBatch();
    }
}
