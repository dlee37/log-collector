package com.example.logcollector.constants;

import java.util.List;

public class Constants {
    public static final int CHUNK_SIZE = 4096; // 4kb for initial chunk size
    public static final int DEFAULT_LIMIT = 100;
    public static final List<String> DEFAULT_LOG_FILES = List.of("syslog", "messages");
    public static final long CACHE_TTL_IN_MS = 2 * 60 * 1000; // 2 minutes
    public static final int MAX_CACHE_ENTRIES = 100;
    public static final int MAX_REQUEST_TIMEOUT_IN_SECONDS = 10;
}
