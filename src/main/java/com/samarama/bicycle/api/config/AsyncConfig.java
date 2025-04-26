package com.samarama.bicycle.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Ta klasa włącza obsługę asynchronicznego przetwarzania
    // pozwalając na wysyłanie e-maili w tle
}