# 《基于QLExpress4的可视化编排设计方案》

## 一、执行摘要

基于对 QLExpress4 源码的深度分析，本方案提出一套完整的 Web 可视化编辑器架构设计。QLExpress4 是一个基于 ANTLR4 的类 Java 语法规则引擎，具备完善的 AST 解析、指令集执行和表达式追踪机制，非常适合进行可视化编排。

**核心结论**：QLExpress4 **完全支持**可视化编排，其架构具备以下优势：
1. ✅ 暴露了完整的 AST 结构（通过 ANTLR4 ParseTree）
2. ✅ 具备表达式追踪机制（TracePointTree）
3. ✅ 支持自定义 Operator/Function 注册
4. ✅ 指令集架构清晰，便于双向映射

---

## 二、核心机制分析

### 2.1 AST 支持分析

#### 2.1.1 语法树结构

QLExpress4 使用 ANTLR4 生成解析器，核心语法定义于：
- `QLexer.g4` - 词法规则
- `QLParser.g4` - 语法规则

**AST 节点类型映射**（来自 `QLParser.g4`）：

```
program
  └── blockStatements
       └── blockStatement
            ├── localVariableDeclarationStatement
            ├── whileStatement
            ├── traditionalForStatement
            ├── forEachStatement
            ├── functionStatement
            ├── macroStatement
            ├── breakContinueStatement
            ├── returnStatement
            ├── throwStatement
            ├── expressionStatement
            └── emptyStatement
```

#### 2.1.2 关键接口暴露

```java
// Express4Runner.java:339-348
public QLParser.ProgramContext parseToSyntaxTree(String script) {
    return SyntaxTreeFactory.buildTree(script,
        operatorManager,
        initOptions.isDebug(),
        false,
        initOptions.getDebugInfoConsumer(),
        initOptions.getInterpolationMode(),
        initOptions.getSelectorStart(),
        initOptions.getSelectorEnd());
}
```

**可直接获取 ANTLR4 的 ParseTree**，支持双向转换。

#### 2.1.3 表达式追踪机制

```java
// TracePointTree.java - 追踪点树结构
public class TracePointTree {
    private final TraceType type;      // OPERATOR, FUNCTION, METHOD, FIELD, LIST, MAP, IF...
    private final String token;        // 表达式token
    private final List<TracePointTree> children;  // 子节点
    private final int line, col, position;  // 位置信息
}
```

**TraceType 枚举**：
- 父节点类型：`OPERATOR`, `FUNCTION`, `METHOD`, `FIELD`, `LIST`, `MAP`, `IF`, `RETURN`, `BLOCK`
- 子节点类型：`VARIABLE`, `VALUE`, `DEFINE_FUNCTION`, `DEFINE_MACRO`
- 复合类型：`PRIMARY`, `STATEMENT`

### 2.2 指令集架构

#### 2.2.1 核心指令类图

```
QLInstruction (抽象基类)
├── execute(QContext, QLOptions) : QResult
├── stackInput() : int
├── stackOutput() : int
└── println(index, depth, debug)

主要指令实现 (44+ 个)：
├── 控制流指令
│   ├── ForInstruction        // for 循环
│   ├── WhileInstruction      // while 循环
│   ├── ForEachInstruction    // for-each 循环
│   ├── JumpInstruction       // 无条件跳转
│   ├── JumpIfInstruction     // 条件跳转
│   ├── JumpIfPopInstruction  // 条件跳转并出栈
│   ├── TryCatchInstruction   // try-catch-finally
│   ├── BreakContinueInstruction // break/continue
│   └── ReturnInstruction     // return
│
├── 数据操作指令
│   ├── ConstInstruction      // 常量压栈
│   ├── LoadInstruction       // 变量加载
│   ├── DefineLocalInstruction // 本地变量定义
│   ├── GetFieldInstruction   // 字段获取
│   ├── IndexInstruction      // 索引操作
│   └── SliceInstruction      // 切片操作
│
├── 运算指令
│   ├── OperatorInstruction   // 二元运算符
│   ├── UnaryInstruction      // 一元运算符
│   └── CastInstruction       // 类型转换
│
├── 调用指令
│   ├── CallInstruction       // 函数调用
│   ├── CallFunctionInstruction // 自定义函数调用
│   ├── MethodInvokeInstruction // 方法调用
│   └── DefineFunctionInstruction // 函数定义
│
├── 数据结构指令
│   ├── NewListInstruction    // 创建 List
│   ├── NewMapInstruction     // 创建 Map
│   ├── NewArrayInstruction   // 创建数组
│   └── NewInstanceInstruction // 创建对象
│
└── 辅助指令
    ├── PopInstruction        // 出栈
    ├── NewScopeInstruction   // 新作用域
    ├── CloseScopeInstruction // 关闭作用域
    ├── ThrowInstruction      // 抛出异常
    └── StringJoinInstruction // 字符串拼接
```

#### 2.2.2 执行流程

```
用户脚本 (QL Script)
        ↓
  QLexer (词法分析)
        ↓
  QLParser (语法分析) → ParseTree (AST)
        ↓
  QvmInstructionVisitor (访问者模式)
        ↓
  QLInstruction[] (字节码指令序列)
        ↓
  QLambdaDefinitionInner (Lambda定义包装)
        ↓
  QvmRuntime.execute() (执行引擎)
        ↓
  FixedSizeStack (执行栈)
        ↓
  结果值 + 追踪信息
```

### 2.3 元数据提取机制

#### 2.3.1 自定义函数注册

```java
// CustomFunction 接口
public interface CustomFunction {
    Object call(QContext qContext, Parameters parameters) throws Throwable;
}

// 通过注解自动注册
@QLFunction({"add", "plus"})
public int add(int a, int b) {
    return a + b;
}

// Express4Runner 注册方法
runner.addFunction("myFunc", (ctx, params) -> {
    return params.get(0).get();
});
runner.addObjFunction(myObject);      // 对象方法
runner.addStaticFunction(MyClass.class); // 静态方法
```

#### 2.3.2 自定义操作符注册

```java
// BinaryOperator 接口
public interface BinaryOperator extends Operator {
    String getOperator();
    int getPriority();
    Object execute(Value left, Value right, QRuntime runtime,
                   QLOptions options, ErrorReporter reporter);
}

// 注册自定义操作符
runner.addOperator("myOp", (left, right) -> {
    return left.get() + " " + right.get();
}, QLPrecedences.ADD);
```

#### 2.3.3 优先级体系

```java
// QLPrecedences.java
ASSIGN = 0;      // = += -= ...
TERNARY = 1;     // ?:
OR = 2;          // || or
AND = 3;         // && and
BIT_OR = 4;      // |
XOR = 5;         // ^
BIT_AND = 6;     // &
EQUAL = 7;       // == !=
COMPARE = 8;     // < <= > >= instanceof
BIT_MOVE = 9;    // << >> >>>
IN_LIKE = 10;    // in like
ADD = 11;        // + -
MULTI = 12;      // * / %
UNARY = 13;      // ! ++ -- ~ + -
UNARY_SUFFIX = 14; // i++ i--
GROUP = 15;      // . ()
```

