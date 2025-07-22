package org.example.vibelist.domain.integration.controller;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.integration.service.DevIntegrationTokenInfoService;
import org.example.vibelist.domain.integration.service.SpotifyAuthService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/dev_auth")
public class DevIntegrationController {
    private final SpotifyAuthService spotifyAuthService;
    private final DevIntegrationTokenInfoService devIntegrationTokenInfoService;
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
