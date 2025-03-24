package com.example.logcollector.model.logs;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ListEntriesResponse {
    private List<String> logs;

    private Long offset;

    private Integer limit;

    private Boolean hasMore;

    private Long nextOffset;
}
