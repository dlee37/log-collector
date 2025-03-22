package com.example.logcollector.validation;

import com.example.logcollector.model.ListLogsRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ListLogsRequestValidator {
    public void validate(ListLogsRequest request) {
        if (request.getFileName() == null || request.getFileName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fileName cannot be missing or empty. fileName is also case sensitive!");
        }

        if (request.getLimit() != null && (request.getLimit() < 1 || request.getLimit() > 1000)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limit must be between 1 and 1000");
        }
    }
}
