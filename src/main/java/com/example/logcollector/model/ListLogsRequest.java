package com.example.logcollector.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ListLogsRequest {
    private String fileName;

    private String searchTerm;

    private Integer limit; // default limit

    private Long nextLine;
}
