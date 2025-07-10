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

    private EmotionType emotion;
    private EmotionModeType mode;
}
