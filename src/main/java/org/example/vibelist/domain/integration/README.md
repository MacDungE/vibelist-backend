# Integration Package 📱🎵

외부 서비스 연동을 위한 토큰 관리 시스템입니다. Spotify, Google, Apple Music 등 다양한 음악 스트리밍 서비스와의 연동을 지원합니다.

## 📁 패키지 구조

```
src/main/java/org/example/vibelist/global/integration/
├── controller/
│   └── IntegrationController.java      # RESTful API 컨트롤러
├── dto/
│   ├── IntegrationTokenResponse.java   # 토큰 정보 응답 DTO
│   ├── IntegrationStatusResponse.java  # 연동 상태 응답 DTO
│   └── RefreshTokenRequest.java        # 토큰 갱신 요청 DTO
├── entity/
│   └── IntegrationTokenInfo.java       # 토큰 정보 엔티티
├── repository/
│   └── IntegrationTokenInfoRepository.java # 데이터 접근 레이어
├── service/
│   └── IntegrationTokenInfoService.java    # 비즈니스 로직 서비스
└── README.md                           # 이 문서
```

## 🚀 주요 기능

### ✅ 보안 고려
- 민감한 토큰 값은 응답에서 제외하고 메타데이터만 반환
- 액세스 토큰과 리프레시 토큰의 안전한 저장 및 관리

### ✅ 인증 기반
- Spring Security를 통한 사용자 인증 확인
- 사용자별 토큰 격리 및 접근 제어

### ✅ RESTful 설계
- 표준 HTTP 메서드와 상태 코드 사용
- 직관적이고 일관된 API 엔드포인트 설계

### ✅ 에러 처리
- 적절한 예외 처리 및 에러 메시지 제공
- HTTP 상태 코드를 통한 명확한 응답

### ✅ 로깅
- Slf4j를 통한 상세한 로깅 지원
- 토큰 생성, 갱신, 삭제 등 모든 작업 추적

### ✅ 유연한 조회
- 다양한 필터링 옵션 제공
- JSON 기반 복잡한 쿼리 지원

## 🌐 API 엔드포인트

### 📊 연동 상태 관리

| HTTP Method | 엔드포인트 | 설명 | 응답 타입 |
|------------|-----------|------|----------|
| `GET` | `/v1/integrations/status` | 현재 사용자의 모든 연동 상태 조회 | `IntegrationStatusResponse` |
| `GET` | `/v1/integrations/providers` | 연동된 제공자 목록 조회 | `Map<String, Object>` |

### 🔍 토큰 정보 조회

| HTTP Method | 엔드포인트 | 설명 | 응답 타입 |
|------------|-----------|------|----------|
| `GET` | `/v1/integrations/{provider}` | 특정 제공자의 토큰 정보 조회 | `IntegrationTokenResponse` |
| `GET` | `/v1/integrations/{provider}/valid` | 특정 제공자의 유효한 토큰 정보 조회 | `IntegrationTokenResponse` |
| `GET` | `/v1/integrations/{provider}/exists` | 특정 제공자의 토큰 존재 여부 확인 | `Map<String, Object>` |

### 🎯 필터링 및 검색

| HTTP Method | 엔드포인트 | 설명 | 응답 타입 |
|------------|-----------|------|----------|
| `GET` | `/v1/integrations/by-scope?scope=XXX` | 특정 권한을 가진 연동 조회 | `List<IntegrationTokenResponse>` |

### 🗑️ 연동 해제

| HTTP Method | 엔드포인트 | 설명 | 응답 타입 |
|------------|-----------|------|----------|
| `DELETE` | `/v1/integrations/{provider}` | 특정 제공자의 연동 해제 | `Map<String, String>` |
| `DELETE` | `/v1/integrations/all` | 모든 연동 해제 | `Map<String, String>` |

## 📝 DTO 클래스

### IntegrationTokenResponse
토큰 정보 응답을 위한 DTO입니다.

```java
{
    "id": 1,
    "provider": "SPOTIFY",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "scope": "user-read-private user-read-email",
    "tokenIssuedAt": "2024-01-01T10:00:00",
    "tokenExpiresAt": "2024-01-01T11:00:00",
    "isActive": true,
    "isValid": true,
    "isExpired": false,
    "additionalInfo": {...},
    "createdAt": "2024-01-01T09:00:00",
    "updatedAt": "2024-01-01T10:00:00"
}
```

