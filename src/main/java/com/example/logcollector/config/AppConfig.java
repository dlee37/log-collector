package com.example.logcollector.config;

import com.example.logcollector.cache.Cache;
import com.example.logcollector.cache.impl.LogCache;
import com.example.logcollector.model.logs.ListEntriesRequest;
import com.example.logcollector.model.logs.ListEntriesResponse;
import com.example.logcollector.util.TimeoutExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public Cache<ListEntriesRequest, ListEntriesResponse> logCache() {
        return new LogCache();
    }

    @Bean
    public TimeoutExecutor getTimeoutExecutor() {
        return new TimeoutExecutor();
    }
}
