package com.example.logcollector.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class LogPage {
    private List<String> logs;

    private Boolean hasMore;
}
