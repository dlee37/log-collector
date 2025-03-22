package com.example.logcollector.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ListLogsResponse {
    private List<String> logs;
}
