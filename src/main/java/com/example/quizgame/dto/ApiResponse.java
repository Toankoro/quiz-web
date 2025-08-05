package com.example.quizgame.dto;

import lombok.*;

@RequiredArgsConstructor
@Getter
@Setter
public class ApiResponse <T> {
    private Meta meta;
    private T data;
    private Object message;
    private int statusCode;

    public ApiResponse(T data, String message) {
        this.data = data;
        this.message = message;
    }

    public ApiResponse(T data, String message, Meta meta) {
        this.data = data;
        this.message = message;
        this.meta = meta;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Meta {
        private int page;
        private int pageSize;
        private int pages;
        private long total;
    }

}



