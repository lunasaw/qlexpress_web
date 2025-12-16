# QLExpress 完整双向转换方案设计

## 1. 概述

### 1.1 目标
实现 QLExpress 脚本与可视化 JSON 的完整双向转换，支持复杂业务规则（如 blueTooth.groovy）。

### 1.2 当前问题
现有解析器仅支持基础语法，无法处理：
- import 语句
- 函数定义 (function)
- 类型转换 ((Type)expr)
- new 表达式
- else/else if 分支
- 嵌套代码块递归解析
- 静态方法调用
- Map/List 字面量

### 1.3 参考案例
`blueTooth.groovy` - 358行复杂业务规则，包含：
- 37个 import 语句
- 5个函数定义
- 多层嵌套 if-else
- for 循环
- 类型转换和 new 表达式

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        QLExpress Web                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐    │
│  │   QL Script  │ ←→  │  AST Layer   │ ←→  │  JSON Model  │    │
│  │   (.groovy)  │     │  (QLExpress4)│     │  (Visual)    │    │
│  └──────────────┘     └──────────────┘     └──────────────┘    │
│         ↑                    ↑                    ↑             │
│         │                    │                    │             │
│  ┌──────┴──────┐     ┌──────┴──────┐     ┌──────┴──────┐      │
│  │ QL Emitter  │     │ AST Visitor │     │ JSON Schema │      │
│  │ (JSON→QL)   │     │ (Traversal) │     │ (Model)     │      │
│  └─────────────┘     └─────────────┘     └─────────────┘      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 核心组件

```
com.ql.qlexpress.web
├── model/
│   └── visual/                    # JSON 模型层
│       ├── VisualFlowSchema.java  # 流程定义(扩展)
│       ├── VisualNode.java        # 节点基类
│       ├── VisualEdge.java        # 边定义
│       ├── Expression.java        # 表达式模型(扩展)
│       ├── nodes/                 # 节点类型(新增)
│       │   ├── ImportNode.java
│       │   ├── FunctionDefNode.java
│       │   ├── IfElseNode.java
│       │   ├── ForLoopNode.java
│       │   ├── WhileLoopNode.java
│       │   ├── TryCatchNode.java
│       │   └── ...
│       └── expressions/           # 表达式类型(新增)
│           ├── CastExpression.java
│           ├── NewExpression.java
│           ├── MethodCallExpression.java
│           ├── StaticMethodExpression.java
│           ├── MapLiteralExpression.java
│           └── ListLiteralExpression.java
│
├── core/
│   ├── parser/                    # QL → JSON
│   │   ├── QLToVisualParser.java  # 主解析器(重构)
│   │   ├── ASTVisitor.java        # AST 遍历器(新增)
│   │   ├── ExpressionParser.java  # 表达式解析(新增)
│   │   └── StatementParser.java   # 语句解析(新增)
│   │
│   └── transpiler/                # JSON → QL
│       ├── VisualToQLTranspiler.java  # 主转译器(扩展)
│       ├── NodeEmitter.java       # 节点代码生成(新增)
│       └── ExpressionEmitter.java # 表达式代码生成(新增)
│
└── service/
    ├── ParseService.java          # 解析服务
    └── TranspileService.java      # 转译服务
```

---

## 3. JSON Schema 设计 (V2)

### 3.1 VisualFlowSchema 扩展

```json
{
  "version": "2.0.0",
  "metadata": {
    "name": "蓝牙开门规则",
    "description": "处理蓝牙APP开门指令",
    "author": "qlexpress",
    "createTime": "2025-01-01T00:00:00Z",
    "sourceFile": "blueTooth.groovy"
  },
  "imports": [
    "com.alibaba.fastjson2.JSON",
    "com.alibaba.fastjson2.JSONArray",
    "com.alibaba.fastjson2.JSONObject"
  ],
  "functions": [
    {
      "id": "func_1",
      "name": "executeAppBluetoothCommand",
      "parameters": [
        { "name": "appBluetoothCommandModel", "type": "AppBluetoothCommandModel" },
        { "name": "characteristic", "type": "BleCharacteristic" }
      ],
      "returnType": "AppBluetoothCommandReponse",
      "body": {
        "nodes": [...],
        "edges": [...]
      }
    }
  ],
  "variables": [
    { "name": "aBCMStr", "type": "String", "scope": "global" }
  ],
  "nodes": [...],
  "edges": [...]
}
```

