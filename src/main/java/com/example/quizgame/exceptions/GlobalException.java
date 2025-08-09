package com.example.quizgame.exceptions;

import com.example.quizgame.dto.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalException{
    @ExceptionHandler(value = {
            IllegalArgumentException.class,
            UserAlreadyExistException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException (IllegalArgumentException illegalArgumentException) {
        ApiResponse<Object> res = new ApiResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setMessage(illegalArgumentException.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
