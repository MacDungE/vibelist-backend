package org.example.vibelist.domain.track.runner;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.track.service.TrackBatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("spotify-batch") // "youtube-batch" profile만 실행
public class SpotifyImportRunner implements CommandLineRunner {

    private final TrackBatchService trackBatchService;

    @Override
    public void run(String... args) throws Exception {
        trackBatchService.executeBatch();
    }
}
