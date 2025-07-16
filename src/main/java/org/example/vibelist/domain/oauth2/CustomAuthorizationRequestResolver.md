# CustomAuthorizationRequestResolver

## 개요
Spring Security OAuth2에서 제공하는 기본 Authorization Request를 커스터마이징하여 각 소셜 로그인 제공자(특히 Spotify)에 맞는 추가 파라미터를 설정하는 컴포넌트입니다.

## 주요 기능

### 1. Spotify OAuth2 최적화
- **Refresh Token 확보**: Spotify에서 refresh token을 확실히 받기 위한 파라미터 추가
  - `show_dialog=true`: 사용자가 매번 동의 화면을 보도록 강제하여 refresh token 발급 확률 증가
  - `access_type=offline`: 오프라인 액세스를 위한 refresh token 명시적 요청

### 2. Integration State 처리
- 일반 로그인과 통합(integration) 요청을 구분하기 위한 state 파라미터 처리
- 사용자 정의 state와 Spring Security의 기본 state를 결합하여 관리

### 3. 다중 Provider 지원
- Spotify, Kakao, Google 등 여러 소셜 로그인 제공자 식별
- Authorization URI를 통한 제공자 자동 감지

## 클래스 구조

### 핵심 메서드

#### `resolve()` 메서드들
- Spring Security의 기본 resolver를 래핑하여 커스터마이징 적용
- 두 가지 오버로드 버전 제공 (일반/특정 클라이언트 ID)

#### `customizeAuthorizationRequest()`
- 제공자별 커스터마이징 로직 분기 처리
- 현재는 Spotify만 특별 처리, 향후 확장 가능

#### `customizeSpotifyAuthorizationRequest()`
- Spotify 전용 Authorization Request 커스터마이징
- 추가 파라미터 설정 및 state 처리

#### `extractRegistrationId()`
- Authorization URI 분석을 통한 소셜 제공자 식별
- 도메인 기반 매칭 (accounts.spotify.com, kauth.kakao.com 등)

## 사용 목적

### 문제 해결
1. **Spotify Refresh Token 이슈**: 기본 OAuth2 플로우에서 refresh token이 항상 발급되지 않는 문제
2. **Integration 요청 구분**: 일반 로그인과 음악 서비스 연동 요청의 구분 필요
3. **Provider별 최적화**: 각 소셜 로그인 제공자의 특성에 맞는 파라미터 설정

### 기술적 이점
- Spring Security의 기본 동작을 유지하면서 필요한 부분만 확장
- 로깅을 통한 디버깅 및 모니터링 지원
- 확장 가능한 구조로 새로운 제공자 추가 용이

## 설정 및 사용

### Bean 등록
`@Component` 어노테이션을 통해 Spring 컨테이너에 자동 등록됩니다.

### 의존성
- `ClientRegistrationRepository`: OAuth2 클라이언트 등록 정보 저장소
- `DefaultOAuth2AuthorizationRequestResolver`: Spring Security 기본 resolver

### 로깅
SLF4J를 사용하여 OAuth2 플로우의 주요 단계를 로깅합니다.

## 확장 가능성
- 새로운 소셜 로그인 제공자 추가 시 `customizeAuthorizationRequest()` 메서드에 분기 추가
- 각 제공자별 전용 커스터마이징 메서드 구현 가능
- state 파라미터 처리 로직 확장 가능