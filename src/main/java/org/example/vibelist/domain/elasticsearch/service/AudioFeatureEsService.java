package org.example.vibelist.domain.elasticsearch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.audiofeature.entity.AudioFeature;
import org.example.vibelist.domain.audiofeature.repository.AudioFeatureRepository;
import org.example.vibelist.domain.elasticsearch.dto.AudioFeatureEsDoc;
import org.example.vibelist.domain.elasticsearch.repository.AudioFeatureEsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioFeatureEsService {
    private final AudioFeatureEsRepository audioFeatureEsRepository;//ElasticSearch 접근
    private final AudioFeatureRepository audioFeatureRepository; //Rds 접근

    @Autowired
    private ElasticsearchOperations elasticsearchOperations; // 심층 검색

    @Transactional
    /*
    1000개만 Es에 저장하는 메소드입니다.
     */
    public void executeTestInsert(){
        log.info("테스트 insert 작업 시작");
        long start = System.currentTimeMillis();
        List<AudioFeature> features = audioFeatureRepository
                .findAll(PageRequest.of(0, 1000))
                .getContent();
        audioFeatureEsRepository.saveAll(features.stream().map(this::convertToEs).toList());
        long end = System.currentTimeMillis();
        log.info("작업 종료 소요 시간 {}ms", (end - start));
    }

    @Transactional
    /*
    Rds에서 데이터를 읽어와 Es에 저장하는 메소드입니다.
     */
    public void executeBatchInsert() {
        log.info("Batch Insert 작업 시작");
        int batchSize = 1000;
        long totalStart = System.currentTimeMillis();
        long totalCount = audioFeatureRepository.count(); // RDS의 총 개수
        int totalPages = (int) Math.ceil((double) totalCount / batchSize);

        for (int i = 0; i < totalPages; i++) {
            log.info("Batch {} 시작", i);

            long start = System.currentTimeMillis();
            List<AudioFeature> features = audioFeatureRepository
                    .findAll(PageRequest.of(i, batchSize))
                    .getContent();

            audioFeatureEsRepository.saveAll(
                    features.stream().map(this::convertToEs).toList()
            );

            long end = System.currentTimeMillis();
            log.info("Batch {} 완료, 소요 시간: {}ms", i, (end - start));
        }
        long totalEnd  = System.currentTimeMillis();

        log.info("Rds에서 데이터 불러오기 종료 소요시간 {}", (totalEnd - totalStart));

    }

    public AudioFeatureEsDoc convertToEs(AudioFeature audioFeature) {
        /*
        Rds에 저장되어있는 genre는 하나의 String 값입니다.
        ; 기준으로 Split 했습니다.
         */
        List<String> generList = Arrays.stream(audioFeature.getGenres()
                        .split(";"))
                .map(String::trim)
                .toList();

        //elasticSearch insert을 위한 변환
        AudioFeatureEsDoc esDoc = new AudioFeatureEsDoc();

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

        return esDoc;


    }
}
