package com.example.logcollector.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TimeoutExecutorTest {

    private TimeoutExecutor timeoutExecutor;

    @BeforeEach
    void setUp() {
        timeoutExecutor = new TimeoutExecutor();
    }

    @Test
    void runWithTimeout_testSuccessWithinTimeout_returnsProperValue() throws Exception {
        String result = timeoutExecutor.runWithTimeout(() -> "test", 1, TimeUnit.SECONDS);
        assertEquals("test", result);
    }

    @Test
    void runWithTimeout_timeout_throwsProperError() {
        assertThrows(TimeoutException.class, () -> timeoutExecutor.runWithTimeout(() -> {
            Thread.sleep(1000);
            return "test";
        }, 500, TimeUnit.MILLISECONDS));
    }
}
