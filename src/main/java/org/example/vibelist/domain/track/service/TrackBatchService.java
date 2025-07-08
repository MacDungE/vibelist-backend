package org.example.vibelist.domain.track.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.batch.service.BatchService;
import org.example.vibelist.domain.track.repository.TrackRepository;
import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.audiofeature.repository.AudioFeatureRepository;
import org.example.vibelist.domain.track.entity.Track;
import org.example.vibelist.domain.track.client.SpotifyApiClient;
import org.example.vibelist.domain.track.dto.SpotifyTrackMetaDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackBatchService implements BatchService {

    private final TrackRepository trackRepository;
    private final AudioFeatureRepository audioFeatureRepository;
    private final SpotifyApiClient spotifyApiClient;

    private static final int MAX_RETRIES = 3; // 재시도 횟수
    private static final int RETRY_DELAY_MS = 3000; // API 요청 제한 시 딜레이 시간
    private static final String FAILED_TRACKS_FILE = "spotify_failed_tracks.log"; // 실패한 트랙 -> 따로 로그 파일 저장

    @Override
    @Transactional
    public void executeBatch() {
        int page = 0;
        Set<Long> failedIds = loadFailedTrackIds();
        Page<AudioFeature> afPage;
        do {
            Pageable pageable = PageRequest.of(page++, 100);
            afPage = audioFeatureRepository.findByTrackIsNull(pageable);

            for (AudioFeature feature : afPage.getContent()) {
                if (failedIds.contains(feature.getId())) {
                    log.info("⏭️ [건너뜀] 이전 실패: {}", feature.getId());
                    continue;
                }

                boolean success = processTrackWithRetry(feature);
                if (!success) {
                    recordFailedTrack(feature.getId());
                }
            }
        } while (!afPage.isLast());
    }

    private boolean processTrackWithRetry(AudioFeature feature) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("🎧 [시도 {}] Spotify ID: {}", attempt, feature.getSpotifyId());
                SpotifyTrackMetaDto dto = spotifyApiClient.getTrackMeta(feature.getSpotifyId());

                Track track = new Track();
                track.setTitle(dto.getTitle());
                track.setArtist(dto.getArtist());
                track.setAlbum(dto.getAlbum());
                track.setDurationMs(dto.getDurationMs());
                track.setExplicit(dto.isExplicit());
                track.setPopularity(dto.getPopularity());
                track.setImageUrl(dto.getImageUrl());
                track.setAudioFeature(feature);
                feature.setTrack(track);

                trackRepository.save(track);
                log.info("✅ [성공] {} - {}", dto.getTitle(), dto.getArtist());
                return true;

            } catch (Exception e) {
                log.warn("❌ [실패 {}회차] Spotify ID: {} | {}", attempt, feature.getSpotifyId(), e.getMessage());
                log.debug("🔍 예외", e);
            }

            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ignored) {}
        }

        return false;
    }

    private boolean isQuotaExceeded(SpotifyTrackMetaDto dto) {
        //return dto.getUrl() != null && dto.getUrl().contains("quotaExceeded");
        return true;
    }

    private void recordFailedTrack(Long trackId) {
        try {
            Files.write(Paths.get(FAILED_TRACKS_FILE),
                    (trackId + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("🚨 실패 로그 기록 오류: {}", e.getMessage());
        }
    }

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
