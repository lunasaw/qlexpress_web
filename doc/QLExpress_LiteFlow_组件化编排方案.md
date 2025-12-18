# QLExpress + LiteFlow 组件化编排通用技术方案

## 一、方案概述

### 1.1 设计目标

构建一个**通用的组件化流程编排系统**，将 **QLExpress** 作为业务逻辑实现载体，**LiteFlow** 作为流程编排引擎，实现：

- **业务无关性**：框架层不包含任何业务逻辑，所有业务通过组件动态注入
- **高度可扩展**：支持任意业务领域通过配置接入，无需修改核心代码
- **可视化编排**：基于 liteflow-editor-client 的图形化流程设计
- **热更新能力**：组件和流程支持运行时动态加载、更新、卸载
- **完整的生命周期**：组件从定义、验证、测试到执行的全流程管理

### 1.2 核心架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              应用层 (Application Layer)                      │
│  ┌─────────────────────────────��────────────────────────────────────────┐  │
│  │  业务领域 A        业务领域 B        业务领域 C        ...             │  │
│  │  (IoT设备控制)    (订单处理)       (风控规则)                         │  │
│  │       ↓               ↓               ↓                               │  │
│  │  [组件库 A]       [组件库 B]       [组件库 C]                         │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ↓ 组件注册/流程定义
┌─────────────────────────────────────────────────────────────────────────────┐
│                              平台层 (Platform Layer)                         │
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐     │
│  │   组件管理服务    │  │   流程编排服务    │  │     执行引擎服务         │     │
│  │                  │  │                  │  │                         │     │
│  │ • 组件 CRUD      │  │ • 流程设计       │  │ • 动态节点加载          │     │
│  │ • 脚本验证       │  │ • EL 表达式生成  │  │ • 流程链构建            │     │
│  │ • 组件测试       │  │ • 结构验证       │  │ • 执行追踪              │     ��
│  │ • 版本管理       │  │ • 预览/调试      │  │ • 结果收集              │     │
│  └─────────────────┘  └─────────────────┘  └───────��─────────────────┘     │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │                         上下文管理 (Context Management)                │  │
│  │                                                                        │  │
│  │  FlowContext: 类型安全的数据存取 + 作用域隔离 + 线程安全              │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────���──┘
                                      │
                                      ↓
┌─────────────────────────────────────────────────────────────────────────────┐
│                              引擎层 (Engine Layer)                           │
│                                                                              │
│  ┌─────────────────────────────────┐  ┌─────────────────────────────────┐  │
│  │          LiteFlow 2.15+          │  │         QLExpress 4.0+          │  │
│  │                                  │  │                                  │  │
│  │  • 流程编排 (EL 表达式)          │  │  • 脚本解析与执行                │  │
│  │  • 节点调度 (串行/并行)          │  │  • 表达式计算                    │  │
│  │  • 条件分支 (IF/SWITCH)          │  │  • 自定义操作符/函数             │  │
│  │  • 循环控制 (FOR/WHILE)          │  │  • 上下文变量绑定                │  │
│  │  • 异常处理 (CATCH)              │  │  • 语法树分析                    │  │
│  └─────────────────────────────────┘  └─────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 设计原则

| 原则 | 描述 | 实现方式 |
|------|------|---------|
| **业务无关** | 核心框架不包含业务逻辑 | 所有业务通过 QLComponent 动态注入 |
| **开闭原则** | 对扩展开放，对修改关闭 | 通过组件注册机制扩展，不修改框架代码 |
| **单一职责** | 组件职责单一明确 | 每个组件完成一个原子操作 |
| **依赖倒置** | 高层不依赖低层实现 | 通过 FlowContext 抽象解耦 |
| **接口隔离** | 组件间通过上下文通信 | getData/setData 标准接口 |

---

## 二、核心数据模型

### 2.1 组件定义模型 (QLComponent)

> 复用现有实现：`com.ql.qlexpress.web.liteflow.model.QLComponent`

