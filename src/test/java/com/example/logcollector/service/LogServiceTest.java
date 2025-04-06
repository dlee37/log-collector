
package com.example.logcollector.service;

import com.example.logcollector.cache.Cache;
import com.example.logcollector.model.logs.ListEntriesRequest;
import com.example.logcollector.model.logs.ListEntriesResponse;
import com.example.logcollector.model.logs.ListFilesResponse;
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
    private static final String OUTSIDE_LOG_FILE = "outside-log.log";
    private static final String MAIN_DIR = "some-dir";
    private static final String REQUEST_ID = "test-request-id";
    private static final String SUB_DIR = "other-directory";
    private static final String SYSLOG = "syslog";
    private static final String MESSAGES = "messages";

    private LogService logService;

    @Mock
    private Cache<ListEntriesRequest, ListEntriesResponse> mockCache;

    @TempDir
    private Path tempDir;

    private AutoCloseable mocks;

    @BeforeEach
    public void init() throws IOException {
        mocks = openMocks(this);
        File file = new File(tempDir.toFile(), MAIN_DIR);
        logService = new LogService(mockCache, file.getCanonicalPath());
    }

    @AfterEach
    public void afterEach() throws Exception {
        mocks.close();
    }

    private void createLogFile(List<String> lines) throws IOException {
        File file = tempDir.resolve(String.format("%s/%s", MAIN_DIR, LOG_NAME)).toFile();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        File subDirectory = tempDir.resolve(String.format("%s/%s", MAIN_DIR, SUB_DIR)).toFile();

        try (FileWriter writer = new FileWriter(file)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
        if (!subDirectory.exists()) {
            subDirectory.mkdir();
        }
    }

    private void createOutsideLogFile(List<String> lines) throws IOException {
        File outsideFile = tempDir.resolve(OUTSIDE_LOG_FILE).toFile();

        try (FileWriter writer = new FileWriter(outsideFile)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }
    }

    @Test
    public void testListLogEntries_searchTerm_returnsFilteredResults() throws IOException, InterruptedException {
        createLogFile(List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure"));

        ListEntriesRequest request = ListEntriesRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .offset(0L)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ListEntriesResponse response = logService.listLogEntries(request, REQUEST_ID);

        assertNotNull(response);
        assertEquals(3, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("5 ERROR"));
    }

    @Test
    public void testListLogEntries_listWithLimit_returnsPaginatedResults() throws IOException, InterruptedException {
        createLogFile(List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure"));

        ListEntriesRequest request = ListEntriesRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .limit(1)
                .offset(0L)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ListEntriesResponse response = logService.listLogEntries(request, REQUEST_ID);

        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("5 ERROR"));
        assertTrue(response.getHasMore());

        // ensure 2nd request is properly paginated
        request = ListEntriesRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .limit(1)
                .offset(response.getNextOffset())
                .build();
        response = logService.listLogEntries(request, REQUEST_ID);
        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("3 ERROR"));
        assertTrue(response.getHasMore());

        // ensure last edge case of the first line is properly paginated
        // ensure 2nd request is properly paginated
        request = ListEntriesRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .limit(1)
                .offset(response.getNextOffset())
                .build();
        response = logService.listLogEntries(request, REQUEST_ID);
        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("1 ERROR"));
        assertFalse(response.getHasMore());
    }

    @Test
    public void testListLogEntries_hasCache_returnsCachedInput() throws IOException, InterruptedException {
        createLogFile(List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure"));

        ListEntriesRequest request = ListEntriesRequest.builder()
                .fileName(LOG_NAME)
                .searchTerm("ERROR")
                .offset(0L)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(true);
        when(mockCache.buildCacheKey(request)).thenReturn("some-key");
        when(mockCache.get(anyString())).thenReturn(
                ListEntriesResponse.builder().logs(List.of("sample-log-line"))
                        .build());

        ListEntriesResponse response = logService.listLogEntries(request, REQUEST_ID);

        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("sample-log-line"));
    }

    @Test
    public void testListLogEntries_throwsWhenFileMissing_throwsProperError() {
        ListEntriesRequest request = ListEntriesRequest.builder()
                .fileName("nonexistent.log")
                .searchTerm("ERROR")
                .limit(5)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> logService.listLogEntries(request, REQUEST_ID));
        assertEquals(404, exception.getStatusCode().value());
        assertTrue(Objects.requireNonNull(exception.getReason()).contains("not found"));
    }

    @Test
    public void testListLogEntries_throwsWhenNoFilesInDirectoryWithoutFilename_throwsProperError() {
        ListEntriesRequest request = ListEntriesRequest.builder()
                .limit(5)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> logService.listLogEntries(request, REQUEST_ID));
        assertEquals(404, exception.getStatusCode().value());
        assertTrue(Objects.requireNonNull(exception.getReason()).contains("does not exist"));
    }

    @Test
    public void testListLogFiles_getsLogFiles_returnsList() throws IOException {
        createLogFile(List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure"));

        ListFilesResponse response = logService.listLogFiles(REQUEST_ID);
        assertNotNull(response.getFiles());
        List<String> files = response.getFiles();
        assertEquals(1, files.size());
        assertEquals(LOG_NAME, files.getFirst());
    }

    @Test
    public void testLogEntries_defaultsToSyslog_returnsList() throws IOException, InterruptedException {
        List<String> logList = List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure");
        File file = tempDir.resolve(String.format("%s/%s", MAIN_DIR, SYSLOG)).toFile();
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            for (String line : logList) {
                writer.write(line + "\n");
            }
        }

        ListEntriesRequest request = ListEntriesRequest.builder()
                .searchTerm("DEBUG")
                .limit(5)
                .offset(0L)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);
        ListEntriesResponse response = logService.listLogEntries(request, REQUEST_ID);

        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("4 DEBUG"));
    }

    @Test
    public void testLogEntries_defaultsToMessages_returnsList() throws IOException, InterruptedException {
        List<String> logList = List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure");
        File file = tempDir.resolve(String.format("%s/%s", MAIN_DIR, MESSAGES)).toFile();
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            for (String line : logList) {
                writer.write(line + "\n");
            }
        }

        ListEntriesRequest request = ListEntriesRequest.builder()
                .searchTerm("DEBUG")
                .limit(5)
                .offset(0L)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);
        ListEntriesResponse response = logService.listLogEntries(request, REQUEST_ID);

        assertNotNull(response);
        assertEquals(1, response.getLogs().size());
        assertTrue(response.getLogs().getFirst().contains("4 DEBUG"));
    }

    @Test
    public void testListLogEntries_noSearchTerm_returnsResults() throws IOException, InterruptedException {
        createLogFile(List.of(
                "1 ERROR Something went wrong",
                "2 INFO Starting system",
                "3 ERROR Critical failure",
                "4 DEBUG Ignored event",
                "5 ERROR Critical failure"));

        ListEntriesRequest request = ListEntriesRequest.builder()
                .fileName(LOG_NAME)
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ListEntriesResponse response = logService.listLogEntries(request, REQUEST_ID);

        assertNotNull(response);
        assertEquals(5, response.getLogs().size());
    }

    @Test
    public void testListLogEntries_throwsWhenOutsideOfDirectory_throwsProperError() throws IOException {
        createOutsideLogFile(List.of("maybe sensitive information"));
        ListEntriesRequest request = ListEntriesRequest.builder()
                .fileName(String.format("../%s", OUTSIDE_LOG_FILE))
                .build();

        when(mockCache.isCacheable(request)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> logService.listLogEntries(request, REQUEST_ID));
        assertEquals(400, exception.getStatusCode().value());
        assertTrue(Objects.requireNonNull(exception.getReason()).contains("is not in /var/log"));
    }
}