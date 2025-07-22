# VibeList 프로젝트 API 표준 응답/에러/성공 코드 가이드

## 1. 전체 구조 요약

- **응답/에러/성공 코드**: `ResponseCode` enum (성공/실패 모두 포함, HttpStatus/코드/메시지)
- **표준 응답 DTO**: `RsData<T>` (success, code, message, data, timestamp)
- **예외 처리**: `GlobalException`(ResponseCode 기반), `GlobalExceptionHandler`(RsData.fail로 변환)
- **Swagger 문서화**: `@CommonApiResponses` 커스텀 어노테이션으로 모든 엔드포인트 통일
- **Controller/Service 패턴**:  
  - Service: throw new GlobalException(ResponseCode.XXX), RsData.success(ResponseCode.XXX, data)
  - Controller: @CommonApiResponses, ResponseEntity<RsData<?>>

---

## 2. 예시 코드

### ResponseCode 예시
```java
public enum ResponseCode {
    USER_CREATED(HttpStatus.CREATED, "USER_201", "사용자 생성 성공"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "존재하지 않는 사용자입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SYS_500", "서버 내부 오류입니다."),
    // ... 기타 도메인별 코드
}
```

### RsData 예시
```java
public class RsData<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;
    // ... 생성자, success/fail 팩토리 메서드
}
```

### Service 예시
```java
public RsData<UserDto> getUser(Long userId) {
    try {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ResponseCode.USER_NOT_FOUND));
        return RsData.success(ResponseCode.USER_FOUND, convertToUserDto(user));
    } catch (GlobalException ce) {
        throw ce;
    } catch (Exception e) {
        throw new GlobalException(ResponseCode.INTERNAL_SERVER_ERROR);
    }
}
```

### Controller 예시
```java
@Operation(summary = "사용자 조회", description = "특정 사용자 정보 조회")
@GetMapping("/{userId}")
public ResponseEntity<RsData<?>> getUser(@PathVariable Long userId) {
    RsData<?> result = userService.getUser(userId);
    return ResponseEntity.status(result.isSuccess() ? 200 : 404).body(result);
}
```

---

## 3. 테스트 가이드

- 모든 API는 성공/실패 시 RsData<ResponseCode, T>로 응답
- Swagger UI에서 모든 엔드포인트가 동일한 응답 구조로 노출
- 예외 발생 시에도 code/message가 ResponseCode 기준으로 일관되게 반환
- 테스트 코드에서도 RsData의 code, message, data, success 필드 검증

---

## 4. 팀/리뷰어 공유용 요약

- 모든 API 응답은 RsData<ResponseCode, T>로 통일
- 성공/실패/에러 모두 ResponseCode 기반으로 관리
- 예외는 반드시 GlobalException(ResponseCode)로 throw
- Swagger 문서화는 @CommonApiResponses로 통일
- 새로운 도메인/핸들러 추가 시에도 동일 패턴 적용

---

## 5. 추가 참고

- ResponseCode, RsData, GlobalException, @CommonApiResponses는 global/response 패키지에서 관리
- 실제 코드 예시는 각 도메인별 컨트롤러/서비스 참고
- 테스트/문서화/운영 전 최종 점검 필수 