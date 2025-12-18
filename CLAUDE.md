# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

QLExpress4 可视化编排 Web 应用 - 基于 QLExpress4 规则引擎的可视化编排系统，提供图形化规则编排、脚本执行和调试功能。集成 LiteFlow 实现组件化流程编排。

## Tech Stack

- **Backend**: Java 8+, Spring Boot 2.7.x, QLExpress4 4.0.7, LiteFlow 2.15.2
- **Frontend** (规划中): React 18+, React Flow (@xyflow/react), Monaco Editor, TypeScript

## Common Commands

```bash
# 编译项目
mvn clean install

# 运行应用
mvn spring-boot:run

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=MetadataServiceTest

# 跳过测试编译
mvn clean install -DskipTests
```

## Architecture

### 后端核心模块

```
src/main/java/com/ql/qlexpress/web/
├── controller/              # REST API 控制器
│   ├── ParseController      # QL脚本 → 可视化流程 (/api/v1/parse)
│   ├── TranspileController  # 可视化流程 → QL脚本 (/api/v1/transpile)
│   ├── ExecuteController    # 脚本/流程执行 (/api/v1/execute)
│   ├── ValidateController   # 语法/流程验证 (/api/v1/validate)
│   └── MetadataController   # 元数据查询 (/api/v1/metadata)
├── service/                 # 业务服务层
├── core/transpiler/         # 核心转译引擎 (JSON ⇄ QL脚本)
├── model/
│   ├── visual/              # 可视化模型 (VisualNode, VisualEdge, FlowSchema)
│   ├── dto/                 # 数据传输对象
│   └── enums/               # 枚举定义
└── exception/               # 异常处理
```

### 核心数据流

```
可视化编排 (JSON) ←→ TranspileService ←→ QL脚本
                        ↓
                  FlowGraph (DAG)
                        ↓
                  QLExpress4 执行
```

### API 接口

| 端点 | 方法 | 功能 |
|------|------|------|
| `/api/v1/parse/to-visual` | POST | QL脚本 → 可视化流程 |
| `/api/v1/transpile/to-ql` | POST | 可视化流程 → QL脚本 |
| `/api/v1/execute/script` | POST | 执行QL脚本 |
| `/api/v1/execute/flow` | POST | 执行可视化流程 |
| `/api/v1/validate/script` | POST | 验证脚本语法 |
| `/api/v1/validate/flow` | POST | 验证流程定义 |
| `/api/v1/metadata/*` | GET | 获取操作符/函数/关键字元数据 |

### LiteFlow 集成架构

项目通过 LiteFlow 实现组件化流程编排:

```
前端层 (规划中)
├── 组件设计器: QLExpress 脚本组件定义
└── 流程编排器: 基于 liteflow-editor-client 的可视化编排

后端层
├── 组件管理: QLComponent CRUD, 脚本验证
├── 编排转换: CmpProperty (JSON) ⇄ LiteFlow EL 表达式
└── 执行服务: LiteFlowDynamicService 动态执行

引擎层
├── LiteFlow: 流程编排、节点调度、条件分支
└── QLExpress: 脚本执行、规则计算
```

### 组件类型

| 类型 | LiteFlow节点 | 用途 |
|------|-------------|------|
| BOOLEAN | boolean_script | IF条件判断, 返回 true/false |
| SWITCH | switch_script | 多路分支路由, 返回分支标识 |
| SCRIPT | script | 执行业务逻辑, 通过 FlowContext 传递数据 |

## Configuration

```yaml
# application.yml
liteflow:
  print-execution-log: true
  script-enable: true
  script-language: qlexpress
```

## Code Style

- 使用 ali-code-style.xml 格式化模板
- 遵守 P3C 插件规范
- 提交代码前必须格式化
