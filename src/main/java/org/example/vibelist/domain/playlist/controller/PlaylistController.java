package org.example.vibelist.domain.playlist.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;
import org.example.vibelist.domain.playlist.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/playlist")
@RequiredArgsConstructor
@Tag(name = "Spotify Playlist 삽입")
public class PlaylistController {
    final private PlaylistService playlistService;
    @Operation(summary = "Spotify에 Playlist 삽입", description = "유저가 선택한 Playlist를 Spotify에 삽입합니다.")
    @PostMapping("add")
    public ResponseEntity<?> addPlaylist(@RequestBody List<TrackRsDto> trackRsDtos) throws Exception {
            playlistService.createPlaylist(trackRsDtos);
            return ResponseEntity.ok().build();
    }

}