### 2.4 类型系统分析

QLExpress4 采用**动态类型**系统，但支持：
- Java 基本类型声明：`int`, `long`, `double`, `boolean`, `char`, `byte`, `short`, `float`
- 泛型类型声明：`List<String>`, `Map<String, Integer>`
- 类型转换：`(int) value`

**类型推断时机**：运行时动态推断，编译时不做严格类型检查。

---

## 三、架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        可视化编辑器前端                              │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐ │
│  │  画布渲染    │  │  组件面板    │  │  属性编辑器  │  │  代码预览   │ │
│  │  (ReactFlow)│  │  (Palette)  │  │  (Props)    │  │  (Monaco)  │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘ │
│                           ↕ JSON Schema                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                   可视化模型层 (Visual Model)                  │  │
│  │   { nodes: [...], edges: [...], metadata: {...} }            │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                    ↕ REST API
┌─────────────────────────────────────────────────────────────────────┐
│                           后端服务层                                 │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────┐ │
│  │  转译器服务      │  │  元数据服务      │  │  执行器服务          │ │
│  │  (Transpiler)   │  │  (Metadata)     │  │  (Executor)        │ │
│  └────────┬────────┘  └────────┬────────┘  └─────────┬───────────┘ │
│           │                    │                     │             │
│  ┌────────▼────────────────────▼─────────────────────▼───────────┐ │
│  │                   QLExpress4 Core Engine                      │ │
│  │  ┌─────────┐  ┌─────────────┐  ┌────────────┐  ┌───────────┐  │ │
│  │  │ Parser  │→ │ Instruction │→ │  Runtime   │→ │  Result   │  │ │
│  │  │ (ANTLR4)│  │  Visitor    │  │  (QVM)     │  │           │  │ │
│  │  └─────────┘  └─────────────┘  └────────────┘  └───────────┘  │ │
│  └───────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 前端技术选型建议

#### 3.2.1 推荐方案：React Flow + Monaco Editor

**理由**：
1. QLExpress4 语法偏向**类 Java 脚本**而非纯 DAG 图
2. 需要同时支持**流程图编排**和**代码表达式编辑**
3. React Flow 支持自定义节点和复杂连线逻辑

**技术栈**：
```
前端框架: React 18+
画布库:   React Flow (@xyflow/react)
代码编辑: Monaco Editor (配置 QL 语法高亮)
UI 组件:  Ant Design / Radix UI
状态管理: Zustand / Jotai
类型安全: TypeScript
```

#### 3.2.2 备选方案对比

| 方案 | 优势 | 劣势 | 适用场景 |
|------|------|------|---------|
| **React Flow** | 灵活、可定制、社区活跃 | 需要自行实现业务节点 | 复杂规则编排 |
| **LogicFlow** | 国产、文档中文、扩展性好 | 社区相对较小 | 企业内部系统 |
| **X6/AntV** | 阿里出品、功能强大 | 学习曲线陡 | 复杂图形应用 |
| **Blockly** | 适合逻辑编程教学 | 生成代码风格固定 | 简单规则配置 |

### 3.3 数据结构设计

#### 3.3.1 可视化 JSON Schema

```typescript
// 完整的 JSON Schema 定义
interface VisualFlowSchema {
  version: "1.0.0";
  metadata: {
    name: string;
    description?: string;
    createdAt: string;
    updatedAt: string;
    author?: string;
  };
  imports: string[];  // import 声明列表
  nodes: VisualNode[];
  edges: VisualEdge[];
  variables: VariableDefinition[];  // 全局变量定义
  functions: FunctionDefinition[];  // 自定义函数定义
}

// 节点定义
interface VisualNode {
  id: string;
  type: NodeType;
  position: { x: number; y: number };
  data: NodeData;
  style?: NodeStyle;
}

// 节点类型枚举
type NodeType =
  // 控制流节点
  | "start" | "end"
  | "if" | "switch"
  | "for" | "foreach" | "while"
  | "try" | "catch" | "finally"
  | "break" | "continue" | "return" | "throw"
  // 数据操作节点
  | "variable" | "assignment" | "expression"
  | "function_call" | "method_call"
  | "list" | "map" | "object"
  // 业务组件节点
  | "api_call" | "data_transform" | "custom";

// 节点数据
interface NodeData {
  label: string;
  // 根据节点类型不同，包含不同字段
  [key: string]: any;
}

// IF 节点数据
interface IfNodeData extends NodeData {
  type: "if";
  condition: Expression;  // 条件表达式
  thenBranch?: string;    // then 分支出口 edge id
  elseBranch?: string;    // else 分支出口 edge id
}

// FOR 循环节点数据
interface ForNodeData extends NodeData {
  type: "for";
  init?: Expression;      // 初始化表达式
  condition?: Expression; // 条件表达式
  update?: Expression;    // 更新表达式
  bodyEntrance?: string;  // 循环体入口 edge id
}

// FOREACH 循环节点数据
interface ForEachNodeData extends NodeData {
  type: "foreach";
  variableType?: string;  // 迭代变量类型
  variableName: string;   // 迭代变量名
  iterable: Expression;   // 可迭代对象
  bodyEntrance?: string;
}

// 函数调用节点数据
interface FunctionCallNodeData extends NodeData {
  type: "function_call";
  functionName: string;
  arguments: Expression[];
  resultVariable?: string;  // 存储结果的变量名
}

// 方法调用节点数据
interface MethodCallNodeData extends NodeData {
  type: "method_call";
  target: Expression;      // 调用目标对象
  methodName: string;
  arguments: Expression[];
  resultVariable?: string;
}

// 表达式节点数据
interface ExpressionNodeData extends NodeData {
  type: "expression";
  expression: Expression;
  resultVariable?: string;
}

// API 调用节点（业务组件示例）
interface ApiCallNodeData extends NodeData {
  type: "api_call";
  endpoint: string;
  method: "GET" | "POST" | "PUT" | "DELETE";
  headers?: Record<string, Expression>;
  body?: Expression;
  resultVariable?: string;
  errorHandler?: string;  // 错误处理分支 edge id
}

// 表达式定义
interface Expression {
  type: "literal" | "variable" | "operator" | "function" | "method" | "raw";
  value?: any;
  variableName?: string;
  operator?: string;
  left?: Expression;
  right?: Expression;
  functionName?: string;
  target?: Expression;
  methodName?: string;
  arguments?: Expression[];
  raw?: string;  // 直接使用 QL 表达式
}

// 边定义
interface VisualEdge {
  id: string;
  source: string;      // 源节点 id
  target: string;      // 目标节点 id
  sourceHandle?: string; // 源节点连接点
  targetHandle?: string; // 目标节点连接点
  type?: "default" | "conditional" | "loop" | "error";
  label?: string;
  data?: {
    condition?: Expression;  // 条件边的条件
  };
}

// 变量定义
interface VariableDefinition {
  name: string;
  type?: string;  // 类型声明（可选）
  initialValue?: Expression;
  scope: "global" | "local";
}

// 函数定义
interface FunctionDefinition {
  name: string;
  parameters: ParameterDefinition[];
  body: string[];  // 函数体中的节点 id 列表
  returnType?: string;
}

interface ParameterDefinition {
  name: string;
  type?: string;
}

// 节点样式
interface NodeStyle {
  width?: number;
  height?: number;
  backgroundColor?: string;
  borderColor?: string;
  borderRadius?: number;
}
```

