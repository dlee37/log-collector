package com.example.logcollector.service;

import com.example.logcollector.model.ListLogsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {
    public ListLogsResponse readLogs() {
        return ListLogsResponse.builder()
                .logs(List.of("sample response"))
                .build();
    }
}
