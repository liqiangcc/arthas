package demo;

import java.sql.*;
import java.util.concurrent.TimeUnit;

/**
 * 简单的测试应用 - 用于验证trace-flow功能
 */
public class TestApp {
    
    public static void main(String[] args) {
        System.out.println("=== Arthas trace-flow 测试应用 ===");
        System.out.println("这个应用会模拟数据库操作来测试trace-flow功能");
        
        TestApp app = new TestApp();
        
        try {
            // 持续运行，等待Arthas连接
            while (true) {
                createRealObjects();
                System.out.println("\n[" + new java.util.Date() + "] 执行测试操作...");
                
                // 模拟数据库操作
                app.simulateDatabaseOperations();
                
                // 模拟HTTP操作
                app.simulateHttpOperations();
                
                // 模拟文件操作
                app.simulateFileOperations();
                
                System.out.println("等待1秒后继续...");
                TimeUnit.SECONDS.sleep(1);
            }
            
        } catch (InterruptedException e) {
            System.out.println("应用被中断");
        } catch (Exception e) {
            System.err.println("应用运行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 模拟数据库操作
     */
    private void simulateDatabaseOperations() {
        try {
            System.out.println("  -> 模拟数据库操作");
            
            // 模拟创建连接
            simulateMethod("java.sql.DriverManager.getConnection", 50);
            
            // 模拟准备语句
            simulateMethod("java.sql.Connection.prepareStatement", 10);
            
            // 模拟执行查询
            simulateMethod("java.sql.PreparedStatement.executeQuery", 150);
            
            // 模拟执行更新
            simulateMethod("java.sql.PreparedStatement.executeUpdate", 80);
            
            System.out.println("     数据库操作完成");
            
        } catch (Exception e) {
            System.err.println("数据库操作模拟失败: " + e.getMessage());
        }
    }
    
    /**
     * 模拟HTTP操作
     */
    private void simulateHttpOperations() {
        try {
            System.out.println("  -> 模拟HTTP操作");
            
            // 模拟HTTP请求处理
            simulateMethod("javax.servlet.http.HttpServlet.service", 200);
            
            // 模拟HTTP客户端请求
            simulateMethod("java.net.HttpURLConnection.connect", 300);
            
            System.out.println("     HTTP操作完成");
            
        } catch (Exception e) {
            System.err.println("HTTP操作模拟失败: " + e.getMessage());
        }
    }
    
    /**
     * 模拟文件操作
     */
    private void simulateFileOperations() {
        try {
            System.out.println("  -> 模拟文件操作");
            
            // 模拟文件读取
            simulateMethod("java.io.FileInputStream.read", 20);
            
            // 模拟文件写入
            simulateMethod("java.io.FileOutputStream.write", 30);
            
            System.out.println("     文件操作完成");
            
        } catch (Exception e) {
            System.err.println("文件操作模拟失败: " + e.getMessage());
        }
    }
    
    /**
     * 模拟方法调用（包含执行时间）
     */
    private void simulateMethod(String methodName, long executionTimeMs) {
        try {
            System.out.println("    执行: " + methodName + " (预计耗时: " + executionTimeMs + "ms)");
            
            // 模拟方法执行时间
            Thread.sleep(executionTimeMs);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 创建一些真实的对象来触发类加载
     */
    private static void createRealObjects() {
        try {
            // 创建一些真实的对象，这样Arthas可以拦截到真实的方法调用
            
            // 文件操作
            java.io.File tempFile = new java.io.File("temp.txt");
            if (!tempFile.exists()) {
                tempFile.createNewFile();
            }
            
            java.io.FileInputStream fis = new java.io.FileInputStream(tempFile);
            byte[] buffer = new byte[1024];
            fis.read(buffer);
            fis.close();
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write("test data".getBytes());
            fos.close();
            
            tempFile.delete();
            
        } catch (Exception e) {
            // 忽略错误
        }
    }
}
