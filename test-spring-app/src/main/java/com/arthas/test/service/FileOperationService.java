package com.arthas.test.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件操作服务 - 用于测试文件操作的链路跟踪
 */
@Service
public class FileOperationService {
    
    private static final String LOG_DIR = "logs";
    private static final String DATA_DIR = "data";
    
    /**
     * 写入日志文件
     */
    public Map<String, Object> writeLogFile(String message) {
        System.out.println("FileOperationService.writeLogFile() - Writing log message");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 确保日志目录存在
            createDirectoryIfNotExists(LOG_DIR);
            
            // 2. 生成日志文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = LOG_DIR + "/app-" + timestamp + ".log";
            
            // 3. 写入日志
            String logEntry = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) 
                            + " [INFO] " + message + "\n";
            
            FileOutputStream fos = new FileOutputStream(fileName, true); // 追加模式
            fos.write(logEntry.getBytes());
            fos.close();
            
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("message", "Log written successfully");
            
            System.out.println("FileOperationService.writeLogFile() - Log written to: " + fileName);
            
        } catch (IOException e) {
            System.err.println("FileOperationService.writeLogFile() - Error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 读取日志文件
     */
    public Map<String, Object> readLogFile() {
        System.out.println("FileOperationService.readLogFile() - Reading log file");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 生成今天的日志文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = LOG_DIR + "/app-" + timestamp + ".log";
            
            // 2. 检查文件是否存在
            File logFile = new File(fileName);
            if (!logFile.exists()) {
                result.put("success", false);
                result.put("message", "Log file not found: " + fileName);
                return result;
            }
            
            // 3. 读取文件内容
            FileInputStream fis = new FileInputStream(fileName);
            byte[] buffer = new byte[1024];
            StringBuilder content = new StringBuilder();
            
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead));
            }
            fis.close();
            
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("content", content.toString());
            result.put("fileSize", logFile.length());
            
            System.out.println("FileOperationService.readLogFile() - Read " + logFile.length() + " bytes");
            
        } catch (IOException e) {
            System.err.println("FileOperationService.readLogFile() - Error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 保存用户数据到文件
     */
    public Map<String, Object> saveUserDataToFile(String userData) {
        System.out.println("FileOperationService.saveUserDataToFile() - Saving user data");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 确保数据目录存在
            createDirectoryIfNotExists(DATA_DIR);
            
            // 2. 生成数据文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String fileName = DATA_DIR + "/user-data-" + timestamp + ".json";
            
            // 3. 写入数据
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(userData.getBytes());
            fos.close();
            
            result.put("success", true);
            result.put("fileName", fileName);
            result.put("dataSize", userData.length());
            
            System.out.println("FileOperationService.saveUserDataToFile() - Data saved to: " + fileName);
            
        } catch (IOException e) {
            System.err.println("FileOperationService.saveUserDataToFile() - Error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 批量文件操作
     */
    public Map<String, Object> batchFileOperations() {
        System.out.println("FileOperationService.batchFileOperations() - Starting batch operations");
        
        Map<String, Object> result = new HashMap<>();
        List<String> operations = new ArrayList<>();
        
        try {
            // 确保数据目录存在
            createDirectoryIfNotExists(DATA_DIR);
            
            // 1. 写入多个测试文件
            for (int i = 1; i <= 3; i++) {
                String fileName = DATA_DIR + "/test-file-" + i + ".txt";
                String content = "This is test file " + i + " created at " + LocalDateTime.now();
                
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(content.getBytes());
                fos.close();
                
                operations.add("Created: " + fileName);
            }
            
            // 2. 读取所有测试文件
            for (int i = 1; i <= 3; i++) {
                String fileName = DATA_DIR + "/test-file-" + i + ".txt";
                
                FileInputStream fis = new FileInputStream(fileName);
                byte[] buffer = new byte[1024];
                int bytesRead = fis.read(buffer);
                fis.close();
                
                operations.add("Read: " + fileName + " (" + bytesRead + " bytes)");
            }
            
            // 3. 删除测试文件
            for (int i = 1; i <= 3; i++) {
                String fileName = DATA_DIR + "/test-file-" + i + ".txt";
                File file = new File(fileName);
                if (file.delete()) {
                    operations.add("Deleted: " + fileName);
                }
            }
            
            result.put("success", true);
            result.put("operations", operations);
            result.put("totalOperations", operations.size());
            
        } catch (IOException e) {
            System.err.println("FileOperationService.batchFileOperations() - Error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("operations", operations);
        }
        
        System.out.println("FileOperationService.batchFileOperations() - Completed " + operations.size() + " operations");
        return result;
    }
    
    /**
     * 创建目录（如果不存在）
     */
    private void createDirectoryIfNotExists(String dirName) throws IOException {
        Path dirPath = Paths.get(dirName);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            System.out.println("Created directory: " + dirName);
        }
    }
}
