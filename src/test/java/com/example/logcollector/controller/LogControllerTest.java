package com.example.logcollector.controller;

import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import com.example.logcollector.service.LogService;
import com.example.logcollector.util.TimeoutExecutor;
import com.example.logcollector.validation.ListLogsRequestValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class LogControllerTest {
    private LogController logController;
    @Mock
    private LogService mockLogService;
    @Mock
    private ListLogsRequestValidator mockListLogsRequestValidator;
    @Mock
    private TimeoutExecutor mockTimeoutExecutor;

    private AutoCloseable mocks;

    @BeforeEach
    public void init() {
        mocks = openMocks(this);
        logController = new LogController(mockLogService, mockListLogsRequestValidator, mockTimeoutExecutor);
    }

    @AfterEach
    public void afterEach() throws Exception {
        mocks.close();
    }

    @Test
    public void listLogs_goesThroughPipeline_returnsResponse() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        doNothing().when(mockListLogsRequestValidator).validate(any(ListLogsRequest.class));
        when(mockTimeoutExecutor.runWithTimeout(any(Callable.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(ListLogsResponse.builder().logs(List.of("sample")).build());
        ResponseEntity<ListLogsResponse> response = logController.listLogs(ListLogsRequest.builder().build());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void listLogs_timeout_throwsTimeoutError() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        doNothing().when(mockListLogsRequestValidator).validate(any(ListLogsRequest.class));
        when(mockTimeoutExecutor.runWithTimeout(any(Callable.class), anyLong(), any(TimeUnit.class)))
                .thenThrow(TimeoutException.class);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> logController.listLogs(ListLogsRequest.builder().build()));
        assertEquals(HttpStatus.REQUEST_TIMEOUT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Log retrieval timed out"));
    }

    @Test
    public void listLogs_serverError_throwsError() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        doNothing().when(mockListLogsRequestValidator).validate(any(ListLogsRequest.class));
        when(mockTimeoutExecutor.runWithTimeout(any(Callable.class), anyLong(), any(TimeUnit.class)))
                .thenThrow(RuntimeException.class);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> logController.listLogs(ListLogsRequest.builder().build()));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Internal server error occurred"));
    }
}
