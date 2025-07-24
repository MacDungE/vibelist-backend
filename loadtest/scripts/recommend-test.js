import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    vus: 50,          // 동시에 50명의 가상 유저
    duration: '10s',  // 10초간 테스트
};

export default function () {
    // 요청 바디 예시(실제 RecommendRqDto 구조에 맞춰서 수정)
    const payload = JSON.stringify({
        text: "",
        userValence: 0.7,
        userEnergy: 0.5,
        mode: "MAINTAIN",
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // AWS 서버 주소/포트에 맞게 수정
    const res = http.post('http://host.docker.internal:8080/v1/recommend', payload, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
        // 필요하면 응답 내용도 검사 가능
    });

    sleep(0.5); // 각 가상 유저가 1초 대기
}