### 3.2 节点类型定义

#### 3.2.1 基础节点类型

| 类型 | 说明 | 示例 |
|------|------|------|
| `start` | 流程开始 | 入口点 |
| `end` | 流程结束 | 出口点 |
| `assignment` | 赋值语句 | `a = 1` |
| `expression` | 表达式语句 | `obj.method()` |
| `return` | 返回语句 | `return result` |

#### 3.2.2 控制流节点类型

| 类型 | 说明 | 分支 |
|------|------|------|
| `if` | 条件分支 | then, else, elseIf[] |
| `for` | for循环 | body, next |
| `foreach` | foreach循环 | body, next |
| `while` | while循环 | body, next |
| `break` | 跳出循环 | - |
| `continue` | 继续循环 | - |
| `try_catch` | 异常处理 | try, catch[], finally |
| `throw` | 抛出异常 | - |

#### 3.2.3 新增节点类型

| 类型 | 说明 | 数据结构 |
|------|------|----------|
| `import` | 导入语句 | `{ className: string }` |
| `function_def` | 函数定义 | `{ name, params[], returnType, body }` |
| `function_call` | 函数调用 | `{ name, arguments[], resultVar }` |
| `block` | 代码块 | `{ statements[] }` |

### 3.3 表达式类型定义

```typescript
// Expression 基础结构
interface Expression {
  type: ExpressionType;
  // 具体字段根据 type 不同而不同
}

enum ExpressionType {
  // 基础类型
  LITERAL = "literal",           // 字面量: 1, "hello", true, null
  VARIABLE = "variable",         // 变量引用: x, obj
  OPERATOR = "operator",         // 运算符: a + b, x > 0

  // 调用类型
  FUNCTION_CALL = "function",    // 函数调用: max(a, b)
  METHOD_CALL = "method",        // 方法调用: obj.method(args)
  STATIC_CALL = "static",        // 静态调用: Class.method(args)
  CONSTRUCTOR = "new",           // 构造函数: new ClassName(args)

  // 特殊类型
  CAST = "cast",                 // 类型转换: (Type)expr
  ARRAY_ACCESS = "array_access", // 数组访问: arr[i]
  FIELD_ACCESS = "field_access", // 字段访问: obj.field
  TERNARY = "ternary",          // 三元运算: a ? b : c

  // 集合类型
  MAP_LITERAL = "map",           // Map字面量: new HashMap()
  LIST_LITERAL = "list",         // List字面量: new ArrayList()

  // 原始类型
  RAW = "raw"                    // 原始表达式: 不解析直接保留
}
```

### 3.4 详细表达式定义

```json
// 1. 字面量
{ "type": "literal", "value": 42, "dataType": "Integer" }
{ "type": "literal", "value": "hello", "dataType": "String" }
{ "type": "literal", "value": true, "dataType": "Boolean" }
{ "type": "literal", "value": null, "dataType": "null" }

// 2. 变量引用
{ "type": "variable", "name": "count" }

// 3. 运算符
{
  "type": "operator",
  "operator": "+",
  "left": { "type": "variable", "name": "a" },
  "right": { "type": "literal", "value": 1 }
}

// 4. 方法调用
{
  "type": "method",
  "object": { "type": "variable", "name": "list" },
  "methodName": "get",
  "arguments": [{ "type": "literal", "value": 0 }]
}

// 5. 静态方法调用
{
  "type": "static",
  "className": "JSON",
  "methodName": "toJSONString",
  "arguments": [{ "type": "variable", "name": "obj" }]
}

// 6. 构造函数 (new)
{
  "type": "new",
  "className": "HashMap",
  "arguments": []
}

// 7. 类型转换
{
  "type": "cast",
  "targetType": "AppBluetoothCommandModel",
  "expression": { "type": "variable", "name": "obj" }
}

// 8. 链式调用
{
  "type": "method",
  "object": {
    "type": "method",
    "object": { "type": "variable", "name": "str" },
    "methodName": "trim",
    "arguments": []
  },
  "methodName": "toLowerCase",
  "arguments": []
}
```

---

## 4. 节点详细设计

### 4.1 If-Else 节点 (支持 else if)

