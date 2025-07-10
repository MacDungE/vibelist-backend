# 토큰 보안 강화 리팩토링 (v2.0)

## 개요

VibeList Backend의 인증 시스템을 보안 강화를 위해 리팩토링했습니다. 주요 변경사항은 Access Token을 데이터베이스에 저장하지 않고 메모리에서만 관리하는 것입니다.

## 주요 변경사항

### 1. UserSocial 엔티티 수정

**변경 전:**
```java
private String accessTokenEnc;    // 암호화된 소셜 access token
private String refreshTokenEnc;   // 암호화된 소셜 refresh token
```

**변경 후:**
```java
private String refreshTokenEnc;   // 암호화된 소셜 refresh token만 저장
```

### 2. 생성자 및 메서드 수정

**변경 전:**
```java
// 일반 로그인용 생성자
public UserSocial(User user, String accessToken, String refreshToken, String tokenType)

// 소셜 로그인용 생성자  
public UserSocial(User user, SocialProvider provider, String providerUserId, 
                 String providerEmail, String accessToken, String refreshToken)

// 토큰 업데이트 메서드
public void updateTokens(String accessToken, String refreshToken)
public void updateAccessToken(String accessToken)
public void updateRefreshToken(String refreshToken)
```

**변경 후:**
```java
// 일반 로그인용 생성자
public UserSocial(User user, String refreshToken, String tokenType)

// 소셜 로그인용 생성자
public UserSocial(User user, SocialProvider provider, String providerUserId, 
                 String providerEmail, String refreshToken)

// Refresh Token만 업데이트
public void updateRefreshToken(String refreshToken)
```

### 3. 서비스 레이어 수정

#### AuthService
- `socialLogin()` 메서드에서 accessToken 매개변수 제거
- `linkSocialAccount()` 메서드에서 accessToken 매개변수 제거
- `createUserSocial()` 메서드에서 accessToken 매개변수 제거
- `createRegularUserSocial()` 메서드에서 accessToken 매개변수 제거

#### SocialAuthUtil
- `findOrCreateSocialUser()` 메서드에서 accessToken 매개변수 제거
- `createNewSocialUser()` 메서드에서 accessToken 매개변수 제거
- `linkSocialAccount()` 메서드에서 accessToken 매개변수 제거

#### OAuth2UserService
- `upsertUserSocial()` 메서드에서 accessToken 매개변수 제거
- `updateTokens()` 호출을 `updateRefreshToken()` 호출로 변경

### 4. 컨트롤러 레이어

#### AuthController
- 소셜 계정 연동 API에서 accessToken 매개변수는 하위 호환성을 위해 유지하되 실제로는 사용하지 않음
- 주석으로 deprecated 표시

### 5. DTO 수정

#### SocialLoginRequest
```java
@Deprecated // 더 이상 사용되지 않음. accessToken은 메모리에서만 관리됨
private String accessTokenEnc;
```

## 데이터베이스 마이그레이션

### SQL 스크립트
```sql
-- accessTokenEnc 컬럼 제거
ALTER TABLE user_social DROP COLUMN IF EXISTS access_token_enc;
```

### 마이그레이션 파일
- `src/main/resources/sql/base.sql`에 마이그레이션 스크립트 추가

## 보안 개선 효과

### 1. 데이터베이스 보안 강화
- Access Token이 데이터베이스에 저장되지 않아 데이터 유출 위험 감소
- 데이터베이스 해킹 시에도 Access Token은 안전

### 2. 토큰 관리 개선
- Access Token은 메모리에서만 관리되어 더 빠른 접근
- Refresh Token만 데이터베이스에 저장하여 장기간 인증 관리

### 3. 소셜 로그인 보안 강화
- 소셜 로그인에서도 동일한 보안 정책 적용
- 소셜 Access Token도 메모리에서만 관리

## 하위 호환성

### API 호환성
- 기존 API 엔드포인트는 그대로 유지
- `SocialLoginRequest`의 `accessTokenEnc` 필드는 deprecated로 표시하되 유지
- 클라이언트 코드 수정 없이 동작

### 데이터 호환성
- 기존 데이터는 마이그레이션 스크립트로 안전하게 처리
- `accessTokenEnc` 컬럼 제거로 데이터베이스 크기 감소

## 테스트 권장사항

### 1. 단위 테스트
- UserSocial 엔티티의 새로운 생성자 및 메서드 테스트
- AuthService의 수정된 메서드들 테스트
- SocialAuthUtil의 수정된 메서드들 테스트

### 2. 통합 테스트
- 소셜 로그인 플로우 테스트
- 토큰 갱신 플로우 테스트
- 소셜 계정 연동 테스트

### 3. 데이터베이스 테스트
- 마이그레이션 스크립트 실행 테스트
- 기존 데이터 보존 확인

## 배포 가이드

### 1. 개발 환경
1. 코드 변경사항 적용
2. 데이터베이스 마이그레이션 실행
3. 단위 테스트 및 통합 테스트 실행
4. 기능 테스트 수행

### 2. 운영 환경
1. 데이터베이스 백업 생성
2. 마이그레이션 스크립트 실행
3. 애플리케이션 배포
4. 모니터링 및 로그 확인

## 롤백 계획

### 1. 코드 롤백
- Git을 통한 이전 버전으로 롤백
- 데이터베이스 마이그레이션 롤백 스크립트 준비

### 2. 데이터 롤백
```sql
-- 롤백 스크립트 (필요시)
ALTER TABLE user_social ADD COLUMN access_token_enc VARCHAR(255);
```

## 결론

이번 리팩토링을 통해 VibeList Backend의 인증 시스템 보안이 크게 강화되었습니다. Access Token을 데이터베이스에 저장하지 않음으로써 데이터 유출 위험을 줄이고, Refresh Token만 저장하여 장기간 인증을 안전하게 관리할 수 있게 되었습니다. 