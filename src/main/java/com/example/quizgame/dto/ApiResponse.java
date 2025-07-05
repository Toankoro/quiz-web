package com.example.quizgame.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class ApiResponse <T> {
    private Meta meta;
    private T data;
    private Object message;
    private int statusCode;

    public static class Meta {
        private int page;
        private int pageSize;
        private int pages;
        private long total;
    }

}



