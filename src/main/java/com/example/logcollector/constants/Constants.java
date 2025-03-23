package com.example.logcollector.constants;

import java.util.List;

public class Constants {
    public static final String LOG_PATH = "/var/logs";
    public static final String SAMPLE_LOG_PATH = "./sample-logs";
    public static final int CHUNK_SIZE = 4096; // 4kb for initial chunk size
    public static final int DEFAULT_LIMIT = 100;
    public static final List<String> DEFAULT_LOG_FILES = List.of("syslog", "messages");
}
