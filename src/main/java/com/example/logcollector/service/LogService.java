package com.example.logcollector.service;

import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

@Service
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    public ListLogsResponse listLogs(ListLogsRequest request) {
        String fileName = request.getFileName();
        String searchTerm = request.getSearchTerm();
        int limit = request.getLimit();
        StopWatch watch = new StopWatch();
        watch.start();
        String requestString = String.format("file: %s, searchTerm: %s, limit: %s", fileName, searchTerm, limit);
        logger.info("Received list logs request for request: {}", requestString);
        watch.stop();
        logger.info("Logs for request {} took {} ms", requestString, watch.getTotalTimeMillis());
        return ListLogsResponse.builder()
                .logs(List.of("sample response"))
                .build();
    }
}
