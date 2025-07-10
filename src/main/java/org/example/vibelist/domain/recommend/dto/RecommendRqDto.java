package org.example.vibelist.domain.recommend.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecommendRqDto {

    private EmotionType emotion;
    private EmotionMode mode;
}
