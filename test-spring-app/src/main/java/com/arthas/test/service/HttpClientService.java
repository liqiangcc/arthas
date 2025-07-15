package com.arthas.test.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP客户端服务 - 用于测试HTTP客户端调用的链路跟踪
 */
@Service
public class HttpClientService {
    
    private final HttpClient httpClient;
    
    public HttpClientService() {
        this.httpClient = HttpClients.createDefault();
    }
    
    /**
     * 调用外部API - GET请求
     */
    public Map<String, Object> callExternalApi(String url) {
        System.out.println("HttpClientService.callExternalApi() - Calling: " + url);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 创建GET请求
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Content-Type", "application/json");
            httpGet.setHeader("User-Agent", "Arthas-Test-App/1.0");
            
            // 2. 执行请求
            HttpResponse response = httpClient.execute(httpGet);
            
            // 3. 处理响应
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            result.put("statusCode", statusCode);
            result.put("responseBody", responseBody);
            result.put("success", statusCode >= 200 && statusCode < 300);
            
            System.out.println("HttpClientService.callExternalApi() - Response status: " + statusCode);
            
        } catch (IOException e) {
            System.err.println("HttpClientService.callExternalApi() - Error: " + e.getMessage());
            result.put("error", e.getMessage());
            result.put("success", false);
        }
        
        return result;
    }
    
    /**
     * 调用多个外部API
     */
    public Map<String, Object> callMultipleApis() {
        System.out.println("HttpClientService.callMultipleApis() - Starting multiple API calls");
        
        Map<String, Object> results = new HashMap<>();
        
        // 1. 调用JSONPlaceholder API (公共测试API)
        Map<String, Object> jsonPlaceholderResult = callExternalApi("https://jsonplaceholder.typicode.com/posts/1");
        results.put("jsonplaceholder", jsonPlaceholderResult);
        
        // 2. 调用httpbin API (HTTP测试服务)
        Map<String, Object> httpbinResult = callExternalApi("https://httpbin.org/get");
        results.put("httpbin", httpbinResult);
        
        // 3. 调用本地API（如果可用）
        Map<String, Object> localResult = callExternalApi("http://localhost:8080/api/test/simple");
        results.put("local", localResult);
        
        System.out.println("HttpClientService.callMultipleApis() - Completed all API calls");
        return results;
    }
    
    /**
     * POST请求示例
     */
    public Map<String, Object> postToExternalApi(String url, String jsonData) {
        System.out.println("HttpClientService.postToExternalApi() - Posting to: " + url);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 创建POST请求
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("User-Agent", "Arthas-Test-App/1.0");
            
            // 2. 设置请求体
            StringEntity entity = new StringEntity(jsonData);
            httpPost.setEntity(entity);
            
            // 3. 执行请求
            HttpResponse response = httpClient.execute(httpPost);
            
            // 4. 处理响应
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            result.put("statusCode", statusCode);
            result.put("responseBody", responseBody);
            result.put("success", statusCode >= 200 && statusCode < 300);
            
            System.out.println("HttpClientService.postToExternalApi() - Response status: " + statusCode);
            
        } catch (IOException e) {
            System.err.println("HttpClientService.postToExternalApi() - Error: " + e.getMessage());
            result.put("error", e.getMessage());
            result.put("success", false);
        }
        
        return result;
    }
    
    /**
     * 模拟复杂的HTTP调用链
     */
    public Map<String, Object> complexHttpChain() {
        System.out.println("HttpClientService.complexHttpChain() - Starting complex HTTP call chain");
        
        Map<String, Object> chainResult = new HashMap<>();
        
        try {
            // 1. 第一个调用：获取用户信息
            Map<String, Object> userInfo = callExternalApi("https://jsonplaceholder.typicode.com/users/1");
            chainResult.put("step1_userInfo", userInfo);
            
            // 2. 第二个调用：获取用户的帖子
            Map<String, Object> userPosts = callExternalApi("https://jsonplaceholder.typicode.com/users/1/posts");
            chainResult.put("step2_userPosts", userPosts);
            
            // 3. 第三个调用：创建新帖子
            String newPostJson = "{\"title\":\"Test Post\",\"body\":\"This is a test post\",\"userId\":1}";
            Map<String, Object> createPost = postToExternalApi("https://jsonplaceholder.typicode.com/posts", newPostJson);
            chainResult.put("step3_createPost", createPost);
            
            chainResult.put("chainSuccess", true);
            
        } catch (Exception e) {
            System.err.println("HttpClientService.complexHttpChain() - Chain failed: " + e.getMessage());
            chainResult.put("chainSuccess", false);
            chainResult.put("error", e.getMessage());
        }
        
        System.out.println("HttpClientService.complexHttpChain() - Chain completed");
        return chainResult;
    }
}