#### 3.3.2 示例 JSON 实例

```json
{
  "version": "1.0.0",
  "metadata": {
    "name": "订单折扣计算",
    "description": "根据用户等级和订单金额计算折扣",
    "createdAt": "2024-01-15T10:00:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  "imports": ["java.util.List", "java.math.BigDecimal"],
  "variables": [
    {
      "name": "discount",
      "type": "double",
      "initialValue": { "type": "literal", "value": 1.0 },
      "scope": "global"
    }
  ],
  "nodes": [
    {
      "id": "start_1",
      "type": "start",
      "position": { "x": 100, "y": 100 },
      "data": { "label": "开始" }
    },
    {
      "id": "if_1",
      "type": "if",
      "position": { "x": 100, "y": 200 },
      "data": {
        "label": "判断VIP等级",
        "condition": {
          "type": "operator",
          "operator": ">=",
          "left": { "type": "variable", "variableName": "user.vipLevel" },
          "right": { "type": "literal", "value": 3 }
        },
        "thenBranch": "edge_then",
        "elseBranch": "edge_else"
      }
    },
    {
      "id": "assign_1",
      "type": "assignment",
      "position": { "x": 50, "y": 350 },
      "data": {
        "label": "设置VIP折扣",
        "variableName": "discount",
        "expression": { "type": "literal", "value": 0.8 }
      }
    },
    {
      "id": "if_2",
      "type": "if",
      "position": { "x": 200, "y": 350 },
      "data": {
        "label": "判断订单金额",
        "condition": {
          "type": "operator",
          "operator": ">",
          "left": { "type": "variable", "variableName": "order.amount" },
          "right": { "type": "literal", "value": 1000 }
        }
      }
    },
    {
      "id": "assign_2",
      "type": "assignment",
      "position": { "x": 200, "y": 450 },
      "data": {
        "label": "设置大额折扣",
        "variableName": "discount",
        "expression": { "type": "literal", "value": 0.9 }
      }
    },
    {
      "id": "expr_1",
      "type": "expression",
      "position": { "x": 150, "y": 550 },
      "data": {
        "label": "计算最终价格",
        "expression": {
          "type": "operator",
          "operator": "*",
          "left": { "type": "variable", "variableName": "order.amount" },
          "right": { "type": "variable", "variableName": "discount" }
        },
        "resultVariable": "finalPrice"
      }
    },
    {
      "id": "return_1",
      "type": "return",
      "position": { "x": 150, "y": 650 },
      "data": {
        "label": "返回结果",
        "expression": { "type": "variable", "variableName": "finalPrice" }
      }
    }
  ],
  "edges": [
    { "id": "e1", "source": "start_1", "target": "if_1" },
    { "id": "edge_then", "source": "if_1", "target": "assign_1", "type": "conditional", "label": "是VIP" },
    { "id": "edge_else", "source": "if_1", "target": "if_2", "type": "conditional", "label": "非VIP" },
    { "id": "e4", "source": "assign_1", "target": "expr_1" },
    { "id": "e5", "source": "if_2", "target": "assign_2", "type": "conditional", "label": "大额订单" },
    { "id": "e6", "source": "if_2", "target": "expr_1", "type": "conditional", "label": "普通订单" },
    { "id": "e7", "source": "assign_2", "target": "expr_1" },
    { "id": "e8", "source": "expr_1", "target": "return_1" }
  ],
  "functions": []
}
```

---

## 四、代码生成策略

### 4.1 转译器核心类设计

