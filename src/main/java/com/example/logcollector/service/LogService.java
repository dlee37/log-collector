package com.example.logcollector.service;

import com.example.logcollector.constants.Constants;
import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import static com.example.logcollector.constants.Constants.CHUNK_SIZE;

@Service
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    public ListLogsResponse listLogs(ListLogsRequest request) throws IOException {
        String fileName = request.getFileName();
        String searchTerm = request.getSearchTerm();
        int limit = request.getLimit() == null ? Constants.DEFAULT_LIMIT : request.getLimit();
        File file = new File(String.format("%s/%s", Constants.SAMPLE_LOG_PATH, fileName));
        if (!file.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        StopWatch watch = new StopWatch();
        watch.start();
        String requestString = String.format("file: %s, searchTerm: %s, limit: %s", fileName, searchTerm, limit);
        logger.info("Received list logs request for request: {}", requestString);
        // start of the business logic here
        List<String> logs = new ArrayList<>();
        int linesFound = 0;
        byte[] chunks = new byte[CHUNK_SIZE];
        StringBuilder currentLine = new StringBuilder();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // go to the end of the file initially
            long filePointer = raf.length();
            while (filePointer > 0 && linesFound < limit) {
                int bytesToRead = (int) Math.min(CHUNK_SIZE, filePointer);
                // load the chunk into memory
                filePointer -= bytesToRead;
                raf.seek(filePointer);
                raf.readFully(chunks, 0, bytesToRead);
                for (int i = bytesToRead - 1; i >= 0; i--) {
                    char c = (char) chunks[i];
                    if (c == '\n') {
                        if (!currentLine.isEmpty()) {
                            String line = currentLine.reverse().toString();
                            currentLine.setLength(0);
                            if (searchTerm == null || line.contains(searchTerm)) {
                                logs.add(line.trim());
                                linesFound++;
                                if (linesFound == limit) {
                                    break;
                                }
                            }
                        }
                    } else {
                        currentLine.append(c);
                    }
                }
            }
        }

        // process the first line of the file since there are no more \n characters to process
        if (linesFound < limit && !currentLine.isEmpty()) {
            String line = currentLine.reverse().toString();
            if (searchTerm == null || line.contains(searchTerm)) {
                logs.add(line.trim());
            }
        }

        watch.stop();
        logger.info("Logs for request {} took {} ms", requestString, watch.getTotalTimeMillis());
        return ListLogsResponse.builder()
                .logs(logs)
                .build();
    }
}
