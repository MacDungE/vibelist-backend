import http from 'loadtest/http';
import { sleep } from 'k6';

export let options = {
    vus: 20,         // 가상 유저 수
    duration: '30s', // 총 실행 시간
};

export default function () {
    http.get('http://<AWS-백엔드-주소>/api/endpoint'); // 실제 엔드포인트로 변경
    sleep(1);
}