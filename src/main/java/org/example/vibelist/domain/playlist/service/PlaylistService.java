package org.example.vibelist.domain.playlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.auth.service.DevAuthTokenService;
import org.example.vibelist.domain.batch.spotify.service.SpotifyAuthService;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {

    private final SpotifyAuthService spotifyAuthService;
    @Transactional
    /*
    PlayList를 생성 후, track들을 insert합니다.
     */
    public void createPlaylist(List<TrackRsDto> trackRsDtos) throws Exception {
        String accessToken;

        accessToken = spotifyAuthService.getAccessToken();

        String userId = spotifyAuthService.getSpotifyUserId(accessToken);

        String url = "https://api.spotify.com/v1/users/" + userId + "/playlists";

        RestTemplate restTemplate = new RestTemplate();
        //header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization","Bearer "+accessToken);

        //body 설정

        Map<String,Object> body = new HashMap<>();
        body.put("name", "vibelist Playlist");
        body.put("description", "playlist for vibelist");
        body.put("public", false); // 설정 가능

        //요청 만들기

        HttpEntity<Map<String,Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        //playlist_id 추출
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(responseBody);
        String playlistId = root.get("id").asText();
        addTrack(trackRsDtos,playlistId,accessToken);

        log.info("response: {}", response.getBody());
    }
    @Transactional
    public void addTrack(List<TrackRsDto> trackRsDtos,
                         String playlistId,
                         String accessToken){
        String url ="https://api.spotify.com/v1/playlists/"+playlistId+"/tracks";
        RestTemplate restTemplate = new RestTemplate();
        //header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization","Bearer "+accessToken);

        //body 설정
        List<String> uris = trackRsDtos.stream()
                .map(track->"spotify:track:"+track.getSpotifyId())
                .collect(Collectors.toList());
        Map<String,Object> body = new HashMap<>();
        body.put("uris",uris );

        //요청 만들기

        HttpEntity<Map<String,Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        log.info("Track add response: {}", response.getBody());
    }

}
