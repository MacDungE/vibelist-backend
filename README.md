# VibeList Backend

Spring Boot 기반의 백엔드 API 서버입니다.

## 주요 기능

- JWT 토큰 기반 인증 시스템
- 사용자 관리 (회원가입, 로그인, 프로필 관리)
- 소셜 로그인 지원 (카카오, 구글)
- RESTful API
- 글로벌 예외 처리 시스템

## 기술 스택

- Spring Boot 3.5.3
- JDK 21
- Spring Security
- Spring Data JPA
- JWT (JSON Web Token)
- Spring OAuth2
- PostgreSQL
- Gradle
- Lombok

## 프로젝트 구조

```
src/main/java/org/example/vibelist/
├── global/
│   ├── auth/           # 인증 관련 (JWT, OAuth2)
│   ├── config/         # 보안 설정
│   ├── constants/      # 상수 정의
│   ├── exception/      # 글로벌 예외 처리
│   ├── jpa/           # JPA 기본 엔티티
│   ├── oauth2/        # OAuth2 설정
│   ├── security/      # 보안 관련
│   ├── user/          # 사용자 관리
│   └── util/          # 유틸리티
└── VibeListApplication.java
```

## JWT 인증 시스템

### 토큰 구조
- **Access Token**: API 요청 시 사용 (1시간 유효) - 메모리에서만 관리
- **Refresh Token**: Access Token 갱신 시 사용 (24시간 유효) - 데이터베이스에 암호화 저장

### 보안 강화 (v2.0)
- **Access Token**: 데이터베이스에 저장하지 않고 메모리에서만 관리하여 보안 강화
- **Refresh Token**: 데이터베이스에 암호화하여 저장하여 장기간 인증 관리
- 소셜 로그인의 경우에도 동일한 정책 적용


### 카카오/구글 OAuth 인가 코드 기반 로그인

1. **프론트엔드**: 카카오/구글 OAuth 인가 코드 → **백엔드 /v1/auth/{provider}** 전달
2. **백엔드 처리 과정**:
   1. 인가 코드 → SNS Access Token 발급
   2. provider_user_id로 user_social 조회 → 없으면 **신규 가입**
      - user 레코드 + 랜덤 username 생성 (iu_8347 등)
      - user_profile insert (이메일·닉네임 등)
      - user_social insert
   3. **JWT Access/Refresh** 발급
3. **프론트엔드**: Access Token 헤더 보관, Refresh Token은 쿠키 자동 저장

## 데이터베이스 구조

### 주요 엔티티
- **User**: 사용자 기본 정보 (username, password, role)
- **UserProfile**: 사용자 프로필 정보 (email, name, phone, avatarUrl, bio)
- **UserSocial**: 소셜 계정 연동 정보 (provider, providerUserId, providerEmail)

### 관계 매핑
- User ↔ UserProfile: 1:1 관계 (ID 공유)
- User ↔ UserSocial: 1:N 관계 (소셜 계정 다중 연동 가능)

## API 엔드포인트

### 인증 관련
- `POST /v1/auth/signup` - 회원가입
- `POST /v1/auth/login` - 로그인
- `POST /v1/auth/refresh` - 토큰 갱신
- `POST /v1/auth/kakao` - 카카오 소셜 로그인 (인가 코드 기반)
- `POST /v1/auth/google` - 구글 소셜 로그인 (인가 코드 기반)
- `POST /v1/auth/social/login` - 소셜 로그인 (기존 방식)

### 사용자 관리
- `POST /v1/users` - 사용자 생성
- `GET /v1/users/me` - 현재 사용자 정보 조회
- `PUT /v1/users/me` - 현재 사용자 프로필 업데이트
- `DELETE /v1/users/me` - 현재 사용자 삭제
- `GET /v1/users/search` - 사용자 검색 (이름 기반)

### 관리자 기능
- `GET /v1/users` - 모든 사용자 조회
- `GET /v1/users/{userId}` - 특정 사용자 조회
- `PUT /v1/users/{userId}/profile` - 특정 사용자 프로필 업데이트
- `DELETE /v1/users/{userId}` - 특정 사용자 삭제

## 설정

### JWT 설정 (application.properties)
```properties
jwt.secret=your-secret-key-here-make-it-long-and-secure-for-production
jwt.access-token-validity=3600000
jwt.refresh-token-validity=86400000
```

### OAuth 설정 (application.properties)
```properties
oauth.kakao.client-id=your-kakao-client-id
oauth.kakao.client-secret=your-kakao-client-secret
oauth.google.client-id=your-google-client-id
oauth.google.client-secret=your-google-client-secret
```

## 실행 방법

1. 의존성 설치
```bash
./gradlew build
```

2. 애플리케이션 실행
```bash
./gradlew bootRun
```

## API 사용 예시

### 일반 로그인
```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 사용자 생성
```bash
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "password123",
    "email": "user@example.com",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "role": "USER"
  }'
```

### 카카오 소셜 로그인
```bash
curl -X POST http://localhost:8080/v1/auth/kakao \
  -H "Content-Type: application/json" \
  -d '{
    "authorizationCode": "authorization_code_from_kakao",
    "redirectUri": "your_redirect_uri"
  }'
```

### 구글 소셜 로그인
```bash
curl -X POST http://localhost:8080/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "authorizationCode": "authorization_code_from_google",
    "redirectUri": "your_redirect_uri"
  }'
```

### 토큰을 사용한 API 요청
```bash
curl -X GET http://localhost:8080/v1/users/me \
  -H "Authorization: Bearer {accessToken}"
```

### 사용자 프로필 업데이트
```bash
curl -X PUT http://localhost:8080/v1/users/me/profile \
  -H "Authorization: Bearer {accessToken}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "새로운 이름",
    "phone": "010-9876-5432",
    "avatarUrl": "https://example.com/avatar.jpg",
    "bio": "자기소개입니다."
  }'
```

### 사용자 검색
```bash
curl -X GET "http://localhost:8080/v1/users/search?name=홍길동" \
  -H "Authorization: Bearer {accessToken}"
```

### 토큰 갱신
```bash
curl -X POST http://localhost:8080/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "{refreshToken}"
  }'
```

## 소셜 로그인 응답 예시

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 1703123456789,
  "userId": 1,
  "username": "iu_8347",
  "role": "USER"
}
```

## 예외 처리

### 글로벌 예외 처리
- `CustomException`: 커스텀 비즈니스 예외
- `IllegalArgumentException`: 잘못된 인자 예외
- `MethodArgumentNotValidException`: 유효성 검증 실패 예외
- `Exception`: 기타 모든 예외

### 에러 응답 형식
```json
{
  "code": "ERROR_CODE",
  "message": "에러 메시지"
}
```

## 개발 환경

### 개발 모드
- `application-dev.properties` 사용
- 개발용 보안 설정 적용
- 디버그 로그 활성화

### 프로덕션 모드
- `application-prod.properties` 사용
- 프로덕션용 보안 설정 적용
- 로그 레벨 조정

## 코드 품질

### 최근 개선사항
- 중복 코드 제거 및 리팩토링
- 엔티티 관계 매핑 최적화
- 예외 처리 통합
- 타입 안전성 향상
- 코드 가독성 개선