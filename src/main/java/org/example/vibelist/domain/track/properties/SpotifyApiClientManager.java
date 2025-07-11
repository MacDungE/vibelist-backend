package org.example.vibelist.domain.track.properties;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.vibelist.domain.track.client.SpotifyApiClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ConfigurationProperties(prefix = "spotify")
@Slf4j
public class SpotifyApiClientManager {

    private List<ClientCredential> clients;
    private final List<SpotifyApiClient> clientInstances = new ArrayList<>();
    private int currentIndex = 0;

    // 설정 파일에서 바인딩될 클래스
    @Getter @Setter
    public static class ClientCredential {
        private String clientId;
        private String clientSecret;
    }

    public void setClients(List<ClientCredential> clients) {
        this.clients = clients;
        clientInstances.clear();
        for (ClientCredential credential : clients) {
            clientInstances.add(new SpotifyApiClient(credential.getClientId(), credential.getClientSecret()));
        }
    }

    public SpotifyApiClient getCurrentClient() {
        return clientInstances.get(currentIndex);
    }

    public boolean switchToNextClient() {
        if (currentIndex + 1 >= clientInstances.size()) {
            log.info("❌ 모든 클라이언트 소진! 금일 배치 작업 종료");
            return false;
        }
        currentIndex++;
        log.warn("🔄 Spotify 클라이언트 전환: {} → {}", currentIndex - 1, currentIndex);
        log.warn("⏱️ [429 Too Many Requests] Spotify 제한 - client ID: {} | 다른 클라이언트로 전환", clientInstances.get(currentIndex-1).getClientId());
        return true;
    }

    public void reset() {
        currentIndex = 0;
    }
}