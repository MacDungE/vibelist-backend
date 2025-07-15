package org.example.vibelist.domain.integration.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.integration.service.SpotifyAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dev_auth")
public class DevAuthController {
    private final SpotifyAuthService spotifyAuthService;
    @GetMapping("/login")
    public void redirectToSpotify(HttpServletResponse response) throws IOException {
        response.sendRedirect(spotifyAuthService.getAuthorizationUrl());
    }

    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam("code") String code) {
        String accessToken = spotifyAuthService.exchangeCodeForTokens(code);
        String refreshToken = spotifyAuthService.getRefreshToken();
        return ResponseEntity.ok("Access token & Refresh token 발급 완료!" +
                "\n access_token : " + accessToken +
                "\n refresh_token :"+ refreshToken);
    }
}
