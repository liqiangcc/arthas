package com.arthas.test.controller;

import com.arthas.test.entity.User;
import com.arthas.test.service.FileOperationService;
import com.arthas.test.service.HttpClientService;
import com.arthas.test.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试控制器 - 用于验证阶段3的链路跟踪功能
 * 每个端点都会触发不同类型的操作，方便测试多探针协同工作
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private HttpClientService httpClientService;
    
    @Autowired
    private FileOperationService fileOperationService;
    
    /**
     * 简单测试端点 - 只有HTTP Server操作
     */
    @GetMapping("/simple")
    public ResponseEntity<Map<String, Object>> simpleTest() {
        System.out.println("=== TestController.simpleTest() - HTTP Server only ===");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Simple HTTP response");
        response.put("timestamp", System.currentTimeMillis());
        response.put("operations", "HTTP Server only");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 数据库操作测试 - HTTP Server + Database
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> databaseTest() {
        System.out.println("=== TestController.databaseTest() - HTTP Server + Database ===");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 创建测试用户
            User user = userService.createUser("Test User", "test@example.com", "1234567890");
            
            // 2. 查询所有用户
            List<User> allUsers = userService.getAllUsers();
            
            // 3. 搜索用户
            List<User> searchResults = userService.searchUsers("Test");
            
            response.put("success", true);
            response.put("createdUser", user);
            response.put("totalUsers", allUsers.size());
            response.put("searchResults", searchResults.size());
            response.put("operations", "HTTP Server + Database (CREATE, SELECT, SEARCH)");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 文件操作测试 - HTTP Server + File Operations
     */
    @GetMapping("/file")
    public ResponseEntity<Map<String, Object>> fileTest() {
        System.out.println("=== TestController.fileTest() - HTTP Server + File Operations ===");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 写入日志文件
            Map<String, Object> writeResult = fileOperationService.writeLogFile("Test log message from API");
            
            // 2. 读取日志文件
            Map<String, Object> readResult = fileOperationService.readLogFile();
            
            // 3. 批量文件操作
            Map<String, Object> batchResult = fileOperationService.batchFileOperations();
            
            response.put("success", true);
            response.put("writeResult", writeResult);
            response.put("readResult", readResult);
            response.put("batchResult", batchResult);
            response.put("operations", "HTTP Server + File Operations (WRITE, READ, BATCH)");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * HTTP客户端测试 - HTTP Server + HTTP Client
     */
    @GetMapping("/http")
    public ResponseEntity<Map<String, Object>> httpClientTest() {
        System.out.println("=== TestController.httpClientTest() - HTTP Server + HTTP Client ===");
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 单个API调用
            Map<String, Object> singleCall = httpClientService.callExternalApi("https://httpbin.org/get");
            
            // 2. 多个API调用
            Map<String, Object> multipleCalls = httpClientService.callMultipleApis();
            
            response.put("success", true);
            response.put("singleCall", singleCall);
            response.put("multipleCalls", multipleCalls);
            response.put("operations", "HTTP Server + HTTP Client (GET requests)");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 复杂操作测试 - 所有操作类型的组合
     * 这是测试阶段3链路跟踪的关键端点
     */
    @GetMapping("/complex")
    public ResponseEntity<Map<String, Object>> complexTest() {
        System.out.println("=== TestController.complexTest() - ALL OPERATIONS COMBINED ===");
        System.out.println("This endpoint tests: HTTP Server + Database + File Operations + HTTP Client");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> results = new HashMap<>();
        
        try {
            // 1. HTTP Server操作（当前请求处理）
            System.out.println("Step 1: HTTP Server - Processing incoming request");
            
            // 2. 数据库操作
            System.out.println("Step 2: Database - Creating and querying users");
            // 使用时间戳确保邮箱唯一性
            String timestamp = String.valueOf(System.currentTimeMillis());
            String uniqueEmail = "complex-" + timestamp + "@test.com";
            User user = userService.createUser("Complex Test User " + timestamp, uniqueEmail, "9876543210");
            List<User> users = userService.getAllUsers();
            Map<String, Object> dbResult = new HashMap<>();
            dbResult.put("createdUser", user);
            dbResult.put("totalUsers", users.size());
            results.put("database", dbResult);
            
            // 3. 文件操作
            System.out.println("Step 3: File Operations - Writing logs and data");
            Map<String, Object> logResult = fileOperationService.writeLogFile("Complex operation executed");
            Map<String, Object> dataResult = fileOperationService.saveUserDataToFile(user.toString());
            Map<String, Object> fileResult = new HashMap<>();
            fileResult.put("logResult", logResult);
            fileResult.put("dataResult", dataResult);
            results.put("fileOperations", fileResult);
            
            // 4. HTTP客户端操作
            System.out.println("Step 4: HTTP Client - Calling external APIs");
            Map<String, Object> httpResult = httpClientService.complexHttpChain();
            results.put("httpClient", httpResult);
            
            // 5. 再次数据库操作（更新用户）
            System.out.println("Step 5: Database - Updating user information");
            User updatedUser = userService.updateUser(user.getId(), "Updated Complex User", null, null);
            results.put("databaseUpdate", updatedUser);
            
            // 6. 最终文件操作（记录完成日志）
            System.out.println("Step 6: File Operations - Recording completion");
            Map<String, Object> completionLog = fileOperationService.writeLogFile("Complex operation completed successfully");
            results.put("completionLog", completionLog);
            
            response.put("success", true);
            response.put("message", "Complex operation completed - check trace-flow output!");
            response.put("operations", "HTTP Server + Database + File Operations + HTTP Client (Full Chain)");
            response.put("results", results);
            response.put("traceInfo", "This request should show a complete trace tree with all operation types");
            
        } catch (Exception e) {
            System.err.println("Complex operation failed: " + e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("operations", "HTTP Server + Partial operations (failed)");
        }
        
        System.out.println("=== TestController.complexTest() - COMPLETED ===");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 用户管理端点 - 用于额外的数据库操作测试
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        System.out.println("=== TestController.getAllUsers() - Database query ===");
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody Map<String, String> userData) {
        System.out.println("=== TestController.createUser() - Database insert ===");

        String name = userData.get("name");
        String email = userData.get("email");
        String phone = userData.get("phone");

        User user = userService.createUser(name, email, phone);
        return ResponseEntity.ok(user);
    }

    /**
     * 清理数据库 - 方便重复测试
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup() {
        System.out.println("=== TestController.cleanup() - Cleaning database ===");

        Map<String, Object> response = new HashMap<>();

        try {
            // 获取清理前的用户数量
            List<User> allUsers = userService.getAllUsers();
            int beforeCount = allUsers.size();

            // 删除所有用户
            for (User user : allUsers) {
                userService.deleteUser(user.getId());
            }

            // 验证清理结果
            List<User> remainingUsers = userService.getAllUsers();
            int afterCount = remainingUsers.size();

            response.put("success", true);
            response.put("message", "Database cleaned successfully");
            response.put("deletedUsers", beforeCount);
            response.put("remainingUsers", afterCount);
            response.put("operations", "HTTP Server + Database (DELETE operations)");

            System.out.println("Database cleanup completed: " + beforeCount + " users deleted");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
