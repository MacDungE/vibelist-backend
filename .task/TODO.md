# React 연동을 위한 OAuth2 설정 변경 작업 목록

## 1. 환경 변수 설정

- [ ] `env.example` 파일에 프론트엔드 리디렉션 URL 환경 변수 추가
  - `FRONTEND_LOGIN_URL=https://your-frontend-domain.com/login`
  - `FRONTEND_LOGOUT_URL=https://your-frontend-domain.com/logout`
  - `FRONTEND_CALLBACK_URL=https://your-frontend-domain.com/oauth/callback`
- [ ] `application.properties` 파일에 환경 변수를 읽어오는 설정 추가
  - `frontend.login.url=${FRONTEND_LOGIN_URL}`
  - `frontend.logout.url=${FRONTEND_LOGOUT_URL}`
  - `frontend.callback.url=${FRONTEND_CALLBACK_URL}`
- [ ] `application-dev.properties` 에 개발 환경용 URL 설정
  - `frontend.login.url=http://localhost:3000/login`
  - `frontend.logout.url=http://localhost:3000/logout`
  - `frontend.callback.url=http://localhost:3000/oauth/callback`

## 2. OAuth2 핸들러 수정

- [ ] `OAuth2LoginSuccessHandler.java` 수정
  - `@Value` 어노테이션을 사용하여 `application.properties`에 설정한 프론트엔드 URL 주입
  - `onAuthenticationSuccess` 메소드 내의 `response.sendRedirect()` 호출 부분을 주입받은 URL로 변경
    - **성공 시:** `frontend.callback.url` 로 리디렉션
    - **실패 시:** `frontend.login.url` 에 에러 쿼리 파라미터를 추가하여 리디렉션
- [ ] `OAuth2LogoutSuccessHandler.java` 수정 (파일이 존재할 경우)
  - `@Value` 어노테이션을 사용하여 `frontend.logout.url` 주입
  - `onLogoutSuccess` 메소드 내의 리디렉션 URL을 주입받은 값으로 변경

## 3. CORS(Cross-Origin Resource Sharing) 설정

- [ ] `SecurityConfig.java` 파일 수정
  - `corsConfigurationSource()` 메소드 추가
  - `http.cors(cors -> cors.configurationSource(corsConfigurationSource()))` 설정 추가
  - 허용할 오리진(origin)에 프론트엔드 개발 서버 주소(`http://localhost:3000`)와 프로덕션 도메인 추가

## 4. `static` 리소스 정리

- [ ] `src/main/resources/static` 폴더의 아래 파일들 삭제
  - `index.html`
  - `login.html`
  - `main.html`
  - `social-signup.html`

## 5. (선택) 소셜 가입 페이지 로직 변경

- [ ] `OAuth2LoginSuccessHandler.java`의 `handleRegularLogin` 메소드에서 `isNewUser`가 `true`일 때, `social-signup.html`로 리디렉션하는 로직을 프론트엔드의 소셜 가입 페이지 URL로 변경
  - `frontend.callback.url` 에 `isNewUser=true` 와 같은 쿼리 파라미터를 붙여서 리디렉션하는 방식 고려
