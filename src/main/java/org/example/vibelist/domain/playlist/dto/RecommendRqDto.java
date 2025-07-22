package org.example.vibelist.domain.playlist.dto;

import lombok.*;
import org.example.vibelist.domain.playlist.emotion.type.EmotionModeType;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "추천 요청 DTO (텍스트 또는 좌표 기반)")
public class RecommendRqDto {
    // 추천 요청 시 클라이언트로부터 받는 감정 좌표 및 모드 정보를 담는 DTO
    @Schema(description = "추천 텍스트(자연어)", example = "기분 좋은 여름 노래 추천해줘", required = false)
    private String text;
    @Schema(description = "사용자 valence(감정 좌표)", example = "0.7", required = false)
    private Double userValence;
    @Schema(description = "사용자 energy(감정 좌표)", example = "0.5", required = false)
    private Double userEnergy;
    @Schema(description = "추천 모드", example = "DEFAULT", required = false)
    private EmotionModeType mode;
}
