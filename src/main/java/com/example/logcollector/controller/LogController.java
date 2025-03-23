package com.example.logcollector.controller;

import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import com.example.logcollector.service.LogService;
import com.example.logcollector.validation.ListLogsRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;


@RestController
@RequestMapping("/logs")
public class LogController {
    private final LogService logService;

    private final ListLogsRequestValidator listLogsRequestValidator;

    @Autowired
    public LogController(LogService logService, ListLogsRequestValidator listLogsRequestValidator) {
        this.logService = logService;
        this.listLogsRequestValidator = listLogsRequestValidator;
    }

    @GetMapping
    public ResponseEntity<ListLogsResponse> listLogs(@ModelAttribute ListLogsRequest request) throws IOException {
        listLogsRequestValidator.validate(request);
        ListLogsResponse response = logService.listLogs(request);
        return ResponseEntity.ok(response);
    }
}
