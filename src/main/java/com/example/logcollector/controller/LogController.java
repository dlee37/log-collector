package com.example.logcollector.controller;

import com.example.logcollector.constants.Constants;
import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import com.example.logcollector.service.LogService;
import com.example.logcollector.util.TimeoutExecutor;
import com.example.logcollector.validation.ListLogsRequestValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;


@RestController
@RequestMapping("/logs")
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);

    private final LogService logService;

    private final ListLogsRequestValidator listLogsRequestValidator;

    private final TimeoutExecutor timeoutExecutor;

    @Autowired
    public LogController(LogService logService, ListLogsRequestValidator listLogsRequestValidator, TimeoutExecutor timeoutExecutor) {
        this.logService = logService;
        this.listLogsRequestValidator = listLogsRequestValidator;
        this.timeoutExecutor = timeoutExecutor;
    }

    @GetMapping
    public ResponseEntity<ListLogsResponse> listLogs(@ModelAttribute ListLogsRequest request) {
        String reqId = UUID.randomUUID().toString();
        logger.info("Starting request id: {}", reqId);
        listLogsRequestValidator.validate(request);
        ListLogsResponse response = timeoutExecutor.runWithTimeout(
                () -> logService.listLogs(request),
                Constants.MAX_REQUEST_TIMEOUT_IN_SECONDS,
                TimeUnit.SECONDS);
        return ResponseEntity.ok(response);
    }
}
