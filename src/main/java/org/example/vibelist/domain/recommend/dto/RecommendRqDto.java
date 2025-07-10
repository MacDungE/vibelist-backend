package org.example.vibelist.domain.recommend.dto;

import lombok.*;
import org.example.vibelist.domain.emotion.EmotionModeType;
import org.example.vibelist.domain.emotion.EmotionType;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendRqDto {

    private double userValence;
    private double userEnergy;
    private EmotionModeType mode;
}
