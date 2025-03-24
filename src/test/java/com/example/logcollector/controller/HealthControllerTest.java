package com.example.logcollector.controller;

import com.example.logcollector.model.health.HealthCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HealthControllerTest {
    private HealthController healthController;
    @BeforeEach
    public void init() {
        healthController = new HealthController();
    }

    @Test
    public void listLogs_goesThroughPipeline_returnsResponse() throws IOException, ExecutionException, InterruptedException, TimeoutException {
        HealthCheckResponse response = healthController.getHealth().getBody();
        assertEquals("ok", response.getStatus());
        assertEquals("1.0.0", response.getVersion());
        assertNotNull(response.getTimestamp());
    }
}
