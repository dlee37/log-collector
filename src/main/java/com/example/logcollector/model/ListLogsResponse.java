package com.example.logcollector.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ListLogsResponse {
    private List<String> logs;

    private Long offset;

    private Integer limit;

    private Boolean hasMore;

    private Long nextOffset;
}
