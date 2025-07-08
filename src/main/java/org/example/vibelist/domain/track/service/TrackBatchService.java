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

    /**
     * AudioFeature 엔티티 중 Track이 없는 데이터를 찾아,
     * Spotify API로 메타데이터를 조회하여 Track으로 저장하는 배치 작업을 수행한다.
     * 일정 개수마다 flush를 호출하여 메모리 사용을 최적화하며,
     * 실패한 ID는 로그 파일로 별도 기록한다.
     */
    @Override
    @Transactional
    public void executeBatch() {
        int page = 0;
        int count = 0;
        Set<Long> failedIds = loadFailedTrackIds();
        Page<AudioFeature> afPage;
        do {
            Pageable pageable = PageRequest.of(page++, 1000);
            afPage = audioFeatureRepository.findByTrackIsNull(pageable);
            log.info("🔍 AudioFeature 조회 결과: {}건", afPage.getTotalElements());
            log.info("🔍 현재 페이지 번호: {}", page - 1);

            for (AudioFeature feature : afPage.getContent()) {
                if (failedIds.contains(feature.getId())) {
                    log.info("⏭️ [건너뜀] 이전 실패: {}", feature.getId());
                    continue;
                }
                log.info("🚀 처리 시작 - AudioFeature ID: {}, SpotifyID: {}", feature.getId(), feature.getSpotifyId());

                boolean success = processTrackWithRetry(feature);
                if (!success) {
                    recordFailedTrack(feature.getId());
                }
                if (++count % 1000 == 0) {
                    trackRepository.flush();
                    log.info("💾 {}개 단위 flush 완료", count);
                }
            }
        } while (!afPage.isLast());
    }

    /**
     * Spotify API를 호출하여 트랙 정보를 조회하고 저장한다.
     * 최대 3회까지 재시도하며, 성공 시 true를 반환하고 실패 시 false를 반환한다.
     */
    private boolean processTrackWithRetry(AudioFeature feature) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("🎧 [시도 {}] Spotify ID: {}", attempt, feature.getSpotifyId());
                SpotifyTrackMetaDto dto = spotifyApiClient.getTrackMeta(feature.getSpotifyId());
                log.info("📥 수신된 DTO: {}", dto);

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

                log.info("🎧 트랙 정보: title={}, artist={}, album={}", dto.getTitle(), dto.getArtist(), dto.getAlbum());
                trackRepository.save(track);
                log.info("💾 저장된 Track: {}", track);
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
        log.warn("❗ 최종 실패 - AudioFeature ID: {}", feature.getId());
        return false;
    }

    /**
     * Spotify API 호출 실패한 AudioFeature ID를 로그 파일에 기록한다.
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
     * 실패 로그 파일로부터 AudioFeature ID 목록을 로드한다.
     * 파일이 없거나 읽기에 실패하면 빈 Set을 반환한다.
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
