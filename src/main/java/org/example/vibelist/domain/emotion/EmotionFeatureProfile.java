package org.example.vibelist.domain.emotion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmotionFeatureProfile {
    /**
     * 감정 검색에 사용할 valence, energy의 범위를 저장하는 객체
     */

    private DoubleRange valence;
    private DoubleRange energy;

}
