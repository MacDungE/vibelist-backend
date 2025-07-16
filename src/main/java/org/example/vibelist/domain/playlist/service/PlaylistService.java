package org.example.vibelist.domain.playlist.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.integration.entity.DevAuthToken;
import org.example.vibelist.domain.integration.entity.IntegrationTokenInfo;
import org.example.vibelist.domain.integration.repository.DevAuthTokenRepository;
import org.example.vibelist.domain.integration.repository.IntegrationTokenInfoRepository;
import org.example.vibelist.domain.integration.service.DevAuthTokenService;
import org.example.vibelist.domain.integration.service.IntegrationTokenInfoService;
import org.example.vibelist.domain.integration.service.SpotifyAuthService;
import org.example.vibelist.domain.playlist.dto.SpotifyPlaylistDto;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaylistService {

    private final SpotifyAuthService spotifyAuthService;

    private final IntegrationTokenInfoService integrationTokenInfoService;

    private final DevAuthTokenService devAuthTokenService;
    @Transactional
    /*
    PlayList를 생성 후, track들을 insert합니다.
     */
    public SpotifyPlaylistDto createPlaylist(Long userid,List<TrackRsDto> tracks) throws Exception {
        //이용자의 accesstoken 가져오기
        String accessToken = resolveValidAccessToken(userid);
        //여기서 로직 정리
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
        String spotifyId = root.get("id").asText();
        addTrack(tracks,spotifyId,accessToken);

        log.info("spotify playlist id : {}",spotifyId);

        SpotifyPlaylistDto spotifyPlaylistDto = new SpotifyPlaylistDto();
        spotifyPlaylistDto.setSpotifyId(spotifyId);
        return spotifyPlaylistDto;
    }
    private String resolveValidAccessToken(Long userid) {
        Optional<IntegrationTokenInfo> optionalInfo = integrationTokenInfoService.getValidTokenInfo(userid, "SPOTIFY");

        if (optionalInfo.isPresent()) {
            IntegrationTokenInfo info = optionalInfo.get();

            if (info.isExpired()) {
                log.info("사용자의 Access Token이 만료됨. Refresh 진행..");
                Map<String, String> tokenMap = spotifyAuthService.refreshAccessToken(info.getRefreshToken());

                if (!info.getRefreshToken().equals(tokenMap.get("refresh_token"))) {
                    throw new IllegalArgumentException("refresh_token이 동일하지 않습니다.");
                }

                String newAccessToken = tokenMap.get("access_token");
                int expiresIn = Integer.parseInt(tokenMap.get("expires_in"));

                integrationTokenInfoService.updateAccessToken(userid, "SPOTIFY", newAccessToken, expiresIn);
                return newAccessToken;
            }

            return info.getAccessToken();
        } else {
            DevAuthToken dev = devAuthTokenService.getDevAuth("sung_1");
            if (dev.getTokenExpiresAt() == null || LocalDateTime.now().isAfter(dev.getTokenExpiresAt().minusSeconds(60))) {
                log.info("개발자의 Access Token 만료됨. Refresh 진행..");

                Map<String, String> tokenMap = spotifyAuthService.refreshAccessToken(dev.getRefreshToken());

                if (!dev.getRefreshToken().equals(tokenMap.get("refresh_token"))) {
                    throw new IllegalArgumentException("refresh_token이 동일하지 않습니다.");
                }

                String newAccessToken = tokenMap.get("access_token");
                LocalDateTime newExpires = LocalDateTime.now().plusSeconds(Integer.parseInt(tokenMap.get("expires_in")));

                devAuthTokenService.updateDev("sung_1", newAccessToken, dev.getRefreshToken(), newExpires);
                return newAccessToken;
            }

            return dev.getAccessToken();
        }
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
