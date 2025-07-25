# OAuth2 신규 사용자 서버사이드 사용자명 설정 구현 가이드

## 📋 개요
현재 OAuth2로 처음 로그인하는 사용자는 프론트엔드로 리다이렉트되어 사용자명을 설정합니다. 이를 서버의 정적 뷰(Thymeleaf)에서 처리하도록 변경하는 작업입니다.

## 🔄 현재 플로우
1. 사용자가 OAuth2 로그인 시도
2. `OAuth2UserProcessor`에서 신규 사용자 감지
3. 임시 사용자명(`user_XXXX`)으로 사용자 생성
4. `OAuth2LoginSuccessHandler`에서 프론트엔드로 리다이렉트
   - URL: `${FRONTEND_CALLBACK_URL}?isNewUser=true&tempUserId={id}&provider={provider}`

## 🎯 목표 플로우
1. 사용자가 OAuth2 로그인 시도
2. `OAuth2UserProcessor`에서 신규 사용자 감지
3. 임시 사용자명으로 사용자 생성
4. **서버의 사용자명 설정 페이지로 리다이렉트**
5. 사용자가 사용자명 입력 후 제출
6. 사용자명 업데이트 후 프론트엔드로 최종 리다이렉트

## 📝 구현 작업 목록

### 1. Thymeleaf 의존성 추가
```gradle
// build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
}
```

### 2. Thymeleaf 설정
```yaml
# application.yml
spring:
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    mode: HTML
    encoding: UTF-8
    cache: false  # 개발 환경에서는 false
```

### 3. 사용자명 설정 컨트롤러 생성
새로운 컨트롤러 파일: `src/main/java/org/example/vibelist/domain/user/controller/UserSetupController.java`

```java
@Controller
@RequiredArgsConstructor
public class UserSetupController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${frontend.callback.url}")
    private String frontendCallbackUrl;
    
    // 사용자명 설정 페이지 표시
    @GetMapping("/setup/username")
    public String showUsernameSetupPage(
            @RequestParam("tempUserId") Long tempUserId,
            @RequestParam("provider") String provider,
            @RequestParam(value = "token", required = false) String temporaryToken,
            Model model) {
        
        // 임시 토큰 검증 (선택적)
        if (temporaryToken != null && !isValidTemporaryToken(temporaryToken)) {
            return "redirect:/login?error=invalid_token";
        }
        
        model.addAttribute("tempUserId", tempUserId);
        model.addAttribute("provider", provider);
        model.addAttribute("temporaryToken", temporaryToken);
        
        return "username-setup";
    }
    
    // 사용자명 설정 처리
    @PostMapping("/setup/username")
    public String processUsernameSetup(
            @RequestParam("tempUserId") Long tempUserId,
            @RequestParam("username") String username,
            @RequestParam(value = "temporaryToken", required = false) String temporaryToken,
            RedirectAttributes redirectAttributes) {
        
        try {
            // 사용자명 중복 검사
            if (userService.isUsernameExists(username)) {
                redirectAttributes.addFlashAttribute("error", "이미 사용중인 사용자명입니다.");
                return "redirect:/setup/username?tempUserId=" + tempUserId;
            }
            
            // 사용자명 업데이트
            User user = userService.updateUsername(tempUserId, username);
            
            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user.getId());
            
            // 프론트엔드로 리다이렉트
            return "redirect:" + frontendCallbackUrl + 
                   "?accessToken=" + accessToken + 
                   "&isNewUser=true" +
                   "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
                   
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "사용자명 설정 중 오류가 발생했습니다.");
            return "redirect:/setup/username?tempUserId=" + tempUserId;
        }
    }
}
```

### 4. OAuth2LoginSuccessHandler 수정
`src/main/java/org/example/vibelist/global/auth/handler/OAuth2LoginSuccessHandler.java` 수정:

