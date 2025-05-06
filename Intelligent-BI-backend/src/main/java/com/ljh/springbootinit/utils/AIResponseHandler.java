package com.ljh.springbootinit.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIResponseHandler {

    public static List<String> handleResponse(String response) {
        try {
            JsonElement jsonElement = JsonParser.parseString(response);
            JsonObject rootObject = jsonElement.getAsJsonObject();

            // 检查 "choices" 数组是否存在且不为空
            if (!rootObject.has("choices") || rootObject.getAsJsonArray("choices").isEmpty()) {
                throw new IllegalArgumentException("响应中缺少 'choices' 字段或它为空");
            }

            // 获取 choices[0].message.content
            JsonObject choicesObject = rootObject.getAsJsonArray("choices").get(0).getAsJsonObject();
            JsonObject messageObject = choicesObject.getAsJsonObject("message");
            String content = messageObject.get("content").getAsString();

            // 提取和清理 "content" 中的分析结论和图表配置
            String conclusion = extractConclusion(content);
            String chartConfig = extractChartConfig(content);

            List<String> results = new ArrayList<>();
            results.add(chartConfig);
            results.add(conclusion);

            return results;

        } catch (Exception e) {
            throw new RuntimeException("解析响应时发生错误: " + e.getMessage(), e);
        }
    }

    // 提取分析结论部分
    private static String extractConclusion(String content) {
        // 使用正则表达式匹配 "分析结论：" 到 "图表配置：" 之间的内容
        Pattern pattern = Pattern.compile("text：\\s*(.*?)\\s*chart：", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            throw new IllegalArgumentException("无法提取分析结论");
        }
    }

    // 提取图表配置部分
    private static String extractChartConfig(String content) {
        // 使用正则表达式匹配 Markdown 中的 ```json 和 ``` 之间的内容
        Pattern pattern = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            String chartConfig = matcher.group(1).trim();
            try {
                // 解析 JSON 确保它是一个合法的 JSON 对象
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(chartConfig);
                return jsonNode.toString();
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("图表配置不是有效的 JSON 格式", e);
            }
        } else {
            throw new IllegalArgumentException("无法提取图表配置");
        }
    }

    public static void main(String[] args) {
        String aiResponse = "{\"code\":0,\"message\":\"Success\",\"sid\":\"cha000b7911@dx192b7ff8a62b8f3532\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"分析结论：\\n从给定的数据来看，人数随着日期的增加而增加。在第一天，人数是20人；到了第二天，人数增加到30人；第三天人数进一步增加到40人。这表明随着时间的推移，参与的人数也在逐渐增加。\\n\\n图表配置：\\n```json\\n{\\n  \\\"title\\\": {\\n    \\\"text\\\": \\\"每日人数变化图\\\"\\n  },\\n  \\\"tooltip\\\": {},\\n  \\\"xAxis\\\": {\\n    \\\"type\\\": \\\"category\\\",\\n    \\\"data\\\": [\\\"1\\\", \\\"2\\\", \\\"3\\\"]\\n  },\\n  \\\"yAxis\\\": {\\n    \\\"type\\\": \\\"value\\\"\\n  },\\n  \\\"series\\\": [{\\n    \\\"name\\\": \\\"人数\\\",\\n    \\\"type\\\": \\\"line\\\",\\n    \\\"data\\\": [20, 30, 40]\\n  }]\\n}\\n```\"},\"index\":0}],\"usage\":{\"prompt_tokens\":96,\"completion_tokens\":186,\"total_tokens\":282}}";

        List<String> results = AIResponseHandler.handleResponse(aiResponse);

        System.out.println("图表配置: " + results.get(0));
        System.out.println("分析结论: " + results.get(1));
    }
}