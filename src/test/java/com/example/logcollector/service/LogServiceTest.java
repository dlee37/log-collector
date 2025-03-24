
package com.example.logcollector.service;

import com.example.logcollector.cache.Cache;
import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class LogServiceTest {
    private static final String LOG_NAME = "test.log";

    private LogService logService;

    @Mock
    private Cache<ListLogsRequest, ListLogsResponse> mockCache;

    @TempDir
    private Path tempDir;

    private AutoCloseable mocks;

    @BeforeEach
    public void init() throws IOException {
        mocks = openMocks(this);
        logService = new LogService(mockCache, tempDir.toString());
    }

    @AfterEach
    public void afterEach() throws Exception {
        mocks.close();
    }

    private void createLogFile(List<String> lines) throws IOException {
        File file = tempDir.resolve(LOG_NAME).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
    }

    @Test
    public void testListLogs_searchTerm_returnsFilteredResults() throws IOException, InterruptedException {
        createLogFile(List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure"));

        ListLogsRequest request = ListLogsRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .offset(0L)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ListLogsResponse response = logService.listLogs(request);

        assertNotNull(response);
        assertEquals(3, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("5 ERROR"));
    }

    @Test
    public void testListLogs_listWithLimit_returnsPaginatedResults() throws IOException, InterruptedException {
        createLogFile(List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure"));

        ListLogsRequest request = ListLogsRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .limit(1)
                .offset(0L)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ListLogsResponse response = logService.listLogs(request);

        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("5 ERROR"));
        assertTrue(response.getHasMore());

        // ensure 2nd request is properly paginated
        request = ListLogsRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .limit(1)
                .offset(response.getNextOffset())
                .build();
        response = logService.listLogs(request);
        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("3 ERROR"));
        assertTrue(response.getHasMore());

        // ensure last edge case of the first line is properly paginated
        // ensure 2nd request is properly paginated
        request = ListLogsRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .limit(1)
                .offset(response.getNextOffset())
                .build();
        response = logService.listLogs(request);
        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("1 ERROR"));
        assertFalse(response.getHasMore());
    }

    @Test
    public void testListLogs_hasCache_returnsCachedInput() throws IOException, InterruptedException {
        createLogFile(List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure"));

        ListLogsRequest request = ListLogsRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .offset(0L)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(true);
        when(mockCache.buildCacheKey(request)).thenReturn("some-key");
        when(mockCache.get(anyString())).thenReturn(
                ListLogsResponse.builder().logs(List.of("sample-log-line"))
                        .build());

        ListLogsResponse response = logService.listLogs(request);

        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("sample-log-line"));
    }

    @Test
    public void testListLogs_throwsWhenFileMissing() {
        ListLogsRequest request = ListLogsRequest.builder()
                .fileName("nonexistent.log")
                .searchTerm("ERROR")
                .limit(5)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> logService.listLogs(request));
        assertEquals(404, exception.getStatusCode().value());
        assertTrue(Objects.requireNonNull(exception.getReason()).contains("File not found"));
    }
}
