package com.example.logcollector.controller;

import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import com.example.logcollector.service.LogService;
import com.example.logcollector.validation.ListLogsRequestValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class LogControllerTest {
    private LogController logController;
    @Mock
    private LogService mockLogService;
    @Mock
    private ListLogsRequestValidator mockListLogsRequestValidator;

    private AutoCloseable mocks;

    @BeforeEach
    public void init() {
        mocks = openMocks(this);
        logController = new LogController(mockLogService, mockListLogsRequestValidator);
    }

    @AfterEach
    public void afterEach() throws Exception {
        mocks.close();
    }

    @Test
    public void listLogs_goesThroughPipeline_returnsResponse() throws IOException {
        doNothing().when(mockListLogsRequestValidator).validate(any(ListLogsRequest.class));
        when(mockLogService.listLogs(any(ListLogsRequest.class)))
                .thenReturn(ListLogsResponse.builder().logs(List.of("sample")).build());
        ResponseEntity<ListLogsResponse> response = logController.listLogs(ListLogsRequest.builder().build());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
