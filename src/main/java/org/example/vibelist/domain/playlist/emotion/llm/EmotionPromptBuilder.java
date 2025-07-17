package org.example.vibelist.domain.playlist.emotion.llm;

import org.example.vibelist.domain.playlist.emotion.type.EmotionModeType;

public class EmotionPromptBuilder {
    public static String build(String userText, EmotionModeType mode) {
        return """
        아래 텍스트와 모드를 바탕으로 검색에 사용할 Audio Feature의 범위와 감정 분류(emotionType)를 모두 JSON 형식으로만 출력하세요.
        사용될 Audio Feature: 
        - danceability (0.0~1.0, 주로 0.2~0.9)
        - energy       (0.0~1.0, 주로 0.1~1.0)
        - speechiness  (0.1~1.0, 주로 0.2 이상)
        - acousticness (0.0~1.0, 양극 분포)
        - liveness     (0.0~1.0, 주로 0.1~0.4)
        - valence      (0.0~1.0, 고르게 분포, 주로 0.1~0.8)
        - loudness     (-60~0 dB, 주로 -30~-5 dB)
        - tempo        (0~250, 주로 60~180)
        각 범위는 해당 분포를 고려해 실제 곡이 존재하는 값으로 지정하세요.
        
        감정 타입(emotionType)은 아래 중 하나로 분류해 함께 JSON에 포함시키세요:
        [DEPRESSED, SLEEPY, TENSE, SAD, NEUTRAL, FEAR, CALM, JOY, EXCITED]
                

        감정 설명: "%s"
        모드: "%s"

        JSON 출력 예시:
        {
            "emotionType": "JOY",
            "danceability": { "min": 0.3, "max": 0.8 },
            "energy": { "min": 0.4, "max": 0.9 },
            "speechiness": { "min": 0.0, "max": 0.25 },
            "acousticness": { "min": 0.0, "max": 0.2 },
            "liveness": { "min": 0.1, "max": 0.3 },
            "valence": { "min": 0.2, "max": 0.7 },
            "loudness": { "min": -25, "max": -6 },
            "tempo": { "min": 80, "max": 140 },
        }                                       
        """.formatted(userText, mode.name());
    }
}
