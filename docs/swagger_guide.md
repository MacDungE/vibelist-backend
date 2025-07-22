# VibeList API 문서 가이드

## 📚 개요

VibeList 백엔드 API는 OpenAPI 3.0 규격을 따르는 Swagger 문서를 제공합니다. 
이 문서를 통해 모든 API 엔드포인트를 쉽게 탐색하고 테스트할 수 있습니다.

## 🔗 접근 방법

### Swagger UI 웹 인터페이스
- **개발 서버**: http://localhost:8080/swagger-ui.html
- **운영 서버**: https://api.vibelist.com/swagger-ui.html

### OpenAPI JSON 스펙
- **개발 서버**: http://localhost:8080/v3/api-docs
- **운영 서버**: https://api.vibelist.com/v3/api-docs

## 📖 API 분류

### 1. 인증 관리 (`/v1/auth`)
- 토큰 갱신, 로그아웃
- 소셜 로그인 연동 관리
- 회원가입 완료 처리
- 인증 상태 확인

### 2. 사용자 관리 (`/v1/users`)
- 사용자 생성, 조회, 수정, 삭제
- 프로필 관리
- 사용자 검색

### 3. 외부 서비스 연동 (`/v1/integrations`)
- Spotify, Google Music 등 연동 관리
- 토큰 정보 조회 및 관리
- 연동 상태 확인

### 4. 시스템 상태 (`/health`)
- 서버 상태 확인
- 헬스체크

## 🔐 인증 방식

### JWT Bearer Token
```
Authorization: Bearer <your-jwt-token>
```

### Cookie 인증
```
Cookie: refreshToken=<your-refresh-token>
```

## 🚀 사용 방법

### 1. Swagger UI 접속
1. 브라우저에서 `http://localhost:8080/swagger-ui.html` 접속
2. API 목록과 상세 정보 확인

### 2. API 테스트
1. 원하는 API 엔드포인트 클릭
2. "Try it out" 버튼 클릭
3. 필요한 파라미터 입력
4. "Execute" 버튼으로 실행

### 3. 인증이 필요한 API 테스트
1. 먼저 로그인 API를 통해 토큰 획득
2. Swagger UI 상단의 "Authorize" 버튼 클릭
3. Bearer Token 입력 후 "Authorize" 클릭
4. 이후 모든 인증 API 자동으로 토큰 포함

## 🛠️ 개발자를 위한 정보

### API 문서 자동 생성
- `@Operation`: API 메서드 설명
- `@ApiResponses`: 응답 코드별 설명
- `@Parameter`: 파라미터 설명
- `@SecurityRequirement`: 인증 필요 표시
- `@Tag`: 컨트롤러 그룹화

### 설정 파일
- **Swagger 설정**: `src/main/java/org/example/vibelist/global/config/SwaggerConfig.java`
- **보안 설정**: `src/main/java/org/example/vibelist/global/config/DevSecurityConfig.java`
- **애플리케이션 설정**: `src/main/resources/application.properties`

### 주요 설정값
```properties
# Swagger 설정
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.packages-to-scan=org.example.vibelist
springdoc.paths-to-match=/v1/**,/health
```

## 📝 추가 참고사항

### 응답 형식
- 성공: HTTP 200/201 + JSON 데이터
- 실패: HTTP 4xx/5xx + 에러 메시지

### 페이지네이션
- 목록 조회 API에서 사용 (추후 구현 예정)

### API 버전 관리
- URL 경로에 버전 포함: `/v1/...`

---

## 🔧 문제 해결

### 일반적인 문제들

**Q: Swagger UI에 접속이 안 돼요**
A: 서버가 실행 중인지 확인하고, 개발 프로필(`dev`)로 실행되고 있는지 확인하세요.

**Q: 인증 API가 작동하지 않아요**
A: JWT 토큰이 올바르게 설정되었는지 확인하고, 토큰 만료 시간을 체크하세요.

**Q: API 문서가 업데이트되지 않아요**
A: 애플리케이션을 재시작하거나 브라우저 캐시를 비워보세요.

---

*VibeList API 문서는 지속적으로 업데이트됩니다. 문의사항이 있으시면 개발팀에 연락해주세요.* 