```java
/**
 * 可视化 JSON 到 QLExpress 脚本的转译器
 */
public class VisualToQLTranspiler {

    private final StringBuilder scriptBuilder = new StringBuilder();
    private int indentLevel = 0;
    private static final String INDENT = "    ";

    /**
     * 主入口：将 JSON Schema 转换为 QL 脚本
     */
    public String transpile(VisualFlowSchema schema) {
        // 1. 生成 import 声明
        generateImports(schema.getImports());

        // 2. 生成全局变量定义
        generateVariables(schema.getVariables());

        // 3. 生成自定义函数
        generateFunctions(schema.getFunctions());

        // 4. 生成主逻辑流程（基于拓扑排序）
        generateMainFlow(schema.getNodes(), schema.getEdges());

        return scriptBuilder.toString();
    }

    private void generateImports(List<String> imports) {
        for (String importPath : imports) {
            appendLine("import " + importPath + ";");
        }
        if (!imports.isEmpty()) {
            appendLine("");
        }
    }

    private void generateVariables(List<VariableDefinition> variables) {
        for (VariableDefinition var : variables) {
            if (var.getType() != null) {
                append(var.getType() + " ");
            }
            append(var.getName());
            if (var.getInitialValue() != null) {
                append(" = " + expressionToQL(var.getInitialValue()));
            }
            appendLine(";");
        }
        if (!variables.isEmpty()) {
            appendLine("");
        }
    }

    /**
     * 基于拓扑排序生成主流程
     */
    private void generateMainFlow(List<VisualNode> nodes, List<VisualEdge> edges) {
        // 构建图结构
        Map<String, VisualNode> nodeMap = nodes.stream()
            .collect(Collectors.toMap(VisualNode::getId, n -> n));
        Map<String, List<VisualEdge>> outEdges = edges.stream()
            .collect(Collectors.groupingBy(VisualEdge::getSource));

        // 找到起始节点
        VisualNode startNode = nodes.stream()
            .filter(n -> "start".equals(n.getType()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Missing start node"));

        // 深度优先遍历生成代码
        Set<String> visited = new HashSet<>();
        generateNode(startNode, nodeMap, outEdges, visited);
    }

    private void generateNode(VisualNode node, Map<String, VisualNode> nodeMap,
            Map<String, List<VisualEdge>> outEdges, Set<String> visited) {
        if (visited.contains(node.getId())) {
            return;
        }
        visited.add(node.getId());

        switch (node.getType()) {
            case "start":
                // start 节点不生成代码，直接处理后续节点
                processNextNodes(node.getId(), nodeMap, outEdges, visited);
                break;

            case "if":
                generateIfNode((IfNodeData) node.getData(), node.getId(),
                               nodeMap, outEdges, visited);
                break;

            case "for":
                generateForNode((ForNodeData) node.getData(), node.getId(),
                               nodeMap, outEdges, visited);
                break;

            case "foreach":
                generateForEachNode((ForEachNodeData) node.getData(), node.getId(),
                                   nodeMap, outEdges, visited);
                break;

            case "while":
                generateWhileNode((WhileNodeData) node.getData(), node.getId(),
                                 nodeMap, outEdges, visited);
                break;

            case "assignment":
                generateAssignment((AssignmentNodeData) node.getData());
                processNextNodes(node.getId(), nodeMap, outEdges, visited);
                break;

            case "expression":
                generateExpression((ExpressionNodeData) node.getData());
                processNextNodes(node.getId(), nodeMap, outEdges, visited);
                break;

            case "function_call":
                generateFunctionCall((FunctionCallNodeData) node.getData());
                processNextNodes(node.getId(), nodeMap, outEdges, visited);
                break;

            case "method_call":
                generateMethodCall((MethodCallNodeData) node.getData());
                processNextNodes(node.getId(), nodeMap, outEdges, visited);
                break;

            case "return":
                generateReturn((ReturnNodeData) node.getData());
                break;

            case "break":
                appendLine("break;");
                break;

            case "continue":
                appendLine("continue;");
                break;

            case "throw":
                generateThrow((ThrowNodeData) node.getData());
                break;

            case "try":
                generateTryCatch((TryCatchNodeData) node.getData(), node.getId(),
                                nodeMap, outEdges, visited);
                break;

            case "end":
                // end 节点不生成代码
                break;

            default:
                // 自定义业务节点
                generateCustomNode(node.getData(), node.getId(), nodeMap, outEdges, visited);
        }
    }

    /**
     * 生成 IF 语句
     */
    private void generateIfNode(IfNodeData data, String nodeId,
            Map<String, VisualNode> nodeMap, Map<String, List<VisualEdge>> outEdges,
            Set<String> visited) {

        appendLine("if (" + expressionToQL(data.getCondition()) + ") {");
        indentLevel++;

        // 生成 then 分支
        if (data.getThenBranch() != null) {
            VisualNode thenTarget = findEdgeTarget(data.getThenBranch(), outEdges, nodeMap);
            if (thenTarget != null) {
                generateNode(thenTarget, nodeMap, outEdges, visited);
            }
        }

        indentLevel--;

        // 生成 else 分支
        if (data.getElseBranch() != null) {
            appendLine("} else {");
            indentLevel++;
            VisualNode elseTarget = findEdgeTarget(data.getElseBranch(), outEdges, nodeMap);
            if (elseTarget != null) {
                generateNode(elseTarget, nodeMap, outEdges, visited);
            }
            indentLevel--;
        }

        appendLine("}");

        // 处理汇合后的后续节点
        processNextNodes(nodeId, nodeMap, outEdges, visited);
    }

    /**
     * 生成 FOR 循环
     */
    private void generateForNode(ForNodeData data, String nodeId,
            Map<String, VisualNode> nodeMap, Map<String, List<VisualEdge>> outEdges,
            Set<String> visited) {

        StringBuilder forHeader = new StringBuilder("for (");

        // 初始化部分
        if (data.getInit() != null) {
            forHeader.append(expressionToQL(data.getInit()));
        }
        forHeader.append("; ");

        // 条件部分
        if (data.getCondition() != null) {
            forHeader.append(expressionToQL(data.getCondition()));
        }
        forHeader.append("; ");

        // 更新部分
        if (data.getUpdate() != null) {
            forHeader.append(expressionToQL(data.getUpdate()));
        }
        forHeader.append(") {");

        appendLine(forHeader.toString());
        indentLevel++;

        // 生成循环体
        if (data.getBodyEntrance() != null) {
            VisualNode bodyNode = findEdgeTarget(data.getBodyEntrance(), outEdges, nodeMap);
            if (bodyNode != null) {
                generateNode(bodyNode, nodeMap, outEdges, visited);
            }
        }

        indentLevel--;
        appendLine("}");

        processNextNodes(nodeId, nodeMap, outEdges, visited);
    }

    /**
     * 生成 FOREACH 循环
     */
    private void generateForEachNode(ForEachNodeData data, String nodeId,
            Map<String, VisualNode> nodeMap, Map<String, List<VisualEdge>> outEdges,
            Set<String> visited) {

        StringBuilder forHeader = new StringBuilder("for (");
        if (data.getVariableType() != null) {
            forHeader.append(data.getVariableType()).append(" ");
        }
        forHeader.append(data.getVariableName())
                 .append(" : ")
                 .append(expressionToQL(data.getIterable()))
                 .append(") {");

        appendLine(forHeader.toString());
        indentLevel++;

        if (data.getBodyEntrance() != null) {
            VisualNode bodyNode = findEdgeTarget(data.getBodyEntrance(), outEdges, nodeMap);
            if (bodyNode != null) {
                generateNode(bodyNode, nodeMap, outEdges, visited);
            }
        }

        indentLevel--;
        appendLine("}");

        processNextNodes(nodeId, nodeMap, outEdges, visited);
    }

    /**
     * 表达式转 QL 脚本
     */
    private String expressionToQL(Expression expr) {
        if (expr == null) return "null";

        switch (expr.getType()) {
            case "literal":
                return literalToQL(expr.getValue());

            case "variable":
                return expr.getVariableName();

            case "operator":
                return "(" + expressionToQL(expr.getLeft())
                     + " " + expr.getOperator() + " "
                     + expressionToQL(expr.getRight()) + ")";

            case "function":
                String args = expr.getArguments().stream()
                    .map(this::expressionToQL)
                    .collect(Collectors.joining(", "));
                return expr.getFunctionName() + "(" + args + ")";

            case "method":
                String methodArgs = expr.getArguments().stream()
                    .map(this::expressionToQL)
                    .collect(Collectors.joining(", "));
                return expressionToQL(expr.getTarget())
                     + "." + expr.getMethodName() + "(" + methodArgs + ")";

            case "raw":
                return expr.getRaw();

            default:
                throw new IllegalArgumentException("Unknown expression type: " + expr.getType());
        }
    }

    private String literalToQL(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "\"" + escapeString((String) value) + "\"";
        if (value instanceof Boolean) return value.toString();
        if (value instanceof Number) return value.toString();
        if (value instanceof List) {
            String items = ((List<?>) value).stream()
                .map(this::literalToQL)
                .collect(Collectors.joining(", "));
            return "[" + items + "]";
        }
        if (value instanceof Map) {
            String entries = ((Map<?, ?>) value).entrySet().stream()
                .map(e -> literalToQL(e.getKey()) + ": " + literalToQL(e.getValue()))
                .collect(Collectors.joining(", "));
            return "{" + entries + "}";
        }
        return value.toString();
    }

    private String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // 辅助方法
    private void append(String s) {
        scriptBuilder.append(s);
    }

    private void appendLine(String s) {
        for (int i = 0; i < indentLevel; i++) {
            scriptBuilder.append(INDENT);
        }
        scriptBuilder.append(s).append("\n");
    }

    private void processNextNodes(String nodeId, Map<String, VisualNode> nodeMap,
            Map<String, List<VisualEdge>> outEdges, Set<String> visited) {
        List<VisualEdge> edges = outEdges.getOrDefault(nodeId, Collections.emptyList());
        for (VisualEdge edge : edges) {
            VisualNode target = nodeMap.get(edge.getTarget());
            if (target != null && !visited.contains(target.getId())) {
                generateNode(target, nodeMap, outEdges, visited);
            }
        }
    }

    private VisualNode findEdgeTarget(String edgeId, Map<String, List<VisualEdge>> outEdges,
            Map<String, VisualNode> nodeMap) {
        return outEdges.values().stream()
            .flatMap(List::stream)
            .filter(e -> e.getId().equals(edgeId))
            .findFirst()
            .map(e -> nodeMap.get(e.getTarget()))
            .orElse(null);
    }
}
```