```java
@Override
public void onAuthenticationSuccess(HttpServletRequest request, 
                                    HttpServletResponse response,
                                    Authentication authentication) throws IOException {
    
    OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
    CustomOAuth2User oAuth2User = (CustomOAuth2User) oAuth2Token.getPrincipal();
    
    Long userId = oAuth2User.getUserId();
    boolean isNewUser = oAuth2User.isNewUser();
    String provider = oAuth2User.getProvider();
    
    // 신규 사용자인 경우 서버 사용자명 설정 페이지로 리다이렉트
    if (isNewUser) {
        // 임시 토큰 생성 (선택적 - 보안 강화)
        String temporaryToken = generateTemporaryToken(userId);
        
        String redirectUrl = "/setup/username" +
                            "?tempUserId=" + userId +
                            "&provider=" + provider +
                            "&token=" + temporaryToken;
        
        response.sendRedirect(redirectUrl);
        return;
    }
    
    // 기존 사용자는 기존 로직대로 처리
    String accessToken = jwtTokenProvider.createAccessToken(userId);
    response.sendRedirect(frontendCallbackUrl + "?accessToken=" + accessToken);
}
```

### 5. Thymeleaf 템플릿 생성
파일 경로: `src/main/resources/templates/username-setup.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>사용자명 설정 - VibeList</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background-color: #f5f5f5;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            margin: 0;
        }
        .setup-container {
            background: white;
            padding: 2rem;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            width: 100%;
            max-width: 400px;
        }
        .logo {
            text-align: center;
            font-size: 2rem;
            font-weight: bold;
            color: #333;
            margin-bottom: 1rem;
        }
        .welcome-message {
            text-align: center;
            color: #666;
            margin-bottom: 2rem;
        }
        .form-group {
            margin-bottom: 1.5rem;
        }
        label {
            display: block;
            margin-bottom: 0.5rem;
            color: #333;
            font-weight: 500;
        }
        input[type="text"] {
            width: 100%;
            padding: 0.75rem;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 1rem;
            transition: border-color 0.3s;
        }
        input[type="text"]:focus {
            outline: none;
            border-color: #4CAF50;
        }
        .username-hint {
            font-size: 0.875rem;
            color: #666;
            margin-top: 0.5rem;
        }
        .error-message {
            color: #f44336;
            font-size: 0.875rem;
            margin-top: 0.5rem;
        }
        .submit-button {
            width: 100%;
            padding: 0.75rem;
            background-color: #4CAF50;
            color: white;
            border: none;
            border-radius: 4px;
            font-size: 1rem;
            font-weight: 500;
            cursor: pointer;
            transition: background-color 0.3s;
        }
        .submit-button:hover {
            background-color: #45a049;
        }
        .submit-button:disabled {
            background-color: #ccc;
            cursor: not-allowed;
        }
        .provider-info {
            text-align: center;
            margin-top: 1.5rem;
            font-size: 0.875rem;
            color: #666;
        }
    </style>
</head>
<body>
    <div class="setup-container">
        <div class="logo">VibeList</div>
        <div class="welcome-message">
            <h2>환영합니다!</h2>
            <p>VibeList에서 사용할 사용자명을 설정해주세요.</p>
        </div>
        
        <form th:action="@{/setup/username}" method="post" id="usernameForm">
            <input type="hidden" name="tempUserId" th:value="${tempUserId}">
            <input type="hidden" name="temporaryToken" th:value="${temporaryToken}">
            
            <div class="form-group">
                <label for="username">사용자명</label>
                <input type="text" 
                       id="username" 
                       name="username" 
                       placeholder="사용자명 입력" 
                       required
                       pattern="^[a-zA-Z0-9_]{3,20}$"
                       minlength="3"
                       maxlength="20">
                <div class="username-hint">
                    3-20자의 영문, 숫자, 언더스코어(_)만 사용 가능합니다.
                </div>
                <div th:if="${error}" class="error-message" th:text="${error}"></div>
            </div>
            
            <button type="submit" class="submit-button" id="submitButton">
                계속하기
            </button>
        </form>
        
        <div class="provider-info">
            <span th:text="${provider}"></span>로 로그인 중
        </div>
    </div>
    
    <script>
        // 클라이언트 사이드 검증
        const usernameInput = document.getElementById('username');
        const submitButton = document.getElementById('submitButton');
        const form = document.getElementById('usernameForm');
        
        usernameInput.addEventListener('input', function(e) {
            const value = e.target.value;
            const isValid = /^[a-zA-Z0-9_]{3,20}$/.test(value);
            
            if (!isValid && value.length > 0) {
                e.target.setCustomValidity('3-20자의 영문, 숫자, 언더스코어(_)만 사용 가능합니다.');
            } else {
                e.target.setCustomValidity('');
            }
        });
        
        form.addEventListener('submit', function(e) {
            submitButton.disabled = true;
            submitButton.textContent = '처리 중...';
        });
    </script>
</body>
</html>
```

