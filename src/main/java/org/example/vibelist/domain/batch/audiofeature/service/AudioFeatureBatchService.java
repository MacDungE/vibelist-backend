package org.example.vibelist.domain.batch.audiofeature.service;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.batch.service.BatchService;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.response.GlobalException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AudioFeatureBatchService implements BatchService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void executeBatch() {
        String csvFile = "src/main/resources/audio_features.csv";
        String line;
        String delimiter = ",";

        List<Object[]> batchArgs = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = br.readLine(); // Skip header
            //배치 크기 정하기
            int batchSize = 1000;
            int cnt = 1;

            String sql = "INSERT INTO audio_feature " +
                    "(danceability, energy, key, loudness, mode, speechiness, acousticness, instrumentalness, liveness, valence, tempo, duration_ms, time_signature, spotify_id, genres) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            while ((line = br.readLine()) != null) {
                String[] fields = line.split(delimiter, -1); // Allow empty fields

                Object[] args = new Object[] {
                        Double.parseDouble(fields[0]), // danceability
                        Double.parseDouble(fields[1]), // energy
                        Integer.parseInt(fields[2]),   // key
                        Double.parseDouble(fields[3]), // loudness
                        Integer.parseInt(fields[4]),   // mode
                        Double.parseDouble(fields[5]), // speechiness
                        Double.parseDouble(fields[6]), // acousticness
                        Double.parseDouble(fields[7]), // instrumentalness
                        Double.parseDouble(fields[8]), // liveness
                        Double.parseDouble(fields[9]), // valence
                        Double.parseDouble(fields[10]), // tempo
                        Integer.parseInt(fields[11]),   // durationMs
                        Double.parseDouble(fields[12]),   // timeSignature
                        fields[13],                     // spotifyId
                        fields[14]                      // genres
                };

                batchArgs.add(args);

                //배치사이즈만큼 리스트가 차면 배치 작업 진행 후 리스트 초기화
                if (batchArgs.size() >= batchSize) {
                    System.out.println(cnt + "번째 배치 작업 진행");
                    cnt++;
                    jdbcTemplate.batchUpdate(sql, batchArgs);
                    batchArgs.clear();
                }
            }
            //리스트에 남아있는 데이터 처리
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