### 4.2 生成示例

**输入 JSON**（简化版）：
```json
{
  "type": "if",
  "condition": {
    "type": "operator",
    "operator": ">=",
    "left": { "type": "variable", "variableName": "user.vipLevel" },
    "right": { "type": "literal", "value": 3 }
  }
}
```

**输出 QL 脚本**：
```java
if ((user.vipLevel >= 3)) {
    discount = 0.8;
} else {
    if ((order.amount > 1000)) {
        discount = 0.9;
    }
}
finalPrice = (order.amount * discount);
return finalPrice;
```

### 4.3 反向解析策略（QL → JSON）

利用 QLExpress4 的 `parseToSyntaxTree()` 方法获取 AST，实现反向映射：

```java
/**
 * QL 脚本到可视化 JSON 的解析器
 */
public class QLToVisualParser {

    private final Express4Runner runner;
    private int nodeCounter = 0;

    public QLToVisualParser(Express4Runner runner) {
        this.runner = runner;
    }

    public VisualFlowSchema parse(String script) {
        // 获取 AST
        QLParser.ProgramContext ast = runner.parseToSyntaxTree(script);

        VisualFlowSchema schema = new VisualFlowSchema();
        schema.setVersion("1.0.0");

        // 解析 import
        schema.setImports(parseImports(ast));

        // 解析语句为节点
        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        // 添加起始节点
        VisualNode startNode = createNode("start", "开始", 100, 50);
        nodes.add(startNode);

        // 遍历语句
        String prevNodeId = startNode.getId();
        if (ast.blockStatements() != null) {
            for (QLParser.BlockStatementContext stmt : ast.blockStatements().blockStatement()) {
                ParseResult result = parseStatement(stmt, nodes, edges, 100, nodes.size() * 120 + 50);
                if (result != null) {
                    edges.add(createEdge(prevNodeId, result.getEntryNodeId()));
                    prevNodeId = result.getExitNodeId();
                }
            }
        }

        // 添加结束节点
        VisualNode endNode = createNode("end", "结束", 100, nodes.size() * 120 + 50);
        nodes.add(endNode);
        edges.add(createEdge(prevNodeId, endNode.getId()));

        schema.setNodes(nodes);
        schema.setEdges(edges);

        return schema;
    }

    private ParseResult parseStatement(QLParser.BlockStatementContext stmt,
            List<VisualNode> nodes, List<VisualEdge> edges, int x, int y) {

        if (stmt instanceof QLParser.ExpressionStatementContext) {
            return parseExpressionStatement((QLParser.ExpressionStatementContext) stmt, nodes, x, y);
        }

        if (stmt instanceof QLParser.QlIfContext ||
            stmt.getChild(0) instanceof QLParser.QlIfContext) {
            return parseIfStatement(stmt, nodes, edges, x, y);
        }

        if (stmt instanceof QLParser.TraditionalForStatementContext) {
            return parseForStatement((QLParser.TraditionalForStatementContext) stmt, nodes, edges, x, y);
        }

        if (stmt instanceof QLParser.ReturnStatementContext) {
            return parseReturnStatement((QLParser.ReturnStatementContext) stmt, nodes, x, y);
        }

        // ... 其他语句类型
        return null;
    }

    private Expression parseExpression(QLParser.ExpressionContext expr) {
        // 递归解析表达式 AST 为 Expression 对象
        // ...
    }

    private VisualNode createNode(String type, String label, int x, int y) {
        VisualNode node = new VisualNode();
        node.setId("node_" + (++nodeCounter));
        node.setType(type);
        node.setPosition(new Position(x, y));
        NodeData data = new NodeData();
        data.setLabel(label);
        node.setData(data);
        return node;
    }

    private VisualEdge createEdge(String source, String target) {
        VisualEdge edge = new VisualEdge();
        edge.setId("edge_" + source + "_" + target);
        edge.setSource(source);
        edge.setTarget(target);
        return edge;
    }
}
```

---

## 五、元数据提取服务

### 5.1 元数据服务设计

