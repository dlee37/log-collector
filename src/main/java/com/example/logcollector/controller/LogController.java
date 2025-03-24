package com.example.logcollector.controller;

import com.example.logcollector.constants.Constants;
import com.example.logcollector.model.logs.ListEntriesRequest;
import com.example.logcollector.model.logs.ListEntriesResponse;
import com.example.logcollector.model.logs.ListFilesResponse;
import com.example.logcollector.service.LogService;
import com.example.logcollector.util.RequestIdGenerator;
import com.example.logcollector.util.TimeoutExecutor;
import com.example.logcollector.validation.ListLogsRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/logs")
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    private final LogService logService;

    private final ListLogsRequestValidator listEntriesRequestValidator;

    private final TimeoutExecutor timeoutExecutor;

    private final RequestIdGenerator requestIdGenerator;

    @Autowired
    public LogController(LogService logService,
                         ListLogsRequestValidator listLogsRequestValidator,
                         TimeoutExecutor timeoutExecutor,
                         RequestIdGenerator requestIdGenerator) {
        this.logService = logService;
        this.listEntriesRequestValidator = listLogsRequestValidator;
        this.timeoutExecutor = timeoutExecutor;
        this.requestIdGenerator = requestIdGenerator;
    }

    @GetMapping("/entries")
    public ResponseEntity<ListEntriesResponse> listLogEntries(@ModelAttribute ListEntriesRequest request) {
        String reqId = requestIdGenerator.generateRequestId();
        logger.info("Starting request id: {}", reqId);
        listEntriesRequestValidator.validate(request);
        ListEntriesResponse response = timeoutExecutor.runWithTimeout(
                () -> logService.listLogEntries(request, reqId),
                Constants.MAX_REQUEST_TIMEOUT_IN_SECONDS,
                TimeUnit.SECONDS);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/files")
    public ResponseEntity<ListFilesResponse> listLogFiles() {
        String reqId = requestIdGenerator.generateRequestId();
        ListFilesResponse response = timeoutExecutor.runWithTimeout(
                () -> logService.listLogFiles(reqId),
                Constants.MAX_REQUEST_TIMEOUT_IN_SECONDS,
                TimeUnit.SECONDS);
        return ResponseEntity.ok(response);
    }
}
