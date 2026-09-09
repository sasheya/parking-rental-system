package com.parking.common_security;

import lombok.Value;

@Value
public class ApiResponse<T> {
    boolean success;
    T data;
    ErrorData error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorData(code, message));
    }

    @Value
    public static class ErrorData {
        String code;
        String message;
    }
}
