package org.example.vibelist.domain.playlist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.repository.PlaylistRepository;
import org.example.vibelist.domain.recommend.dto.TrackRsDto;
import org.example.vibelist.domain.track.client.SpotifyApiClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final SpotifyApiClient spotifyApiClient;

    @Transactional
    /*
    PlayList를 생성 후, track들을 insert합니다.
     */
    public void createPlaylist() {
        String userId="Sung_1"; //테스트용
        String accessToken = spotifyApiClient.getAccessToken();
        String url = "https://api.spotify.com/v1/users/" + userId + "/playlsit";

        RestTemplate restTemplate = new RestTemplate();
        //header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.setBearerAuth(accessToken);

        //body 설정

        Map<String,Object> body = new HashMap<>();
        body.put("name", "vibelist Playlist");
        body.put("description", "playlist for vibelist");
        body.put("public", false); // 설정 가능

        //요청 만들기

        HttpEntity<Map<String,Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        log.info("response: {}", response.getBody());
    }
//    @Transactional
//    public void addTrack(List<TrackRsDto> trackRsDtos) {
//
//    }

}
