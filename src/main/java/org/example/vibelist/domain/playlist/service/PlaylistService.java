package org.example.vibelist.domain.playlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.playlist.repository.PlaylistRepository;
import org.example.vibelist.domain.recommend.dto.TrackRsDto;
import org.example.vibelist.domain.track.client.SpotifyApiClient;
import org.example.vibelist.domain.track.entity.Track;
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
    private final PlaylistRepository playlistRepository;
    private final SpotifyApiClient spotifyApiClient;

    @Transactional
    /*
    PlayList를 생성 후, track들을 insert합니다.
     */
    public void createPlaylist(List<TrackRsDto> trackRsDtos) throws Exception {
        String userId="31bsfolbgiksxcsa4o5dg2ofvmye"; //테스트용
        /*
        accessToken 또한 테스트용, spotify 로그인시 저장된 값을 복사해서 사용했습니다.
        유저가 Spotify로 로그인한 Case->유저 spotify로 로그인할때 넘어온 accesstoken 사용
        유저가 Spotify로 로그인 하지 않은 Case-> 개발자가 직접 accessToken 받아옴

        */
        String accessToken = "BQAN9oKNYuRxuUPOOCEnQK8pfFgJ3DWfjAyFs7H0nRH4E66atmkRh2UnhU0sfwPWhk0B2ft3Pn-w2wcRajIgJSgJchEQQYRYo7rG8CDT4j5UNrNE-cPXHJk4oLDXwGTF-Ibm8ZGWZQNf_OTA0apmpOF0VZtps-3z4QP1SgfMT1mEf2d5Mj80xGnniT7gnwEM6OHHBh4M4ZrHEI0l4rHJ4C_OowZiwkeIrQZX8hf0NqYLta1a4e19jK-yyMYSzgNdL4ZVYPRrGeumbrxkMsbI8rY5wmGtXPHL7c0MgxwcO5RH_h64urF-C06hcZZb_CIo0PJTjej-LGj2RJhMD1IHWrwQDSsaGfzWVr4KGPk7xJMfkRN8jwtMX-xSKfiHixNXqQ";
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
