package org.example.vibelist.global.oauth2.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SocialProviderStrategy 팩토리
 */
@Slf4j
@Component
public class SocialProviderFactory {
    
    private final List<SocialProviderStrategy> strategies;
    private final Map<String, SocialProviderStrategy> strategyMap;
    
    public SocialProviderFactory(List<SocialProviderStrategy> strategies) {
        this.strategies = strategies;
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.getProviderName().toLowerCase(),
                        Function.identity()
                ));
        
        log.info("[OAUTH2_PROVIDER] 등록된 Provider 전략들: {}", strategyMap.keySet());
    }
    
    /**
     * Provider에 해당하는 전략을 가져옵니다.
     */
    public SocialProviderStrategy getStrategy(String provider) {
        SocialProviderStrategy strategy = strategyMap.get(provider.toLowerCase());
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        }
        return strategy;
    }
    
    /**
     * Provider가 지원되는지 확인합니다.
     */
    public boolean isSupported(String provider) {
        return strategyMap.containsKey(provider.toLowerCase());
    }
} 