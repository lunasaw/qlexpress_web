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

---

## 前后端交互协议规范

### 1. 通用响应格式

所有 API 响应都使用 `ApiResponse<T>` 包装：

```typescript
interface ApiResponse<T> {
  success: boolean;      // 请求是否成功
  code: number;          // 状态码 (200=成功, 其他=错误)
  message: string;       // 响应消息
  data: T;               // 响应数据
  timestamp: number;     // 时间戳
}
```

### 2. 分页数据格式

#### 标准分页格式 `PageData<T>`

```typescript
interface PageData<T> {
  content: T[];          // 数据列表
  total: number;         // 总记录数
  page: number;          // 当前页码 (从1开始)
  size: number;          // 每页大小
  totalPages?: number;   // 总页数
}
```

#### 流程列表特殊格式 `FlowListData`

```typescript
interface FlowListData {
  flows: FlowDesign[];                    // 流程列表 (注意: 是 flows 不是 content)
  categories: string[];                   // 分类列表
  categoryStats: Record<string, number>;  // 分类统计
  total: number;                          // 总记录数
  page?: number;                          // 当前页码
  size?: number;                          // 每页大小
}
```

#### 组件列表格式 `ComponentListData<T>`

```typescript
interface ComponentListData<T> {
  content: T[];          // 组件列表
  total: number;         // 总记录数
  categories: string[];  // 分类列表
}
```

### 3. 枚举定义

#### 组件类型 `ComponentType`

```typescript
enum ComponentType {
  SCRIPT = 'SCRIPT',     // 脚本组件 - 执行业务逻辑
  BOOLEAN = 'BOOLEAN',   // 布尔组件 - IF条件判断, 返回 true/false
  SWITCH = 'SWITCH',     // 选择组件 - 多路分支路由, 返回分支标识
}
```

#### 部署状态 `DeployStatus`

```typescript
enum DeployStatus {
  NOT_DEPLOYED = 'NOT_DEPLOYED',  // 未部署
  DEPLOYED = 'DEPLOYED',          // 已部署
  MODIFIED = 'MODIFIED',          // 已修改 (部署后有变更)
  FAILED = 'FAILED',              // 部署失败
}
```

#### 流程节点类型 `NodeTypeEnum`

```typescript
enum NodeTypeEnum {
  // 串行/并行
  THEN = 'THEN',         // 串行执行
  WHEN = 'WHEN',         // 并行执行

  // 条件分支
  IF = 'IF',             // IF-ELSE 分支
  SWITCH = 'SWITCH',     // 多路选择分支

  // 循环结构
  FOR = 'FOR',           // FOR 循环
  WHILE = 'WHILE',       // WHILE 循环
  ITERATOR = 'ITERATOR', // 迭代器循环

  // 特殊节点
  CATCH = 'CATCH',       // 异常捕获
  AND = 'AND',           // 与逻辑
  OR = 'OR',             // 或逻辑
  NOT = 'NOT',           // 非逻辑

  // 组件引用
  COMPONENT = 'COMPONENT', // 组件节点 (引用已注册的组件)
}
```

#### 条件类型 `ConditionTypeEnum`

```typescript
enum ConditionTypeEnum {
  TYPE_THEN = 'TYPE_THEN',
  TYPE_WHEN = 'TYPE_WHEN',
  TYPE_SWITCH = 'TYPE_SWITCH',
  TYPE_IF = 'TYPE_IF',
  TYPE_ELSE = 'TYPE_ELSE',
  TYPE_ELIF = 'TYPE_ELIF',
  TYPE_FOR = 'TYPE_FOR',
  TYPE_WHILE = 'TYPE_WHILE',
  TYPE_ITERATOR = 'TYPE_ITERATOR',
  TYPE_CATCH = 'TYPE_CATCH',
  TYPE_AND = 'TYPE_AND',
  TYPE_OR = 'TYPE_OR',
  TYPE_NOT = 'TYPE_NOT',
}
```

### 4. 组件数据结构

#### 组件定义 `QLComponent`

