package com.example.logcollector.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResponseErrorHandlerTest {
    private ResponseErrorHandler errorHandler;

    @BeforeEach
    public void init() {
        errorHandler = new ResponseErrorHandler();
    }

    @Test
    public void handle_hasResponseStatusException_returnsProperEntity() {
        ResponseEntity<Map<String, String>> entity = errorHandler.handle(new ResponseStatusException(HttpStatus.NOT_FOUND, "reason"));
        assertEquals(1, entity.getBody().size());
    }
}
