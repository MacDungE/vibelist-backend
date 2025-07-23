package org.example.vibelist.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseCode {
    // ====== POST ======
    POST_CREATED(HttpStatus.CREATED, "POST_201", "게시글 생성 성공"),
    POST_UPDATED(HttpStatus.NO_CONTENT, "POST_204", "게시글 수정 성공"),
    POST_DELETED(HttpStatus.NO_CONTENT, "POST_204", "게시글 삭제 성공"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_404", "존재하지 않는 게시글입니다."),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "POST_403", "게시글에 대한 권한이 없습니다."),
    POST_SAVE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "POST_503", "게시글 저장에 실패했습니다."),
    // ====== COMMENT ======
    COMMENT_CREATED(HttpStatus.CREATED, "COMMENT_201", "댓글 생성 성공"),
    COMMENT_UPDATED(HttpStatus.OK, "COMMENT_200", "댓글 수정 성공"),
    COMMENT_DELETED(HttpStatus.NO_CONTENT, "COMMENT_204", "댓글 삭제 성공"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_404", "존재하지 않는 댓글입니다."),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMENT_403", "댓글에 대한 권한이 없습니다."),
    // ====== LIKE ======
    LIKE_SUCCESS(HttpStatus.OK, "LIKE_200", "좋아요 성공"),
    LIKE_CANCELLED(HttpStatus.OK, "LIKE_201", "좋아요 취소"),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE_404", "존재하지 않는 좋아요입니다."),
    LIKE_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LIKE_500", "좋아요 처리 중 오류가 발생했습니다."),
    // ====== COMMENT_LIKE ======
    COMMENT_LIKE_SUCCESS(HttpStatus.OK, "COMMENT_LIKE_200", "댓글 좋아요 성공"),
    COMMENT_LIKE_CANCELLED(HttpStatus.OK, "COMMENT_LIKE_201", "댓글 좋아요 취소"),
    COMMENT_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_LIKE_404", "존재하지 않는 댓글 좋아요입니다."),
    COMMENT_LIKE_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMENT_LIKE_500", "댓글 좋아요 처리 중 오류가 발생했습니다."),
    // ====== USER ======
    USER_FOUND(HttpStatus.OK, "USER_200", "사용자 조회 성공"),
    USER_CREATED(HttpStatus.CREATED, "USER_201", "사용자 생성 성공"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_409", "이미 존재하는 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "존재하지 않는 사용자입니다."),
    USER_PASSWORD_INVALID(HttpStatus.UNAUTHORIZED, "USER_401", "비밀번호가 일치하지 않습니다."),
    USER_LOGIN_SUCCESS(HttpStatus.OK, "USER_200", "로그인 성공"),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_401", "로그인이 필요합니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_403", "권한이 없습니다."),
    USERNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USER_500", "사용자명 생성에 실패했습니다."),
    // ====== INTEGRATION ======
    INTEGRATION_NOT_FOUND(HttpStatus.NOT_FOUND, "INTEGRATION_404", "연동 정보를 찾을 수 없습니다."),
    INTEGRATION_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "INTEGRATION_401", "유효하지 않은 연동 토큰입니다."),
    INTEGRATION_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "INTEGRATION_404", "연동 토큰을 찾을 수 없습니다."),
    INTEGRATION_SPOTIFY_REFRESH_FAIL(HttpStatus.BAD_GATEWAY, "INTEGRATION_501", "스포티파이 ACCESS 토큰 갱신 실패"), //토큰 refreshing 실패
    INTEGRATION_ALREADY_CONNECTED(HttpStatus.CONFLICT, "INTEGRATION_409", "이미 연동된 서비스입니다."),
    INTEGRATION_DISCONNECTED(HttpStatus.OK, "INTEGRATION_200", "연동 해제 성공"),
    INTEGRATION_DISCONNECTED_ALL(HttpStatus.OK, "INTEGRATION_200", "모든 연동 해제 성공"),
    INTEGRATION_TOKEN_CHECK(HttpStatus.OK, "INTEGRATION_200", "토큰 존재 여부 확인 성공"),
    INTEGRATION_PROVIDERS_LIST(HttpStatus.OK, "INTEGRATION_200", "연동된 제공자 목록 조회 성공"),
    INTEGRATION_BY_SCOPE(HttpStatus.OK, "INTEGRATION_200", "권한별 연동 조회 성공"),
    INTEGRATION_SPOTIFY_CONNECT_SUCCESS(HttpStatus.OK, "INTEGRATION_200", "스포티파이 연동 시작 성공"),
    INTEGRATION_SPOTIFY_CONNECT_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "INTEGRATION_500", "스포티파이 연동 시작 실패"),
    INTEGRATION_SPOTIFY_DEBUG_SUCCESS(HttpStatus.OK, "INTEGRATION_200", "스포티파이 디버그 정보 조회 성공"),
    INTEGRATION_SPOTIFY_DEBUG_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "INTEGRATION_500", "스포티파이 디버그 정보 조회 실패"),
    INTEGRATION_SPOTIFY_EXTRACT_USERID_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "INTEGRATION_500", "스포티파이 유저정보 추출 실패"),//유저 정보 추출 실패
    // ====== PLAYLIST ======
    PLAYLIST_CREATED(HttpStatus.CREATED, "PLAYLIST_201", "플레이리스트 생성 성공"),
    PLAYLIST_CREATE_FAIL(HttpStatus.BAD_GATEWAY, "PLAYLIST_502", "플레이리스트 생성에 실패했습니다."),
    // ====== RECOMMEND ======
    RECOMMEND_SUCCESS(HttpStatus.OK, "RECOMMEND_200", "추천 성공"),
    RECOMMEND_INVALID_INPUT(HttpStatus.BAD_REQUEST, "RECOMMEND_400", "추천 입력값이 잘못되었습니다."),
    // ====== LLM/AI ======
    LLM_API_ERROR(HttpStatus.BAD_GATEWAY, "LLM_502", "LLM API 호출 실패"),
    LLM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "LLM_504", "LLM API 타임아웃"),
    LLM_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "LLM_500", "LLM 응답 파싱 실패"),
    LLM_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "LLM_400", "LLM 응답 포맷 오류"),
    // ====== TAG ======
    TAG_AUTOCOMPLETE_SUCCESS(HttpStatus.OK, "TAG_200", "태그 자동완성 성공"),
    TAG_INVALID(HttpStatus.BAD_REQUEST, "TAG_400", "유효하지 않은 태그입니다."),
    // ====== EXPLORE ======
    EXPLORE_SEARCH_SUCCESS(HttpStatus.OK, "EXPLORE_200", "탐색 검색 성공"),
    EXPLORE_FEED_SUCCESS(HttpStatus.OK, "EXPLORE_201", "탐색 피드 성공"),
    TREND_CAPTURE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EXPLORE_500", "트렌드 데이터 생성에 실패했습니다."),
    // ====== BATCH/ETC ======
    BATCH_SUCCESS(HttpStatus.OK, "BATCH_200", "배치 작업 성공"),
    ES_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ES_500", "Elasticsearch 검색 실패"),
    // ====== HEALTH ======
    HEALTH_OK(HttpStatus.OK, "HEALTH_200", "서버 정상 동작"),
    // ====== SYSTEM ======
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", "서버 내부 오류입니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "SYS_400", "잘못된 요청입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ResponseCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}