```java
/**
 * 元数据服务 - 提取可注册的函数和操作符信息
 */
@Service
public class MetadataService {

    private final Express4Runner runner;

    /**
     * 获取所有已注册的自定义函数元数据
     */
    public List<FunctionMetadata> getRegisteredFunctions() {
        // 从 runner 获取 userDefineFunction
        // 通过反射或提供的 API 获取函数签名
        return Collections.emptyList(); // 示例
    }

    /**
     * 扫描指定类的 @QLFunction 注解方法
     */
    public List<FunctionMetadata> scanClassFunctions(Class<?> clazz) {
        List<FunctionMetadata> result = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            QLFunction annotation = method.getAnnotation(QLFunction.class);
            if (annotation != null) {
                FunctionMetadata meta = new FunctionMetadata();
                meta.setNames(Arrays.asList(annotation.value()));
                meta.setReturnType(method.getReturnType().getSimpleName());

                List<ParameterMetadata> params = new ArrayList<>();
                for (Parameter param : method.getParameters()) {
                    ParameterMetadata paramMeta = new ParameterMetadata();
                    paramMeta.setName(param.getName());
                    paramMeta.setType(param.getType().getSimpleName());
                    params.add(paramMeta);
                }
                meta.setParameters(params);

                result.add(meta);
            }
        }
        return result;
    }

    /**
     * 获取所有内置操作符
     */
    public List<OperatorMetadata> getBuiltInOperators() {
        return Arrays.asList(
            // 算术运算符
            new OperatorMetadata("+", "加法", QLPrecedences.ADD, "any", "any", "any"),
            new OperatorMetadata("-", "减法", QLPrecedences.ADD, "any", "any", "any"),
            new OperatorMetadata("*", "乘法", QLPrecedences.MULTI, "any", "any", "any"),
            new OperatorMetadata("/", "除法", QLPrecedences.MULTI, "any", "any", "any"),
            new OperatorMetadata("%", "取模", QLPrecedences.MULTI, "any", "any", "any"),

            // 比较运算符
            new OperatorMetadata("==", "等于", QLPrecedences.EQUAL, "any", "any", "boolean"),
            new OperatorMetadata("!=", "不等于", QLPrecedences.EQUAL, "any", "any", "boolean"),
            new OperatorMetadata("<>", "不等于", QLPrecedences.EQUAL, "any", "any", "boolean"),
            new OperatorMetadata(">", "大于", QLPrecedences.COMPARE, "any", "any", "boolean"),
            new OperatorMetadata("<", "小于", QLPrecedences.COMPARE, "any", "any", "boolean"),
            new OperatorMetadata(">=", "大于等于", QLPrecedences.COMPARE, "any", "any", "boolean"),
            new OperatorMetadata("<=", "小于等于", QLPrecedences.COMPARE, "any", "any", "boolean"),

            // 逻辑运算符
            new OperatorMetadata("&&", "逻辑与", QLPrecedences.AND, "boolean", "boolean", "boolean"),
            new OperatorMetadata("||", "逻辑或", QLPrecedences.OR, "boolean", "boolean", "boolean"),
            new OperatorMetadata("!", "逻辑非", QLPrecedences.UNARY, "boolean", null, "boolean"),

            // 位运算符
            new OperatorMetadata("&", "按位与", QLPrecedences.BIT_AND, "int", "int", "int"),
            new OperatorMetadata("|", "按位或", QLPrecedences.BIT_OR, "int", "int", "int"),
            new OperatorMetadata("^", "按位异或", QLPrecedences.XOR, "int", "int", "int"),
            new OperatorMetadata("<<", "左移", QLPrecedences.BIT_MOVE, "int", "int", "int"),
            new OperatorMetadata(">>", "右移", QLPrecedences.BIT_MOVE, "int", "int", "int"),
            new OperatorMetadata(">>>", "无符号右移", QLPrecedences.BIT_MOVE, "int", "int", "int"),

            // 集合运算符
            new OperatorMetadata("in", "包含于", QLPrecedences.IN_LIKE, "any", "Collection", "boolean"),
            new OperatorMetadata("like", "模糊匹配", QLPrecedences.IN_LIKE, "String", "String", "boolean"),

            // 其他
            new OperatorMetadata("instanceof", "类型检查", QLPrecedences.COMPARE, "any", "Class", "boolean")
        );
    }

    /**
     * 获取所有关键字
     */
    public List<KeywordMetadata> getKeywords() {
        return Arrays.asList(
            new KeywordMetadata("if", "条件判断", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("else", "否则分支", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("for", "循环", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("while", "条件循环", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("break", "跳出循环", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("continue", "继续循环", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("return", "返回", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("throw", "抛出异常", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("try", "异常捕获", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("catch", "捕获异常", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("finally", "最终执行", NodeCategory.CONTROL_FLOW),
            new KeywordMetadata("function", "函数定义", NodeCategory.DEFINITION),
            new KeywordMetadata("macro", "宏定义", NodeCategory.DEFINITION),
            new KeywordMetadata("new", "创建对象", NodeCategory.DATA),
            new KeywordMetadata("import", "导入", NodeCategory.DEFINITION),
            new KeywordMetadata("null", "空值", NodeCategory.LITERAL),
            new KeywordMetadata("true", "真", NodeCategory.LITERAL),
            new KeywordMetadata("false", "假", NodeCategory.LITERAL)
        );
    }

    /**
     * 获取基本类型
     */
    public List<TypeMetadata> getPrimitiveTypes() {
        return Arrays.asList(
            new TypeMetadata("int", "整数", "0"),
            new TypeMetadata("long", "长整数", "0L"),
            new TypeMetadata("double", "双精度浮点", "0.0"),
            new TypeMetadata("float", "单精度浮点", "0.0f"),
            new TypeMetadata("boolean", "布尔值", "false"),
            new TypeMetadata("char", "字符", "''"),
            new TypeMetadata("byte", "字节", "0"),
            new TypeMetadata("short", "短整数", "0")
        );
    }
}

// 元数据模型
@Data
public class FunctionMetadata {
    private List<String> names;
    private String description;
    private String returnType;
    private List<ParameterMetadata> parameters;
    private String category;  // 分类：内置、自定义、业务
}

@Data
public class ParameterMetadata {
    private String name;
    private String type;
    private boolean required;
    private Object defaultValue;
    private String description;
}

@Data
public class OperatorMetadata {
    private String symbol;
    private String description;
    private int precedence;
    private String leftType;
    private String rightType;
    private String resultType;
}
```

### 5.2 前端类型检查

基于元数据服务，前端可实现连线时的类型检查：

```typescript
// TypeChecker.ts
export class TypeChecker {
  private operatorMeta: OperatorMetadata[];
  private functionMeta: FunctionMetadata[];

  constructor(metadata: MetadataResponse) {
    this.operatorMeta = metadata.operators;
    this.functionMeta = metadata.functions;
  }

  /**
   * 检查两个节点是否可以连线
   */
  canConnect(sourceNode: VisualNode, targetNode: VisualNode,
             sourceHandle: string, targetHandle: string): ValidationResult {
    // 获取源节点输出类型
    const sourceType = this.getOutputType(sourceNode, sourceHandle);

    // 获取目标节点期望输入类型
    const expectedType = this.getExpectedInputType(targetNode, targetHandle);

    // 检查类型兼容性
    if (!this.isTypeCompatible(sourceType, expectedType)) {
      return {
        valid: false,
        message: `类型不匹配：期望 ${expectedType}，实际为 ${sourceType}`
      };
    }

    return { valid: true };
  }

  /**
   * 检查类型兼容性
   */
  private isTypeCompatible(actualType: string, expectedType: string): boolean {
    // any 类型与任何类型兼容
    if (expectedType === 'any' || actualType === 'any') {
      return true;
    }

    // 相同类型兼容
    if (actualType === expectedType) {
      return true;
    }

    // 数字类型之间的隐式转换
    const numberTypes = ['int', 'long', 'float', 'double', 'byte', 'short'];
    if (numberTypes.includes(actualType) && numberTypes.includes(expectedType)) {
      return true;
    }

    // Object 类型接受任何类型
    if (expectedType === 'Object') {
      return true;
    }

    return false;
  }

  /**
   * 获取节点输出类型
   */
  private getOutputType(node: VisualNode, handle: string): string {
    switch (node.type) {
      case 'expression':
        return this.inferExpressionType((node.data as ExpressionNodeData).expression);
      case 'function_call':
        const funcName = (node.data as FunctionCallNodeData).functionName;
        const func = this.functionMeta.find(f => f.names.includes(funcName));
        return func?.returnType || 'any';
      case 'if':
        return handle === 'condition' ? 'boolean' : 'any';
      default:
        return 'any';
    }
  }

  /**
   * 推断表达式类型
   */
  private inferExpressionType(expr: Expression): string {
    switch (expr.type) {
      case 'literal':
        return this.inferLiteralType(expr.value);
      case 'variable':
        return 'any'; // 变量类型需要上下文信息
      case 'operator':
        const op = this.operatorMeta.find(o => o.symbol === expr.operator);
        return op?.resultType || 'any';
      case 'function':
        const func = this.functionMeta.find(f => f.names.includes(expr.functionName!));
        return func?.returnType || 'any';
      default:
        return 'any';
    }
  }

  private inferLiteralType(value: any): string {
    if (value === null) return 'null';
    if (typeof value === 'boolean') return 'boolean';
    if (typeof value === 'number') {
      return Number.isInteger(value) ? 'int' : 'double';
    }
    if (typeof value === 'string') return 'String';
    if (Array.isArray(value)) return 'List';
    if (typeof value === 'object') return 'Map';
    return 'any';
  }
}

interface ValidationResult {
  valid: boolean;
  message?: string;
}
```

