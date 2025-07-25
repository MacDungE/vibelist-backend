package org.example.vibelist.domain.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.vibelist.domain.user.entity.User;
import org.example.vibelist.domain.user.service.UserService;
import org.example.vibelist.global.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class UserSetupController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${frontend.callback.url}")
    private String frontendCallbackUrl;
    
    @GetMapping("/setup/username")
    public String showUsernameSetupPage(
            @RequestParam(value = "tempUserId",  required = false) Long tempUserId,
            @RequestParam(value = "provider",  required = false) String provider,
            @RequestParam(value = "token", required = false) String temporaryToken,
            Model model) {
        
        model.addAttribute("tempUserId", tempUserId);
        model.addAttribute("provider", provider);
        model.addAttribute("temporaryToken", temporaryToken);
        
        return "username-setup";
    }
    
    @PostMapping("/setup/username")
    public String processUsernameSetup(
            @RequestParam("tempUserId") Long tempUserId,
            @RequestParam("username") String username,
            @RequestParam(value = "temporaryToken", required = false) String temporaryToken,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (userService.isUsernameExists(username)) {
                redirectAttributes.addFlashAttribute("error", "이미 사용중인 사용자명입니다.");
                return "redirect:/setup/username?tempUserId=" + tempUserId;
            }
            
            User user = userService.updateUsername(tempUserId, username);
            
            String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
            
            return "redirect:" + frontendCallbackUrl + 
                   "?accessToken=" + accessToken;
                   
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "사용자명 설정 중 오류가 발생했습니다.");
            return "redirect:/setup/username?tempUserId=" + tempUserId;
        }
    }
}