```java
/**
 * QLExpress 组件定义 - 通用模型
 *
 * 设计要点：
 * 1. componentId 全局唯一，作为 LiteFlow nodeId
 * 2. componentType 决定组件行为和返回值约束
 * 3. script 为纯粹的业务逻辑，通过 flowContext 访问上下文
 * 4. inputs/outputs 定义组件契约，用于验证和文档
 */
@Data
@Builder
public class QLComponent {

    // ========== 标识信息 ==========
    /** 组件唯一标识 (全局唯一，对应 LiteFlow nodeId) */
    @NotBlank
    private String componentId;

    /** 组件名称 (人类可读) */
    @NotBlank
    private String componentName;

    /** 组件描述 */
    private String description;

    /** 组件分类 (用于组织和检索) */
    private String category;

    /** 组件图标 (前端显示) */
    private String icon;

    // ========== 类型与行为 ==========
    /**
     * 组件类型 - 决定执行行为和返回值约束
     */
    @NotNull
    private ComponentType componentType;

    /** 脚本语言 (默认 qlexpress) */
    @Builder.Default
    private String language = "qlexpress";

    // ========== 脚本内容 ==========
    /** QLExpress 脚本 - 实际业务逻辑 */
    @NotBlank
    private String script;

    // ========== 参数契约 ==========
    /** 输入参数定义 - 组件期望从上下文获取的数据 */
    @Valid
    private List<ParameterDefinition> inputs;

    /** 输出参数定义 - 组件会设置到上下文的数据 */
    @Valid
    private List<ParameterDefinition> outputs;

    /** Switch 分支定义 (仅 SWITCH 类型) */
    private List<SwitchBranch> switchBranches;

    /** 默认分支 (SWITCH 类型) */
    private String defaultBranch;

    // ========== 版本与状态 ==========
    @Builder.Default
    private String version = "1.0.0";

    @Builder.Default
    private boolean enabled = true;

    /** 扩展元数据 */
    private Map<String, Object> metadata;

    /**
     * 组件类型枚举 - 对应 LiteFlow 节点类型
     */
    public enum ComponentType {
        /** 普通脚本 - 执行逻辑，无返回值要求，通过上下文传递数据 */
        SCRIPT("script"),

        /** 布尔判断 - 必须返回 true/false，用于 IF 条件 */
        BOOLEAN("boolean_script"),

        /** 路由选择 - 返回分支标识字符串，用于 SWITCH 多路分支 */
        SWITCH("switch_script");

        private final String liteFlowNodeType;
        // ...
    }
}
```

### 2.2 参数定义模型

```java
/**
 * 参数定义 - 描述组件的输入输出契约
 */
@Data
@Builder
public static class ParameterDefinition {
    /** 参数名称 (对应 flowContext 中的 key) */
    @NotBlank
    private String name;

    /** 参数类型 (Java 类型，如 String, Integer, List<String>) */
    @NotBlank
    private String type;

    /** 参数描述 */
    private String description;

    /** 是否必填 */
    @Builder.Default
    private boolean required = true;

    /** 默认值 */
    private Object defaultValue;

    /** 验证规则 (如 "min:0", "max:100", "pattern:^[a-z]+$") */
    private String validation;
}

/**
 * Switch 分支定义
 */
@Data
@Builder
public static class SwitchBranch {
    /** 分支标识 (脚本返回值匹配) */
    @NotBlank
    private String branchId;

    /** 分支名称 (人类可读) */
    @NotBlank
    private String branchName;

    /** 分支描述 */
    private String description;

    /** 分支样式 (前端可选) */
    private String style;
}
```

### 2.3 可视化编排模型 (CmpProperty)

> 复用现有实现：`com.ql.qlexpress.web.liteflow.model.CmpProperty`

