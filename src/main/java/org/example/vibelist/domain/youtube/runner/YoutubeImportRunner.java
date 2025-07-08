package org.example.vibelist.domain.youtube.runner;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.youtube.service.YoutubeBatchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
* Youtube Batch만 별도 실행 시 실행하는 클래스
**/
@Component
@RequiredArgsConstructor
@Profile("youtube-batch")
public class YoutubeImportRunner implements CommandLineRunner {

    private final YoutubeBatchService youtubeBatchService;

    @Override
    public void run(String... args) throws Exception {
        youtubeBatchService.executeBatch();
    }
}
