package com.example.logcollector.cache.impl;

import com.example.logcollector.cache.Cache;
import com.example.logcollector.cache.entry.CacheEntry;
import com.example.logcollector.constants.Constants;
import com.example.logcollector.model.logs.ListEntriesRequest;
import com.example.logcollector.model.logs.ListEntriesResponse;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class LogCache implements Cache<ListEntriesRequest, ListEntriesResponse> {
    private Map<String, CacheEntry<ListEntriesResponse>> cache;

    @PostConstruct
    public void init() {
        cache = Collections.synchronizedMap(new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<ListEntriesResponse>> eldest) {
                return size() > Constants.MAX_CACHE_ENTRIES;
            }
        });
    }

    public boolean isCacheable(ListEntriesRequest request) {
        Long offset = request.getOffset();
        String searchTerm = request.getSearchTerm();
        return (searchTerm != null && !searchTerm.isBlank()) || (offset != null && offset > 100);
    }

    public String buildCacheKey(ListEntriesRequest request) {
        return String.format("%s|%s|%s|%s",
                request.getFileName() == null ? "" : request.getFileName(),
                request.getSearchTerm() == null ? "" : request.getSearchTerm(),
                request.getOffset() == null ? 0 : request.getOffset(),
                request.getLimit() == null ? 100L : request.getLimit());
    }

    public ListEntriesResponse get(String key) {
        CacheEntry<ListEntriesResponse> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.getValue();
    }

    public void put(String key, ListEntriesResponse response) {
        cache.put(key, new CacheEntry<>(response, Constants.CACHE_TTL_IN_MS));
    }

    @Scheduled(fixedRate = Constants.CACHE_TTL_IN_MS)
    public void evictExpired() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
