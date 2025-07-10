package org.example.vibelist.domain.emotion;

import org.springframework.stereotype.Component;

/**
 * 감정 타입에 따라 valence, energy의 검색 범위를 매핑하는 클래스
 */
@Component
public class EmotionMapper {

    /**
     * 감정과 모드에 따라 EmotionFeatureProfile (valence, energy 범위)을 반환
     */
    public EmotionFeatureProfile map(EmotionType emotion, EmotionModeType mode) {
        // 감정 전이 (감정 + 모드 → 새로운 감정)
        EmotionType transitioned = EmotionTransitionMap.getNext(emotion, mode);
        return map(transitioned); // 전이된 감정을 다시 매핑
    }

    /**
     * 특정 감정에 대한 valence/energy 범위 매핑
     */
    public EmotionFeatureProfile map(EmotionType emotion) {
        switch (emotion) {
            case DEPRESSED:
                return build(0.000, 0.281, 0.000, 0.415);
            case SLEEPY:
                return build(0.000, 0.281, 0.415, 0.722);
            case TENSE:
                return build(0.000, 0.281, 0.722, 1.000);
            case SAD:
                return build(0.281, 0.586, 0.000, 0.415);
            case NEUTRAL:
                return build(0.281, 0.586, 0.415, 0.722);
            case FEAR:
                return build(0.281, 0.586, 0.722, 1.000);
            case CALM:
                return build(0.586, 1.000, 0.000, 0.415);
            case JOY:
                return build(0.586, 1.000, 0.415, 0.722);
            case EXCITED:
                return build(0.586, 1.000, 0.722, 1.000);
            default:
                throw new IllegalArgumentException("Unknown emotion: " + emotion);
        }
    }

    private EmotionFeatureProfile build(double valMin, double valMax, double energyMin, double energyMax) {
        return EmotionFeatureProfile.builder()
                .valence(new DoubleRange(valMin, valMax))
                .energy(new DoubleRange(energyMin, energyMax))
                .build();
    }
}

