package com.example.datn_sd_29.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String message;
    private T data;
    private List<Map<String, String>> errors;

    public ApiResponse(String message, T data, List<Map<String, String>> errors) {
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, null);
    }

    public static <T> ApiResponse<T> error(String message, List<Map<String, String>> errors) {
        List<Map<String, String>> safeErrors =
                (errors == null || errors.isEmpty()) ? null : errors;
        return new ApiResponse<>(message, null, safeErrors);
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public List<Map<String, String>> getErrors() {
        return errors;
    }
}
