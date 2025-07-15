package org.example.vibelist.domain.post.dto;


import java.util.List;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;

/** 플레이리스트 + 트랙 전체 정보 */
public record PlaylistDetailResponse(
        Long                 id,
        String               spotifyUrl,
        Integer              totalTracks,
        Integer              totalLengthSec,
        List<TrackRsDto>     tracks
) { }