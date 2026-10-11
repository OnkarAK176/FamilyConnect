package com.familyconnect.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
    @Test
    void includesTheReasonAndHttpStatusInProblemDetail() {
        var response = new ApiExceptionHandler().handleResponseStatusException(
            new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Group calling is not configured yet.")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("Group calling is not configured yet.", response.getBody().getDetail());
    }
}