```json
{
  "id": "if_1",
  "type": "if",
  "position": { "x": 250, "y": 200 },
  "data": {
    "label": "类型判断",
    "condition": {
      "type": "operator",
      "operator": "==",
      "left": { "type": "variable", "name": "type" },
      "right": { "type": "literal", "value": 1 }
    },
    "thenBranch": "node_then_1",
    "elseIfBranches": [
      {
        "condition": {
          "type": "operator",
          "operator": "==",
          "left": { "type": "variable", "name": "type" },
          "right": { "type": "literal", "value": 2 }
        },
        "branch": "node_elseif_1"
      },
      {
        "condition": {
          "type": "operator",
          "operator": "==",
          "left": { "type": "variable", "name": "type" },
          "right": { "type": "literal", "value": 3 }
        },
        "branch": "node_elseif_2"
      }
    ],
    "elseBranch": "node_else_1"
  }
}
```

**生成的 QL 代码:**
```groovy
if (type == 1) {
    // thenBranch
} else if (type == 2) {
    // elseIfBranch 1
} else if (type == 3) {
    // elseIfBranch 2
} else {
    // elseBranch
}
```

### 4.2 函数定义节点

```json
{
  "id": "func_def_1",
  "type": "function_def",
  "data": {
    "name": "executeAppBluetoothCommand",
    "parameters": [
      {
        "name": "appBluetoothCommandModel",
        "type": "AppBluetoothCommandModel"
      },
      {
        "name": "characteristic",
        "type": "BleCharacteristic"
      }
    ],
    "returnType": "AppBluetoothCommandReponse",
    "body": {
      "entryNode": "func_body_start",
      "nodes": [...],
      "edges": [...]
    }
  }
}
```

**生成的 QL 代码:**
```groovy
function executeAppBluetoothCommand(AppBluetoothCommandModel appBluetoothCommandModel, BleCharacteristic characteristic) {
    // body nodes
};
```

### 4.3 For 循环节点

```json
{
  "id": "for_1",
  "type": "for",
  "data": {
    "label": "遍历设备列表",
    "init": {
      "type": "operator",
      "operator": "=",
      "left": { "type": "variable", "name": "i", "declareType": "int" },
      "right": { "type": "literal", "value": 0 }
    },
    "condition": {
      "type": "operator",
      "operator": "<",
      "left": { "type": "variable", "name": "i" },
      "right": {
        "type": "method",
        "object": { "type": "variable", "name": "doorServiceList" },
        "methodName": "size",
        "arguments": []
      }
    },
    "update": {
      "type": "operator",
      "operator": "=",
      "left": { "type": "variable", "name": "i" },
      "right": {
        "type": "operator",
        "operator": "+",
        "left": { "type": "variable", "name": "i" },
        "right": { "type": "literal", "value": 1 }
      }
    },
    "bodyEntrance": "for_body_1"
  }
}
```

**生成的 QL 代码:**
```groovy
for (int i = 0; i < doorServiceList.size(); i++) {
    // body
}
```

### 4.4 Try-Catch 节点

```json
{
  "id": "try_1",
  "type": "try_catch",
  "data": {
    "label": "异常处理",
    "tryBranch": "try_body_1",
    "catchHandlers": [
      {
        "exceptionType": "Exception",
        "exceptionVariable": "e",
        "catchBranch": "catch_body_1"
      }
    ],
    "finallyBranch": "finally_body_1"
  }
}
```

---

## 5. 解析器设计 (QL → JSON)

### 5.1 基于 QLExpress4 AST 的解析

```java
/**
 * 增强版 QL 到 Visual 解析器
 * 基于 QLExpress4 的 AST 进行深度解析
 */
@Component
public class EnhancedQLToVisualParser {

    private final Express4Runner runner;

    /**
     * 解析 QL 脚本
     */
    public ParseResult parse(String script) {
        // 1. 语法检查
        runner.check(script);

        // 2. 获取 AST
        QLParser.ProgramContext ast = runner.parseToSyntaxTree(script);

        // 3. 使用 Visitor 模式遍历 AST
        ASTToVisualVisitor visitor = new ASTToVisualVisitor();
        VisualFlowSchema schema = visitor.visit(ast);

        return ParseResult.success(schema);
    }
}
```

### 5.2 AST Visitor 实现