```java
/**
 * 可视化组件树结构 - 用于前端画布与后端 EL 表达式双向转换
 *
 * 设计要点：
 * 1. 树形结构表示编排关系
 * 2. type 区分编排类型(THEN/WHEN/IF等)和节点类型(组件引用)
 * 3. componentRef 引用 QLComponent.componentId
 * 4. properties 支持 LiteFlow 修饰符配置
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CmpProperty {

    /** 节点 ID */
    private String id;

    /**
     * 节点类型：
     * - 编排类型: THEN, WHEN, IF, SWITCH, FOR, WHILE, CATCH, AND, OR, NOT
     * - 节点类型: CommonNode, BooleanNode, SwitchNode, COMPONENT
     */
    private String type;

    /** 组件引用 (叶子节点引用 QLComponent.componentId) */
    private String componentRef;

    /** 属性配置 */
    private Properties properties;

    /** 条件节点 (IF/SWITCH/FOR/WHILE 使用) */
    private CmpProperty condition;

    /** 子节点列表 */
    private List<CmpProperty> children;

    /**
     * 属性详情 - 映射 LiteFlow 修饰符
     */
    @Data
    @Builder
    public static class Properties {
        /** .id("xxx") - 节点标识 */
        private String id;

        /** .tag("xxx") - 节点标签 */
        private String tag;

        /** .data("xxx") - 节点数据 */
        private String data;

        /** SWITCH 分支标识 */
        private String branch;

        /** WHEN 最大等待时间 */
        private Integer maxWaitSeconds;

        /** 是否忽略错误 */
        private Boolean ignoreError;

        /** WHEN 是否必须成功 */
        private Boolean must;

        /** FOR 循环次数 */
        private Integer loopCount;

        /** BREAK 条件 */
        private CmpProperty breakCondition;
    }

    /**
     * 编排类型枚举
     */
    public enum CmpType {
        // 编排类型
        THEN, WHEN, IF, SWITCH, FOR, WHILE, CATCH, AND, OR, NOT,
        // 节点类型
        CommonNode, BooleanNode, SwitchNode, BREAK, COMPONENT
    }
}
```

### 2.4 流程设计模型 (FlowDesign)

```java
/**
 * 流程设计 - 完整的流程定义
 */
@Data
@Builder
public class FlowDesign {
    /** 流程唯一标识 */
    @NotBlank
    private String flowId;

    /** 流程名称 */
    @NotBlank
    private String flowName;

    /** 流程描述 */
    private String description;

    /** 流程分类 */
    private String category;

    /** 编排定义 (根节点) */
    @NotNull
    private CmpProperty root;

    /** 流程版本 */
    @Builder.Default
    private String version = "1.0.0";

    /** 流程标签 */
    private List<String> tags;

    /** 是否启用 */
    @Builder.Default
    private boolean enabled = true;

    /** 扩展元数据 */
    private Map<String, Object> metadata;
}
```

---

## 三、核心服务设计

### 3.1 组件管理服务 (QLComponentService)

> 复用现有实现：`com.ql.qlexpress.web.liteflow.service.QLComponentService`

```java
/**
 * 组件管理服务 - 提供组件全生命周期管理
 */
@Service
@RequiredArgsConstructor
public class QLComponentService {

    private final QLComponentRepository componentRepository;
    private final LiteFlowDynamicService liteFlowDynamicService;
    private final Express4Runner expressRunner;

    // ==================== 组件 CRUD ====================

    /**
     * 创建组件
     * 1. 检查 ID 唯一性
     * 2. 验证脚本语法
     * 3. 保存组件
     * 4. 同步到 LiteFlow
     */
    public QLComponent createComponent(QLComponent component);

    /**
     * 更新组件
     * 1. 验证脚本语法
     * 2. 保存更新
     * 3. 刷新 LiteFlow 节点
     */
    public QLComponent updateComponent(String componentId, QLComponent component);

    public QLComponent getComponent(String componentId);
    public void deleteComponent(String componentId);
    public List<QLComponent> listComponents(String category, ComponentType type, Boolean enabled);
    public List<QLComponent> searchComponents(String keyword);

    // ==================== 脚本验证 ====================

    /**
     * 验证组件脚本
     * 1. 语法检查 (QLExpress 解析)
     * 2. 类型约束检查 (BOOLEAN 必须有 return，SWITCH 必须定义分支等)
     */
    public ScriptValidationResult validateScript(QLComponent component);

    /**
     * 单独验证脚本语法
     */
    public ScriptValidationResult validateScriptOnly(String script, String language);

    // ==================== 组件测试 ====================

    /**
     * 测试组件执行
     * 1. 验证脚本
     * 2. 准备测试上下文 (模拟 flowContext)
     * 3. 执行脚本
     * 4. 返回执行结果和上下文变化
     */
    public ComponentTestResult testComponent(String componentId, Map<String, Object> testParams);
    public ComponentTestResult testComponentExecution(QLComponent component, Map<String, Object> testParams);

    // ==================== LiteFlow 同步 ====================

    public void syncToLiteFlow(QLComponent component);
    public int syncAllToLiteFlow();
}
```

