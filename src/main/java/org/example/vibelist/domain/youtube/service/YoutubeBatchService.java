package org.example.vibelist.domain.youtube.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.batch.service.BatchService;
import org.example.vibelist.domain.track.entity.Track;
import org.example.vibelist.domain.track.repository.TrackRepository;
import org.example.vibelist.domain.youtube.client.YoutubeApiClient;
import org.example.vibelist.domain.youtube.dto.YoutubeVideoMetaDto;
import org.example.vibelist.domain.youtube.entity.Youtube;
import org.example.vibelist.domain.youtube.repository.YoutubeRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeBatchService implements BatchService {

    private final TrackRepository trackRepository;
    private final YoutubeApiClient youtubeApiClient;
    private final YoutubeRepository youtubeRepository;

    private static final int MAX_RETRIES = 3; // 재시도 횟수
    private static final int RETRY_DELAY_MS = 3000; // API 요청 제한 시 딜레이 시간
    private static final String FAILED_TRACKS_FILE = "failed_tracks.log"; // 실패한 트랙 -> 따로 로그 파일 저장

    /**
     * 배치 작업을 수행하여 Youtube 정보가 없는 트랙들을 찾아
     * Youtube API로 메타데이터를 조회한 후 DB에 저장한다.
     * 일정 단위마다 flush를 호출하여 DB에 반영하며,
     * 실패한 트랙은 로그 파일에 기록한다.
     */

    @Override
    @Transactional
    public void executeBatch() {
        int page = 0;
        int count = 0;
        Set<Long> failedIds = loadFailedTrackIds();
        Page<Track> trackPage;

        do {
            Pageable pageable = PageRequest.of(page++, 100);
            trackPage = trackRepository.findByYoutubeIsNull(pageable);

            for (Track track : trackPage.getContent()) {
                if (failedIds.contains(track.getId())) {
                    log.info("⏭️ [건너뜀] 이전 실패: {}", track.getId());
                    continue;
                }

                boolean success = processTrackWithRetry(track);
                if (!success) {
                    recordFailedTrack(track.getId());
                }

                if (++count % 100 == 0) {
                    youtubeRepository.flush();
                    log.info("💾 {}개 단위 flush 완료", count);
                }
            }
        } while (!trackPage.isLast());
    }

    /**
     * 트랙 정보를 기반으로 Youtube API를 호출하여 메타데이터를 수집하고 저장한다.
     * 최대 3회까지 재시도하며, 실패 시 false를 반환한다.
     */
    private boolean processTrackWithRetry(Track track) {
        String query = track.getTitle() + " " + track.getArtist();
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("🎵 [시도 {}] {}", attempt, query);
                YoutubeVideoMetaDto videoMeta = youtubeApiClient.getYoutubeVideo(query);

                if (isQuotaExceeded(videoMeta)) {
                    log.warn("🚫 [쿼터 초과] 요청 중단 후 {}ms 대기", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                    continue;
                }

                Youtube youtube = Youtube.builder()
                        .url(videoMeta.getUrl())
                        .durationSeconds(videoMeta.getDuration())
                        .build();
                youtube.setTrack(track);
                youtubeRepository.save(youtube);

                log.info("✅ [성공] {} - {} ({}초)", track.getTitle(), track.getArtist(), videoMeta.getDuration());
                return true;

            } catch (Exception e) {
                log.warn("❌ [실패 {}회차] {} - {} | {}", attempt, track.getTitle(), track.getArtist(), e.getMessage());
                log.debug("🔍 예외", e);
            }
        }
        return false;
    }

    /**
     * Youtube API 응답이 쿼터 초과(quotaExceeded) 상태인지 여부를 판단한다.
     */
    private boolean isQuotaExceeded(YoutubeVideoMetaDto dto) {
        return dto.getUrl() != null && dto.getUrl().contains("quotaExceeded");
    }

    /**
     * 실패한 트랙 ID를 로그 파일에 기록한다.
     */
    private void recordFailedTrack(Long trackId) {
        try {
            Files.write(Paths.get(FAILED_TRACKS_FILE),
                    (trackId + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("🚨 실패 로그 기록 오류: {}", e.getMessage());
        }
    }

    /**
     * 이전에 실패한 트랙 ID 목록을 로그 파일로부터 읽어온다.
     */
    private Set<Long> loadFailedTrackIds() {
        Set<Long> ids = new HashSet<>();
        Path path = Paths.get(FAILED_TRACKS_FILE);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    try {
                        ids.add(Long.parseLong(line.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            } catch (IOException e) {
                log.error("⚠️ 실패 로그 로딩 오류: {}", e.getMessage());
            }
        }
        return ids;
    }
}
