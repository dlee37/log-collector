package com.example.logcollector.controller;

import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import com.example.logcollector.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/logs")
public class LogController {
    @Autowired
    private LogService logService;

    @GetMapping
    public ResponseEntity<ListLogsResponse> getLogs(@ModelAttribute ListLogsRequest request) {
        return ResponseEntity.ok(logService.readLogs());
    }
}
