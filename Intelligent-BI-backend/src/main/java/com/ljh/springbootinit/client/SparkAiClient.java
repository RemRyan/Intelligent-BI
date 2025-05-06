package com.ljh.springbootinit.client;


import com.ljh.springbootinit.config.SparkAiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class SparkAiClient {

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private SparkAiConfig sparkAiConfig;

    public String callAiService(String userInput) {
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sparkAiConfig.getApiKey());
        headers.set("Content-Type", "application/json");

        // 创建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "lite");
        // 替换为实际的用户唯一ID
        requestBody.put("user", "11111");
        requestBody.put("temperature", 0.5);
        requestBody.put("max_tokens", 1024);
        requestBody.put("stream", false);

        // 构建 messages 列表
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个数据分析师");

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userInput);

        requestBody.put("messages", Arrays.asList(systemMessage, userMessage));

        // 构建请求实体
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 发送 POST 请求
        String apiUrl = sparkAiConfig.getApiUrl();
        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            // 检查响应
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("Failed to call AI service: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            System.out.println("Error status code: " + e.getStatusCode());
            System.out.println("Error response body: " + e.getResponseBodyAsString());
            throw e;
        }
    }
}