---

## 六、前端组件设计

### 6.1 节点组件示例

```tsx
// nodes/IfNode.tsx
import { Handle, Position } from '@xyflow/react';
import { FC, memo } from 'react';

interface IfNodeProps {
  data: {
    label: string;
    condition: Expression;
  };
  selected: boolean;
}

export const IfNode: FC<IfNodeProps> = memo(({ data, selected }) => {
  return (
    <div className={`if-node ${selected ? 'selected' : ''}`}>
      {/* 输入连接点 */}
      <Handle type="target" position={Position.Top} id="input" />

      {/* 节点内容 */}
      <div className="node-header">
        <span className="node-icon">🔀</span>
        <span className="node-label">{data.label}</span>
      </div>

      <div className="node-body">
        <div className="condition-display">
          <code>{expressionToString(data.condition)}</code>
        </div>
      </div>

      {/* 输出连接点 */}
      <Handle
        type="source"
        position={Position.Bottom}
        id="then"
        style={{ left: '25%' }}
      />
      <div className="handle-label" style={{ left: '25%' }}>是</div>

      <Handle
        type="source"
        position={Position.Bottom}
        id="else"
        style={{ left: '75%' }}
      />
      <div className="handle-label" style={{ left: '75%' }}>否</div>
    </div>
  );
});

// nodes/ForNode.tsx
export const ForNode: FC<ForNodeProps> = memo(({ data, selected }) => {
  return (
    <div className={`for-node ${selected ? 'selected' : ''}`}>
      <Handle type="target" position={Position.Top} id="input" />

      <div className="node-header">
        <span className="node-icon">🔄</span>
        <span className="node-label">{data.label}</span>
      </div>

      <div className="node-body">
        <div className="for-parts">
          <div className="for-part">
            <span className="part-label">初始化:</span>
            <code>{expressionToString(data.init)}</code>
          </div>
          <div className="for-part">
            <span className="part-label">条件:</span>
            <code>{expressionToString(data.condition)}</code>
          </div>
          <div className="for-part">
            <span className="part-label">更新:</span>
            <code>{expressionToString(data.update)}</code>
          </div>
        </div>
      </div>

      {/* 循环体入口 */}
      <Handle
        type="source"
        position={Position.Right}
        id="body"
      />
      <div className="handle-label right">循环体</div>

      {/* 循环结束出口 */}
      <Handle
        type="source"
        position={Position.Bottom}
        id="exit"
      />
      <div className="handle-label bottom">完成</div>
    </div>
  );
});

// nodes/FunctionCallNode.tsx
export const FunctionCallNode: FC<FunctionCallNodeProps> = memo(({ data, selected }) => {
  return (
    <div className={`function-node ${selected ? 'selected' : ''}`}>
      <Handle type="target" position={Position.Top} id="input" />

      <div className="node-header">
        <span className="node-icon">ƒ</span>
        <span className="node-label">{data.label}</span>
      </div>

      <div className="node-body">
        <div className="function-name">{data.functionName}</div>
        <div className="arguments">
          {data.arguments.map((arg, index) => (
            <div key={index} className="argument">
              <span className="arg-index">{index + 1}:</span>
              <code>{expressionToString(arg)}</code>
            </div>
          ))}
        </div>
        {data.resultVariable && (
          <div className="result">
            <span className="result-label">结果 →</span>
            <code>{data.resultVariable}</code>
          </div>
        )}
      </div>

      <Handle type="source" position={Position.Bottom} id="output" />
    </div>
  );
});

// 节点类型注册
export const nodeTypes = {
  start: StartNode,
  end: EndNode,
  if: IfNode,
  for: ForNode,
  foreach: ForEachNode,
  while: WhileNode,
  expression: ExpressionNode,
  assignment: AssignmentNode,
  function_call: FunctionCallNode,
  method_call: MethodCallNode,
  return: ReturnNode,
  break: BreakNode,
  continue: ContinueNode,
  throw: ThrowNode,
  try: TryCatchNode,
  api_call: ApiCallNode,
  custom: CustomNode,
};
```

### 6.2 画布主组件

```tsx
// FlowEditor.tsx
import { useCallback, useState } from 'react';
import {
  ReactFlow,
  Controls,
  Background,
  MiniMap,
  Panel,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
} from '@xyflow/react';
import { nodeTypes } from './nodes';
import { ComponentPalette } from './ComponentPalette';
import { PropertyEditor } from './PropertyEditor';
import { CodePreview } from './CodePreview';
import { TypeChecker } from './TypeChecker';

export const FlowEditor: FC = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [typeChecker] = useState(() => new TypeChecker(metadata));

  // 连线验证
  const isValidConnection = useCallback((connection: Connection) => {
    const sourceNode = nodes.find(n => n.id === connection.source);
    const targetNode = nodes.find(n => n.id === connection.target);

    if (!sourceNode || !targetNode) return false;

    const result = typeChecker.canConnect(
      sourceNode,
      targetNode,
      connection.sourceHandle || '',
      connection.targetHandle || ''
    );

    if (!result.valid) {
      // 显示错误提示
      showToast(result.message!, 'error');
    }

    return result.valid;
  }, [nodes, typeChecker]);

  // 处理连线
  const onConnect = useCallback((connection: Connection) => {
    setEdges((eds) => addEdge({
      ...connection,
      type: 'smoothstep',
      animated: true,
    }, eds));
  }, [setEdges]);

  // 从组件面板拖放
  const onDrop = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    const nodeType = event.dataTransfer.getData('nodeType');
    const position = screenToFlowPosition({
      x: event.clientX,
      y: event.clientY,
    });

    const newNode = createNode(nodeType, position);
    setNodes((nds) => [...nds, newNode]);
  }, [setNodes]);

  // 生成代码
  const generateCode = useCallback(() => {
    const schema: VisualFlowSchema = {
      version: '1.0.0',
      metadata: { name: 'Untitled', createdAt: new Date().toISOString() },
      imports: [],
      nodes: nodes,
      edges: edges,
      variables: [],
      functions: [],
    };

    // 调用后端转译 API
    return transpileToQL(schema);
  }, [nodes, edges]);

  return (
    <div className="flow-editor">
      {/* 左侧组件面板 */}
      <ComponentPalette />

      {/* 中间画布 */}
      <div className="canvas-container">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          isValidConnection={isValidConnection}
          onDrop={onDrop}
          onDragOver={(e) => e.preventDefault()}
          nodeTypes={nodeTypes}
          fitView
        >
          <Background />
          <Controls />
          <MiniMap />
          <Panel position="top-right">
            <button onClick={generateCode}>生成代码</button>
          </Panel>
        </ReactFlow>
      </div>

      {/* 右侧属性编辑器 */}
      <PropertyEditor
        selectedNode={selectedNode ? nodes.find(n => n.id === selectedNode) : null}
        onUpdate={updateNode}
      />

      {/* 底部代码预览 */}
      <CodePreview code={generatedCode} />
    </div>
  );
};
```

