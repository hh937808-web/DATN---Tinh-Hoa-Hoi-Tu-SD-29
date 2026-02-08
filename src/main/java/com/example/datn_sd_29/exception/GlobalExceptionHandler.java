package com.example.datn_sd_29.exception;

import com.example.datn_sd_29.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> item = new HashMap<>();
            item.put("field", error.getField());
            item.put("error", error.getDefaultMessage());
            errors.add(item);
        }
        ApiResponse<Object> response = ApiResponse.error("Validation Failed", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Dữ liệu không hợp lệ!";

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                message = String.format("Giá trị không hợp lệ!");
            }
        }
        ApiResponse<Object> response = ApiResponse.error(message, null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}