### 6. UserService 메서드 추가
`src/main/java/org/example/vibelist/domain/user/service/UserService.java`에 추가:

```java
@Transactional
public User updateUsername(Long userId, String newUsername) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    
    // 사용자명 중복 검사
    if (userRepository.existsByUsername(newUsername)) {
        throw new IllegalArgumentException("Username already exists");
    }
    
    user.setUsername(newUsername);
    return userRepository.save(user);
}

@Transactional(readOnly = true)
public boolean isUsernameExists(String username) {
    return userRepository.existsByUsername(username);
}
```

### 7. Security 설정 수정
`SecurityConfig.java`에서 사용자명 설정 페이지 접근 허용:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/setup/username").permitAll()  // 추가
            .requestMatchers("/api/v1/auth/**").permitAll()
            // ... 기존 설정
        )
        // ... 나머지 설정
}
```

### 8. 임시 토큰 구현 (선택적 - 보안 강화)
```java
@Component
@RequiredArgsConstructor
public class TemporaryTokenService {
    private final RedisTemplate<String, String> redisTemplate;
    
    public String generateTemporaryToken(Long userId) {
        String token = UUID.randomUUID().toString();
        String key = "temp_token:" + token;
        
        // 10분 동안 유효
        redisTemplate.opsForValue().set(key, userId.toString(), 10, TimeUnit.MINUTES);
        
        return token;
    }
    
    public boolean validateTemporaryToken(String token, Long userId) {
        String key = "temp_token:" + token;
        String storedUserId = redisTemplate.opsForValue().get(key);
        
        if (storedUserId != null && storedUserId.equals(userId.toString())) {
            redisTemplate.delete(key); // 일회용 토큰
            return true;
        }
        
        return false;
    }
}
```

## 🔍 주요 고려사항

### 1. 보안
- 임시 토큰을 사용하여 무단 사용자명 변경 방지
- CSRF 보호 (Spring Security 기본 제공)
- 사용자명 검증 (클라이언트/서버 양쪽)

### 2. 사용자 경험
- 깔끔한 UI/UX
- 실시간 입력 검증
- 명확한 오류 메시지
- 로딩 상태 표시

### 3. 에러 처리
- 중복 사용자명
- 잘못된 형식
- 네트워크 오류
- 토큰 만료

### 4. 프론트엔드 통합
- 최종 리다이렉트 시 필요한 모든 정보 전달
- 기존 프론트엔드 콜백 핸들러와 호환성 유지

## 🧪 테스트 시나리오

1. **정상 플로우**
   - OAuth2 신규 로그인 → 사용자명 설정 페이지 → 사용자명 입력 → 프론트엔드 리다이렉트

2. **중복 사용자명**
   - 이미 존재하는 사용자명 입력 시 오류 메시지

3. **잘못된 형식**
   - 특수문자, 짧은/긴 사용자명 등

4. **보안 테스트**
   - 직접 URL 접근 시도
   - 잘못된 tempUserId로 접근

## 📦 필요한 추가 작업

1. **프론트엔드 수정**
   - 콜백 핸들러에서 `isNewUser=true`일 때의 처리 로직 확인
   - 사용자명이 이미 설정되었음을 인지하도록 수정

2. **모니터링**
   - 사용자명 설정 성공/실패 로그
   - 신규 사용자 전환율 추적

3. **국제화 (i18n)**
   - 다국어 지원을 위한 메시지 번들 추가

## 🚀 배포 체크리스트

- [ ] Thymeleaf 의존성 추가
- [ ] 템플릿 파일 생성
- [ ] 컨트롤러 구현
- [ ] OAuth2LoginSuccessHandler 수정
- [ ] UserService 메서드 추가
- [ ] Security 설정 업데이트
- [ ] 테스트 케이스 작성
- [ ] 프론트엔드 팀과 협의
- [ ] 스테이징 환경 테스트
- [ ] 프로덕션 배포