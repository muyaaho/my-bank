package com.mybank.common.exception;

import com.mybank.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapBusinessExceptionToStatus(ex.getErrorCode());
        String userMessage = getUserFriendlyMessage(ex.getErrorCode(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(userMessage)
                .error(ApiResponse.ErrorDetails.builder()
                        .code(ex.getErrorCode())
                        .message(userMessage)
                        .detail(ex.getDetail())
                        .build())
                .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());

        // Get first validation error message for user-friendly response
        String errorMessage = "입력하신 정보를 다시 확인해주세요.";
        if (ex.getBindingResult().hasErrors()) {
            FieldError fieldError = (FieldError) ex.getBindingResult().getAllErrors().get(0);
            errorMessage = fieldError.getDefaultMessage();
        }

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(errorMessage)
                .error(ApiResponse.ErrorDetails.builder()
                        .code("VALIDATION_ERROR")
                        .message(errorMessage)
                        .build())
                .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.error("Authentication exception: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Authentication failed")
                .error(ApiResponse.ErrorDetails.builder()
                        .code("AUTHENTICATION_ERROR")
                        .message(ex.getMessage())
                        .build())
                .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.error("Access denied: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Access denied")
                .error(ApiResponse.ErrorDetails.builder()
                        .code("ACCESS_DENIED")
                        .message(ex.getMessage())
                        .build())
                .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception occurred", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                .error(ApiResponse.ErrorDetails.builder()
                        .code("INTERNAL_ERROR")
                        .message("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
                        .build())
                .correlationId(request.getHeader(CORRELATION_ID_HEADER))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Map business exception codes to HTTP status codes
     */
    private HttpStatus mapBusinessExceptionToStatus(String code) {
        return switch (code) {
            case "USER_EXISTS" -> HttpStatus.CONFLICT;
            case "INVALID_CREDENTIALS", "INVALID_TOKEN" -> HttpStatus.UNAUTHORIZED;
            case "ACCOUNT_LOCKED", "ACCOUNT_INACTIVE" -> HttpStatus.FORBIDDEN;
            case "USER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    /**
     * Provide user-friendly Korean error messages
     */
    private String getUserFriendlyMessage(String code, String defaultMessage) {
        return switch (code) {
            case "USER_EXISTS" -> "이미 등록된 이메일입니다.";
            case "INVALID_CREDENTIALS" -> "이메일 또는 비밀번호가 올바르지 않습니다.";
            case "ACCOUNT_LOCKED" -> "계정이 잠겼습니다. 잠시 후 다시 시도해주세요.";
            case "ACCOUNT_INACTIVE" -> "비활성화된 계정입니다. 고객센터에 문의해주세요.";
            case "USER_NOT_FOUND" -> "사용자를 찾을 수 없습니다.";
            case "INVALID_TOKEN" -> "유효하지 않은 토큰입니다. 다시 로그인해주세요.";
            default -> defaultMessage != null ? defaultMessage : "요청 처리 중 오류가 발생했습니다.";
        };
    }
}
