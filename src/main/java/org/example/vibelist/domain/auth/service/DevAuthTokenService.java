package org.example.vibelist.domain.auth.service;

import lombok.RequiredArgsConstructor;

import org.example.vibelist.domain.auth.entity.DevAuthToken;
import org.example.vibelist.domain.auth.repository.DevAuthTokenRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DevAuthTokenService {
    private final DevAuthTokenRepository devAuthTokenRepository;
    public String getRefreshToken(){
        DevAuthToken devAuthToken = devAuthTokenRepository.findById(1L).orElse(null);
        return devAuthToken != null ? devAuthToken.getRefreshToken() : null;
    }
    public void saveRefreshToken(String name,String refreshToken){
        DevAuthToken devAuthToken = devAuthTokenRepository.findByName(name);
        if(devAuthToken == null){ //아예 Table이 비어있을 때,
            devAuthToken = new DevAuthToken();
            devAuthToken.setId(1L);
            devAuthToken.setName(name);
        }
        devAuthToken.setRefreshToken(refreshToken);
        devAuthTokenRepository.save(devAuthToken);
    }
}
