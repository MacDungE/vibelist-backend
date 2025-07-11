package org.example.vibelist.domain.track.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.batch.service.BatchService;
import org.example.vibelist.domain.track.properties.SpotifyApiClientManager;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackBatchService implements BatchService {

    private final SpotifyApiClientManager apiClientManager;
    private final TrackRepository trackRepository;
    private final AudioFeatureRepository audioFeatureRepository;

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 3000;
    private static final String FAILED_TRACKS_FILE = "spotify_failed_tracks.log";

    @Override
    public void executeBatch() {
        int count = 0;
        Set<Long> failedIds = loadFailedAudioFeatureIds();
        Page<AudioFeature> afPage;
        do {
            Pageable pageable = PageRequest.of(0, 1000, Sort.by("id").ascending());
            afPage = audioFeatureRepository.findByTrackIsNull(pageable);
            log.info("🔍 AudioFeature 조회 결과: {}건", afPage.getTotalElements());

            for (AudioFeature feature : afPage.getContent()) {
                if (failedIds.contains(feature.getId())) {
                    log.info("⏭️ [건너뜀] 이전 실패: {}", feature.getId());
                    continue;
                }
                log.info("🚀 처리 시작 - AudioFeature ID: {}, SpotifyID: {}", feature.getId(), feature.getSpotifyId());

                String res = processTrackWithRetry(feature);
                if ("false".equals(res)) {
                    recordFailedAudioFeature(feature.getId());
                } else if ("done".equals(res)) {
                    System.exit(0);
                }
            }
        } while (!afPage.isLast());
    }

    private String processTrackWithRetry(AudioFeature feature) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                SpotifyApiClient client = apiClientManager.getCurrentClient();
                log.info("🎧 [시도 {}] Spotify ID: {}", attempt, feature.getSpotifyId());
                SpotifyTrackMetaDto dto = client.getTrackMeta(feature.getSpotifyId());
                log.info("📥 수신된 DTO: {}", dto);

                Track track = Track.builder()
                        .title(dto.getTitle())
                        .artist(dto.getArtist())
                        .album(dto.getAlbum())
                        .durationMs(dto.getDurationMs())
                        .explicit(dto.isExplicit())
                        .popularity(dto.getPopularity())
                        .imageUrl(dto.getImageUrl())
                        .spotifyId(dto.getSpotifyId())
                        .audioFeature(feature)
                        .build();

                feature.setTrack(track);

                log.info("🎧 트랙 정보: title={}, artist={}, album={}", dto.getTitle(), dto.getArtist(), dto.getAlbum());
                trackRepository.save(track);
                log.info("💾 저장된 Track: {}", track);
                log.info("✅ [성공] {} - {}", dto.getTitle(), dto.getArtist());
                return "true";
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("⏱️ [429 Too Many Requests] Spotify 제한 -audio_feature ID: {} | 다른 클라이언트로 전환", feature.getSpotifyId());
                if (!apiClientManager.switchToNextClient()) {
                    return "done";
                }
            } catch (Exception e) {
                log.warn("❌ [실패 {}회차] Spotify ID: {} | {}", attempt, feature.getSpotifyId(), e.getMessage());
                log.debug("🔍 예외", e);
            }

            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ignored) {}
        }
        log.warn("❗ 최종 실패 - AudioFeature ID: {}", feature.getId());
        return "false";
    }

    private void recordFailedAudioFeature(Long trackId) {
        Path path = Paths.get(FAILED_TRACKS_FILE);
        try {
            // 이미 기록된 ID들을 불러옴
            Set<Long> existingIds = new HashSet<>();
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    try {
                        existingIds.add(Long.parseLong(line.trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }

            // 이미 존재하지 않는 경우에만 기록
            if (!existingIds.contains(trackId)) {
                Files.write(path,
                        (trackId + System.lineSeparator()).getBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            log.error("🚨 실패 로그 기록 오류: {}", e.getMessage());
        }
    }

    private Set<Long> loadFailedAudioFeatureIds() {
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