package com.example.logcollector.controller;

import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import com.example.logcollector.service.LogService;
import com.example.logcollector.validation.ListLogsRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequestMapping("/logs")
public class LogController {
    @Autowired
    private LogService logService;

    @Autowired
    private ListLogsRequestValidator listLogsRequestValidator;

    @GetMapping
    public ResponseEntity<ListLogsResponse> listLogs(@ModelAttribute ListLogsRequest request) throws IOException {
        listLogsRequestValidator.validate(request);
        ListLogsResponse response = logService.listLogs(request);
        return ResponseEntity.ok(response);
    }
}