### IntegrationStatusResponse
전체 연동 상태 응답을 위한 DTO입니다.

```java
{
    "userId": 123,
    "totalIntegrations": 3,
    "activeIntegrations": 2,
    "expiredIntegrations": 1,
    "integrations": [
        {
            "provider": "SPOTIFY",
            "status": "ACTIVE",
            "isValid": true,
            "lastUpdated": "2024-01-01T10:00:00",
            "expiresAt": "2024-01-01T11:00:00",
            "scope": "user-read-private"
        }
    ],
    "statusCheckedAt": "2024-01-01T10:30:00"
}
```

### RefreshTokenRequest
토큰 갱신 요청을 위한 DTO입니다.

```java
{
    "provider": "SPOTIFY",
    "forceRefresh": false
}
```

## 🔧 사용 예시

### 1. 사용자의 모든 연동 상태 확인
```bash
curl -X GET "/v1/integrations/status" \
  -H "Authorization: Bearer {access_token}"
```

### 2. Spotify 연동 상태 확인
```bash
curl -X GET "/v1/integrations/spotify" \
  -H "Authorization: Bearer {access_token}"
```

### 3. 특정 권한을 가진 연동 조회
```bash
curl -X GET "/v1/integrations/by-scope?scope=user-read-private" \
  -H "Authorization: Bearer {access_token}"
```

### 4. 연동 해제
```bash
curl -X DELETE "/v1/integrations/spotify" \
  -H "Authorization: Bearer {access_token}"
```

## 🛡️ 보안 고려사항

### 민감한 정보 보호
- **액세스 토큰**: 응답에서 완전히 제외
- **리프레시 토큰**: 응답에서 완전히 제외
- **시크릿 키**: 모든 민감한 필드는 `isSecretField()` 메서드를 통해 필터링

### 접근 제어
- 모든 API는 인증된 사용자만 접근 가능
- 사용자는 자신의 토큰 정보만 조회/관리 가능

## 🗄️ 데이터베이스 스키마

`integration_token_info` 테이블은 다음과 같은 구조를 가집니다:

- `user_id`: 사용자 ID (외래키)
- `provider`: 서비스 제공자 (SPOTIFY, GOOGLE 등)
- `access_token`: 암호화된 액세스 토큰
- `refresh_token`: 암호화된 리프레시 토큰
- `token_response`: JSON 형태의 추가 토큰 정보
- `token_type`: 토큰 타입 (Bearer)
- `expires_in`: 만료 시간 (초)
- `scope`: 권한 범위
- `token_issued_at`: 토큰 발급 시간
- `token_expires_at`: 토큰 만료 시간
- `is_active`: 토큰 활성 상태

## 🔄 토큰 생명주기

1. **토큰 생성**: OAuth2 로그인 성공 시 자동 생성
2. **토큰 갱신**: 만료 시 자동 갱신 (리프레시 토큰 사용)
3. **토큰 비활성화**: 사용자 요청 또는 만료 시 비활성화
4. **토큰 정리**: 배치 작업을 통한 만료된 토큰 정리

## 📈 모니터링 및 로깅

모든 토큰 관련 작업은 다음과 같이 로깅됩니다:

```
[INTEGRATION_TOKEN] 토큰 정보 저장/업데이트 - userId: 123, provider: SPOTIFY
[INTEGRATION_TOKEN] 새로운 토큰 정보 생성 - provider: SPOTIFY
[INTEGRATION_TOKEN] 토큰 정보 저장 완료 - tokenId: 456, isValid: true
```

## 🚫 제한사항

- 사용자당 제공자별 하나의 활성 토큰만 허용
- 토큰 갱신은 리프레시 토큰이 있는 경우에만 가능
- 민감한 토큰 정보는 API를 통해 직접 조회 불가

## 🔮 향후 개선 계획

- [ ] 토큰 자동 갱신 스케줄러 추가
- [ ] 토큰 사용량 통계 및 분석 기능
- [ ] 다중 토큰 지원 (사용자당 제공자별 여러 토큰)
- [ ] 토큰 백업 및 복원 기능
- [ ] API 사용량 제한 (Rate Limiting) 