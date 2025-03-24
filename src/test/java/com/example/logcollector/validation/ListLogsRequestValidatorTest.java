package com.example.logcollector.validation;

import com.example.logcollector.model.logs.ListEntriesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ListLogsRequestValidatorTest {
    private ListLogsRequestValidator validator;
    @BeforeEach
    public void init() throws IOException {
        validator = new ListLogsRequestValidator();
    }

    @Test
    public void validate_hasNegativeLimit_throwsError() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> validator.validate(ListEntriesRequest.builder().limit(0).build()));
        assertEquals(400, exception.getStatusCode().value());
        assertTrue(Objects.requireNonNull(exception.getReason()).contains("Limit must be between 1 and 1000"));
    }

    @Test
    public void validate_hasLargeLimit_throwsError() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> validator.validate(ListEntriesRequest.builder().limit(1001).build()));
        assertEquals(400, exception.getStatusCode().value());
        assertTrue(Objects.requireNonNull(exception.getReason()).contains("Limit must be between 1 and 1000"));
    }
}
