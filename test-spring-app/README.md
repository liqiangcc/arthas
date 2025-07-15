# Arthas Trace Flow Test Application

这是一个用于测试Arthas trace-flow功能的Spring Boot应用程序。

## 快速启动

### 方法1：使用Maven Wrapper（推荐）
```bash
# Windows
run-app.bat

# 或者手动执行
mvnw.cmd spring-boot:run
```

### 方法2：使用系统Maven
```bash
mvn spring-boot:run
```

## 测试端点

应用启动后，访问以下端点进行测试：

### 基础测试
- `GET /api/test/simple` - 简单HTTP响应（仅HTTP Server）
- `GET /api/test/database` - 数据库操作测试（HTTP Server + Database）
- `GET /api/test/file` - 文件操作测试（HTTP Server + File Operations）
- `GET /api/test/http` - HTTP客户端测试（HTTP Server + HTTP Client）

### 关键测试（阶段3验证）
- `GET /api/test/complex` - **复杂链路测试（所有操作类型）**

这个端点会依次执行：
1. HTTP Server - 处理请求
2. Database - 创建和查询用户
3. File Operations - 写入日志和数据文件
4. HTTP Client - 调用外部API
5. Database - 更新用户信息
6. File Operations - 记录完成日志

### 用户管理
- `GET /api/users` - 获取所有用户
- `POST /api/users` - 创建用户

## Arthas测试步骤

1. 启动应用：`run-app.bat`
2. 启动Arthas：`java -jar arthas-boot.jar`
3. 连接到应用进程
4. 启用unsafe模式：`options unsafe true`
5. 启动链路跟踪：`trace-flow -n 1 -v`
6. 访问测试端点：`http://localhost:8080/api/test/complex`

## 预期结果

应该看到包含以下节点类型的链路跟踪：
- `[HTTP_SERVER]` - HTTP请求处理
- `[DATABASE]` - 数据库操作
- `[FILE_OPERATION]` - 文件读写
- `[HTTP_CLIENT]` - HTTP客户端调用

所有操作应该有相同的Trace ID，证明链路跟踪正常工作。

## 数据库控制台

访问 `http://localhost:8080/h2-console` 查看H2数据库内容：
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (空)

## 故障排除

1. 如果Maven Wrapper下载失败，请检查网络连接
2. 如果端口8080被占用，修改 `application.yml` 中的端口配置
3. 如果Arthas连接失败，确保应用正在运行并检查进程ID
