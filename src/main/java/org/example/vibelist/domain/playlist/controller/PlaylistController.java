package org.example.vibelist.domain.playlist.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.integration.service.SpotifyAuthService;
import org.example.vibelist.domain.playlist.dto.SpotifyPlaylistDto;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.service.PlaylistService;
import org.example.vibelist.global.response.GlobalException;
import org.example.vibelist.global.response.ResponseCode;
import org.example.vibelist.global.security.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import org.example.vibelist.global.response.RsData;

@RestController
@RequestMapping("/v1/playlist")
@RequiredArgsConstructor
@Tag(name = "Spotify Playlist 삽입")
public class PlaylistController {
    final private PlaylistService playlistService;

    @Operation(summary = "Spotify에 Playlist 삽입", description = "유저가 선택한 Playlist를 Spotify에 삽입합니다.")
    @PostMapping("/add")
    public ResponseEntity<RsData<?>> addPlaylist(@RequestBody List<TrackRsDto> tracks) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new GlobalException(ResponseCode.AUTH_REQUIRED, "로그인이 필요합니다.");
        }
        RsData<?> result = playlistService.createPlaylist(userId, tracks);
        return ResponseEntity.status(result.isSuccess() ? 201 : 400).body(result);
    }
}
