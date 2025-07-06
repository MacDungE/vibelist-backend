package org.example.vibelist.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    /**
     * 🌐 공통 에러 코드 정의 클래스
     *
     * [작성 규칙]
     * - 에러는 도메인별로 블록 구분 (AUTH, USER 등)
     * - 각 항목은 (HttpStatus, "도메인_에러번호", "에러 메시지") 형태로 구성
     * - HttpStatus는 실제 응답과 일치하도록 설정
     * - 에러 메시지는 사용자에게 보여지는 친절한 문장으로 작성 (한글 권장)
     *
     * [에러 코드 구성 예시]
     *  - "AUTH_001": 인증 관련 에러의 첫 번째 항목
     *  - "MOVIE_006": 영화 도메인의 여섯 번째 에러
     *
     * [주요 HttpStatus 설명]
     * - 400 (BAD_REQUEST): 잘못된 요청 (ex. 유효성 검증 실패, 파라미터 오류 등)
     * - 401 (UNAUTHORIZED): 인증되지 않은 사용자 (ex. 토큰 없음/만료)
     * - 403 (FORBIDDEN): 인증은 되었으나 권한이 부족한 경우
     * - 404 (NOT_FOUND): 리소스를 찾을 수 없음 (ex. 존재하지 않는 사용자/영화 등)
     * - 409 (CONFLICT): 중복 발생 등 충돌 상황 (ex. 이미 존재하는 이메일)
     * - 405 (METHOD_NOT_ALLOWED): 지원하지 않는 HTTP 메서드 사용
     * - 500 (INTERNAL_SERVER_ERROR): 서버 내부 에러
     * - 502 (BAD_GATEWAY): 외부 API 통신 오류 (ex. KMDb 등)
     *
     * [사용 예시]
     * throw new CustomException(ErrorCode.USER_NOT_FOUND);
     */


    // ⚙️ 시스템/서버 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_001", "서버 오류입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "SYS_002", "잘못된 요청입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "SYS_003", "허용되지 않은 HTTP 메서드입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}