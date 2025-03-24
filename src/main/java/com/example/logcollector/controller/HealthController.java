package com.example.logcollector.controller;

import com.example.logcollector.model.health.HealthCheckResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static com.example.logcollector.constants.Constants.HEALTH_CHECK_STATUS_OK;
import static com.example.logcollector.constants.Constants.HEALTH_CHECK_VERSION;

@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping
    public ResponseEntity<HealthCheckResponse> getHealth() {
        return ResponseEntity.ok(HealthCheckResponse.builder()
                .status(HEALTH_CHECK_STATUS_OK)
                .version(HEALTH_CHECK_VERSION)
                .timestamp(Instant.now().toString())
                .build());
    }
}