### 3.2 流程转换服务 (FlowConvertService)

> 复用现有实现：`com.ql.qlexpress.web.liteflow.service.FlowConvertService`

```java
/**
 * 流程转换服务 - CmpProperty <-> LiteFlow EL 双向转换
 */
@Service
@RequiredArgsConstructor
public class FlowConvertService {

    private final QLComponentRepository componentRepository;

    /**
     * 完整转换：FlowDesign -> ConvertResult
     * 1. 收集所有使用的组件 ID
     * 2. 加载组件定义，生成节点列表
     * 3. 递归生成 EL 表达式
     */
    public ConvertResult convert(FlowDesign flowDesign);

    /**
     * 仅生成 EL 表达式
     */
    public String generateELOnly(CmpProperty root);

    /**
     * 验证流程定义
     * 1. 基础结构验证
     * 2. 组件存在性验证
     * 3. EL 语法验证
     */
    public ValidationResult validate(FlowDesign flowDesign);

    // ==================== EL 生成 (内部方法) ====================

    /**
     * 递归生成 EL 表达式
     *
     * 支持的编排类型：
     * - THEN: 顺序执行 -> THEN(a, b, c)
     * - WHEN: 并行执行 -> WHEN(a, b, c).maxWaitSeconds(10)
     * - IF: 条件分支 -> IF(cond, trueNode, falseNode)
     * - SWITCH: 多路分支 -> SWITCH(cond).to(a, b, c).id('A', 'B', 'C')
     * - FOR: 循环 -> FOR(count).DO(body).BREAK(cond)
     * - WHILE: 条件循环 -> WHILE(cond).DO(body)
     * - CATCH: 异常处理 -> CATCH(tryBody).DO(catchBody)
     * - AND/OR/NOT: 逻辑操作
     */
    private String generateEL(CmpProperty node);
}
```

### 3.3 动态执行服务 (LiteFlowDynamicService)

> 复用现有实现：`com.ql.qlexpress.web.liteflow.service.LiteFlowDynamicService`

```java
/**
 * LiteFlow 动态执行服务 - 支持运行时动态加载和执行
 */
@Service
@RequiredArgsConstructor
public class LiteFlowDynamicService {

    private final FlowExecutor flowExecutor;
    private final ScriptExecutor scriptExecutor;

    // ==================== 动态节点管理 ====================

    /**
     * 添加脚本节点
     * @param nodeId 节点 ID
     * @param nodeName 节点名称
     * @param script 脚本内容
     * @param nodeType script | boolean_script | switch_script
     * @param language 脚本语言
     */
    public void addScriptNode(String nodeId, String nodeName, String script,
                              String nodeType, String language);

    /**
     * 刷新脚本节点 (热更新)
     */
    public void reloadScriptNode(String nodeId, String script, String nodeType, String language);

    /**
     * 移除脚本节点
     */
    public void removeScriptNode(String nodeId);

    // ==================== 动态流程链管理 ====================

    /**
     * 添加/更新流程链
     * @param chainId 链 ID
     * @param el EL 表达式
     */
    public void addOrUpdateChain(String chainId, String el);

    /**
     * 移除流程链
     */
    public void removeChain(String chainId);

    // ==================== 流程执行 ====================

    /**
     * 执行流程
     * @param chainId 流程链 ID
     * @param params 初始参数
     * @return 执行结果
     */
    public ExecuteResult executeFlow(String chainId, Map<String, Object> params);

    /**
     * 带超时的执行
     */
    public ExecuteResult executeWithTimeout(String chainId, Map<String, Object> params,
                                            long timeout, TimeUnit unit);

    /**
     * 直接执行 EL 表达式 (无需预先注册 Chain)
     */
    public ExecuteResult executeEL(String el, Map<String, Object> params);
}
```

### 3.4 上下文管理 (FlowContext)

