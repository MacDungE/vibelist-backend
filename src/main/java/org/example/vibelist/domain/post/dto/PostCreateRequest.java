package org.example.vibelist.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.vibelist.domain.playlist.dto.TrackRsDto;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {

    @Schema(description = "게시글 내용", example = "오늘 만든 집중용 재즈 플레이리스트!")
    private String content;

    /** 태그: 중복·공백은 프론트/백엔드 모두에서 필터링 */
    @Schema(description = "태그 목록", example = "[\"study\", \"jazz\"]")
//    @Size(max = 10, message = "태그는 최대 10개까지만 지정 가능합니다.")
    private List<String> tags;

    @Schema(description = "공개 여부", defaultValue = "true")
    private Boolean isPublic;

    /** 새 플레이리스트를 바로 생성할 때 사용 */
    @Schema(description = "트랙 목록")
    private List<TrackRsDto> tracks;



}
