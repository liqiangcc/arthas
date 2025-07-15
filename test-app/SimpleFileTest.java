import java.io.*;

/**
 * 简单的文件操作测试 - 用于验证Arthas拦截
 */
public class SimpleFileTest {
    
    public static void main(String[] args) {
        System.out.println("=== 简单文件操作测试 ===");
        
        SimpleFileTest test = new SimpleFileTest();
        
        // 持续执行文件操作
        for (int i = 0; i < 100; i++) {
            System.out.println("\n第 " + (i + 1) + " 次文件操作:");
            test.performFileOperations();
            
            try {
                Thread.sleep(5000); // 每5秒执行一次
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    public void performFileOperations() {
        String fileName = "test-" + System.currentTimeMillis() + ".txt";
        
        try {
            // 1. 写入文件
            System.out.println("  写入文件: " + fileName);
            FileOutputStream fos = new FileOutputStream(fileName);
            String content = "Hello Arthas! Time: " + new java.util.Date();
            fos.write(content.getBytes());
            fos.close();
            System.out.println("  ✓ 写入完成");
            
            // 2. 读取文件
            System.out.println("  读取文件: " + fileName);
            FileInputStream fis = new FileInputStream(fileName);
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            fis.close();
            System.out.println("  ✓ 读取完成，读取了 " + bytesRead + " 字节");
            
            // 3. 删除文件
            File file = new File(fileName);
            if (file.delete()) {
                System.out.println("  ✓ 文件删除成功");
            }
            
        } catch (IOException e) {
            System.err.println("  ✗ 文件操作失败: " + e.getMessage());
        }
    }
}
