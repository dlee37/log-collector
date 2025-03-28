package com.example.logcollector.controller;

import com.example.logcollector.model.logs.ListEntriesRequest;
import com.example.logcollector.model.logs.ListEntriesResponse;
import com.example.logcollector.model.logs.ListFilesResponse;
import com.example.logcollector.service.LogService;
import com.example.logcollector.util.RequestIdGenerator;
import com.example.logcollector.util.TimeoutExecutor;
import com.example.logcollector.validation.ListLogsRequestValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    @Mock
    private RequestIdGenerator mockRequestIdGenerator;

    private AutoCloseable mocks;

    @BeforeEach
    public void init() {
        mocks = openMocks(this);
        when(mockRequestIdGenerator.generateRequestId()).thenReturn("test-request-id");
        logController = new LogController(mockLogService, mockListLogsRequestValidator, mockTimeoutExecutor, mockRequestIdGenerator);
    }

    @AfterEach
    public void afterEach() throws Exception {
        mocks.close();
    }

    @Test
    public void listLogEntries_goesThroughPipeline_returnsResponse() {
        doNothing().when(mockListLogsRequestValidator).validate(any(ListEntriesRequest.class));
        when(mockTimeoutExecutor.runWithTimeout(any(Callable.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(ListEntriesResponse.builder().logs(List.of("sample")).build());
        ResponseEntity<ListEntriesResponse> response = logController.listLogEntries(ListEntriesRequest.builder().build());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void listLogFiles_listFiles_returnsResponse() {
        when(mockTimeoutExecutor.runWithTimeout(any(Callable.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(ListFilesResponse.builder().files(List.of("sample")).build());
        ResponseEntity<ListFilesResponse> response = logController.listLogFiles();
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
