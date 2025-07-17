package org.example.vibelist.domain.integration.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.integration.service.DevAuthTokenService;
import org.example.vibelist.domain.integration.service.SpotifyAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dev_auth")
public class DevAuthController {
    private final SpotifyAuthService spotifyAuthService;
    private final DevAuthTokenService devAuthTokenService;
//    @GetMapping("/login")
//    public void redirectToSpotify(HttpServletResponse response) throws IOException {
//        //
//    }
//
//    @GetMapping("/callback")
//    public ResponseEntity<String> handleCallback(@RequestParam("code") String code) {
//        Map<String,String> tokenMap = spotifyAuthService.exchangeCodeForTokens(code);
//        String name ="sung_1";
//        String accessToken = tokenMap.get("access_token");
//        String refreshToken = tokenMap.get("refresh_token");
//        LocalDateTime expiresIn = LocalDateTime.parse(tokenMap.get("expires_in"));
//        devAuthTokenService.insertDev(name,accessToken,refreshToken,expiresIn);
//        return ResponseEntity.ok("Access token & Refresh token 발급 완료!" +
//                "\n access_token : " + accessToken +
//                "\n refresh_token :"+ refreshToken);
//    }
}
