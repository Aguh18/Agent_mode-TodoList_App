package com.example.demo.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.demo.dto.response.GeneralResponse;

import lombok.Data;

@Data
public class ResponseUtils {

    public static <T> ResponseEntity<GeneralResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(new GeneralResponse<>(true, message, data));
    }

    public static ResponseEntity<GeneralResponse<Object>> fail(String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(new GeneralResponse<>(false, message, null));
    }

}