```java
/**
 * AST 到 Visual 模型的访问器
 */
public class ASTToVisualVisitor extends QLParserBaseVisitor<Object> {

    private final List<String> imports = new ArrayList<>();
    private final List<FunctionDefinition> functions = new ArrayList<>();
    private final List<VisualNode> nodes = new ArrayList<>();
    private final List<VisualEdge> edges = new ArrayList<>();

    private int nodeCounter = 0;
    private int edgeCounter = 0;

    @Override
    public Object visitProgram(QLParser.ProgramContext ctx) {
        // 处理所有顶层语句
        for (QLParser.StatementContext stmt : ctx.statement()) {
            visitStatement(stmt);
        }

        return buildSchema();
    }

    @Override
    public Object visitImportStatement(QLParser.ImportStatementContext ctx) {
        String className = ctx.qualifiedName().getText();
        imports.add(className);
        return null;
    }

    @Override
    public Object visitFunctionDefinition(QLParser.FunctionDefinitionContext ctx) {
        String funcName = ctx.IDENTIFIER().getText();
        List<ParameterDefinition> params = parseParameters(ctx.parameterList());

        // 递归解析函数体
        FunctionBodyVisitor bodyVisitor = new FunctionBodyVisitor();
        FunctionBody body = bodyVisitor.visit(ctx.block());

        FunctionDefinition funcDef = FunctionDefinition.builder()
            .name(funcName)
            .parameters(params)
            .body(body)
            .build();

        functions.add(funcDef);
        return funcDef;
    }

    @Override
    public Object visitIfStatement(QLParser.IfStatementContext ctx) {
        // 解析 if 条件
        Expression condition = parseExpression(ctx.expression());

        // 解析 then 分支
        String thenBranch = parseBlock(ctx.block(0));

        // 解析 else if 分支
        List<ElseIfBranch> elseIfBranches = new ArrayList<>();
        for (int i = 0; i < ctx.ELSE_IF().size(); i++) {
            Expression elseIfCond = parseExpression(ctx.elseIfExpression(i));
            String elseIfBranch = parseBlock(ctx.elseIfBlock(i));
            elseIfBranches.add(new ElseIfBranch(elseIfCond, elseIfBranch));
        }

        // 解析 else 分支
        String elseBranch = null;
        if (ctx.ELSE() != null) {
            elseBranch = parseBlock(ctx.elseBlock());
        }

        // 创建 If 节点
        IfNodeData data = IfNodeData.builder()
            .condition(condition)
            .thenBranch(thenBranch)
            .elseIfBranches(elseIfBranches)
            .elseBranch(elseBranch)
            .build();

        VisualNode node = createNode("if", data);
        nodes.add(node);

        return node;
    }

    @Override
    public Object visitForStatement(QLParser.ForStatementContext ctx) {
        // 解析 for 循环各部分
        Expression init = parseExpression(ctx.forInit());
        Expression condition = parseExpression(ctx.expression());
        Expression update = parseExpression(ctx.forUpdate());

        // 递归解析循环体
        String bodyEntrance = parseBlock(ctx.block());

        ForNodeData data = ForNodeData.builder()
            .init(init)
            .condition(condition)
            .update(update)
            .bodyEntrance(bodyEntrance)
            .build();

        VisualNode node = createNode("for", data);
        nodes.add(node);

        return node;
    }

    // ... 其他 visit 方法
}
```

### 5.3 表达式解析器

