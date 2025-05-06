package com.ljh.springbootinit.config;

import io.github.briqt.spark4j.SparkClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "xun-fei.client")
public class XingHuoConfig {
    private String appId;
    private String apiSecret;
    private String apiKey;

    @Bean
    public SparkClient sparkClient() {
        SparkClient sparkClient = new SparkClient();
//        sparkClient.apiKey = apiKey;
//        sparkClient.apiSecret = apiSecret;
//        sparkClient.appid = appId;
        sparkClient.apiKey = "7146500010c016d242e2deb36cb62c5e";
        sparkClient.apiSecret = "YmYwYzMxY2U0NjU3N2Q3MzVkZmVmZDc4";
        sparkClient.appid = "99e64c45";
        return sparkClient;
    }
}
