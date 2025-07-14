package org.example.vibelist.domain.playlist.controller;

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
public class PlaylistController {
    final private PlaylistService playlistService;
    @PostMapping("add")
    public ResponseEntity<?> addPlaylist(@RequestBody List<TrackRsDto> trackRsDtos) throws Exception {
            playlistService.createPlaylist(trackRsDtos);
            return ResponseEntity.ok().build();
    }

}
