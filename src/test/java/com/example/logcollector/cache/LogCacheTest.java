package com.example.logcollector.cache;

import com.example.logcollector.cache.entry.CacheEntry;
import com.example.logcollector.cache.impl.LogCache;
import com.example.logcollector.constants.Constants;
import com.example.logcollector.model.logs.ListEntriesRequest;
import com.example.logcollector.model.logs.ListEntriesResponse;
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
        ListEntriesRequest request = ListEntriesRequest.builder().searchTerm("a").build();
        assertTrue(cache.isCacheable(request));
    }

    @Test
    public void isCacheable_hasOffset_returnsTrue() {
        ListEntriesRequest request = ListEntriesRequest.builder().offset(900L).build();
        assertTrue(cache.isCacheable(request));
    }

    @Test
    public void isCacheable_hasNeitherAbove_returnsFalse() {
        ListEntriesRequest request = ListEntriesRequest.builder().build();
        assertFalse(cache.isCacheable(request));
    }

    @Test
    public void buildCacheKey_fromRequest_returnsKey() {
        ListEntriesRequest request = ListEntriesRequest.builder()
                .offset(900L)
                .build();
        String key = cache.buildCacheKey(request);

        assertEquals("||900|100", key);
    }

    @Test
    public void get_fromKeyHasCacheValue_returnsValue() {
        ListEntriesResponse response = ListEntriesResponse.builder().build();
        cache.put("some-key", response);
        ListEntriesResponse cachedResponse = cache.get("some-key");
        assertNotNull(cachedResponse);
    }

    @Test
    public void get_fromKeyNoCacheValue_returnsNull() {
        ListEntriesResponse cachedResponse = cache.get("some-key");
        assertNull(cachedResponse);
    }

    @Test
    public void get_fromKeyExpiredValue_returnsNull() {
        cache.getCache().put("some-key", new CacheEntry<>(ListEntriesResponse.builder().build(), -1));
        ListEntriesResponse cachedResponse = cache.get("some-key");
        assertNull(cachedResponse);
    }

    @Test
    public void evictExpired_hasEntriesToRemove_succeeded() {
        cache.getCache().put("some-key", new CacheEntry<>(ListEntriesResponse.builder().build(), -1));
        cache.getCache().put("some-key1", new CacheEntry<>(ListEntriesResponse.builder().build(), Constants.CACHE_TTL_IN_MS));
        cache.getCache().put("some-key2", new CacheEntry<>(ListEntriesResponse.builder().build(), -1));
        cache.evictExpired();

        assertEquals(1, cache.getCache().size());
    }
}