```java
/**
 * 表达式解析器 - 将 AST 表达式转换为 Expression 模型
 */
public class ExpressionParser {

    public Expression parse(QLParser.ExpressionContext ctx) {
        // 字面量
        if (ctx.literal() != null) {
            return parseLiteral(ctx.literal());
        }

        // 变量引用
        if (ctx.IDENTIFIER() != null && ctx.getChildCount() == 1) {
            return Expression.variable(ctx.IDENTIFIER().getText());
        }

        // 方法调用: obj.method(args)
        if (ctx.DOT() != null && ctx.methodCall() != null) {
            Expression object = parse(ctx.expression(0));
            String methodName = ctx.methodCall().IDENTIFIER().getText();
            List<Expression> args = parseArguments(ctx.methodCall().arguments());
            return Expression.method(object, methodName, args);
        }

        // 静态方法调用: Class.method(args)
        if (ctx.staticMethodCall() != null) {
            String className = ctx.staticMethodCall().className().getText();
            String methodName = ctx.staticMethodCall().IDENTIFIER().getText();
            List<Expression> args = parseArguments(ctx.staticMethodCall().arguments());
            return Expression.staticMethod(className, methodName, args);
        }

        // new 表达式: new ClassName(args)
        if (ctx.NEW() != null) {
            String className = ctx.className().getText();
            List<Expression> args = parseArguments(ctx.arguments());
            return Expression.newInstance(className, args);
        }

        // 类型转换: (Type)expr
        if (ctx.castExpression() != null) {
            String targetType = ctx.castExpression().type().getText();
            Expression expr = parse(ctx.castExpression().expression());
            return Expression.cast(targetType, expr);
        }

        // 二元运算符
        if (ctx.binaryOperator() != null) {
            String operator = ctx.binaryOperator().getText();
            Expression left = parse(ctx.expression(0));
            Expression right = parse(ctx.expression(1));
            return Expression.operator(operator, left, right);
        }

        // 三元运算符
        if (ctx.QUESTION() != null) {
            Expression condition = parse(ctx.expression(0));
            Expression thenExpr = parse(ctx.expression(1));
            Expression elseExpr = parse(ctx.expression(2));
            return Expression.ternary(condition, thenExpr, elseExpr);
        }

        // 数组访问: arr[i]
        if (ctx.LBRACKET() != null) {
            Expression array = parse(ctx.expression(0));
            Expression index = parse(ctx.expression(1));
            return Expression.arrayAccess(array, index);
        }

        // 其他情况：保留原始文本
        return Expression.raw(ctx.getText());
    }

    private Expression parseLiteral(QLParser.LiteralContext ctx) {
        if (ctx.IntegerLiteral() != null) {
            return Expression.literal(Integer.parseInt(ctx.getText()));
        }
        if (ctx.FloatingPointLiteral() != null) {
            return Expression.literal(Double.parseDouble(ctx.getText()));
        }
        if (ctx.StringLiteral() != null) {
            String text = ctx.getText();
            // 去掉引号
            return Expression.literal(text.substring(1, text.length() - 1));
        }
        if (ctx.BooleanLiteral() != null) {
            return Expression.literal(Boolean.parseBoolean(ctx.getText()));
        }
        if (ctx.NULL() != null) {
            return Expression.literal(null);
        }
        return Expression.raw(ctx.getText());
    }
}
```

---

## 6. 转译器设计 (JSON → QL)

### 6.1 增强版转译器