```typescript
interface QLComponent {
  componentId: string;          // 组件ID (唯一标识)
  componentName: string;        // 组件名称
  description?: string;         // 描述
  category?: string;            // 分类
  componentType: ComponentType; // 组件类型
  language: string;             // 脚本语言 (固定为 'qlexpress')
  script: string;               // QLExpress 脚本内容
  inputs?: ParameterDef[];      // 输入参数定义
  outputs?: ParameterDef[];     // 输出参数定义
  enabled?: boolean;            // 是否启用
  version?: string;             // 版本号
  createTime?: string;          // 创建时间
  updateTime?: string;          // 更新时间
}

interface ParameterDef {
  name: string;                 // 参数名
  type: string;                 // 参数类型 (String, Integer, Boolean, Object 等)
  description?: string;         // 参数描述
  required?: boolean;           // 是否必填
  defaultValue?: unknown;       // 默认值
}
```

### 5. 流程数据结构

#### 流程定义 `FlowDesign`

```typescript
interface FlowDesign {
  flowId: string;               // 流程ID (唯一标识)
  flowName: string;             // 流程名称
  description?: string;         // 描述
  category?: string;            // 分类
  version?: string;             // 版本号
  enabled?: boolean;            // 是否启用
  deployStatus?: DeployStatus;  // 部署状态
  root?: CmpProperty;           // 流程编排树 (根节点)
  createTime?: string;          // 创建时间
  updateTime?: string;          // 更新时间
}
```

#### 流程编排树 `CmpProperty`

流程编排使用树形结构 `CmpProperty` 定义，每个节点表示一个编排操作或组件引用：

```typescript
interface CmpProperty {
  type: NodeTypeEnum;           // 节点类型
  componentRef?: string;        // 组件引用ID (type=COMPONENT 时必填)
  condition?: CmpProperty;      // 条件节点 (type=IF/SWITCH 时使用)
  children?: CmpProperty[];     // 子节点列表

  // 扩展属性 (根据节点类型不同)
  id?: string;                  // 节点标识
  tag?: string;                 // 标签
  parallel?: boolean;           // 是否并行 (WHEN 节点)
  any?: boolean;                // 任一完成 (WHEN 节点)
  ignoreError?: boolean;        // 忽略错误
  must?: boolean;               // 必须执行
  retry?: number;               // 重试次数
  timeout?: number;             // 超时时间 (毫秒)
}
```

### 6. API 端点规范

#### 组件管理 API

| 端点 | 方法 | 请求体 | 响应 | 功能 |
|------|------|--------|------|------|
| `/api/component` | GET | - | `ApiResponse<ComponentListData<QLComponent>>` | 获取组件列表 |
| `/api/component/{id}` | GET | - | `ApiResponse<QLComponent>` | 获取单个组件 |
| `/api/component` | POST | `QLComponent` | `ApiResponse<QLComponent>` | 创建组件 |
| `/api/component/{id}` | PUT | `QLComponent` | `ApiResponse<QLComponent>` | 更新组件 |
| `/api/component/{id}` | DELETE | - | `ApiResponse<void>` | 删除组件 |
| `/api/component/batch` | POST | `QLComponent[]` | `ApiResponse<QLComponent[]>` | 批量创建组件 |

#### 流程管理 API

| 端点 | 方法 | 请求体 | 响应 | 功能 |
|------|------|--------|------|------|
| `/api/flow` | GET | Query: page, size, category, keyword | `ApiResponse<FlowListData>` | 获取流程列表 |
| `/api/flow/{id}` | GET | - | `ApiResponse<FlowDesign>` | 获取单个流程 |
| `/api/flow` | POST | `FlowRequest` | `ApiResponse<FlowDesign>` | 创建流程 |
| `/api/flow/{id}` | PUT | `FlowRequest` | `ApiResponse<FlowDesign>` | 更新流程 |
| `/api/flow/{id}` | DELETE | - | `ApiResponse<void>` | 删除流程 |
| `/api/flow/{id}/deploy` | POST | - | `ApiResponse<FlowDesign>` | 部署流程 |
| `/api/flow/{id}/undeploy` | POST | - | `ApiResponse<FlowDesign>` | 卸载流程 |
| `/api/flow/{id}/copy` | POST | `{newFlowId, newFlowName}` | `ApiResponse<FlowDesign>` | 复制流程 |
| `/api/flow/{id}/toggle` | POST | `{enabled}` | `ApiResponse<FlowDesign>` | 切换启用状态 |
| `/api/flow/batch/delete` | POST | `string[]` | `ApiResponse<void>` | 批量删除 |
| `/api/flow/batch/deploy` | POST | `string[]` | `ApiResponse<void>` | 批量部署 |
| `/api/flow/batch/undeploy` | POST | `string[]` | `ApiResponse<void>` | 批量卸载 |
| `/api/flow/{id}/export` | GET | - | `ApiResponse<FlowDesign>` | 导出流程 |
| `/api/flow/batch/export` | POST | `string[]` | `ApiResponse<FlowDesign[]>` | 批量导出 |
| `/api/flow/import` | POST | `FlowDesign` | `ApiResponse<FlowDesign>` | 导入流程 |
| `/api/flow/categories` | GET | - | `ApiResponse<string[]>` | 获取分类列表 |
| `/api/flow/categories/stats` | GET | - | `ApiResponse<Record<string, number>>` | 获取分类统计 |

