package org.example.vibelist.domain.integration.service;

import lombok.RequiredArgsConstructor;

import org.example.vibelist.domain.integration.entity.DevAuthToken;
import org.example.vibelist.domain.integration.repository.DevAuthTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DevAuthTokenService {
    private final DevAuthTokenRepository devAuthTokenRepository;

    @Transactional
    public String getRefreshToken(String name){
        DevAuthToken devAuthToken = devAuthTokenRepository.findByName(name);
        return devAuthToken != null ? devAuthToken.getRefreshToken() : null;
    }

    @Transactional
    public DevAuthToken getDevAuth(String name){
        //DevAuthToken 테이블의 한 행 전체를 return 합니다.
        DevAuthToken devAuthToken = devAuthTokenRepository.findByName(name);
        if(devAuthToken == null){
            return null;
        }
        return devAuthToken;
    }

    @Transactional
    public void insertDev(String name, String accessToken, String refreshToken, LocalDateTime expiry){
        //새로운 devloper 정보를 삽입합니다.
        if(devAuthTokenRepository.findByName(name) != null){
            throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
        }
        DevAuthToken devAuthToken = new DevAuthToken();
        devAuthToken.setName(name);
        devAuthToken.setAccessToken(accessToken);
        devAuthToken.setRefreshToken(refreshToken);
        devAuthToken.setTokenExpiresAt(expiry);
        devAuthTokenRepository.save(devAuthToken);
    }

    @Transactional
    public void updateDev(String name, String accessToken, String refreshToken, LocalDateTime expiry) {
        DevAuthToken devAuthToken = devAuthTokenRepository.findByName(name);
        if(devAuthToken == null){
            throw new IllegalArgumentException(" 존재하지 않는 사용자입니다.");
        }
        devAuthToken.setAccessToken(accessToken);
        devAuthToken.setRefreshToken(refreshToken);
        devAuthToken.setTokenExpiresAt(expiry);
        devAuthTokenRepository.save(devAuthToken);
    }
}