```java
/**
 * 流程执行上下文 - 线程安全的数据存取
 *
 * 设计要点：
 * 1. 基于 ConcurrentHashMap 保证线程安全
 * 2. 提供类型安全的 getter 方法
 * 3. 支持默认值
 * 4. 组件间通过上下文通信，实现解耦
 */
public class FlowContext extends ConcurrentHashMap<String, Object> {

    // ========== 类型安全的数据存取 ==========

    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) get(key);
    }

    public <T> T getData(String key, T defaultValue) {
        T value = getData(key);
        return value != null ? value : defaultValue;
    }

    public FlowContext setData(String key, Object value) {
        put(key, value);
        return this;
    }

    // ========== 便捷方法 ==========

    public String getString(String key);
    public String getString(String key, String defaultValue);

    public Integer getInt(String key);
    public int getInt(String key, int defaultValue);

    public Long getLong(String key);
    public long getLong(String key, long defaultValue);

    public Boolean getBoolean(String key);
    public boolean getBoolean(String key, boolean defaultValue);

    public <T> List<T> getList(String key);
    public <K, V> Map<K, V> getMap(String key);
}
```

---

## 四、编排类型与 EL 映射

### 4.1 支持的编排类型

| 编排类型 | 用途 | EL 表达式 | 示例 |
|---------|------|-----------|------|
| **THEN** | 顺序执行 | `THEN(a, b, c)` | 依次执行 a、b、c |
| **WHEN** | 并行执行 | `WHEN(a, b, c)` | 同时执行 a、b、c |
| **IF** | 条件分支 | `IF(cond, trueNode, falseNode)` | 根据 cond 结果选择执行 |
| **SWITCH** | 多路分支 | `SWITCH(cond).to(a, b, c).id('A', 'B', 'C')` | 根据 cond 返回值路由 |
| **FOR** | 固定循环 | `FOR(n).DO(body)` | 循环执行 n 次 |
| **WHILE** | 条件循环 | `WHILE(cond).DO(body)` | cond 为真时循环 |
| **CATCH** | 异常处理 | `CATCH(tryBody).DO(catchBody)` | 捕获异常并处理 |
| **AND** | 逻辑与 | `AND(a, b, c)` | 所有条件都为真 |
| **OR** | 逻辑或 | `OR(a, b, c)` | 任一条件为真 |
| **NOT** | 逻辑非 | `NOT(a)` | 取反 |

### 4.2 组件类型与 LiteFlow 节点映射

| QLComponent.ComponentType | LiteFlow 节点类型 | 用途 | 返回值约束 |
|--------------------------|------------------|------|-----------|
| **SCRIPT** | `script` | 执行业务逻辑 | 无返回值要求，通过 flowContext 传递数据 |
| **BOOLEAN** | `boolean_script` | 条件判断 | 必须返回 `true` 或 `false` |
| **SWITCH** | `switch_script` | 多路路由 | 必须返回分支标识字符串 |

### 4.3 EL 修饰符支持

| 修饰符 | 用途 | 示例 |
|-------|------|------|
| `.id("xxx")` | 设置节点标识 | `THEN(a, b).id("main_flow")` |
| `.tag("xxx")` | 设置节点标签 | `node.tag("important")` |
| `.data("xxx")` | 传递节点数据 | `node.data("{\"key\": \"value\"}")` |
| `.maxWaitSeconds(n)` | WHEN 最大等待时间 | `WHEN(a, b).maxWaitSeconds(10)` |
| `.ignoreError(true)` | 忽略执行错误 | `node.ignoreError(true)` |
| `.must()` | WHEN 中必须成功 | `WHEN(a.must(), b)` |
| `.BREAK(cond)` | 循环中断条件 | `FOR(10).DO(body).BREAK(breakCond)` |

---

## 五、API 接口设计

### 5.1 接口总览

