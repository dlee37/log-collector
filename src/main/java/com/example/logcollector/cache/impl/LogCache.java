package com.example.logcollector.cache.impl;

import com.example.logcollector.cache.Cache;
import com.example.logcollector.cache.entry.CacheEntry;
import com.example.logcollector.constants.Constants;
import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class LogCache implements Cache<ListLogsRequest, ListLogsResponse> {
    private Map<String, CacheEntry<ListLogsResponse>> cache;

    @PostConstruct
    public void init() {
        cache = Collections.synchronizedMap(new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<ListLogsResponse>> eldest) {
                return size() > Constants.MAX_CACHE_ENTRIES;
            }
        });
    }

    public boolean isCacheable(ListLogsRequest request) {
        Long offset = request.getOffset();
        String searchTerm = request.getSearchTerm();
        return (searchTerm != null && !searchTerm.isBlank()) || (offset != null && offset > 100);
    }

    public String buildCacheKey(ListLogsRequest request) {
        return String.format("%s|%s|%s|%s",
                request.getFileName(),
                request.getSearchTerm() == null ? "" : request.getSearchTerm(),
                request.getOffset() == null ? 0 : request.getOffset(),
                request.getLimit() == null ? 100L : request.getLimit());
    }

    public ListLogsResponse get(String key) {
        CacheEntry<ListLogsResponse> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    public void put(String key, ListLogsResponse response) {
        cache.put(key, new CacheEntry<>(response, Constants.CACHE_TTL_IN_MS));
    }

    @Scheduled(fixedRate = Constants.CACHE_TTL_IN_MS)
    public void evictExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