---

## 七、API 设计

### 7.1 REST API 接口

```yaml
# OpenAPI 3.0 规范

paths:
  # 转译接口
  /api/v1/transpile/to-ql:
    post:
      summary: JSON 转 QL 脚本
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/VisualFlowSchema'
      responses:
        200:
          content:
            application/json:
              schema:
                type: object
                properties:
                  script: { type: string }
                  errors: { type: array, items: { $ref: '#/components/schemas/TranspileError' } }

  /api/v1/transpile/to-json:
    post:
      summary: QL 脚本转 JSON
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                script: { type: string }
      responses:
        200:
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/VisualFlowSchema'

  # 元数据接口
  /api/v1/metadata/operators:
    get:
      summary: 获取操作符列表
      responses:
        200:
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/OperatorMetadata'

  /api/v1/metadata/functions:
    get:
      summary: 获取函数列表
      responses:
        200:
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/FunctionMetadata'

  /api/v1/metadata/keywords:
    get:
      summary: 获取关键字列表

  /api/v1/metadata/types:
    get:
      summary: 获取类型列表

  # 执行接口
  /api/v1/execute:
    post:
      summary: 执行 QL 脚本
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                script: { type: string }
                context: { type: object }
                options:
                  type: object
                  properties:
                    timeout: { type: integer }
                    cache: { type: boolean }
                    traceExpression: { type: boolean }
      responses:
        200:
          content:
            application/json:
              schema:
                type: object
                properties:
                  result: { }
                  traces: { type: array }
                  executionTime: { type: integer }

  /api/v1/execute/visual:
    post:
      summary: 执行可视化流程
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                flow: { $ref: '#/components/schemas/VisualFlowSchema' }
                context: { type: object }

  # 验证接口
  /api/v1/validate/script:
    post:
      summary: 验证脚本语法
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                script: { type: string }
      responses:
        200:
          content:
            application/json:
              schema:
                type: object
                properties:
                  valid: { type: boolean }
                  errors: { type: array }

  /api/v1/validate/flow:
    post:
      summary: 验证可视化流程
```

---

## 八、总结与建议

### 8.1 可行性结论

| 评估维度 | 评分 | 说明 |
|---------|------|------|
| AST 暴露程度 | ★★★★★ | 完整暴露 ANTLR4 ParseTree，支持双向转换 |
| 指令集清晰度 | ★★★★★ | 44+ 指令类，职责单一，文档完善 |
| 扩展性 | ★★★★☆ | 支持自定义函数/操作符/宏，但类型系统弱 |
| 类型安全 | ★★★☆☆ | 动态类型，编译时无严格检查 |
| 表达式追踪 | ★★★★★ | 内置 TracePointTree 机制，利于调试 |

### 8.2 技术建议

1. **前端选型**：React Flow + Monaco Editor 组合最佳
2. **类型检查**：在前端基于元数据实现软类型检查
3. **双向映射**：利用 `parseToSyntaxTree()` 实现 QL → JSON
4. **业务扩展**：通过 `@QLFunction` 注解快速注册业务组件
5. **调试支持**：启用 `traceExpression` 获取执行追踪

### 8.3 实施路线图

```
Phase 1: 基础框架 (2-3周)
├── 后端: 转译器服务、元数据服务
├── 前端: React Flow 画布、基础节点
└── API: REST 接口定义

Phase 2: 核心功能 (3-4周)
├── 完整节点类型实现
├── 连线类型检查
├── 代码预览与编辑
└── 双向转换

Phase 3: 高级特性 (2-3周)
├── 业务组件库
├── 执行调试
├── 版本管理
└── 协作编辑
```

---

## 附录：核心类图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           QLExpress4 核心类图                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐      ┌─────────────────┐      ┌──────────────────┐    │
│  │ Express4Runner  │──────│ OperatorManager │      │  QCompileCache   │    │
│  │  (执行引擎入口) │      │   (操作符管理)  │      │   (编译缓存)     │    │
│  └────────┬────────┘      └─────────────────┘      └──────────────────┘    │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────┐      ┌─────────────────┐                              │
│  │ SyntaxTreeFactory│──────│    QLParser    │←── ANTLR4 生成              │
│  │  (语法树工厂)   │      │   (语法分析器)  │                              │
│  └────────┬────────┘      └─────────────────┘                              │
│           │                                                                 │
│           ▼                                                                 │
│  ┌─────────────────────────────────────────────────────────┐               │
│  │              QvmInstructionVisitor                      │               │
│  │  (AST访问者 - ParseTree → QLInstruction[])              │               │
│  └────────────────────────┬────────────────────────────────┘               │
│                           │                                                 │
│                           ▼                                                 │
│  ┌─────────────────────────────────────────────────────────┐               │
│  │              QLambdaDefinitionInner                     │               │
│  │  (Lambda定义: instructions[], params[], maxStackSize)   │               │
│  └────────────────────────┬────────────────────────────────┘               │
│                           │                                                 │
│                           ▼                                                 │
│  ┌─────────────────────────────────────────────────────────┐               │
│  │                    QvmRuntime                           │               │
│  │  (虚拟机运行时: execute(), FixedSizeStack)              │               │
│  └─────────────────────────────────────────────────────────┘               │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      QLInstruction 体系                              │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ 控制流:  ForInstruction, WhileInstruction, JumpIfInstruction...      │   │
│  │ 数据:    ConstInstruction, LoadInstruction, DefineLocalInstruction   │   │
│  │ 运算:    OperatorInstruction, UnaryInstruction, CastInstruction      │   │
│  │ 调用:    CallInstruction, MethodInvokeInstruction                    │   │
│  │ 结构:    NewListInstruction, NewMapInstruction, NewInstanceInstruction│   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```
