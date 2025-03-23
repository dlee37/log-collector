package com.example.logcollector.cache;

import com.example.logcollector.cache.entry.CacheEntry;
import com.example.logcollector.cache.impl.LogCache;
import com.example.logcollector.constants.Constants;
import com.example.logcollector.model.ListLogsRequest;
import com.example.logcollector.model.ListLogsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LogCacheTest {
    private LogCache cache;

    @BeforeEach
    public void init() {
        cache = new LogCache();
        cache.init();
    }

    @Test
    public void isCacheable_hasSearchTerm_returnsTrue() {
        ListLogsRequest request = ListLogsRequest.builder().searchTerm("a").build();
        assertTrue(cache.isCacheable(request));
    }

    @Test
    public void isCacheable_hasOffset_returnsTrue() {
        ListLogsRequest request = ListLogsRequest.builder().offset(900L).build();
        assertTrue(cache.isCacheable(request));
    }

    @Test
    public void isCacheable_hasNeitherAbove_returnsFalse() {
        ListLogsRequest request = ListLogsRequest.builder().build();
        assertFalse(cache.isCacheable(request));
    }

    @Test
    public void buildCacheKey_fromRequest_returnsKey() {
        ListLogsRequest request = ListLogsRequest.builder()
                .offset(900L)
                .build();
        String key = cache.buildCacheKey(request);

        assertEquals("||900|100", key);
    }

    @Test
    public void get_fromKeyHasCacheValue_returnsValue() {
        ListLogsResponse response = ListLogsResponse.builder().build();
        cache.put("some-key", response);
        ListLogsResponse cachedResponse = cache.get("some-key");
        assertNotNull(cachedResponse);
    }

    @Test
    public void get_fromKeyNoCacheValue_returnsNull() {
        ListLogsResponse cachedResponse = cache.get("some-key");
        assertNull(cachedResponse);
    }

    @Test
    public void get_fromKeyExpiredValue_returnsNull() {
        cache.getCache().put("some-key", new CacheEntry<>(ListLogsResponse.builder().build(), -1));
        ListLogsResponse cachedResponse = cache.get("some-key");
        assertNull(cachedResponse);
    }

    @Test
    public void evictExpired_hasEntriesToRemove_succeeded() {
        cache.getCache().put("some-key", new CacheEntry<>(ListLogsResponse.builder().build(), -1));
        cache.getCache().put("some-key1", new CacheEntry<>(ListLogsResponse.builder().build(), Constants.CACHE_TTL_IN_MS));
        cache.getCache().put("some-key2", new CacheEntry<>(ListLogsResponse.builder().build(), -1));
        cache.evictExpired();

        assertEquals(1, cache.getCache().size());
    }
}
