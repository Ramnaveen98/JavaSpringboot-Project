package com.autobridge_api.common;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldViolation> fields = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.add(new ApiError.FieldViolation(fe.getField(), fe.getDefaultMessage()));
        }
        var body = new ApiError(
                req.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                Instant.now(),
                fields
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<ApiError.FieldViolation> fields = ex.getConstraintViolations().stream()
                .map(v -> new ApiError.FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        var body = new ApiError(
                req.getRequestURI(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                Instant.now(),
                fields
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleRse(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatusCode sc = ex.getStatusCode();
        // HttpStatusCode doesn't expose reason phrase directly; map to HttpStatus when possible
        String reason = (sc instanceof HttpStatus hs) ? hs.getReasonPhrase() : sc.toString();
        var body = new ApiError(
                req.getRequestURI(),
                sc.value(),
                reason,
                ex.getReason(),
                Instant.now(),
                null
        );
        return ResponseEntity.status(sc).body(body);
    }

    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex, HttpServletRequest req) {
        var body = new ApiError(
                req.getRequestURI(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                Instant.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataConflict(DataIntegrityViolationException ex, HttpServletRequest req) {
        var body = new ApiError(
                req.getRequestURI(),
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Data integrity violation",
                Instant.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        var body = new ApiError(
                req.getRequestURI(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Unexpected error",
                Instant.now(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