```java
/**
 * 增强版 Visual 到 QL 转译器
 */
@Component
public class EnhancedVisualToQLTranspiler {

    private final ExpressionEmitter expressionEmitter = new ExpressionEmitter();
    private final StringBuilder output = new StringBuilder();
    private int indentLevel = 0;

    public TranspileResult transpile(VisualFlowSchema schema, TranspileOptions options) {
        output.setLength(0);
        indentLevel = 0;

        // 1. 生成 import 语句
        emitImports(schema.getImports());

        // 2. 生成全局变量声明
        emitVariables(schema.getVariables());

        // 3. 生成主流程代码
        emitMainFlow(schema);

        // 4. 生成函数定义
        emitFunctions(schema.getFunctions());

        return TranspileResult.success(output.toString());
    }

    private void emitImports(List<String> imports) {
        if (imports == null || imports.isEmpty()) return;

        for (String imp : imports) {
            appendLine("import " + imp + ";");
        }
        appendLine("");
    }

    private void emitMainFlow(VisualFlowSchema schema) {
        // 找到 start 节点
        VisualNode startNode = findNodeByType(schema.getNodes(), "start");
        if (startNode == null) return;

        // 从 start 开始遍历并生成代码
        Set<String> visited = new HashSet<>();
        emitNodeChain(startNode.getId(), schema, visited);
    }

    private void emitNodeChain(String nodeId, VisualFlowSchema schema, Set<String> visited) {
        if (visited.contains(nodeId)) return;
        visited.add(nodeId);

        VisualNode node = findNodeById(schema.getNodes(), nodeId);
        if (node == null || "start".equals(node.getType()) || "end".equals(node.getType())) {
            // 继续到下一个节点
            String nextId = findNextNodeId(nodeId, schema.getEdges());
            if (nextId != null && !"end".equals(findNodeById(schema.getNodes(), nextId).getType())) {
                emitNodeChain(nextId, schema, visited);
            }
            return;
        }

        // 根据节点类型生成代码
        switch (node.getType()) {
            case "assignment":
                emitAssignment(node);
                break;
            case "if":
                emitIfStatement(node, schema, visited);
                return; // if 语句内部处理后续节点
            case "for":
                emitForStatement(node, schema, visited);
                return;
            case "while":
                emitWhileStatement(node, schema, visited);
                return;
            case "return":
                emitReturnStatement(node);
                break;
            case "expression":
                emitExpressionStatement(node);
                break;
            case "function_call":
                emitFunctionCall(node);
                break;
            case "try_catch":
                emitTryCatch(node, schema, visited);
                return;
            case "break":
                appendLine("break;");
                break;
            case "continue":
                appendLine("continue;");
                break;
        }

        // 继续到下一个节点
        String nextId = findNextNodeId(nodeId, schema.getEdges());
        if (nextId != null) {
            emitNodeChain(nextId, schema, visited);
        }
    }

    private void emitIfStatement(VisualNode node, VisualFlowSchema schema, Set<String> visited) {
        IfNodeData data = (IfNodeData) node.getData();

        // if 条件
        append("if (");
        append(expressionEmitter.emit(data.getCondition()));
        appendLine(") {");

        // then 分支
        indentLevel++;
        if (data.getThenBranch() != null) {
            emitNodeChain(data.getThenBranch(), schema, visited);
        }
        indentLevel--;

        // else if 分支
        if (data.getElseIfBranches() != null) {
            for (ElseIfBranch elseIf : data.getElseIfBranches()) {
                append("} else if (");
                append(expressionEmitter.emit(elseIf.getCondition()));
                appendLine(") {");

                indentLevel++;
                if (elseIf.getBranch() != null) {
                    emitNodeChain(elseIf.getBranch(), schema, visited);
                }
                indentLevel--;
            }
        }

        // else 分支
        if (data.getElseBranch() != null) {
            appendLine("} else {");
            indentLevel++;
            emitNodeChain(data.getElseBranch(), schema, visited);
            indentLevel--;
        }

        appendLine("}");

        // 继续到 if 之后的节点
        String nextId = findNextNodeId(node.getId(), schema.getEdges(), "next");
        if (nextId != null) {
            emitNodeChain(nextId, schema, visited);
        }
    }

    private void emitForStatement(VisualNode node, VisualFlowSchema schema, Set<String> visited) {
        ForNodeData data = (ForNodeData) node.getData();

        append("for (");
        append(expressionEmitter.emit(data.getInit()));
        append("; ");
        append(expressionEmitter.emit(data.getCondition()));
        append("; ");
        append(expressionEmitter.emit(data.getUpdate()));
        appendLine(") {");

        // 循环体
        indentLevel++;
        if (data.getBodyEntrance() != null) {
            emitNodeChain(data.getBodyEntrance(), schema, visited);
        }
        indentLevel--;

        appendLine("}");

        // 继续到循环后的节点
        String nextId = findNextNodeId(node.getId(), schema.getEdges(), "next");
        if (nextId != null) {
            emitNodeChain(nextId, schema, visited);
        }
    }

    private void emitFunctions(List<FunctionDefinition> functions) {
        if (functions == null || functions.isEmpty()) return;

        for (FunctionDefinition func : functions) {
            appendLine("");
            emitFunction(func);
        }
    }

    private void emitFunction(FunctionDefinition func) {
        append("function ");
        append(func.getName());
        append("(");

        // 参数列表
        List<ParameterDefinition> params = func.getParameters();
        if (params != null && !params.isEmpty()) {
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) append(", ");
                ParameterDefinition param = params.get(i);
                if (param.getType() != null) {
                    append(param.getType());
                    append(" ");
                }
                append(param.getName());
            }
        }

        appendLine(") {");

        // 函数体
        indentLevel++;
        emitFunctionBody(func.getBody());
        indentLevel--;

        appendLine("};");
    }

    // 辅助方法
    private void append(String text) {
        output.append(text);
    }

    private void appendLine(String text) {
        appendIndent();
        output.append(text).append("\n");
    }

    private void appendIndent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
    }
}
```

