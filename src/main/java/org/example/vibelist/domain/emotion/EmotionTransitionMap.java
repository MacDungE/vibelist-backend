package org.example.vibelist.domain.emotion;

import java.util.HashMap;
import java.util.Map;

import static org.example.vibelist.domain.emotion.EmotionModeType.*;
import static org.example.vibelist.domain.emotion.EmotionType.*;

/**
 * 현재 감정과 모드에 따라 전이될 감정을 정의한 정적 매핑 클래스
 */
public class EmotionTransitionMap {

    private static final Map<EmotionType, Map<EmotionModeType, EmotionType>> map = new HashMap<>();

    static {
        // DEPRESSED
        map.put(DEPRESSED, Map.of(
                MAINTAIN, DEPRESSED,
                ELEVATE, SAD,
                CALM_DOWN, DEPRESSED,
                REVERSE, EXCITED
        ));

        // SLEEPY
        map.put(SLEEPY, Map.of(
                MAINTAIN, SLEEPY,
                ELEVATE, NEUTRAL,
                CALM_DOWN, SLEEPY,
                REVERSE, TENSE
        ));

        // TENSE
        map.put(TENSE, Map.of(
                MAINTAIN, TENSE,
                ELEVATE, FEAR,
                CALM_DOWN, SLEEPY,
                REVERSE, SLEEPY
        ));

        // SAD
        map.put(SAD, Map.of(
                MAINTAIN, SAD,
                ELEVATE, NEUTRAL,
                CALM_DOWN, SAD,
                REVERSE, JOY
        ));

        // NEUTRAL
        map.put(NEUTRAL, Map.of(
                MAINTAIN, NEUTRAL,
                ELEVATE, FEAR,
                CALM_DOWN, SAD,
                REVERSE, SAD
        ));

        // FEAR
        map.put(FEAR, Map.of(
                MAINTAIN, FEAR,
                ELEVATE, EXCITED,
                CALM_DOWN, NEUTRAL,
                REVERSE, CALM
        ));

        // CALM
        map.put(CALM, Map.of(
                MAINTAIN, CALM,
                ELEVATE, JOY,
                CALM_DOWN, CALM,
                REVERSE, FEAR
        ));

        // JOY
        map.put(JOY, Map.of(
                MAINTAIN, JOY,
                ELEVATE, EXCITED,
                CALM_DOWN, CALM,
                REVERSE, SAD
        ));

        // EXCITED
        map.put(EXCITED, Map.of(
                MAINTAIN, EXCITED,
                ELEVATE, EXCITED,
                CALM_DOWN, JOY,
                REVERSE, DEPRESSED
        ));
    }

    public static EmotionType getNext(EmotionType current, EmotionModeType mode) {
        return map.getOrDefault(current, Map.of()).getOrDefault(mode, current);
    }
}