```yaml
# ==================== 组件管理 ====================
POST   /api/v1/component                  # 创建组件
GET    /api/v1/component/{componentId}    # 获取组件详情
PUT    /api/v1/component/{componentId}    # 更新组件
DELETE /api/v1/component/{componentId}    # 删除组件
GET    /api/v1/component/list             # 组件列表 (支持筛选)
GET    /api/v1/component/search           # 搜索组件
GET    /api/v1/component/categories       # 获取所有分类
POST   /api/v1/component/validate         # 验证组件脚本
POST   /api/v1/component/test             # 测试组件执行

# ==================== 流程管理 ====================
POST   /api/v1/flow                       # 创建流程
GET    /api/v1/flow/{flowId}              # 获取流程详情
PUT    /api/v1/flow/{flowId}              # 更新流程
DELETE /api/v1/flow/{flowId}              # 删除流程
GET    /api/v1/flow/list                  # 流程列表
POST   /api/v1/flow/validate              # 验证流程定义
POST   /api/v1/flow/preview-el            # 预览 EL 表达式

# ==================== 流程执行 ====================
POST   /api/v1/flow/{flowId}/execute      # 执行流程
POST   /api/v1/flow/execute-el            # 直接执行 EL 表达式
GET    /api/v1/flow/{flowId}/status       # 获取执行状态

# ==================== EL 转换 (兼容 liteflow-editor-server) ====================
POST   /api/generateEL                    # CmpProperty JSON -> EL 表达式
POST   /api/generateJsonEL                # EL 表达式 -> CmpProperty JSON
```

### 5.2 核心请求/响应格式

#### 创建组件

```json
// POST /api/v1/component
// Request
{
  "componentId": "validate_user_age",
  "componentName": "验证用户年龄",
  "description": "检查用户年龄是否满足条件",
  "category": "用户验证",
  "componentType": "BOOLEAN",
  "script": "age = flowContext.getInt('userAge', 0);\nminAge = flowContext.getInt('minAge', 18);\nresult = age >= minAge;\nflowContext.setData('ageValid', result);\nreturn result;",
  "inputs": [
    { "name": "userAge", "type": "Integer", "description": "用户年龄", "required": true },
    { "name": "minAge", "type": "Integer", "description": "最小年龄", "defaultValue": 18 }
  ],
  "outputs": [
    { "name": "ageValid", "type": "Boolean", "description": "年龄是否有效" }
  ]
}

// Response
{
  "code": 200,
  "data": {
    "componentId": "validate_user_age",
    "componentName": "验证用户年龄",
    "componentType": "BOOLEAN",
    "version": "1.0.0",
    "enabled": true,
    "createTime": "2024-01-01T10:00:00Z"
  }
}
```

#### 执行流程

```json
// POST /api/v1/flow/{flowId}/execute
// Request
{
  "params": {
    "userId": "12345",
    "userAge": 25,
    "orderAmount": 1000
  },
  "options": {
    "timeout": 30000,
    "traceEnabled": true
  }
}

// Response
{
  "code": 200,
  "data": {
    "success": true,
    "flowId": "order_process",
    "executionId": "exec_20240101_001",
    "context": {
      "userId": "12345",
      "userAge": 25,
      "ageValid": true,
      "orderProcessed": true
    },
    "executeSteps": "init(2ms) -> validate_user_age(1ms) -> process_order(15ms) -> notify(3ms)",
    "totalTime": 21,
    "trace": [
      { "node": "init", "time": 2, "status": "success" },
      { "node": "validate_user_age", "time": 1, "status": "success", "result": true },
      { "node": "process_order", "time": 15, "status": "success" },
      { "node": "notify", "time": 3, "status": "success" }
    ]
  }
}
```

---

## 六、组件开发规范

### 6.1 脚本编写规范

```javascript
// ========== 1. 通过 flowContext 获取输入 ==========
// 推荐：使用带默认值的方法，避免空指针
userId = flowContext.getString('userId');
amount = flowContext.getInt('amount', 0);
config = flowContext.getData('config');

// ========== 2. 执行业务逻辑 ==========
// 注意：脚本内可以使用 import 导入 Java 类
import java.util.HashMap;

result = new HashMap();
result.put('success', true);
result.put('message', '处理完成');

// ========== 3. 通过 flowContext 设置输出 ==========
flowContext.setData('processResult', result);
flowContext.setData('processTime', System.currentTimeMillis());

// ========== 4. BOOLEAN 类型必须返回布尔值 ==========
// return true;  或  return false;

// ========== 5. SWITCH 类型必须返回分支标识 ==========
// return 'BRANCH_A';  或  return 'DEFAULT';

// ========== 6. 日志输出 (可选) ==========
// LogUtils.infoRule('处理完成: userId=' + userId);
```

### 6.2 组件类型约束

#### SCRIPT 类型

