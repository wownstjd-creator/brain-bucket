package com.brainbucket.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  public record ApiError(int status, String message, Map<String, String> fields) {}

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).headers(e.getHeaders())
        .body(new ApiError(e.getStatusCode().value(),
            e.getReason() == null ? "요청을 처리할 수 없습니다." : e.getReason(), Map.of()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    Map<String,String> fields = new LinkedHashMap<>();
    e.getBindingResult().getFieldErrors().forEach(error ->
        fields.putIfAbsent(error.getField(), error.getDefaultMessage() == null
            ? "입력값을 확인해주세요." : error.getDefaultMessage()));
    return ResponseEntity.badRequest().body(new ApiError(400,
        fields.values().stream().findFirst().orElse("입력값을 확인해주세요."), fields));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> malformed(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest().body(new ApiError(400,
        "날짜·시간·숫자 등 입력 형식을 확인해주세요.", Map.of()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiError> upload(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(413).body(new ApiError(413,
        "첨부 파일은 10MB 이하, 전체 요청은 12MB 이하여야 합니다.", Map.of()));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiError> conflict(ObjectOptimisticLockingFailureException e) {
    return ResponseEntity.status(409).body(new ApiError(409,
        "다른 변경이 있습니다. 새로고침한 뒤 다시 시도해주세요.", Map.of()));
  }
}
