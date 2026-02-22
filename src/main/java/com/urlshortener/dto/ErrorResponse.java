package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String message;
    private String error;
    private Long timestamp;

    public static ErrorResponse of(int status, String message, String error) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .error(error)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
