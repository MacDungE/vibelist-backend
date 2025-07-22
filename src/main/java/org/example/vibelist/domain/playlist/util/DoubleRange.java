package org.example.vibelist.domain.playlist.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 실수형 값의 범위(min, max)를 표현하는 클래스
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DoubleRange {
    private double min;
    private double max;

}
