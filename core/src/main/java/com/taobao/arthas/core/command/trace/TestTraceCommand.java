package com.taobao.arthas.core.command.trace;

import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 测试trace功能的简单命令
 */
@Name("test-trace")
@Summary("Test trace functionality")
@Description("Test command to verify trace interceptors are working")
public class TestTraceCommand extends AnnotatedCommand {

    @Override
    public void process(CommandProcess process) {
        process.write("=== Testing Trace Functionality ===\n");
        
        // 测试拦截器状态
        testInterceptorStatus(process);
        
        // 测试文件操作
        testFileOperations(process);
        
        process.write("=== Test Completed ===\n");
        process.end();
    }
    
    private void testInterceptorStatus(CommandProcess process) {
        process.write("\n1. Testing Interceptor Status:\n");
        
        InterceptorManager manager = InterceptorManager.getInstance();
        process.write("   Interceptor count: " + manager.getInterceptorCount() + "\n");
        process.write("   Is initialized: " + manager.isInitialized() + "\n");
        
        for (MethodInterceptor interceptor : manager.getInterceptors()) {
            process.write("   - " + interceptor.getName() + " (enabled: " + interceptor.isEnabled() + ")\n");
        }
    }
    
    private void testFileOperations(CommandProcess process) {
        process.write("\n2. Testing File Operations:\n");
        
        try {
            // 创建临时文件
            java.io.File tempFile = new java.io.File("arthas-test.tmp");
            process.write("   Creating temp file: " + tempFile.getName() + "\n");
            
            // 写入文件
            process.write("   Writing to file...\n");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write("Hello Arthas!".getBytes());
            fos.close();
            
            // 读取文件
            process.write("   Reading from file...\n");
            java.io.FileInputStream fis = new java.io.FileInputStream(tempFile);
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            fis.close();
            
            process.write("   Read " + bytesRead + " bytes\n");
            
            // 删除文件
            tempFile.delete();
            process.write("   File operations completed\n");
            
        } catch (Exception e) {
            process.write("   Error during file operations: " + e.getMessage() + "\n");
        }
    }
}
