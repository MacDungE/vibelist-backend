package org.example.vibelist.domain.playlist.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.auth.service.DevAuthTokenService;
import org.example.vibelist.domain.batch.spotify.service.SpotifyAuthService;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.service.PlaylistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/playlist")
@RequiredArgsConstructor
@Tag(name = "Spotify Playlist 삽입")
public class PlaylistController {
    final private PlaylistService playlistService;
    final private SpotifyAuthService spotifyAuthService;

    @Operation(summary = "Spotify에 Playlist 삽입", description = "유저가 선택한 Playlist를 Spotify에 삽입합니다. 개발자가 최초에 로그인을 하지 않았다면, login-dev 호출이 필요합니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "성공적으로 Playlist가 생성되고, Track 리스트가 응답됩니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TrackRsDto.class),
                            examples = @ExampleObject(
                                    name = "샘플 응답",
                                    summary = "Playlist 생성 후 반환된 Track 리스트 예시",
                                    value = "[\n" +
                                            "  {\n" +
                                            "    \"spotifyId\": \"1NZP1K2kgSAgnMRy9HEXVg\",\n" +
                                            "    \"durationMs\": 112882,\n" +
                                            "    \"trackId\": \"7816\",\n" +
                                            "    \"title\": \"Liberator (Live)\",\n" +
                                            "    \"artist\": \"Spear Of Destiny\",\n" +
                                            "    \"album\": \"Best of Spear of Destiny (Live at the Forum)\",\n" +
                                            "    \"popularity\": 1,\n" +
                                            "    \"explicit\": false,\n" +
                                            "    \"imageUrl\": \"https://i.scdn.co/image/ab67616d0000b27328a6b3a33210343626f9eca3\"\n" +
                                            "  },\n" +
                                            "  {\n" +
                                            "    \"spotifyId\": \"1JblQH4F1xPaXT4SbsWn6F\",\n" +
                                            "    \"durationMs\": 172207,\n" +
                                            "    \"trackId\": \"11925\",\n" +
                                            "    \"title\": \"Atlas\",\n" +
                                            "    \"artist\": \"Ceano\",\n" +
                                            "    \"album\": \"Atlas\",\n" +
                                            "    \"popularity\": 0,\n" +
                                            "    \"explicit\": false,\n" +
                                            "    \"imageUrl\": \"https://i.scdn.co/image/ab67616d0000b27387193f2bedc67ee4eaf68448\"\n" +
                                            "  }\n" +
                                            "]"
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "사용자 또는 개발자 로그인 필요", content = @Content),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터", content = @Content)
    })
    @PostMapping("add")
    public ResponseEntity<?> addPlaylist(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "추가할 트랙들(TrackRsDto)의 리스트",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TrackRsDto.class),
                    examples = @ExampleObject(
                            value = "[\n" +
                                    "  {\n" +
                                    "    \"spotifyId\": \"1NZP1K2kgSAgnMRy9HEXVg\",\n" +
                                    "    \"durationMs\": 112882,\n" +
                                    "    \"trackId\": \"7816\",\n" +
                                    "    \"title\": \"Liberator (Live)\",\n" +
                                    "    \"artist\": \"Spear Of Destiny\",\n" +
                                    "    \"album\": \"Best of Spear of Destiny (Live at the Forum)\",\n" +
                                    "    \"popularity\": 1,\n" +
                                    "    \"explicit\": false,\n" +
                                    "    \"imageUrl\": \"https://i.scdn.co/image/ab67616d0000b27328a6b3a33210343626f9eca3\"\n" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"spotifyId\": \"1JblQH4F1xPaXT4SbsWn6F\",\n" +
                                    "    \"durationMs\": 172207,\n" +
                                    "    \"trackId\": \"11925\",\n" +
                                    "    \"title\": \"Atlas\",\n" +
                                    "    \"artist\": \"Ceano\",\n" +
                                    "    \"album\": \"Atlas\",\n" +
                                    "    \"popularity\": 0,\n" +
                                    "    \"explicit\": false,\n" +
                                    "    \"imageUrl\": \"https://i.scdn.co/image/ab67616d0000b27387193f2bedc67ee4eaf68448\"\n" +
                                    "  }\n" +
                                    "]"
                    )
            )
    ) @RequestBody List<TrackRsDto> trackRsDtos) throws Exception {


        playlistService.createPlaylist(trackRsDtos);
            return ResponseEntity.ok().body(trackRsDtos);
    }

    @GetMapping("/login-dev")
    public void redirectToSpotify(HttpServletResponse response) throws IOException {
        response.sendRedirect(spotifyAuthService.getAuthorizationUrl());
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam("code") String code) {
        String accessToken = spotifyAuthService.exchangeCodeForTokens(code);
        String refreshToken = spotifyAuthService.getRefreshToken();
        return ResponseEntity.ok("Access token & Refresh token 발급 완료!" +
                                        "\n access_token : " + accessToken +
                                        "\n refresh_token :"+ refreshToken);
    }
}
