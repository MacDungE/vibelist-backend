# VibeList API Server

[Java](https://img.shields.io/badge/Java-21-007396?style=flat&logo=openjdk&logoColor=white)[Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-6DB33F?style=flat&logo=springboot&logoColor=white)[Spring Security](https://img.shields.io/badge/Spring%20Security-Active-6DB33F?style=flat&logo=springsecurity&logoColor=white[Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-ORM-007396?style=flat&logo=spring&logoColor=white)[JWT](https://img.shields.io/badge/JWT-Authorization-yellow?style=flat&logo=jsonwebtokens&logoColor=black)[OAuth2](https://img.shields.io/badge/OAuth2-SocialLogin-005C97?style=flat&logo=oauth&logoColor=white)[PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat&logo=postgresql&logoColor=white)[Gradle](https://img.shields.io/badge/Gradle-7.x-02303A?style=flat&logo=gradle&logoColor=white)[Lombok](https://img.shields.io/badge/Lombok-Annotation-EA3324?style=flat&logo=lombok&logoColor=white)[Swagger](https://img.shields.io/badge/Swagger-UI-85EA2D?style=flat&logo=swagger&logoColor=black)[Docker](https://img.shields.io/badge/Docker-Containerization-2496ED?style=flat&logo=docker&logoColor=white)[Redis](https://img.shields.io/badge/Redis-InMemory-DC382D?style=flat&logo=redis&logoColor=white)[Elasticsearch](https://img.shields.io/badge/Elasticsearch-Search-005571?style=flat&logo=elasticsearch&logoColor=white)[Logstash](https://img.shields.io/badge/Logstash-Pipeline-000000?style=flat&logo=logstash&logoColor=white)[Kibana](https://img.shields.io/badge/Kibana-Visualization-E8478B?style=flat&logo=kibana&logoColor=white)[GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-CI%2FCD-2088FF?style=flat&logo=githubactions&logoColor=white)

> OAuth2 기반 소셜 로그인 & JWT 인증 백엔드
>

VibeList 백엔드는 음악 기반 감정 공유 소셜 플랫폼의 핵심 API 서버입니다. Spring Boot와 최신 보안/아키텍처 패턴을 적용하여 확장성과 유지보수성을 극대화했습니다.

---

## 🏗️ 프로젝트 개요

- **목적**: 사용자들이 음악과 감정을 공유하며 소통할 수 있는 안전하고 확장성 높은 RESTful API 제공
- **주요 역할**: 인증/인가, 사용자 관리, 소셜 로그인, 음악 서비스 연동, 예외 처리, 보안 강화

---

## ⚙️ 기술 스택

| 분류 | 기술 |
| --- | --- |
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5.3 |
| ORM | Spring Data JPA |
| DB | PostgreSQL |
| 인증 | Spring Security, JWT, OAuth2 |
| 배포 | Docker, AWS EC2, RDS |
| 문서화 | Swagger |
| 빌드툴 | Gradle |
| 기타 | Lombok |

---

## ☁️ AWS 인프라 구성

VibeList 백엔드는 AWS 클라우드 환경에서 아래와 같이 구성되어 있습니다.

### 주요 서비스 구성

- **EC2 (Elastic Compute Cloud)**
   - Spring Boot 애플리케이션을 호스팅하는 가상 서버
   - 배포 자동화(Docker, Github Actions 등) 및 보안 그룹 설정
      - EC2  세부 Spec

     | 인스턴스 타입 | 구성 요소 | 사양 |
             | --- | --- | --- |
     | `t3.medium` | Elasticsearch, Kibana | 2 vCPU / 4 GB RAM |
     | `t2.small` | Logstash, Redis | 1 vCPU / 2 GB RAM |
     | `t2.micro` | Spring Boot Backend | 1 vCPU / 1 GB RAM |
- **ECS (Elastic Container Service)**
   - Docker 컨테이너 기반의 서비스 오케스트레이션
   - Blue/Green 배포, Auto Scaling, 롤링 업데이트 지원
   - EC2 또는 Fargate 런타임 선택 가능
- **RDS (Relational Database Service)**
   - PostgreSQL 데이터베이스 운영
   - 백업, 장애 복구, 보안 그룹 및 파라미터 그룹 관리
- **CloudWatch**
   - EC2/ECS/RDS의 로그 및 메트릭 모니터링
   - 애플리케이션 로그 수집 및 알람 설정
   - 장애 발생 시 자동 알림 및 대시보드 제공

### 아키텍처 흐름

```
[사용자]
   ↓
[Route53/ALB] (도메인 및 HTTPS)
   ↓
[EC2] 또는 [ECS] (Spring Boot API 서버)
   ↓
[RDS] (PostgreSQL DB)
   ↑
[CloudWatch] (로그/모니터링)

```

### 운영 특징

- EC2/ECS에서 Spring Boot 서버를 실행하며, RDS와 연동
- CloudWatch로 모든 인프라 및 애플리케이션 로그/메트릭 실시간 모니터링
- 보안 그룹 및 IAM 역할로 서비스 간 접근 제어
- 환경 변수 및 민감 정보는 AWS Secrets Manager 또는 Parameter Store로 관리

---

## 🗂️ 폴더 구조 및 아키텍처

VibeList 백엔드는 도메인 중심 설계와 계층 분리를 통해 유지보수성과 확장성을 높였습니다. 주요 폴더 구조와 각 역할은 아래와 같습니다.

```
vibelist-backend/
├── .github/                # Github Actions 등 CI/CD 워크플로우
│   └── workflows/
├── .task/                  # 개발 작업 관리(TODO.md 등)
├── config/                 # 추가 환경설정, 감정/로그 등 서브 설정
│   ├── emotion/            # 감정 분석 관련 설정 파일
│   └── logstash/           # local test용 Logstash 파이프라인 설정
├── docs/                   # 설계/보안/배포 등 문서
├── ecs/                    # AWS ECS 관련 설정 및 배포 스크립트
├── src/
│   ├── main/
│   │   ├── java/org/example/vibelist/
│   │   │   ├── global/
│   │   │   │   ├── auth/           # 인증(JWT, OAuth2)
│   │   │   │   ├── config/         # 보안, 환경, CORS 설정
│   │   │   │   ├── constants/      # 전역 상수
│   │   │   │   ├── exception/      # 글로벌 예외 처리
│   │   │   │   ├── jpa/            # JPA 기본 엔티티
│   │   │   │   ├── oauth2/         # 소셜 로그인 및 OAuth2 핸들러
│   │   │   │   ├── security/       # 인증/인가 필터, 보안 유틸리티
│   │   │   │   ├── user/           # 사용자 도메인 및 서비스
│   │   │   │   └── util/           # 공통 유틸리티
│   │   │   ├── domain/             # 게시글, 댓글, 추천, 플레이리스트 등 주요 도메인
│   │   │   │   ├── post/           # 게시글 관련 엔티티/컨트롤러/서비스/리포지토리
│   │   │   │   ├── comment/        # 댓글 관련
│   │   │   │   ├── like/           # 좋아요 관련
│   │   │   │   ├── playlist/       # 플레이리스트 및 트랙 추천
│   │   │   │   ├── explore/        # 검색/트렌드/피드
│   │   │   │   ├── batch/          # 배치 작업 및 데이터 처리
│   │   │   │   └── ...             # 기타 도메인
│   │   │   └── VibeListApplication.java # 메인 엔트리포인트
│   ├── resources/
│       ├── application.properties       # 운영 환경 설정
│       ├── application-dev.properties   # 개발 환경 설정
│       
│                               
│   
├── build.gradle
├── Dockerfile
├── docker-compose.yml

```

### 아키텍처 개요

- **계층 구조**
   - Controller → Service → Repository → Domain(Entity)
   - 인증/인가, 사용자 관리, 소셜 로그인, 예외 처리 등 각 도메인별로 패키지 분리
- **인증/인가**
   - JWT 기반 인증 시스템 (Access/Refresh Token 분리)
   - OAuth2 소셜 로그인(Kakao, Google,Spotify) 지원
   - 토큰 발급/검증/갱신 로직 분리
- **환경 설정**
   - 운영/개발 환경 분리(`application.properties`, `application-dev.properties`)
   - 환경 변수 및 민감 정보는 `.env` 또는 AWS Secrets Manager로 관리
- **예외 처리**
   - 글로벌 예외 핸들러(`@ControllerAdvice`)로 모든 예외를 표준화된 JSON 응답으로 반환
   - 커스텀 예외 및 에러 코드 관리
- **CORS 및 보안**
   - SecurityConfig에서 CORS 허용 오리진, 인증/인가 필터, 세션 정책 등 통합 관리
   - 프론트엔드와의 연동을 위한 리디렉션 URL 환경 변수화
- **배포/운영**
   - AWS EC2, RDS, S3, Secrets Manager, CloudWatch 등 클라우드 인프라 활용
   - Dockerfile 및 CI/CD 스크립트로 자동화 지원

---

> 각 도메인은 독립적으로 관리되며, 확장 시 새로운 기능을 별도 패키지로 추가할 수 있도록
>

---

## 🔒 인증 및 보안

- **JWT 기반 인증**: Access/Refresh Token 분리, HttpOnly 쿠키 사용
- **Refresh Token 암호화**: DB에 암호화 저장, 탈취 방지
- **OAuth2 소셜 로그인**: 카카오/구글/Spotify 지원, 신규 사용자명 서버사이드 설정 플로우
- **CORS/SameSite 정책**: 프론트엔드와 안전한 통신 지원
- **비밀번호 해싱**: BCrypt 적용
- **API Rate Limiting**: (옵션) IP/사용자별 요청 제한

---

## 🗄️ 데이터베이스 설계

- **User**: 기본 정보 (username, password, role)
- **UserProfile**: 프로필(이메일, 이름, 전화번호, 아바타, 자기소개)
- **UserSocial**: 소셜 연동(provider, providerUserId, accessToken 등)
- **관계**: User ↔ UserProfile(1:1), User ↔ UserSocial(1:N)
- **토큰 관리**: Refresh Token 테이블 별도 관리

---

## 🔗 주요 API 엔드포인트

### 인증/인가

- POST /v1/auth/signup : 회원가입
- POST /v1/auth/login : 로그인
- POST /v1/auth/refresh : 토큰 갱신
- POST /v1/auth/kakao : 카카오 로그인
- POST /v1/auth/google : 구글 로그인

### 사용자 관리

- GET /v1/users/me : 내 정보 조회
- PUT /v1/users/me/profile : 프로필 수정
- DELETE /v1/users/me : 회원 탈퇴
- GET /v1/users/search : 사용자 검색

### 관리자

- GET /v1/users : 전체 사용자 조회
- GET /v1/users/{userId} : 특정 사용자 조회

### 음악 서비스 연동

- GET /v1/integration/status : 연동 상태 조회
- POST /v1/integration/spotify : Spotify 연동

---

## 📑 API 문서화

- **Swagger UI**: /swagger-ui/index.html에서 실시간 API 테스트 및 문서 확인
- **OpenAPI Spec**: 자동 생성, 프론트엔드와 연동

---

## 🧑‍💻 예외 처리 및 응답 구조

- **글로벌 예외 핸들러**: 모든 예외를 표준 JSON 응답으로 반환
- **응답 예시**:

    ```
    {
      "code": "USER_NOT_FOUND",
      "message": "존재하지 않는 사용자입니다."
    }
    
    ```

- **커스텀 예외**: 비즈니스 로직별 상세 에러 코드 관리

---

## 🧪 테스트 및 품질 관리

- **API 테스트**: Swagger, Postman Collection 제공
- **CI/CD**: Github Actions 기반 자동 빌드/테스트

---

## 📝사용자 로그 분석: Logstash → Kibana 연동

VibeList 백엔드는 AWS EC2/ECS에서 발생하는 애플리케이션 로그를 실시간으로 분석하기 위해 ELK(Elasticsearch, Logstash, Kibana) 스택을 활용합니다.

### 로그 흐름 아키텍처

![image.png](attachment:b0d020f9-0002-4825-98df-766b5d7606fe:image.png)

### 주요 구성 및 분석 절차

1. **로그 수집**
   - EC2/ECS에서 애플리케이션 로그를 파일 또는 stdout/stderr로 출력
   - 로그 포맷: JSON 또는 패턴 기반 텍스트
2. **Logstash 설정**
   - Logstash가 EC2/ECS의 로그 파일을 실시간으로 수집
   - Json 형식으로 로그를 구조화
   - 필요한 필드(예: 사용자ID, 요청URL, 응답코드, 에러코드 등) 추출
3. **Elasticsearch 저장**
   - Logstash가 구조화된 로그를 Elasticsearch 인덱스에 저장
   - 시간, 사용자, API별로 검색 및 집계 가능
4. **Kibana 시각화**
   - Kibana에서 대시보드 생성
   - 주요 지표: 사용자별 활동, 에러 발생 빈도, API 응답 시간, 트래픽 추이 등
   - 필터링 및 Drill-down 분석 지원

### 예시: 사용자 에러 로그 분석

- 특정 사용자/기간/엔드포인트별 호출 건수

  ![userlog.png](attachment:5f0cc8c4-2d31-4a17-88a3-3ad05c23898f:userlog.png)


### 운영 효과

- 장애 및 이상 징후 조기 탐지
- 사용자 행동 및 서비스 품질 모니터링
- API 성능 및 트래픽 분석을 통한 인프라 최적화

---

> Logstash와 Kibana를 활용해 VibeList의 사용자 로그를 실시간으로 분석·시각화하여 운영
>

## 🚀 개발 및 실행 방법

### 환경 변수 설정

- src/main/resources/application.properties 또는 .env에서 DB/JWT/OAuth2 설정

### 실행

```
./gradlew build
./gradlew bootRun

```

### DB 마이그레이션

- Aws RDS로 스키마 자동 관리

---

## 🛡️ 배포 및 운영

- **Docker 지원**: Dockerfile 및 docker-compose.yml 제공
- **프로덕션 환경**: HTTPS, 환경 변수 분리, 보안 강화
- **모니터링**: K6

---

## 📚 참고 문서

- OAuth2 신규 사용자명 설정 가이드
- 토큰 보안 리팩토링
- Integration 연동 패키지 설명
- Swagger 사용법

---