package org.example.vibelist.domain.playlist.dto;

import lombok.*;
import org.example.vibelist.domain.playlist.emotion.EmotionModeType;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendRqDto {
    // 추천 요청 시 클라이언트로부터 받는 감정 좌표 및 모드 정보를 담는 DTO

    private double userValence;
    private double userEnergy;
    private EmotionModeType mode;
}