### 7. 导入/导出 JSON 格式

支持完整规则编排包的导入导出：

```json
{
  "components": [
    {
      "componentId": "myComponent",
      "componentName": "我的组件",
      "componentType": "SCRIPT",
      "language": "qlexpress",
      "script": "// QLExpress 脚本内容",
      "inputs": [
        { "name": "param1", "type": "String", "description": "参数1" }
      ],
      "outputs": [
        { "name": "result", "type": "Object", "description": "结果" }
      ],
      "enabled": true
    }
  ],
  "flow": {
    "flowId": "myFlow",
    "flowName": "我的流程",
    "description": "流程描述",
    "category": "默认分类",
    "enabled": true,
    "root": {
      "type": "THEN",
      "children": [
        { "type": "COMPONENT", "componentRef": "myComponent" },
        {
          "type": "IF",
          "condition": { "type": "COMPONENT", "componentRef": "conditionComponent" },
          "children": [
            { "type": "COMPONENT", "componentRef": "trueBranch" },
            { "type": "COMPONENT", "componentRef": "falseBranch" }
          ]
        }
      ]
    }
  }
}
```

### 8. 流程编排示例

#### THEN 串行执行

```json
{
  "type": "THEN",
  "children": [
    { "type": "COMPONENT", "componentRef": "step1" },
    { "type": "COMPONENT", "componentRef": "step2" },
    { "type": "COMPONENT", "componentRef": "step3" }
  ]
}
```

对应 LiteFlow EL: `THEN(step1, step2, step3)`

#### WHEN 并行执行

```json
{
  "type": "WHEN",
  "children": [
    { "type": "COMPONENT", "componentRef": "task1" },
    { "type": "COMPONENT", "componentRef": "task2" }
  ]
}
```

对应 LiteFlow EL: `WHEN(task1, task2)`

#### IF-ELSE 条件分支

```json
{
  "type": "IF",
  "condition": { "type": "COMPONENT", "componentRef": "conditionCheck" },
  "children": [
    { "type": "COMPONENT", "componentRef": "trueBranch" },
    { "type": "COMPONENT", "componentRef": "falseBranch" }
  ]
}
```

对应 LiteFlow EL: `IF(conditionCheck, trueBranch, falseBranch)`

#### SWITCH 多路分支

```json
{
  "type": "SWITCH",
  "condition": { "type": "COMPONENT", "componentRef": "router" },
  "children": [
    { "type": "COMPONENT", "componentRef": "branchA", "tag": "A" },
    { "type": "COMPONENT", "componentRef": "branchB", "tag": "B" },
    { "type": "COMPONENT", "componentRef": "branchDefault", "tag": "DEFAULT" }
  ]
}
```

对应 LiteFlow EL: `SWITCH(router).to(branchA, branchB, branchDefault)`

### 9. 错误码规范

| 错误码 | 含义 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 409 | 资源冲突 (如ID已存在) |
| 500 | 服务器内部错误 |

### 10. 注意事项

1. **组件引用**: 流程中使用 `type: "COMPONENT"` + `componentRef` 引用已注册的组件
2. **响应格式差异**: 流程列表返回 `flows` 字段，组件列表返回 `content` 字段
3. **脚本语言**: 固定使用 `qlexpress` 作为脚本语言
4. **分页起始**: 页码从 1 开始
5. **导入顺序**: 导入完整包时，先创建组件，再创建流程 (因为流程依赖组件)
