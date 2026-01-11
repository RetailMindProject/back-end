package com.example.back_end.modules.sales.returns.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReturnProperties.class)
public class ReturnConfig {
}

