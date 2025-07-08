package org.example.vibelist.domain.youtube.runner;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.youtube.service.YoutubeBatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
@Profile("youtube-batch") // "youtube-batch" profile만 실행
public class YoutubeImportRunner implements CommandLineRunner {

    private final YoutubeBatchService youtubeBatchService;

    @Override
    public void run(String... args) throws Exception {
        youtubeBatchService.executeBatch();
    }
}
