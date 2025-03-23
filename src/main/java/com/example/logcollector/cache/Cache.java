package com.example.logcollector.cache;

import com.example.logcollector.model.ListLogsRequest;

public interface Cache<R, T> {
    boolean isCacheable(R request);

    String buildCacheKey(R request);

    T get(String key);

    void put(String key, T response);

    void evictExpired();
}
