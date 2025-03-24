package com.example.logcollector.model.logs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ListEntriesRequest {
    private String fileName;

    private String searchTerm;

    private Integer limit;

    private Long offset;
}
