//package com.UsdtWallet.UsdtWallet.exception;
//
//import com.UsdtWallet.UsdtWallet.model.dto.response.ApiResponse;
//import jakarta.validation.ConstraintViolation;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.coyote.BadRequestException;
//import org.hibernate.exception.ConstraintViolationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.context.request.WebRequest;
//
//import java.util.HashMap;
//import java.util.Map;
//
//
//@RestControllerAdvice
//@Slf4j
//public class GlobalExceptionHandler {
//    @ExceptionHandler(ResourceNotFoundException.class)
//    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
//            ResourceNotFoundException ex, WebRequest request) {
//        log.error("Resource not found: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(BadRequestException.class)
//    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(
//            BadRequestException ex, WebRequest request) {
//        log.error("Bad request: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(UnauthorizedException.class)
//    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
//            UnauthorizedException ex, WebRequest request) {
//        log.error("Unauthorized: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                .body(ApiResponse.error(ex.getMessage()));
//    }
//
//    @ExceptionHandler(AccessDeniedException.class)
//    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
//            AccessDeniedException ex, WebRequest request) {
//        log.error("Access denied: {}", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.FORBIDDEN)
//                .body(ApiResponse.error("Access denied"));
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
//            MethodArgumentNotValidException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach((error) -> {
//            String fieldName = ((FieldError) error).getField();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });
//        log.error("Validation failed: {}", errors);
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error("Validation failed", errors));
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
//            ConstraintViolationException ex) {
//        Map<String, String> errors = new HashMap<>();
//        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
//            String fieldName = violation.getPropertyPath().toString();
//            String errorMessage = violation.getMessage();
//            errors.put(fieldName, errorMessage);
//        }
//        log.error("Constraint violation: {}", errors);
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                .body(ApiResponse.error("Validation failed", errors));
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
//            Exception ex, WebRequest request) {
//        log.error("Unexpected error: ", ex);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(ApiResponse.error("An unexpected error occurred"));
//    }
//}
