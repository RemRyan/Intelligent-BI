package com.ljh.springbootinit.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
@Data
public class SparkAiConfig {
    @Value("${spark.ai.api.url}")
    private String apiUrl;

    @Value("${spark.ai.api.key}")
    private String apiKey;

}