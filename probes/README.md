# 外部探针配置文件目录

这个目录用于存放外部探针配置文件。

## 目录位置优先级

Arthas会按以下优先级查找外部探针配置目录：

1. **Arthas Bootstrap路径** - 从`ArthasBootstrap.arthasHome()`获取的路径下的`probes/`目录
2. **系统属性** - `${arthas.home}/probes/`
3. **当前目录** - `./probes/`（降级选项）

## 使用方法

1. **自动检测**（推荐）：
   - Arthas会自动从Bootstrap中获取arthasHome路径
   - 在该路径下创建`probes/`目录
   - 例如：如果Arthas安装在`/opt/arthas`，则探针目录为`/opt/arthas/probes/`

2. **手动指定**：
   - 通过系统属性：`-Darthas.home=/path/to/arthas`
   - 在指定路径下创建`probes/`目录

3. **使用步骤**：
   - 将探针配置文件（.json格式）放在probes目录下
   - 启动Arthas时，系统会自动扫描并加载该目录下的所有.json文件
   - 使用`tf --reload-probes`命令可以重新加载配置文件
   - 配置文件会在运行时动态加载，无需重新编译JAR包

## 配置文件格式

每个配置文件都应该是有效的JSON格式，包含以下结构：

```json
{
  "name": "探针名称",
  "description": "探针描述",
  "enabled": true,
  "metrics": [
    {
      "name": "指标名称",
      "description": "指标描述",
      "targets": [
        {
          "className": "目标类名",
          "methods": ["目标方法列表"]
        }
      ],
      "source": "数据源表达式",
      "type": "数据类型",
      "capturePoint": "采集时机"
    }
  ],
  "output": {
    "type": "输出类型",
    "template": "输出模板"
  }
}
```

## 示例文件

- `http-server-probe.json` - HTTP服务器探针
- `database-probe.json` - 数据库操作探针

## 注意事项

- 文件名必须以`.json`结尾
- JSON格式必须正确
- 配置文件解析失败不会影响其他文件的加载
- 修改配置文件后需要重启Arthas才能生效
