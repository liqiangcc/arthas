# HTTP测试指南

## 文件说明

### 1. `test-endpoints.http`
- **用途**：基础功能测试
- **包含**：所有端点的基本测试用例
- **适用场景**：验证应用功能是否正常

### 2. `stage3-trace-test.http` ⭐
- **用途**：阶段3链路跟踪专项测试
- **包含**：多探针协同工作的测试用例
- **适用场景**：验证Arthas trace-flow功能

### 3. `performance-test.http`
- **用途**：性能和并发测试
- **包含**：快速连续请求、混合操作测试
- **适用场景**：测试链路跟踪的稳定性

## 在IDEA中使用HTTP文件

### 步骤1：打开HTTP文件
1. 在IDEA中打开 `test-endpoints.http` 或其他HTTP文件
2. IDEA会自动识别HTTP请求格式

### 步骤2：执行请求
1. 点击每个请求左侧的绿色箭头 ▶️
2. 或者使用快捷键 `Ctrl+Enter`
3. 查看响应结果在底部面板

### 步骤3：修改请求
- 可以直接在文件中修改URL、参数、请求体
- 支持变量和环境配置

## 阶段3测试流程

### 准备工作
1. 启动Spring Boot应用：
   ```bash
   cd test-spring-app
   mvnw.cmd spring-boot:run
   ```

2. 启动Arthas：
   ```bash
   java -jar arthas-boot.jar
   # 选择Spring Boot进程
   ```

3. 配置Arthas：
   ```bash
   [arthas@pid]$ options unsafe true
   [arthas@pid]$ trace-flow -n 1 -v
   ```

### 执行测试
1. 在IDEA中打开 `stage3-trace-test.http`
2. 执行 "测试1：完整链路跟踪" 请求
3. 观察Arthas控制台输出

### 预期结果
应该看到包含以下节点类型的链路跟踪：
- `[HTTP_SERVER]` - HTTP请求处理
- `[DATABASE]` - 数据库操作
- `[FILE_OPERATION]` - 文件读写
- `[HTTP_CLIENT]` - HTTP客户端调用

所有操作应该有相同的Trace ID。

## 测试端点说明

### 基础端点
- `/api/test/simple` - 仅HTTP Server操作
- `/api/test/database` - HTTP Server + Database
- `/api/test/file` - HTTP Server + File Operations
- `/api/test/http` - HTTP Server + HTTP Client

### 核心端点
- `/api/test/complex` ⭐ - 所有操作类型组合（阶段3关键测试）

### 用户管理
- `GET /api/users` - 查询用户
- `POST /api/users` - 创建用户

## 故障排除

### 应用无法启动
- 检查8080端口是否被占用
- 确保Java 8+已安装
- 查看控制台错误信息

### HTTP请求失败
- 确认应用已启动（访问 http://localhost:8080/api/test/simple）
- 检查请求格式是否正确
- 查看应用日志

### Arthas无法连接
- 确认应用进程正在运行
- 检查进程ID是否正确
- 尝试重新启动Arthas

### 链路跟踪无输出
- 确认已启用unsafe模式：`options unsafe true`
- 检查trace-flow命令是否正确启动
- 尝试使用简单的tf命令：`tf java.io.FileOutputStream write`

## 成功标准

阶段3测试成功的标志：
1. ✅ 能看到多种节点类型的拦截
2. ✅ 相同请求中所有操作有相同的Trace ID
3. ✅ 不同请求有不同的Trace ID
4. ✅ 输出格式清晰，包含方法、线程、参数信息
5. ✅ 配置驱动的属性提取正常工作
