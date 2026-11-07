package com.mmbank.backend;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(
            IllegalArgumentException exception) {

        String message = exception.getMessage();

        if ("Account not found".equals(message)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(message);
        }

        if ("Account number already exists".equals(message)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(message);
        }

        return ResponseEntity.badRequest().body(message);
    }
}