### 6.2 表达式代码生成器

```java
/**
 * 表达式代码生成器
 */
public class ExpressionEmitter {

    public String emit(Expression expr) {
        if (expr == null) return "";

        switch (expr.getType()) {
            case LITERAL:
                return emitLiteral(expr);
            case VARIABLE:
                return expr.getName();
            case OPERATOR:
                return emitOperator(expr);
            case FUNCTION_CALL:
                return emitFunctionCall(expr);
            case METHOD_CALL:
                return emitMethodCall(expr);
            case STATIC_CALL:
                return emitStaticCall(expr);
            case NEW:
                return emitNew(expr);
            case CAST:
                return emitCast(expr);
            case ARRAY_ACCESS:
                return emitArrayAccess(expr);
            case TERNARY:
                return emitTernary(expr);
            case RAW:
                return expr.getRaw();
            default:
                return expr.toString();
        }
    }

    private String emitLiteral(Expression expr) {
        Object value = expr.getValue();
        if (value == null) return "null";
        if (value instanceof String) return "\"" + escapeString((String) value) + "\"";
        return value.toString();
    }

    private String emitOperator(Expression expr) {
        String left = emit(expr.getLeft());
        String right = emit(expr.getRight());
        String op = expr.getOperator();
        return "(" + left + " " + op + " " + right + ")";
    }

    private String emitFunctionCall(Expression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append(expr.getFunctionName());
        sb.append("(");
        List<Expression> args = expr.getArguments();
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(emit(args.get(i)));
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String emitMethodCall(Expression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append(emit(expr.getObject()));
        sb.append(".");
        sb.append(expr.getMethodName());
        sb.append("(");
        List<Expression> args = expr.getArguments();
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(emit(args.get(i)));
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String emitStaticCall(Expression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append(expr.getClassName());
        sb.append(".");
        sb.append(expr.getMethodName());
        sb.append("(");
        List<Expression> args = expr.getArguments();
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(emit(args.get(i)));
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String emitNew(Expression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(expr.getClassName());
        sb.append("(");
        List<Expression> args = expr.getArguments();
        if (args != null) {
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(emit(args.get(i)));
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String emitCast(Expression expr) {
        return "(" + expr.getTargetType() + ")" + emit(expr.getExpression());
    }

    private String emitArrayAccess(Expression expr) {
        return emit(expr.getArray()) + "[" + emit(expr.getIndex()) + "]";
    }

    private String emitTernary(Expression expr) {
        return "(" + emit(expr.getCondition()) + " ? " +
               emit(expr.getThenExpr()) + " : " +
               emit(expr.getElseExpr()) + ")";
    }

    private String escapeString(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
```

---

## 7. 完整示例

### 7.1 blueTooth.groovy 片段转换示例

**原始 QL 代码:**
```groovy
if(type == 1) {
    LogUtils.infoRule("蓝牙规则 执行 Bluetooth type is " + type);
    String resultData = (String)authAppBluetooth(appBluetoothCommandModel, appBluetoothCommandReponse);
    result.put("verify", resultData);
} else if(type == 2) {
    JSONArray resultData1 = (JSONArray)executeDoorOperate(appBluetoothCommandModel, appBluetoothCommandReponse);
    result.put("door", resultData1);
} else if(type == 3) {
    JSONArray resultData2 = (JSONArray)readDoorStatus();
    result.put("door", resultData2);
}
```

**对应的 JSON 结构:**
```json
{
  "id": "if_type_check",
  "type": "if",
  "data": {
    "label": "类型判断",
    "condition": {
      "type": "operator",
      "operator": "==",
      "left": { "type": "variable", "name": "type" },
      "right": { "type": "literal", "value": 1, "dataType": "Integer" }
    },
    "thenBranch": "then_block_1",
    "elseIfBranches": [
      {
        "condition": {
          "type": "operator",
          "operator": "==",
          "left": { "type": "variable", "name": "type" },
          "right": { "type": "literal", "value": 2, "dataType": "Integer" }
        },
        "branch": "elseif_block_1"
      },
      {
        "condition": {
          "type": "operator",
          "operator": "==",
          "left": { "type": "variable", "name": "type" },
          "right": { "type": "literal", "value": 3, "dataType": "Integer" }
        },
        "branch": "elseif_block_2"
      }
    ],
    "elseBranch": null
  }
}
```

