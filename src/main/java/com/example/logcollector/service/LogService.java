package com.example.logcollector.service;

import com.example.logcollector.cache.Cache;
import com.example.logcollector.constants.Constants;
import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import com.example.logcollector.model.LogPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private final Cache<ListLogsRequest, ListLogsResponse> cache;
    private final String logPath;

    @Autowired
    public LogService(Cache<ListLogsRequest, ListLogsResponse> cache, @Value("/var/log") String logPath) {
        this.cache = cache;
        this.logPath = logPath;
    }

    public ListLogsResponse listLogs(ListLogsRequest request) throws IOException, InterruptedException {
        String fileName = request.getFileName();
        String searchTerm = request.getSearchTerm();
        int limit = request.getLimit() == null ? Constants.DEFAULT_LIMIT : request.getLimit();
        long offset = request.getOffset() == null ? 0 : request.getOffset();
        String requestString = String.format("file: %s, searchTerm: %s, limit: %s, offset: %s", fileName, searchTerm, limit, offset);
        if (cache.isCacheable(request)) {
            String key = cache.buildCacheKey(request);
            ListLogsResponse cachedResponse = cache.get(key);
            if (cachedResponse != null) {
                logger.info("Found cache hit for request: {}! Returning from cache with list size: {}", requestString, cachedResponse.getLogs().size());
                return cachedResponse;
            }
        }

        File file = validateFile(fileName);
        StopWatch watch = new StopWatch();
        watch.start();
        logger.info("Received list logs request for request: {}", requestString);
        // start of the business logic here
        LogPage page = processLogsInReverse(file, searchTerm.toLowerCase(), limit, offset);

        watch.stop();
        logger.info("Logs for request {} took {} ms", requestString, watch.getTotalTimeMillis());
        ListLogsResponse response = ListLogsResponse.builder()
                .logs(page.getLogs())
                .hasMore(page.getHasMore())
                .offset(offset)
                .limit(limit)
                .nextOffset(offset + page.getLogs().size())
                .build();

        // This is a swallowed return. In this situation the controller already returned the proper timeout error
        if (Thread.currentThread().isInterrupted()) {
            return response;
        }

        if (cache.isCacheable(request)) {
            logger.info("Caching request: {}", requestString);
            cache.put(cache.buildCacheKey(request), response);
        }
        return response;
    }

    private File validateFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            logger.warn("No file specified, defaulting to syslog or messages...");
            for (String logFile : Constants.DEFAULT_LOG_FILES) {
                File file = new File(String.format("%s/%s", logPath, logFile));
                if (file.exists()) {
                    return file;
                }
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "syslog or messages log file does not exist!");
        }

        File file = new File(String.format("%s/%s", logPath, fileName));
        if (!file.exists()) {
            File logDirectory = new File(logPath);
            File[] files = logDirectory.listFiles(f ->
                    f.isFile() && !f.getName().endsWith(".gz") && !f.getName().startsWith("."));
            if (files == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("No files exist in %s!", logPath));
            }
            String fullFileList = Arrays.stream(files).map(File::getName).collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("File not found. List of files include: %s", fullFileList));
        } else if (!file.isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("File %s must be a valid file, not a directory!", fileName));
        }
        return file;
    }

    private LogPage processLogsInReverse(File file, String searchTerm, int limit, long offset) throws IOException, InterruptedException {
        List<String> logs = new ArrayList<>();
        byte[] chunk = new byte[CHUNK_SIZE];
        StringBuilder currentLine = new StringBuilder();
        int linesFound = 0;
        int linesSkipped = 0;
        boolean hasMore = false;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long filePointer = raf.length();
            while (filePointer > 0 && linesFound < limit) {
                // This is a swallowed return. In this situation the controller already returned the proper timeout error
                if (Thread.currentThread().isInterrupted()) {
                    return LogPage.builder().build();
                }
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
                            if (searchTerm == null || line.toLowerCase().contains(searchTerm)) {
                                if (linesSkipped < offset) {
                                    linesSkipped++;
                                    continue;
                                }
                                if (linesFound < limit) {
                                    logs.add(line.trim());
                                    linesFound++;
                                } else {
                                    hasMore = true;
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
        hasMore = processFirstLine(linesFound, limit, currentLine, searchTerm, logs, hasMore);

        logger.info("Number of logs retrieved: {}", logs.size());
        return LogPage.builder()
                .logs(logs)
                .hasMore(hasMore)
                .build();
    }

    private boolean processFirstLine(int linesFound,
                                     int limit,
                                     StringBuilder currentLine,
                                     String searchTerm,
                                     List<String> logs,
                                     boolean hasMore) {
        boolean updatedHasMore = hasMore;
        if (linesFound < limit && !currentLine.isEmpty()) {
            String line = currentLine.reverse().toString();
            if (searchTerm == null || line.toLowerCase().contains(searchTerm)) {
                logs.add(line.trim());
            }
        } else if (!updatedHasMore) {
            String line = currentLine.reverse().toString();
            if (searchTerm == null || line.toLowerCase().contains(searchTerm)) {
                updatedHasMore = true;
            }
        }
        return updatedHasMore;
    }
}
