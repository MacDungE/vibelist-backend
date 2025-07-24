# 부하 테스트 가이드

이 폴더는 Vibelist 백엔드 서버의 성능 테스트를 위한 K6 기반 부하 테스트 환경을 제공합니다.

## 📋 목차
- [개요](#개요)
- [사전 요구사항](#사전-요구사항)
- [설치 및 실행](#설치-및-실행)
- [테스트 스크립트](#테스트-스크립트)
- [모니터링](#모니터링)
- [결과 분석](#결과-분석)
- [커스터마이징](#커스터마이징)

## 🎯 개요

이 부하 테스트 환경은 다음 구성 요소로 이루어져 있습니다:
- **K6**: 부하 테스트 실행 엔진
- **InfluxDB**: 테스트 결과 데이터 저장소
- **Grafana**: 실시간 모니터링 및 시각화 대시보드

## 🔧 사전 요구사항

- Docker 및 Docker Compose 설치
- 테스트할 백엔드 서버가 실행 중이어야 함

## 🚀 설치 및 실행 방법

K6 부하 테스트를 실행하는 두 가지 방법을 제공합니다:

### 방법 1: Docker Compose 사용 (권장)

#### 1-1. 모니터링 환경 시작

```bash
# loadtest 디렉토리로 이동
cd loadtest

# Docker Compose로 모니터링 환경 시작
docker-compose -f docker-compose-loadtest.yml up -d influxdb grafana

# 서비스 상태 확인
docker-compose -f docker-compose-loadtest.yml ps
```

#### 1-2. Grafana 대시보드 접속

- URL: http://localhost:3000
- 사용자명: `admin`
- 비밀번호: `admin`

#### 1-3. Docker로 부하 테스트 실행

```bash
# 추천 API 부하 테스트 실행
docker-compose -f docker-compose-loadtest.yml run --rm k6 run /scripts/recommend-test.js

# 기본 예시 테스트 실행
docker-compose -f docker-compose-loadtest.yml run --rm k6 run /scripts/script-example.js
```

### 방법 2: 로컬 K6 설치 사용


#### 2-1. K6 설치

**macOS (Homebrew):**
```bash
brew install k6
```

#### 2-2. 로컬에서 부하 테스트 실행

**기본 실행 (콘솔 출력만):**
```bash
# loadtest 디렉토리에서
k6 run scripts/recommend-test.js
k6 run scripts/script-example.js
```

**InfluxDB 연동 실행 (모니터링 포함):**
```bash
# InfluxDB가 실행 중인 경우
k6 run --out influxdb=http://localhost:8086/k6 scripts/recommend-test.js

# JSON 결과 파일로 저장
k6 run --out json=results.json scripts/recommend-test.js

# CSV 결과 파일로 저장
k6 run --out csv=results.csv scripts/recommend-test.js
```

#### 2-3. 로컬 실행 시 모니터링 설정

**InfluxDB만 Docker로 실행:**
```bash
# InfluxDB만 시작
docker run -d --name influxdb -p 8086:8086 -e INFLUXDB_DB=k6 influxdb:1.8

# K6 실행 시 InfluxDB 연동
k6 run --out influxdb=http://localhost:8086/k6 scripts/recommend-test.js
```

**결과를 파일로 저장 후 분석:**
```bash
# JSON 형태로 결과 저장
k6 run --out json=test-results.json scripts/recommend-test.js

# 저장된 결과 확인
```

## 📝 테스트 스크립트

### 서버 주소 설정 방법

테스트 스크립트에서 서버 주소를 설정하는 방법은 K6 실행 환경에 따라 다릅니다:

#### 로컬 K6 실행 시 (방법 2)
```javascript
// localhost 또는 실제 서버 주소 사용
const res = http.post('http://localhost:8080/v1/recommend', payload, params);

// 또는 AWS 서버 주소
const res = http.post('https://your-server.com/v1/recommend', payload, params);
```

#### Docker 컨테이너 K6 실행 시 (방법 1)
```javascript
// Docker 컨테이너에서 호스트 머신 접근
const res = http.post('http://host.docker.internal:8080/v1/recommend', payload, params);

// 또는 외부 서버 주소 (동일)
const res = http.post('https://your-server.com/v1/recommend', payload, params);
```

> **💡 중요:** Docker 컨테이너에서 `localhost:8080`을 사용하면 컨테이너 내부를 가리키므로 호스트 머신의 서버에 접근할 수 없습니다. 반드시 `host.docker.internal:8080`을 사용해야 합니다.

### recommend-test.js
추천 API (`/v1/recommend`)에 대한 부하 테스트를 수행합니다.

**테스트 설정:**
- 가상 사용자 수: 50명
- 테스트 시간: 10초
- 대상 엔드포인트: `POST /v1/recommend`

**테스트 페이로드:**
```json
{
    "text": "",
    "userValence": 0.7,
    "userEnergy": 0.5,
    "mode": "MAINTAIN"
}
```

**현재 설정:** Docker 컨테이너용 (`host.docker.internal:8080`)

### script-example.js
기본적인 GET 요청 부하 테스트 템플릿입니다.

**테스트 설정:**
- 가상 사용자 수: 20명
- 테스트 시간: 30초
- 요청 간격: 1초

## 📊 모니터링

### Grafana 사전 설정
- InfluxDB 데이터 소스 추가
  - URL: `http://influxdb:8086`
  - 데이터베이스: `k6`
  - 사용자명/비밀번호: 기본값 사용

- 대시보드 임포트
  - `/loadtest` 내 json 파일에서 대시보드 JSON을 가져올 수 있습니다.

### InfluxDB
- 포트: 8086
- 데이터베이스: k6
- 모든 테스트 메트릭이 자동으로 저장됩니다.

### Grafana
- 포트: 3000
- 실시간 대시보드에서 다음 메트릭을 확인할 수 있습니다:
  - 응답 시간 (Response Time)
  - 초당 요청 수 (RPS)
  - 에러율 (Error Rate)
  - 동시 접속자 수 (Virtual Users)

## 📈 결과 분석

테스트 완료 후 확인해야 할 주요 메트릭:

### 성능 지표
- **평균 응답 시간**: 200ms 이하 권장
- **95퍼센타일 응답 시간**: 500ms 이하 권장
- **에러율**: 1% 이하 권장
- **처리량**: 목표 RPS 달성 여부

### 체크포인트
```javascript
check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
});
```

## ⚙️ 커스터마이징

### 1. 테스트 파라미터 조정

테스트 강도를 조정하려면 각 스크립트의 `options` 객체를 수정하세요:

```javascript
export let options = {
    vus: 50,           // 가상 사용자 수
    duration: '60s',   // 테스트 시간
    rps: 100,          // 초당 요청 수 제한 (선택사항)
};
```

### 2. 단계별 부하 테스트

```javascript
export let options = {
    stages: [
        { duration: '30s', target: 20 },  // 30초간 20명까지 증가
        { duration: '60s', target: 100 }, // 60초간 100명까지 증가
        { duration: '30s', target: 0 },   // 30초간 0명까지 감소
    ],
};
```

### 3. 새로운 테스트 스크립트 추가

`scripts/` 폴더에 새로운 `.js` 파일을 생성하고 다음 구조를 따르세요:

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    // 테스트 설정
};

export default function () {
    // 로컬 k6에서 실행
    const res = http.get('http://localhost:8080/your-endpoint');
    // Docker k6 컨테이너에서 실행
    // const res = http.post('http://host.docker.internal:8080/v1/recommend', payload, params); 
    
    check(res, {
        'status is 200': (r) => r.status === 200,
    });
    
    sleep(1);
}
```

### 4. 서버 주소 변경

테스트 대상 서버를 변경하려면 각 스크립트의 URL을 수정하세요:

```javascript
// 로컬 테스트
const res = http.post('http://localhost:8080/v1/recommend', payload, params);

// AWS 서버 테스트
const res = http.post('http://your-aws-server:8080/v1/recommend', payload, params);
```

## 🛠️ 문제 해결

### 일반적인 문제들

1. **Docker 컨테이너 시작 실패**
   ```bash
   docker-compose -f docker-compose-loadtest.yml down
   docker-compose -f docker-compose-loadtest.yml up -d
   ```

2. **Grafana 접속 불가**
   - 컨테이너 상태 확인: `docker ps`
   - 포트 충돌 확인: `lsof -i :3000`

3. **테스트 스크립트 오류**
   - 서버 주소와 포트 확인
   - API 엔드포인트 경로 확인
   - 요청 페이로드 형식 확인

## 📚 추가 자료

- [K6 공식 문서](https://k6.io/docs/)
- [Grafana 사용법](https://grafana.com/docs/)
- [InfluxDB 가이드](https://docs.influxdata.com/influxdb/v1.8/)

## 🔄 환경 정리

테스트 완료 후 리소스 정리:

```bash
# 모든 컨테이너 중지 및 제거
docker-compose -f docker-compose-loadtest.yml down

# 볼륨까지 함께 제거 (데이터 손실 주의)
docker-compose -f docker-compose-loadtest.yml down -v
```
