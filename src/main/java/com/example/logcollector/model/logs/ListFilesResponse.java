package com.example.logcollector.model.logs;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class ListFilesResponse {
    private List<String> files;
}
