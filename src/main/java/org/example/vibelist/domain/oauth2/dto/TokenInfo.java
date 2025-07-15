package org.example.vibelist.domain.oauth2.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

/**
 * 토큰 정보를 담는 공통 DTO
 */
@Getter
@Builder
@ToString
public class TokenInfo {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final Integer expiresIn;
    private final String scope;
    private final Map<String, Object> additionalParameters;
    
    /**
     * additionalParameters에서 특정 키의 값을 가져옵니다.
     */
    public Object getAdditionalParameter(String key) {
        return additionalParameters != null ? additionalParameters.get(key) : null;
    }
    
    /**
     * additionalParameters에서 특정 키의 값을 문자열로 가져옵니다.
     */
    public String getAdditionalParameterAsString(String key) {
        Object value = getAdditionalParameter(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * additionalParameters에서 특정 키의 값을 정수로 가져옵니다.
     */
    public Integer getAdditionalParameterAsInteger(String key) {
        Object value = getAdditionalParameter(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
} 