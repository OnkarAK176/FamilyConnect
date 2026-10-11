package com.familyconnect.api.config;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException exception) {
        String detail = exception.getReason() == null ? "The request could not be completed." : exception.getReason();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatusCode(), detail);
        return ResponseEntity.status(exception.getStatusCode()).body(problem);
    }
}