```javascript
// 无返回值要求，通过 flowContext 传递数据
userId = flowContext.getString('userId');
// ... 业务处理 ...
flowContext.setData('result', processResult);
// 不需要 return
```

#### BOOLEAN 类型

```javascript
// 必须返回 true 或 false
age = flowContext.getInt('age', 0);
isValid = age >= 18;
flowContext.setData('isAdult', isValid);
return isValid;  // 必须返回布尔值
```

#### SWITCH 类型

```javascript
// 必须返回分支标识字符串
orderType = flowContext.getString('orderType');
if ('VIP'.equals(orderType)) {
    return 'VIP_PROCESS';
} else if ('NORMAL'.equals(orderType)) {
    return 'NORMAL_PROCESS';
}
return 'DEFAULT';  // 默认分支
```

### 6.3 最佳实践

| 原则 | 说明 | 示例 |
|------|------|------|
| **单一职责** | 每个组件只做一件事 | `validate_age` 只验证年龄 |
| **幂等性** | 同样输入产生同样输出 | 避免依赖外部可变状态 |
| **显式契约** | 明确定义 inputs/outputs | 便于理解和验证 |
| **防御性编程** | 使用带默认值的 getter | `getInt('key', 0)` |
| **日志可追溯** | 关键步骤记录日志 | `LogUtils.infoRule(...)` |

---

## 七、扩展指南

### 7.1 接入新业务领域

```java
/**
 * 步骤 1: 定义业务领域的组件分类
 */
public class OrderDomainCategories {
    public static final String ORDER_VALIDATE = "订单验证";
    public static final String ORDER_PROCESS = "订单处理";
    public static final String ORDER_NOTIFY = "订单通知";
}

/**
 * 步骤 2: 创建业务组件
 */
QLComponent validateStock = QLComponent.builder()
    .componentId("validate_stock")
    .componentName("库存验证")
    .category(OrderDomainCategories.ORDER_VALIDATE)
    .componentType(ComponentType.BOOLEAN)
    .script("skuId = flowContext.getString('skuId');\n" +
            "quantity = flowContext.getInt('quantity', 1);\n" +
            "// 调用库存服务验证\n" +
            "stockService = flowContext.getData('stockService');\n" +
            "hasStock = stockService.checkStock(skuId, quantity);\n" +
            "flowContext.setData('hasStock', hasStock);\n" +
            "return hasStock;")
    .inputs(Arrays.asList(
        ParameterDefinition.builder().name("skuId").type("String").required(true).build(),
        ParameterDefinition.builder().name("quantity").type("Integer").defaultValue(1).build()
    ))
    .outputs(Arrays.asList(
        ParameterDefinition.builder().name("hasStock").type("Boolean").build()
    ))
    .build();

/**
 * 步骤 3: 注册组件
 */
componentService.createComponent(validateStock);

/**
 * 步骤 4: 设计流程编排
 */
CmpProperty orderFlow = CmpProperty.builder()
    .type("THEN")
    .children(Arrays.asList(
        CmpProperty.builder().componentRef("validate_stock").build(),
        CmpProperty.builder()
            .type("IF")
            .condition(CmpProperty.builder().componentRef("validate_stock").build())
            .children(Arrays.asList(
                CmpProperty.builder().componentRef("process_order").build(),
                CmpProperty.builder().componentRef("notify_no_stock").build()
            ))
            .build()
    ))
    .build();

/**
 * 步骤 5: 注册并执行流程
 */
FlowDesign flow = FlowDesign.builder()
    .flowId("order_create")
    .flowName("创建订单")
    .root(orderFlow)
    .build();

flowService.createFlow(flow);
executeService.executeFlow("order_create", params);
```

### 7.2 自定义上下文服务注入

```java
/**
 * 将业务服务注入到 FlowContext，供组件脚本调用
 */
@Component
public class FlowContextInitializer {

    @Autowired
    private StockService stockService;

    @Autowired
    private NotifyService notifyService;

    public void initContext(FlowContext context) {
        // 注入业务服务
        context.setData("stockService", stockService);
        context.setData("notifyService", notifyService);

        // 注入工具类
        context.setData("dateUtils", new DateUtils());
        context.setData("jsonUtils", new JsonUtils());
    }
}
```

### 7.3 组件脚本中使用注入的服务

