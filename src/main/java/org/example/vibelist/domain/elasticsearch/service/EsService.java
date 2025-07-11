package org.example.vibelist.domain.elasticsearch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.audiofeature.repository.AudioFeatureRepository;
import org.example.vibelist.domain.elasticsearch.dto.EsDoc;
import org.example.vibelist.domain.elasticsearch.dto.TrackMetrics;
import org.example.vibelist.domain.elasticsearch.repository.EsRepository;
import org.example.vibelist.domain.track.entity.Track;
import org.example.vibelist.domain.track.repository.TrackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EsService {
    private final EsRepository esRepository;//ElasticSearch 접근
    private final AudioFeatureRepository audioFeatureRepository; //Rds 접근
    private final TrackRepository trackRepository;


    @Autowired
    private ElasticsearchOperations elasticsearchOperations; // 심층 검색

    @Transactional
    /*
    1000개만 Es에 저장하는 메소드입니다.
     */
    public void executeTestInsert() {
        log.info("테스트 insert 작업 시작");
        long start = System.currentTimeMillis();

        //RDS에 접근하여 track을 가져오기
        List<Track> tracks = trackRepository.findAll(PageRequest.of(0, 100)).getContent();

        //track_id 추출
        List<Long> trackIds = tracks.stream()
                .map(Track::getId)
                .toList();

        /*
        Key = track_id
        Value = track_id에 대응되는 AudioFeature
         */
        Map<Long, AudioFeature> audioFeatureMap = audioFeatureRepository.findAllById(trackIds).stream()
                .collect(Collectors.toMap(AudioFeature::getId, Function.identity()));


        List<EsDoc> esDocs = new ArrayList<>();//ElasticSearch에 접근할 Document

        for (Track track : tracks) {
            AudioFeature af = audioFeatureMap.get(track.getId());

            if (af == null){
                log.warn("track에 대응되는 AudioFeature를 찾을 수 없습니다. trackid : {} ", track.getId());
                continue;
            }
            esDocs.add(convertToEs(af, track));
        }

        esRepository.saveAll(esDocs);


        long end = System.currentTimeMillis();
        log.info("작업 종료 소요 시간 {}ms", (end - start));
    }

//    @Transactional
//    /*
//    Rds에서 모든 데이터를 읽어와 Es에 저장하는 메소드입니다.
//     */
//    public void executeBatchInsert() {
//        log.info("Batch Insert 작업 시작");
//        int batchSize = 1000;
//        long totalStart = System.currentTimeMillis();
//        long totalCount = audioFeatureRepository.count(); // RDS의 총 개수
//        int totalPages = (int) Math.ceil((double) totalCount / batchSize);
//
//        for (int i = 0; i < totalPages; i++) {
//            log.info("Batch {} 시작", i);
//
//            long start = System.currentTimeMillis();
//            List<AudioFeature> features = audioFeatureRepository
//                    .findAll(PageRequest.of(i, batchSize))
//                    .getContent();
//
////            esRepository.saveAll(
////                    features.stream().map(this::convertToEs).toList()
////            );
//
//            long end = System.currentTimeMillis();
//            log.info("Batch {} 완료, 소요 시간: {}ms", i, (end - start));
//        }
//        long totalEnd  = System.currentTimeMillis();
//
//        log.info("Rds에서 데이터 불러오기 종료 소요시간 {}", (totalEnd - totalStart));
//
//    }


    public EsDoc convertToEs(AudioFeature audioFeature, Track track) {
        /*
        Rds에 저장되어있는 genre는 하나의 String 값입니다.
        ; 기준으로 Split 했습니다.
         */
        List<String> generList = Arrays.stream(audioFeature.getGenres()
                        .split(";"))
                .map(String::trim)
                .toList();

        //elasticSearch insert을 위한 변환
        EsDoc esDoc = new EsDoc();

        esDoc.setId(audioFeature.getId().toString());
        esDoc.setDanceability(audioFeature.getDanceability());
        esDoc.setEnergy(audioFeature.getEnergy());
        esDoc.setKey(audioFeature.getKey());
        esDoc.setLoudness(audioFeature.getLoudness());
        esDoc.setMode(audioFeature.getMode());
        esDoc.setSpeechiness(audioFeature.getSpeechiness());
        esDoc.setAcousticness(audioFeature.getAcousticness());
        esDoc.setInstrumentalness(audioFeature.getInstrumentalness());
        esDoc.setLiveness(audioFeature.getLiveness());
        esDoc.setValence(audioFeature.getValence());
        esDoc.setTempo(audioFeature.getTempo());
        esDoc.setDurationMs(audioFeature.getDurationMs());
        esDoc.setTimeSignature(audioFeature.getTimeSignature());
        esDoc.setGenres(generList);
        esDoc.setSpotifyId(audioFeature.getSpotifyId());

        TrackMetrics trackMetrics = new TrackMetrics();
        trackMetrics.setId(track.getId());
        trackMetrics.setAlbum(track.getAlbum());
        trackMetrics.setArtist(track.getArtist());
        trackMetrics.setTitle(track.getTitle());
        trackMetrics.setPopularity(track.getPopularity());
        trackMetrics.setExplicit(track.isExplicit());
        trackMetrics.setImageUrl(track.getImageUrl());
        esDoc.setTrackMetrics(trackMetrics);


        return esDoc;
    }
}
