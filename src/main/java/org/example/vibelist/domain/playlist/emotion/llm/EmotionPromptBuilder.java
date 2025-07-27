package org.example.vibelist.domain.playlist.emotion.llm;

import org.example.vibelist.domain.playlist.emotion.type.EmotionModeType;

public class EmotionPromptBuilder {
    public static String build(String userText, EmotionModeType mode) {
        return """
        아래 텍스트는 사용자의 감정 설명이며, 모드는 감정 전이(유지/변화)의 방향성을 나타냅니다.
        그러나 **이 출력에서는 감정 설명만 고려**하며, 모드는 참고용으로만 입력됩니다.
        검색에 사용할 Audio Feature의 범위와, 최종 감정 분류(emotionType)를 모두 JSON 형식으로만 출력하세요.
        
        - 감정 설명: 사용자의 현재 감정 상태를 의미하는 자유로운 텍스트
        - 모드: maintain, elevate, calm-down, reverse 중 하나, 감정 유지/상승/진정/역전 (참고용)
        
        
        사용될 Audio Feature:
        - danceability (0.0~1.0, 주로 0.2~0.9)
        - energy       (0.0~1.0, 주로 0.1~1.0)
        - speechiness  (0.1~1.0, 주로 0.2 이상)
        - acousticness (0.0~1.0, 양극 분포)
        - liveness     (0.0~1.0, 주로 0.1~0.4)
        - valence      (0.0~1.0, 주로 0.1~0.8)
        - loudness     (-60~0 dB, 주로 -30~-5 dB)
        - tempo        (0~250, 주로 60~180)
        실제 곡이 존재할 수 있는 범위로 산출하세요.
        
        감정 타입(emotionType)은 아래 중 하나만 선택해서 반드시 포함:
        [DEPRESSED, SLEEPY, TENSE, SAD, NEUTRAL, FEAR, CALM, JOY, EXCITED]
        
        감정 설명: "%s"
        모드: "%s" (참고용)
        
        JSON 출력 예시 (슬픔에서 elevate 모드일 때):
        {
            "emotionType": "JOY",
            "danceability": { "min": 0.3, "max": 0.8 },
            "energy": { "min": 0.4, "max": 0.9 },
            "speechiness": { "min": 0.0, "max": 0.25 },
            "acousticness": { "min": 0.0, "max": 0.2 },
            "liveness": { "min": 0.1, "max": 0.3 },
            "valence": { "min": 0.4, "max": 0.8 },
            "loudness": { "min": -20, "max": -6 },
            "tempo": { "min": 100, "max": 150 }
        }
        """.formatted(userText, mode.name());
    }
}
