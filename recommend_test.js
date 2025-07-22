import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { Counter } from 'k6/metrics';

export let options = {
    vus: 50,
    duration: '30s',
};

// 커스텀 메트릭 선언
let cacheDuration = new Trend('cache_duration');
let directDuration = new Trend('direct_duration');
let cacheSuccess = new Counter('cache_success');
let cacheFail = new Counter('cache_fail');
let directSuccess = new Counter('direct_success');
let directFail = new Counter('direct_fail');

const payload = JSON.stringify({
    userValence: 0.3,
    userEnergy: 0.2,
    text: "",
    mode: "ELEVATE"
});
const headers = { 'Content-Type': 'application/json' };

export default function () {
    // 캐시 사용 API
    let resCache = http.post('http://localhost:8080/v1/recommend?source=cache', payload, { headers });
    cacheDuration.add(resCache.timings.duration);

    if (resCache.status === 200) {
        cacheSuccess.add(1);
    } else {
        cacheFail.add(1);
    }
    check(resCache, { 'cache status is 200': (r) => r.status === 200 });

    // ES 직접 조회 API
    let resDirect = http.post('http://localhost:8080/v1/recommend?source=es', payload, { headers });
    directDuration.add(resDirect.timings.duration);

    if (resDirect.status === 200) {
        directSuccess.add(1);
    } else {
        directFail.add(1);
    }
    check(resDirect, { 'direct status is 200': (r) => r.status === 200 });
}