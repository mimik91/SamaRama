package com.samarama.bicycle.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${cache.bike-services.ttl-minutes:15}")
    private int bikeServicesTtlMinutes;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
                // Główny cache wszystkich serwisów - 15 minut, max 3000 elementów
                buildCache("allBikeServices", bikeServicesTtlMinutes, 3000),

                // Cache dla pinów klastrowanych - 10 minut
                buildCache("clusteredPins", 10, 1000),

                // Cache dla pinów mapy - 10 minut
                buildCache("mapPins", 10, 1000),

                // Cache dla szczegółów serwisu - 30 minut
                buildCache("serviceDetails", 30, 3000)
        ));

        return cacheManager;
    }

    private CaffeineCache buildCache(String name, long ttlMinutes, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}