### 7.2 类型转换示例

**原始代码:**
```groovy
AppBluetoothCommandModel aBCM = (AppBluetoothCommandModel)JSON.parseObject(aBCMStr, AppBluetoothCommandModel.class);
```

**JSON 结构:**
```json
{
  "id": "assign_aBCM",
  "type": "assignment",
  "data": {
    "label": "解析蓝牙命令",
    "variable": "aBCM",
    "variableType": "AppBluetoothCommandModel",
    "value": {
      "type": "cast",
      "targetType": "AppBluetoothCommandModel",
      "expression": {
        "type": "static",
        "className": "JSON",
        "methodName": "parseObject",
        "arguments": [
          { "type": "variable", "name": "aBCMStr" },
          {
            "type": "field_access",
            "object": { "type": "variable", "name": "AppBluetoothCommandModel" },
            "field": "class"
          }
        ]
      }
    }
  }
}
```

---

## 8. 实现计划

### Phase 1: 模型层扩展 (3天)

| 任务 | 文件 | 说明 |
|------|------|------|
| 1.1 | Expression.java | 扩展表达式类型枚举和字段 |
| 1.2 | IfNodeData.java | 添加 elseIfBranches 支持 |
| 1.3 | FunctionDefinition.java | 新建函数定义模型 |
| 1.4 | 新表达式类型 | CastExpr, NewExpr, StaticMethodExpr 等 |
| 1.5 | VisualFlowSchema.java | 添加 imports, functions 字段 |

### Phase 2: 解析器重构 (5天)

| 任务 | 文件 | 说明 |
|------|------|------|
| 2.1 | ASTToVisualVisitor.java | 新建 AST 访问器 |
| 2.2 | ExpressionParser.java | 新建表达式解析器 |
| 2.3 | StatementParser.java | 新建语句解析器 |
| 2.4 | EnhancedQLToVisualParser.java | 重构主解析器 |
| 2.5 | 递归解析 | 实现嵌套块递归解析 |

### Phase 3: 转译器扩展 (4天)

| 任务 | 文件 | 说明 |
|------|------|------|
| 3.1 | ExpressionEmitter.java | 新建表达式代码生成器 |
| 3.2 | NodeEmitter.java | 新建节点代码生成器 |
| 3.3 | EnhancedVisualToQLTranspiler.java | 扩展主转译器 |
| 3.4 | 函数生成 | 支持函数定义代码生成 |

### Phase 4: 测试与验证 (3天)

| 任务 | 说明 |
|------|------|
| 4.1 | 单元测试 | 各组件单元测试 |
| 4.2 | 集成测试 | QL→JSON→QL 往返测试 |
| 4.3 | blueTooth.groovy | 完整规则转换验证 |
| 4.4 | 边界测试 | 特殊情况处理 |

---

## 9. 风险与对策

### 9.1 技术风险

| 风险 | 影响 | 对策 |
|------|------|------|
| QLExpress4 AST 不完整 | 解析失败 | 使用 raw 表达式保底 |
| 复杂嵌套解析困难 | 结构错误 | 增加单元测试覆盖 |
| 转译后代码格式差异 | 语义不一致 | 添加语义等价性测试 |

### 9.2 回退策略

如果某些语法无法完美解析：
1. 使用 `raw` 类型保留原始文本
2. 在 JSON 中标记为"部分解析"
3. 转译时直接输出原始文本

---

## 10. 验收标准

### 10.1 功能验收

- [ ] 支持 import 语句解析和生成
- [ ] 支持函数定义解析和生成
- [ ] 支持 if/else if/else 完整结构
- [ ] 支持 for/while 循环及嵌套
- [ ] 支持类型转换表达式
- [ ] 支持 new 表达式
- [ ] 支持静态方法调用
- [ ] 支持 try-catch-finally

### 10.2 质量验收

- [ ] blueTooth.groovy 完整解析成功
- [ ] QL→JSON→QL 往返后语义等价
- [ ] 单元测试覆盖率 > 80%
- [ ] 无内存泄漏

### 10.3 性能验收

- [ ] 358行规则解析 < 500ms
- [ ] 358行规则转译 < 200ms
- [ ] 内存占用 < 100MB
