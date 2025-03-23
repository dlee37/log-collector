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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.logcollector.constants.Constants.CHUNK_SIZE;

@Service
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    public ListLogsResponse listLogs(ListLogsRequest request) throws IOException {
        String fileName = request.getFileName();
        String searchTerm = request.getSearchTerm();
        int limit = request.getLimit() == null ? Constants.DEFAULT_LIMIT : request.getLimit();
        File file = validateFile(fileName);
        StopWatch watch = new StopWatch();
        watch.start();
        String requestString = String.format("file: %s, searchTerm: %s, limit: %s", fileName, searchTerm, limit);
        logger.info("Received list logs request for request: {}", requestString);
        // start of the business logic here
        List<String> logs = processLogsInReverse(file, searchTerm, limit);

        watch.stop();
        logger.info("Logs for request {} took {} ms", requestString, watch.getTotalTimeMillis());
        return ListLogsResponse.builder()
                .logs(logs)
                .build();
    }

    private File validateFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("No file specified, defaulting to syslog or messages...");
            for (String logFile : Constants.DEFAULT_LOG_FILES) {
                File file = new File(String.format("%s/%s", Constants.SAMPLE_LOG_PATH, logFile));
                if (file.exists()) {
                    return file;
                }
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "syslog or messages log file does not exist!");
        }

        File file = new File(String.format("%s/%s", Constants.SAMPLE_LOG_PATH, fileName));
        logger.info("File is: {}", file.isFile());
        if (!file.exists()) {
            File logDirectory = new File(Constants.SAMPLE_LOG_PATH);
            File[] files = logDirectory.listFiles(f ->
                    f.isFile() && !f.getName().endsWith(".gz") && !f.getName().startsWith("."));
            if (files == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No files exist in %s!", Constants.SAMPLE_LOG_PATH));
            }
            String fullFileList = Arrays.stream(files).map(File::getName).collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("File not found. List of files include: %s", fullFileList));
        } else if (!file.isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("File %s must be a valid file, not a directory!", fileName));
        }
        return file;
    }

    private List<String> processLogsInReverse(File file, String searchTerm, int limit) throws IOException {
        List<String> logs = new ArrayList<>();
        byte[] chunk = new byte[CHUNK_SIZE];
        StringBuilder currentLine = new StringBuilder();
        int linesFound = 0;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long filePointer = raf.length();
            while (filePointer > 0 && linesFound < limit) {
                int bytesToRead = (int) Math.min(CHUNK_SIZE, filePointer);
                filePointer -= bytesToRead;
                raf.seek(filePointer);
                raf.readFully(chunk, 0, bytesToRead);
                for (int i = bytesToRead - 1; i >= 0; i--) {
                    char c = (char) chunk[i];
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
        processFirstLine(linesFound, limit, currentLine, searchTerm, logs);

        return logs;
    }

    private void processFirstLine(int linesFound,
                                  int limit,
                                  StringBuilder currentLine,
                                  String searchTerm,
                                  List<String> logs) {
        if (linesFound < limit && !currentLine.isEmpty()) {
            String line = currentLine.reverse().toString();
            if (searchTerm == null || line.contains(searchTerm)) {
                logs.add(line.trim());
            }
        }
    }
}
