# QLExpress4 可视化编排 Web 应用

基于 QLExpress4 规则引擎的可视化编排 Web 应用，提供图形化的规则编排、脚本执行和调试功能。

## 项目简介

本项目是 QLExpress4 可视化编排设计方案的实现，旨在提供：

- **可视化编排**：通过拖拽节点和连线实现规则流程编排
- **双向转换**：支持可视化 JSON 与 QL 脚本的双向转换
- **元数据服务**：提供操作符、函数、关键字等元数据查询
- **脚本执行**：支持脚本验证、执行和表达式追踪
- **调试支持**：集成 TracePointTree 执行追��机制

## 技术栈

### 后端
- Java 8+
- Spring Boot 2.7.x
- QLExpress4 4.0.7

### 前端（规划中）
- React 18+
- React Flow (@xyflow/react)
- Monaco Editor
- TypeScript

## 快速开始

### 环境要求
- JDK 8+
- Maven 3.6+

### 构建运行

```bash
# 克隆项目
git clone <repository-url>

# 进入项目目录
cd qlexpress_web

# 编译项目
mvn clean install

# 运行应用
mvn spring-boot:run
```

### Maven 依赖

```xml
<dependency>
    <groupId>com.ql.qlexpress</groupId>
    <artifactId>qlexpress-web</artifactId>
    <version>${last.version}</version>
</dependency>
```

### 配置说明

```yaml
# application-dev.yml
qlexpress:
  script:
    timeout: 30000          # 执行超时时间（毫秒）
    cache-enabled: true     # 是否启用缓存
    trace-enabled: true     # 是否启用表达式追踪
    debug: true             # 是否开启调试模式
```

## 项目结构

```
qlexpress_web/
├── src/main/java/com/ql/qlexpress/web/
│   ├── Application.java           # 启动类
│   ├── controller/                # REST API 控制器
│   ├── service/                   # 业务服务层
│   ├── model/                     # 数据模型
│   └── config/                    # 配置类
├── src/main/resources/
│   ├── application.yml            # 主配置文件
│   ├── application-dev.yml        # 开发环境配置
│   └── application-test.yml       # 测试环境配置
└── pom.xml                        # Maven 配置
```

## API 接口（规划中）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/transpile/to-ql` | POST | JSON 转 QL 脚本 |
| `/api/v1/transpile/to-json` | POST | QL 脚本转 JSON |
| `/api/v1/metadata/operators` | GET | 获取操作符列表 |
| `/api/v1/metadata/functions` | GET | 获取函数列表 |
| `/api/v1/execute` | POST | 执行 QL 脚本 |
| `/api/v1/validate/script` | POST | 验证脚本语法 |

## 代码规范

- 后端使用同一份代码格式化模板 ali-code-style.xml，Eclipse 直接导入使用，IDEA 使用 Eclipse Code Formatter 插件配置 xml 后使用
- 后端代码非特殊情况遵守 P3C 插件规范
- 注释要尽可能完整明晰，提交的代码必须要先格式化

## 许可证

Apache License 2.0

## 相关链接

- [QLExpress4 GitHub](https://github.com/alibaba/QLExpress)
- [QLExpress4 文档](https://github.com/alibaba/QLExpress/wiki)
