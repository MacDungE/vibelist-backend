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
//        Optional<IntegrationTokenInfo> optionalInfo =
//                integrationTokenInfoService.getValidTokenInfo(userid,"SPOTIFY");//userId와 SPOTIFY 가입 계정 찾기
        String accessToken = "";
//        if(optionalInfo.isPresent()) { //user가 spotify회원일때
//            accessToken = optionalInfo.map(IntegrationTokenInfo::getAccessToken).orElseThrow(() -> new IllegalArgumentException("유효하지 않은 access_token입니다."));
//            if (optionalInfo.map(IntegrationTokenInfo::isExpired).orElse(false)) {
//                //access_token refresh해주기
//                log.info("access_token이 만료되었습니다. refresh 진행..");
//                String refreshToken = optionalInfo.map(IntegrationTokenInfo::getRefreshToken).orElseThrow(
//                        ()-> new IllegalArgumentException("해당 User의 refresh_token이 존재하지 않습니다."));
//
//                Map<String,String> tokenMap = spotifyAuthService.refreshAccessToken(refreshToken);
//                String newAccessToken = tokenMap.get("access_token"); //새로 발급받은 access_token
//                Integer expiresIn = Integer.parseInt(tokenMap.get("expires_in"));
//                if(!refreshToken.equals(tokenMap.get("refresh_token"))){ // DB에 저장되어있는 토큰과 동일하지 확인
//                    throw new IllegalArgumentException("refresh_token이 동일하지 않습니다.");
//                }
//                integrationTokenInfoService.updateAccessToken(userid,"SPOTIFY",accessToken,expiresIn);
//            }
//        }
//        else{ //user가 spotify회원이 아니고 개발자 계정을 이용해야할때
            DevAuthToken devAuthToken = devAuthTokenService.getDevAuth("sung_1");
            LocalDateTime expiresAt = devAuthToken.getTokenExpiresAt(); // DB에 저장된 만료시간
            LocalDateTime now = LocalDateTime.now();

            // 현재 시각이 expiresAt 이후라면 토큰 만료된 것
            if (expiresAt == null || now.isAfter(expiresAt.minusSeconds(60))) {
                // access token이 만료되었음 (또는 거의 만료 직전)
                log.info("Access token 만료됨. Refresh 필요.");
                String refreshToken = devAuthToken.getRefreshToken();
                Map<String,String>tokenMap = spotifyAuthService.refreshAccessToken(refreshToken);
                String newAccessToken = tokenMap.get("access_token");
                expiresAt =LocalDateTime.now().plusSeconds(Integer.parseInt(tokenMap.get("expires_in"))); //만료 시간 갱신
                if(!refreshToken.equals(tokenMap.get("refresh_token"))){ // DB에 저장되어있는 토큰과 동일하지 확인
                    throw new IllegalArgumentException("refresh_token이 동일하지 않습니다.");
                }
                devAuthTokenService.updateDev("sung_1",newAccessToken,refreshToken,expiresAt);
            } else {
                log.info("Access token 아직 유효함.");
                accessToken = devAuthToken.getAccessToken();
            }

            //devAuthToken에 저장되어있는 LocalDataTime expires_at을 사용해 이 accessToken이 유효한지 검사

        //}
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
