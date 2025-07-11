package org.example.vibelist.domain.playlist.emotion;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmotionType {
    DEPRESSED, SLEEPY, TENSE,
    SAD, NEUTRAL, FEAR,
    CALM, JOY, EXCITED
}

    /**
     * 감정 분석: valence와 energy 값을 기준으로 9가지 감정 중 하나로 분류합니다.
     *
     * 분류 기준은 다음과 같이 두 피처를 3등분하여 총 3x3 감정 공간을 만듭니다:
     * - valence (감정의 긍정성): Low < 0.281, Mid < 0.586, High >= 0.586
     * - energy (에너지 수준): Low < 0.415, Mid < 0.722, High >= 0.722
     *
     * 감정 구역:
     * - Low valence + Low energy     → DEPRESSED
     * - Low valence + Mid energy     → SLEEPY
     * - Low valence + High energy    → TENSE
     * - Mid valence + Low energy     → SAD
     * - Mid valence + Mid energy     → NEUTRAL
     * - Mid valence + High energy    → FEAR
     * - High valence + Low energy    → CALM
     * - High valence + Mid energy    → JOY
     * - High valence + High energy   → EXCITED
     *
     * @param valence 0.0 ~ 1.0 사이의 긍정성 지수
     * @param energy 0.0 ~ 1.0 사이의 에너지 지수
     * @return EmotionType 해당 곡의 감정 유형
     */

