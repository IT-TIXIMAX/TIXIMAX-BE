package com.tiximax.txm.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class APIHandleException {


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException e
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Sai tài khoản hoặc mật khẩu"
        );
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationServiceException(
            AuthenticationServiceException e
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                e.getMessage()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
        AccessDeniedException e
) {
    return buildResponse(
            HttpStatus.FORBIDDEN,
            "Bạn không có quyền truy cập chức năng này"
    );
}

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(
            AuthException e
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                e.getMessage()
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<Map<String, Object>> handleBadRequest(
                BadRequestException e
        ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
        );
        }


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(
            NotFoundException e
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                e.getMessage()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException e
    ) {
        return buildResponse(
                HttpStatus.valueOf(e.getStatusCode().value()),
                e.getReason()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandledException(
            Exception e
    ) {
        // ⚠️ log stacktrace để debug
        e.printStackTrace();

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Hệ thống đang gặp lỗi, vui lòng thử lại sau"
        );
    }


    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity
                .status(status)
                .body(body);
    }
}
