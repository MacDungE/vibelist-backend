package org.example.vibelist.domain.integration.service;

import lombok.RequiredArgsConstructor;

import org.example.vibelist.domain.integration.entity.DevIntegrationTokenInfo;
import org.example.vibelist.domain.integration.repository.DevIntegrationTokenInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DevIntegrationTokenInfoService {
    private final DevIntegrationTokenInfoRepository devIntegrationTokenInfoRepository;

    @Transactional
    public String getRefreshToken(String name){
        DevIntegrationTokenInfo devIntegrationTokenInfo = devIntegrationTokenInfoRepository.findByName(name);
        return devIntegrationTokenInfo != null ? devIntegrationTokenInfo.getRefreshToken() : null;
    }

    @Transactional
    public DevIntegrationTokenInfo getDevAuth(String name){
        //DevAuthToken 테이블의 한 행 전체를 return 합니다.
        DevIntegrationTokenInfo devIntegrationTokenInfo = devIntegrationTokenInfoRepository.findByName(name);
        if(devIntegrationTokenInfo == null){
            return null;
        }
        return devIntegrationTokenInfo;
    }

    @Transactional
    public void insertDev(String name, String accessToken, String refreshToken, LocalDateTime expiry){
        //새로운 devloper 정보를 삽입합니다.
        if(devIntegrationTokenInfoRepository.findByName(name) != null){
            throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
        }
        DevIntegrationTokenInfo devIntegrationTokenInfo = new DevIntegrationTokenInfo();
        devIntegrationTokenInfo.setName(name);
        devIntegrationTokenInfo.setAccessToken(accessToken);
        devIntegrationTokenInfo.setRefreshToken(refreshToken);
        devIntegrationTokenInfo.setTokenExpiresAt(expiry);
        devIntegrationTokenInfoRepository.save(devIntegrationTokenInfo);
    }

    @Transactional
    public void updateDev(String name, String accessToken, String refreshToken, LocalDateTime expiry) {
        DevIntegrationTokenInfo devIntegrationTokenInfo = devIntegrationTokenInfoRepository.findByName(name);
        if(devIntegrationTokenInfo == null){
            throw new IllegalArgumentException(" 존재하지 않는 사용자입니다.");
        }
        devIntegrationTokenInfo.setAccessToken(accessToken);
        devIntegrationTokenInfo.setRefreshToken(refreshToken);
        devIntegrationTokenInfo.setTokenExpiresAt(expiry);
        devIntegrationTokenInfoRepository.save(devIntegrationTokenInfo);
    }
}
