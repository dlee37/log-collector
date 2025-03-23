package com.example.logcollector.cache.entry;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheEntry<T> {
    private final T value;
    private final long expiresAt;

    public CacheEntry(T value, long ttlMillis) {
        this.value = value;
        this.expiresAt = System.currentTimeMillis() + ttlMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