```javascript
// 获取注入的服务
stockService = flowContext.getData('stockService');
notifyService = flowContext.getData('notifyService');

// 调用服务方法
skuId = flowContext.getString('skuId');
hasStock = stockService.checkStock(skuId, 1);

if (!hasStock) {
    notifyService.sendAlert('库存不足: ' + skuId);
}

flowContext.setData('hasStock', hasStock);
return hasStock;
```

---

## 八、与 liteflow-editor-server 集成

### 8.1 数据模型映射

| liteflow-editor-server | 本项目 | 说明 |
|------------------------|--------|------|
| `CmpProperty` | 直接复用 | 可视化编排树结构 |
| `CmpProperty.id` | `QLComponent.componentId` | 节点引用组件 |
| `ELInfo` | 直接复用 | EL 表达式容器 |
| `ChainInfo` | 扩展 | 增加组件脚本信息 |

### 8.2 双向转换流程

```
前端可视化画布
      │
      ↓ (CmpProperty JSON)
┌─────────────────────────────────────────────────────┐
│            FlowConvertService.generateEL()          │
│                                                     │
│  CmpProperty (树结构)  ──→  LiteFlow EL 表达式      │
│                                                     │
│  示例:                                              │
│  {                           THEN(                  │
│    "type": "THEN",             init,                │
│    "children": [               IF(checkAge,         │
│      {"componentRef":"init"},    processAdult,      │
│      {"type":"IF", ...}          processMinor       │
│    ]                           )                    │
│  }                           )                      │
└─────────────────────────────────────────────────────┘
      │
      ↓ (EL 表达式)
┌─────────────────────────────────────────────────────┐
│         LiteFlowDynamicService.addOrUpdateChain()   │
│                                                     │
│  1. 解析 EL 表达式                                  │
│  2. 加载组件脚本为 ScriptNode                       │
│  3. 构建 Chain                                      │
└─────────────────────────────────────────────────────┘
      │
      ↓
   LiteFlow 执行引擎
```

---

## 九、配置参考

### 9.1 Spring Boot 配置

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: qlexpress-liteflow-platform

# LiteFlow 配置
liteflow:
  # 启用脚本支持
  script-enable: true
  # 脚本语言
  script-language: qlexpress
  # 打印执行日志
  print-execution-log: true
  # 监控配置
  monitor:
    enable-log: true
    period: 300000ms
  # 线程池配置 (WHEN 并行执行)
  when-max-workers: 16
  when-queue-limit: 512
```

### 9.2 Maven 依赖

```xml
<!-- QLExpress 4 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>qlexpress4</artifactId>
    <version>4.0.7</version>
</dependency>

<!-- LiteFlow -->
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-spring-boot-starter</artifactId>
    <version>2.15.2</version>
</dependency>

<!-- LiteFlow QLExpress 脚本支持 -->
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-script-qlexpress</artifactId>
    <version>2.15.2</version>
</dependency>
```

---

## 十、总结

本方案提供了一个**通用的组件化流程编排框架**，具有以下特点：

### 核心优势

| 特性 | 描述 |
|------|------|
| **业务无关** | 框架层不包含任何业务逻辑，所有业务通过组件动态注入 |
| **高度可扩展** | 新业务通过注册组件接入，无需修改核心代码 |
| **可视化编排** | 支持前端拖拽式流程设计，自动生成 EL 表达式 |
| **热更新** | 支持运行时动态加载、更新组件和流程 |
| **类型安全** | 通过 FlowContext 提供类型安全的数据存取 |
| **完整追踪** | 提供详细的执行追踪信息，便于调试和监控 |

### 适用场景

- **规则引擎**：复杂业务规则的可视化编排
- **流程编排**：多步骤业务流程的定义和执行
- **IoT 设备控制**：设备指令的组合编排
- **审批流程**：多条件分支的审批流
- **风控规则**：多维度风险评估规则
- **营销活动**：促销规则的灵活配置

### 后续演进方向

1. **版本管理**：组件和流程的版本控制、回滚能力
2. **灰度发布**：支持流程的灰度发布和 A/B 测试
3. **监控告警**：执行耗时、错误率的监控和告警
4. **权限控制**：组件和流程的访问权限管理
5. **性能优化**：组件脚本预编译、结果缓存
