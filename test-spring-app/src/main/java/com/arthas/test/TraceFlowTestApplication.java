package com.arthas.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Arthas trace-flow 测试应用
 * 用于验证阶段3的链路跟踪功能
 */
@SpringBootApplication
public class TraceFlowTestApplication {

    public static void main(String[] args) {
        System.out.println(repeat("=", 60));
        System.out.println("Arthas Trace Flow Test Application Starting...");
        System.out.println(repeat("=", 60));
        System.out.println("This application provides endpoints to test:");
        System.out.println("1. HTTP Server requests");
        System.out.println("2. Database operations");
        System.out.println("3. HTTP Client calls");
        System.out.println("4. File operations");
        System.out.println("5. Combined operations in a single request");
        System.out.println(repeat("=", 60));

        SpringApplication.run(TraceFlowTestApplication.class, args);

        System.out.println("Application started successfully!");
        System.out.println("Test endpoints:");
        System.out.println("- GET  /api/test/simple     - Simple response");
        System.out.println("- GET  /api/test/database   - Database operations");
        System.out.println("- GET  /api/test/file       - File operations");
        System.out.println("- GET  /api/test/http       - HTTP client calls");
        System.out.println("- GET  /api/test/complex    - All operations combined");
        System.out.println("- GET  /api/users           - User CRUD operations");
        System.out.println("- POST /api/users           - Create user");
        System.out.println(repeat("=", 60));
    }
    
    /**
     * Java 8兼容的字符串重复方法
     */
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
