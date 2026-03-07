package com.example.back_end.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for Dashboard endpoints.
 * Cache TTL (Time To Live): 1 minute
 * This ensures dashboard data refreshes automatically every minute.
 */
@Configuration
public class CacheConfig {

    /**
     * Configure cache manager with TTL-enabled caches for dashboard.
     * Cache names:
     * Store Dashboard:
     * - storeSummary: Store dashboard summary
     * - storeSalesTrend: 7-day sales trend
     * - storeCategoryCounts: Category product counts
     * - storeTopProducts: Top selling products
     * - storeRecentDaily: Recent daily sales
     * Inventory Dashboard:
     * - inventorySummary: Inventory dashboard summary
     * - recentInventoryMovements: Recent inventory movements
     * - weeklyInventoryTrend: Weekly movement trend
     * - topMovedProducts: Most moved products
     * - categorySalesPie: Category sales pie chart
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            // Store Dashboard caches
            createTtlCache("storeSummary", 60),      // 1 minute TTL
            createTtlCache("storeSalesTrend", 60),   // 1 minute TTL
            createTtlCache("storeCategoryCounts", 60), // 1 minute TTL
            createTtlCache("storeTopProducts", 60),  // 1 minute TTL
            createTtlCache("storeRecentDaily", 60),   // 1 minute TTL
            // Inventory Dashboard caches
            createTtlCache("inventorySummary", 60),  // 1 minute TTL
            createTtlCache("recentInventoryMovements", 60), // 1 minute TTL
            createTtlCache("weeklyInventoryTrend", 60), // 1 minute TTL
            createTtlCache("topMovedProducts", 60),  // 1 minute TTL
            createTtlCache("categorySalesPie", 60)   // 1 minute TTL
        ));

        return cacheManager;
    }

    /**
     * Create a cache with TTL (Time To Live) support.
     *
     * @param name Cache name
     * @param ttlSeconds Time to live in seconds
     * @return TtlConcurrentMapCache instance
     */
    private TtlConcurrentMapCache createTtlCache(String name, long ttlSeconds) {
        return new TtlConcurrentMapCache(name, ttlSeconds);
    }

    /**
     * Custom cache implementation with TTL support.
     * Entries expire after specified TTL.
     */
    @SuppressWarnings("NullableProblems")
    static class TtlConcurrentMapCache extends ConcurrentMapCache {
        private final long ttlMillis;
        private final ConcurrentHashMap<Object, Long> expiryMap = new ConcurrentHashMap<>();

        public TtlConcurrentMapCache(String name, long ttlSeconds) {
            super(name);
            this.ttlMillis = TimeUnit.SECONDS.toMillis(ttlSeconds);
        }

        @Override
        public ValueWrapper get(Object key) {
            // Check if entry has expired
            Long expiryTime = expiryMap.get(key);
            if (expiryTime != null && System.currentTimeMillis() > expiryTime) {
                // Entry expired, evict it
                evict(key);
                return null;
            }
            return super.get(key);
        }

        @Override
        public void put(Object key, Object value) {
            // Store entry with expiry time
            expiryMap.put(key, System.currentTimeMillis() + ttlMillis);
            super.put(key, value);
        }

        @Override
        public void evict(Object key) {
            expiryMap.remove(key);
            super.evict(key);
        }

        @Override
        public void clear() {
            expiryMap.clear();
            super.clear();
        }
    }
}
