package com.example.logcollector.model.health;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class HealthCheckResponse {
    private String status;
    private String version;
    private String timestamp;
}
