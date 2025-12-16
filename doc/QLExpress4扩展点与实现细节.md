# 《QLExpress4 扩展点与实现细节分析》

## 一、Express4Runner 核心API分析

基于对 `/Users/weidian/project/vdian/wd24/QLExpress` 源码的深度分析，本文档详细说明可视化编排所需的核心扩展点。

### 1.1 Express4Runner 关键方法

```java
// 源码位置: src/main/java/com/alibaba/qlexpress4/Express4Runner.java

public class Express4Runner {

    // ==================== 解析相关 ====================

    /**
     * 解析脚本为语法树 (关键方法)
     * 用于：QL → JSON 转换
     */
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

    /**
     * 解析并获取Lambda定义（含追踪信息）
     */
    public QLambdaTrace parseToLambda(String script,
                                       ExpressContext context,
                                       QLOptions qlOptions) {
        // 返回可执行Lambda及表达式追踪树
    }

    /**
     * 解析到缓存（用于编译优化）
     */
    public QCompileCache parseToDefinitionWithCache(String script) {
        // 带缓存的编译
    }

    // ==================== 执行相关 ====================

    /**
     * 执行脚本（Map上下文）
     */
    public QLResult execute(String script,
                            Map<String, Object> context,
                            QLOptions qlOptions) {
        return execute(script, new DefaultExpressContext(context), qlOptions);
    }

    /**
     * 执行脚本（ExpressContext上下文）
     */
    public QLResult execute(String script,
                            ExpressContext context,
                            QLOptions qlOptions) {
        // 核心执行逻辑
    }

    /**
     * 带别名对象执行
     */
    public QLResult executeWithAliasObjects(String script,
                                             QLOptions qlOptions,
                                             Object... objects) {
        // 支持@QLAlias注解的对象
    }

    // ==================== 函数注册 ====================

    /**
     * 添加自定义函数
     */
    public boolean addFunction(String name, CustomFunction function);

    /**
     * 添加可变参数函数
     */
    public boolean addVarArgsFunction(String name, QLFunctionalVarargs func);

    /**
     * 添加对象方法作为函数
     */
    public boolean addObjFunction(Object obj);

    /**
     * 添加静态方法作为函数
     */
    public boolean addStaticFunction(Class<?> clazz);

    // ==================== 操作符注册 ====================

    /**
     * 添加自定义二元操作符
     */
    public boolean addOperator(String operator,
                               CustomBinaryOperator customBinaryOperator);

    /**
     * 添加带优先级的操作符
     */
    public boolean addOperator(String operator,
                               CustomBinaryOperator customBinaryOperator,
                               int priority);

    /**
     * 简化的操作符注册（BiFunction）
     */
    public boolean addOperatorBiFunction(String operator,
                                          BiFunction<Object, Object, Object> function);

    // ==================== 宏定义 ====================

    /**
     * 添加宏
     */
    public boolean addMacro(String name, String expression);

    /**
     * 移除宏
     */
    public boolean removeMacro(String name);

    // ==================== 追踪相关 ====================

    /**
     * 获取表达式追踪点
     */
    public List<TracePointTree> getExpressionTracePoints(String script);
}
```

### 1.2 关键数据结构

#### 1.2.1 QLResult - 执行结果

```java
public class QLResult {
    private final QResultType resultType;  // NEXT_INSTRUCTION, RETURN, BREAK, CONTINUE, JUMP
    private final Value result;            // 结果值

    public Object getResult() {
        return result != null ? result.get() : null;
    }
}
```

#### 1.2.2 QLOptions - 执行选项

```java
public class QLOptions {
    private long timeoutMillis;        // 超时时间
    private boolean cache;             // 是否缓存编译结果
    private int maxLoopCount;          // 最大循环次数
    private boolean preciseBigDecimal; // 精确BigDecimal运算
    private boolean traceExpression;   // 启用表达式追踪
    private boolean shortCircuit;      // 短路求值

    // Builder模式构建
    public static Builder builder() { ... }
}
```

#### 1.2.3 TracePointTree - 追踪点树

```java
public class TracePointTree {
    private final TraceType type;           // 追踪类型
    private final String token;             // 表达式token
    private final List<TracePointTree> children;  // 子节点
    private final int line;                 // 行号
    private final int col;                  // 列号
    private final int position;             // 位置

    // TraceType枚举
    // 父节点: OPERATOR, FUNCTION, METHOD, FIELD, LIST, MAP, IF, RETURN, BLOCK
    // 子节点: VARIABLE, VALUE, DEFINE_FUNCTION, DEFINE_MACRO
    // 复合: PRIMARY, STATEMENT
}
```

---

## 二、ANTLR4语法树结构

### 2.1 语法规则层次 (QLParser.g4)

```
program
└── blockStatements
    └── blockStatement (多个)
        ├── localVariableDeclarationStatement  // 变量声明: int x = 1;
        ├── whileStatement                     // while循环
        ├── traditionalForStatement            // for循环
        ├── forEachStatement                   // foreach循环
        ├── functionStatement                  // 函数定义
        ├── macroStatement                     // 宏定义
        ├── breakContinueStatement             // break/continue
        ├── returnStatement                    // return
        ├── throwStatement                     // throw
        ├── expressionStatement                // 表达式语句
        └── emptyStatement                     // 空语句
```

### 2.2 核心语法节点Context类

```java
// 程序根节点
QLParser.ProgramContext

// 语句块
QLParser.BlockStatementsContext
QLParser.BlockStatementContext

// 控制流
QLParser.QlIfContext                    // if语句
QLParser.WhileStatementContext          // while循环
QLParser.TraditionalForStatementContext // for循环
QLParser.ForEachStatementContext        // foreach循环
QLParser.TryCatchStatementContext       // try-catch

// 表达式
QLParser.ExpressionContext              // 表达式基类
QLParser.PrimaryContext                 // 基础表达式
QLParser.ParExpressionContext           // 括号表达式

// 变量和赋值
QLParser.LocalVariableDeclarationStatementContext
QLParser.AssignmentContext

// 函数相关
QLParser.FunctionStatementContext       // 函数定义
QLParser.MethodCallContext              // 方法调用
QLParser.FunctionCallContext            // 函数调用

// 字面量
QLParser.LiteralContext                 // 字面量
```

### 2.3 语法树访问示例

```java
/**
 * 遍历语法树提取节点信息
 */
public void visitProgram(QLParser.ProgramContext ctx) {
    if (ctx.blockStatements() != null) {
        for (QLParser.BlockStatementContext stmt : ctx.blockStatements().blockStatement()) {
            visitStatement(stmt);
        }
    }
}

private void visitStatement(QLParser.BlockStatementContext stmt) {
    // 获取子节点的第一个元素判断类型
    ParseTree child = stmt.getChild(0);

    if (child instanceof QLParser.QlIfContext) {
        visitIf((QLParser.QlIfContext) child);
    } else if (child instanceof QLParser.TraditionalForStatementContext) {
        visitFor((QLParser.TraditionalForStatementContext) child);
    } else if (child instanceof QLParser.ExpressionStatementContext) {
        visitExpressionStatement((QLParser.ExpressionStatementContext) child);
    }
    // ... 其他类型
}

private void visitIf(QLParser.QlIfContext ctx) {
    // 条件表达式
    QLParser.ParExpressionContext condition = ctx.parExpression();

    // then分支
    QLParser.BlockStatementsContext thenBlock = ctx.blockStatements(0);

    // else分支（如果存在）
    if (ctx.blockStatements().size() > 1) {
        QLParser.BlockStatementsContext elseBlock = ctx.blockStatements(1);
    }
}
```

---

## 三、指令集架构详解

### 3.1 指令继承体系

```
QLInstruction (抽象基类)
├── execute(QContext, QLOptions) : QResult    // 执行逻辑
├── stackInput() : int                        // 消耗��元素数
├── stackOutput() : int                       // 产生栈元素数
└── println(index, depth, debug)              // 调试输出

具体实现:
├── ConstInstruction          // 常量入栈
├── LoadInstruction           // 变量加载
├── DefineLocalInstruction    // 局部变量定义
├── OperatorInstruction       // 二元操作符
├── UnaryInstruction          // 一元操作符
├── CallInstruction           // 函数调用
├── CallFunctionInstruction   // 自定义函数调用
├── MethodInvokeInstruction   // 方法调用
├── GetFieldInstruction       // 字段获取
├── JumpInstruction           // 无条件跳转
├── JumpIfInstruction         // 条件跳转
├── JumpIfPopInstruction      // 条件跳转并出栈
├── ForInstruction            // for循环
├── WhileInstruction          // while循环
├── ForEachInstruction        // foreach循环
├── TryCatchInstruction       // try-catch
├── BreakContinueInstruction  // break/continue
├── ReturnInstruction         // return
├── ThrowInstruction          // throw
├── NewListInstruction        // 创建List
├── NewMapInstruction         // 创建Map
├── NewArrayInstruction       // 创建数组
├── NewInstanceInstruction    // 创建对象
├── IndexInstruction          // 索引访问
├── SliceInstruction          // 切片
├── PopInstruction            // 出栈
├── NewScopeInstruction       // 新作用域
├── CloseScopeInstruction     // 关闭作用域
└── DefineFunctionInstruction // 函数定义
```

### 3.2 指令与可视化节点映射

| 可视化节点类型 | 对应指令 | 说明 |
|--------------|---------|------|
| start | (无) | 起始标记，不生成指令 |
| end | (无) | 结束标记，不生成指令 |
| if | JumpIfInstruction, JumpInstruction | 条件跳转 |
| for | ForInstruction | 循环指令 |
| foreach | ForEachInstruction | 遍历指令 |
| while | WhileInstruction | 条件循环 |
| expression | OperatorInstruction等 | 表达式计算 |
| assignment | DefineLocalInstruction, LoadInstruction | 变量赋值 |
| function_call | CallInstruction/CallFunctionInstruction | 函数调用 |
| method_call | MethodInvokeInstruction | 方法调用 |
| return | ReturnInstruction | 返回 |
| break | BreakContinueInstruction | 跳出循环 |
| continue | BreakContinueInstruction | 继续循环 |
| throw | ThrowInstruction | 抛出异常 |
| try | TryCatchInstruction | 异常处理 |
| variable | DefineLocalInstruction | 变量定义 |
| list | NewListInstruction | 创建列表 |
| map | NewMapInstruction | 创建Map |

### 3.3 指令执行流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        执行引擎工作流程                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  QLambdaInner.call()                                           │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────────────────────────────────┐               │
│  │  for (i = 0; i < instructions.length; i++)  │               │
│  │       │                                      │               │
│  │       ▼                                      │               │
│  │  instruction[i].execute(context, options)   │               │
│  │       │                                      │               │
│  │       ▼                                      │               │
│  │  switch (result.getResultType())            │               │
│  │       ├── NEXT_INSTRUCTION: i++              │               │
│  │       ├── JUMP: i += offset                  │               │
│  │       ├── RETURN: return result              │               │
│  ���       ├── BREAK: return result               │               │
│  │       └── CONTINUE: return result            │               │
│  └─────────────────────────────────────────────┘               │
│                                                                 │
│  FixedSizeStack 操作:                                          │
│  ┌─────────────────────────────────────────────┐               │
│  │ ConstInstruction:  push(value)     [+1]     │               │
│  │ LoadInstruction:   push(var)       [+1]     │               │
│  │ OperatorInstruction: pop,pop,push  [-1]     │               │
│  │ PopInstruction:    pop()           [-1]     │               │
│  │ CallInstruction:   pop(n),push     [1-n]    │               │
│  └─────────────────────────────────────────────┘               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、自定义函数扩展

### 4.1 CustomFunction 接口

```java
// 源码位置: src/main/java/com/alibaba/qlexpress4/runtime/function/CustomFunction.java

/**
 * 自定义函数接口
 */
public interface CustomFunction {
    /**
     * 执行函数
     *
     * @param qContext   执行上下文
     * @param parameters 参数列表
     * @return 函数返回值
     */
    Object call(QContext qContext, Parameters parameters) throws Throwable;
}
```

### 4.2 Parameters 接口

```java
/**
 * 函数参数访问接口
 */
public interface Parameters {
    /**
     * 获取参数数量
     */
    int size();

    /**
     * 获取指定位置的参数
     */
    Value get(int index);

    /**
     * 获取所有参数
     */
    List<Value> getAll();
}
```

### 4.3 函数注册方式

```java
// 方式1: Lambda表达式
runner.addFunction("myFunc", (ctx, params) -> {
    Object arg0 = params.get(0).get();
    Object arg1 = params.get(1).get();
    return doSomething(arg0, arg1);
});

// 方式2: 可变参数函数
runner.addVarArgsFunction("sum", (ctx, args) -> {
    double sum = 0;
    for (Object arg : args) {
        sum += ((Number) arg).doubleValue();
    }
    return sum;
});

// 方式3: 注解方式（对象方法）
public class MyFunctions {
    @QLFunction({"add", "plus"})
    public int add(int a, int b) {
        return a + b;
    }

    @QLFunction("greet")
    public String greet(String name) {
        return "Hello, " + name;
    }
}
runner.addObjFunction(new MyFunctions());

// 方式4: 注解方式（静态方法）
public class StaticFunctions {
    @QLFunction("multiply")
    public static int multiply(int a, int b) {
        return a * b;
    }
}
runner.addStaticFunction(StaticFunctions.class);
```

### 4.4 在可视化编排中的应用

```java
/**
 * 业务函数节点注册
 */
@Component
public class BusinessFunctionRegistry {

    @PostConstruct
    public void registerFunctions(Express4Runner runner) {
        // 注册API调用函数
        runner.addFunction("httpGet", (ctx, params) -> {
            String url = (String) params.get(0).get();
            return httpClient.get(url);
        });

        runner.addFunction("httpPost", (ctx, params) -> {
            String url = (String) params.get(0).get();
            Object body = params.get(1).get();
            return httpClient.post(url, body);
        });

        // 注册数据转换函数
        runner.addFunction("toJson", (ctx, params) -> {
            Object obj = params.get(0).get();
            return objectMapper.writeValueAsString(obj);
        });

        runner.addFunction("fromJson", (ctx, params) -> {
            String json = (String) params.get(0).get();
            String type = (String) params.get(1).get();
            return objectMapper.readValue(json, Class.forName(type));
        });

        // 注册日志函数
        runner.addFunction("log", (ctx, params) -> {
            Object message = params.get(0).get();
            log.info("[Script] {}", message);
            return null;
        });
    }
}
```

---

## 五、自定义操作符扩展

### 5.1 BinaryOperator 接口

```java
/**
 * 二元操作符接口
 */
public interface BinaryOperator extends Operator {
    String getOperator();
    int getPriority();

    Object execute(Value left, Value right,
                   QContext qContext,
                   QLOptions qlOptions,
                   ErrorReporter errorReporter);
}
```

### 5.2 操作符优先级

```java
// 源码位置: src/main/java/com/alibaba/qlexpress4/aparser/QLPrecedences.java

public class QLPrecedences {
    public static final int ASSIGN = 0;      // = += -= *= /= %= &= |= ^= <<= >>= >>>=
    public static final int TERNARY = 1;     // ?:
    public static final int OR = 2;          // || or
    public static final int AND = 3;         // && and
    public static final int BIT_OR = 4;      // |
    public static final int XOR = 5;         // ^
    public static final int BIT_AND = 6;     // &
    public static final int EQUAL = 7;       // == != <>
    public static final int COMPARE = 8;     // < <= > >= instanceof
    public static final int BIT_MOVE = 9;    // << >> >>>
    public static final int IN_LIKE = 10;    // in like
    public static final int ADD = 11;        // + -
    public static final int MULTI = 12;      // * / %
    public static final int UNARY = 13;      // ! ++ -- ~ + -
    public static final int UNARY_SUFFIX = 14; // i++ i--
    public static final int GROUP = 15;      // . ()
}
```

### 5.3 操作符注册方式

```java
// 方式1: CustomBinaryOperator接口
CustomBinaryOperator concatOp = (left, right, ctx, options, reporter) -> {
    return left.get().toString() + right.get().toString();
};
runner.addOperator("@@", concatOp, QLPrecedences.ADD);

// 方式2: BiFunction简化版
runner.addOperatorBiFunction("%%", (left, right) -> {
    return ((Number)left).doubleValue() % ((Number)right).doubleValue();
});

// 方式3: 带类型检查的操作符
runner.addOperator("between", (left, right, ctx, options, reporter) -> {
    if (!(right instanceof List)) {
        reporter.report("between操作符右侧必须是列表 [min, max]");
        return false;
    }
    List<?> range = (List<?>) right.get();
    Comparable value = (Comparable) left.get();
    Comparable min = (Comparable) range.get(0);
    Comparable max = (Comparable) range.get(1);
    return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
}, QLPrecedences.COMPARE);
```

---

## 六、QContext 上下文机制

### 6.1 QContext 接口

```java
/**
 * 执行上下文接口
 */
public interface QContext extends QScope, QRuntime {
    // 来自 QScope
    Value getSymbolValue(String name);
    void setSymbolValue(String name, Value value);
    boolean hasSymbol(String name);

    // 来自 QRuntime
    Object attachment();
    QRuntime getParentRuntime();
    CustomFunction getFunction(String name);
    QLambdaDefinition getMacro(String name);
}
```

### 6.2 上下文层次结构

```
QvmRuntime (根运行时)
├── externalSymbols: Map<String, Value>   // 外部变量
├── functionalMap: Map<String, Function>  // 自定义函数
├── operatorManager: OperatorManager      // 操作符管理器
├── timeoutTimestamp: Long               // 超时时间戳
└── tracePoints: List<TracePointTree>    // 追踪点

QvmGlobalScope (全局作用域)
├── runtime: QvmRuntime
├── symbols: Map<String, Value>
└── userDefineFunction: Map<String, Lambda>

QvmBlockScope (块级作用域)
├── parentScope: QScope
├── symbols: Map<String, Value>
└── localFunctions: Map<String, Lambda>
```

### 6.3 变量访问示例

```java
// 在自定义函数中访问上下文
runner.addFunction("getContext", (ctx, params) -> {
    // 获取变量
    Value userValue = ctx.getSymbolValue("user");
    Object user = userValue != null ? userValue.get() : null;

    // 设置变量
    ctx.setSymbolValue("result", new DataValue(computeResult()));

    // 获取附加数据（通过ExpressContext传入）
    Object attachment = ctx.attachment();

    // 调用其他函数
    CustomFunction otherFunc = ctx.getFunction("otherFunc");
    if (otherFunc != null) {
        return otherFunc.call(ctx, new SimpleParameters(Arrays.asList(arg)));
    }

    return null;
});
```

---

## 七、类型系统分析

### 7.1 Value 值包装体系

```java
/**
 * 值接口
 */
public interface Value {
    Object get();
    default Class<?> getType() { return get() != null ? get().getClass() : null; }
    default String getTypeName() { return getType() != null ? getType().getName() : "null"; }
}

// 具体实现
├── DataValue              // 不可变数据值
├── AssignableDataValue    // 可赋值数据值
├── FieldValue             // 对象字段值
├── ArrayItemValue         // 数组元素值
├── ListItemValue          // 列表元素值
└── MapItemValue           // Map值
```

### 7.2 类型推断

QLExpress4采用动态类型系统，类型推断发生在运行时：

```java
/**
 * 类型推断示例
 */
public class TypeInference {

    /**
     * 推断表达式类型（用于可视化编辑器）
     */
    public String inferType(Expression expr, TypeContext ctx) {
        switch (expr.getType()) {
            case "literal":
                return inferLiteralType(expr.getValue());

            case "variable":
                return ctx.getVariableType(expr.getVariableName());

            case "operator":
                return inferOperatorResultType(
                    expr.getOperator(),
                    inferType(expr.getLeft(), ctx),
                    inferType(expr.getRight(), ctx)
                );

            case "function":
                return getFunctionReturnType(expr.getFunctionName());

            case "method":
                return inferMethodReturnType(
                    inferType(expr.getTarget(), ctx),
                    expr.getMethodName()
                );

            default:
                return "any";
        }
    }

    private String inferLiteralType(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Integer) return "int";
        if (value instanceof Long) return "long";
        if (value instanceof Double) return "double";
        if (value instanceof String) return "String";
        if (value instanceof List) return "List";
        if (value instanceof Map) return "Map";
        return value.getClass().getSimpleName();
    }

    private String inferOperatorResultType(String op, String leftType, String rightType) {
        // 比较操作符返回boolean
        if (Arrays.asList("==", "!=", "<", ">", "<=", ">=", "&&", "||", "in", "like")
                .contains(op)) {
            return "boolean";
        }

        // 字符串拼接
        if ("+".equals(op) && ("String".equals(leftType) || "String".equals(rightType))) {
            return "String";
        }

        // 数值运算
        if (Arrays.asList("+", "-", "*", "/", "%").contains(op)) {
            return promoteNumericType(leftType, rightType);
        }

        return "any";
    }
}
```

---

## 八、编译缓存机制

### 8.1 QCompileCache 结构

```java
/**
 * 编译缓存
 */
public class QCompileCache {
    private final QLambdaDefinitionInner lambdaDefinition;
    private final List<TracePointTree> tracePoints;

    // 缓存key通常是脚本的hash
}
```

### 8.2 缓存策略

```java
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, QCompileCache> compileCacheProvider() {
        return Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(Duration.ofHours(1))
            .recordStats()
            .build();
    }
}

@Service
public class CompileCacheService {

    private final Cache<String, QCompileCache> cache;
    private final Express4Runner runner;

    public QCompileCache getOrCompile(String script) {
        String key = DigestUtils.md5Hex(script);
        return cache.get(key, k -> runner.parseToDefinitionWithCache(script));
    }

    public void invalidate(String script) {
        cache.invalidate(DigestUtils.md5Hex(script));
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public CacheStats getStats() {
        return cache.stats();
    }
}
```

---

## 九、表达式追踪机制

### 9.1 TracePointTree 使用

```java
/**
 * 获取表达式追踪信息
 */
public List<TracePointTree> getTraces(String script) {
    Express4Runner runner = new Express4Runner(InitOptions.DEFAULT);
    return runner.getExpressionTracePoints(script);
}

/**
 * 追踪点转可视化调试信息
 */
public List<DebugInfo> convertToDebugInfo(List<TracePointTree> traces) {
    List<DebugInfo> result = new ArrayList<>();

    for (TracePointTree trace : traces) {
        DebugInfo info = new DebugInfo();
        info.setType(trace.getType().name());
        info.setToken(trace.getToken());
        info.setLine(trace.getLine());
        info.setColumn(trace.getCol());

        // 递归处理子节点
        if (trace.getChildren() != null) {
            info.setChildren(convertToDebugInfo(trace.getChildren()));
        }

        result.add(info);
    }

    return result;
}
```

### 9.2 运行时追踪

```java
/**
 * 启用追踪的执行
 */
public ExecuteResult executeWithTrace(String script, Map<String, Object> context) {
    QLOptions options = QLOptions.builder()
        .traceExpression(true)
        .build();

    ExpressContext ctx = new TraceableExpressContext(context);

    QLResult result = runner.execute(script, ctx, options);

    return ExecuteResult.builder()
        .result(result.getResult())
        .traces(ctx.getTraces())
        .build();
}

/**
 * 可追踪的上下文
 */
public class TraceableExpressContext extends DefaultExpressContext {
    private final List<ExecutionTrace> traces = new ArrayList<>();

    @Override
    public void setSymbolValue(String name, Value value) {
        traces.add(new ExecutionTrace(
            System.nanoTime(),
            "SET_VARIABLE",
            name,
            value.get()
        ));
        super.setSymbolValue(name, value);
    }

    @Override
    public Value getSymbolValue(String name) {
        Value value = super.getSymbolValue(name);
        traces.add(new ExecutionTrace(
            System.nanoTime(),
            "GET_VARIABLE",
            name,
            value != null ? value.get() : null
        ));
        return value;
    }

    public List<ExecutionTrace> getTraces() {
        return traces;
    }
}
```

---

## 十、可视化编排扩展实现

### 10.1 节点注册表

```java
/**
 * 节点类型注册表
 */
@Component
public class NodeRegistry {

    private final Map<String, NodeTranspiler> transpilers = new HashMap<>();
    private final Map<String, NodeTemplate> templates = new HashMap<>();

    @PostConstruct
    public void init() {
        // 注册内置节点
        registerBuiltInNodes();

        // 扫描并注册自定义节点
        scanAndRegisterCustomNodes();
    }

    private void registerBuiltInNodes() {
        // 控制流节点
        register(new StartNodeTranspiler());
        register(new EndNodeTranspiler());
        register(new IfNodeTranspiler());
        register(new ForNodeTranspiler());
        register(new ForEachNodeTranspiler());
        register(new WhileNodeTranspiler());
        register(new BreakNodeTranspiler());
        register(new ContinueNodeTranspiler());
        register(new ReturnNodeTranspiler());
        register(new ThrowNodeTranspiler());
        register(new TryCatchNodeTranspiler());

        // 数据操作节点
        register(new VariableNodeTranspiler());
        register(new AssignmentNodeTranspiler());
        register(new ExpressionNodeTranspiler());

        // 函数调用节点
        register(new FunctionCallNodeTranspiler());
        register(new MethodCallNodeTranspiler());

        // 数据结构节点
        register(new ListNodeTranspiler());
        register(new MapNodeTranspiler());
    }

    public void register(NodeTranspiler transpiler) {
        String type = transpiler.getNodeType();
        transpilers.put(type, transpiler);
        templates.put(type, transpiler.getTemplate());
    }

    public NodeTranspiler getTranspiler(String nodeType) {
        return transpilers.get(nodeType);
    }

    public NodeTemplate getTemplate(String nodeType) {
        return templates.get(nodeType);
    }

    public List<NodeTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }
}
```

### 10.2 业务节点扩展

```java
/**
 * 自定义业务节点接口
 */
public interface BusinessNodeTranspiler extends NodeTranspiler {

    /**
     * 获取依赖的函数
     */
    List<String> getRequiredFunctions();

    /**
     * 注册节点依赖的函数
     */
    void registerFunctions(Express4Runner runner);
}

/**
 * API调用节点示例
 */
@Component
public class ApiCallNodeTranspiler implements BusinessNodeTranspiler {

    @Override
    public String getNodeType() {
        return "api_call";
    }

    @Override
    public NodeTemplate getTemplate() {
        return NodeTemplate.builder()
            .type("api_call")
            .displayName("API调用")
            .description("调用HTTP API")
            .category(NodeCategory.BUSINESS)
            .icon("🌐")
            .color("#4CAF50")
            .inputHandles(Arrays.asList(
                HandleTemplate.builder()
                    .id("input")
                    .type("target")
                    .position("top")
                    .build()
            ))
            .outputHandles(Arrays.asList(
                HandleTemplate.builder()
                    .id("output")
                    .type("source")
                    .position("bottom")
                    .label("成功")
                    .build(),
                HandleTemplate.builder()
                    .id("error")
                    .type("source")
                    .position("bottom")
                    .label("失败")
                    .build()
            ))
            .properties(Arrays.asList(
                PropertyTemplate.builder()
                    .name("endpoint")
                    .displayName("接口地址")
                    .type(PropertyType.STRING)
                    .required(true)
                    .build(),
                PropertyTemplate.builder()
                    .name("method")
                    .displayName("请求方法")
                    .type(PropertyType.SELECT)
                    .options(Arrays.asList(
                        new OptionItem("GET", "GET"),
                        new OptionItem("POST", "POST"),
                        new OptionItem("PUT", "PUT"),
                        new OptionItem("DELETE", "DELETE")
                    ))
                    .defaultValue("GET")
                    .build(),
                PropertyTemplate.builder()
                    .name("headers")
                    .displayName("请求头")
                    .type(PropertyType.CODE)
                    .build(),
                PropertyTemplate.builder()
                    .name("body")
                    .displayName("请求体")
                    .type(PropertyType.EXPRESSION)
                    .build(),
                PropertyTemplate.builder()
                    .name("resultVariable")
                    .displayName("结果变量")
                    .type(PropertyType.VARIABLE)
                    .build()
            ))
            .build();
    }

    @Override
    public NodeTranspileResult transpile(VisualNode node, TranspileContext context) {
        ApiCallNodeData data = (ApiCallNodeData) node.getData();

        StringBuilder code = new StringBuilder();

        // 生成try-catch包装
        code.append("try {\n");

        // 生成API调用
        if (data.getResultVariable() != null) {
            code.append("    ").append(data.getResultVariable()).append(" = ");
        } else {
            code.append("    ");
        }

        code.append("__httpCall(\"")
            .append(data.getMethod())
            .append("\", \"")
            .append(data.getEndpoint())
            .append("\"");

        if (data.getHeaders() != null) {
            code.append(", ").append(data.getHeaders());
        }

        if (data.getBody() != null) {
            code.append(", ").append(data.getBody().toQL());
        }

        code.append(");\n");
        code.append("}");

        return NodeTranspileResult.builder()
            .code(code.toString())
            .hasCustomFlow(true)
            .childGenerator((graph, sb, ctx, visited, depth) -> {
                // 成功分支
                VisualEdge successEdge = graph.getOutEdge(node.getId(), "output");
                if (successEdge != null) {
                    // 在try块内处理
                }

                // 错误分支
                VisualEdge errorEdge = graph.getOutEdge(node.getId(), "error");
                if (errorEdge != null) {
                    appendLine(sb, " catch (Exception __e) {", depth, ctx);
                    ctx.indent();
                    // 生成错误处理代码
                    ctx.dedent();
                    appendLine(sb, "}", depth, ctx);
                }
            })
            .build();
    }

    @Override
    public List<String> getRequiredFunctions() {
        return Arrays.asList("__httpCall");
    }

    @Override
    public void registerFunctions(Express4Runner runner) {
        runner.addFunction("__httpCall", (ctx, params) -> {
            String method = (String) params.get(0).get();
            String url = (String) params.get(1).get();
            Object headers = params.size() > 2 ? params.get(2).get() : null;
            Object body = params.size() > 3 ? params.get(3).get() : null;

            // 执行HTTP调用
            return executeHttp(method, url, headers, body);
        });
    }
}
```

### 10.3 表达式构建器

```java
/**
 * 表达式构建器
 */
public class ExpressionBuilder {

    /**
     * 从可视化表达式构建QL代码
     */
    public String build(Expression expr) {
        return expr.toQL();
    }

    /**
     * 从QL代码解析为可视化表达式
     */
    public Expression parse(String qlCode) {
        // 简单解析
        QLParser.ExpressionContext ctx = parseExpression(qlCode);
        return convertToExpression(ctx);
    }

    private Expression convertToExpression(QLParser.ExpressionContext ctx) {
        // 字面量
        if (isLiteral(ctx)) {
            return Expression.literal(parseLiteral(ctx));
        }

        // 变量引用
        if (isIdentifier(ctx)) {
            return Expression.variable(ctx.getText());
        }

        // 二元运算
        if (isBinaryOp(ctx)) {
            return Expression.operator(
                getOperator(ctx),
                convertToExpression(getLeftOperand(ctx)),
                convertToExpression(getRightOperand(ctx))
            );
        }

        // 一元运算
        if (isUnaryOp(ctx)) {
            return Expression.unary(
                getOperator(ctx),
                convertToExpression(getOperand(ctx))
            );
        }

        // 函数调用
        if (isFunctionCall(ctx)) {
            List<Expression> args = getArguments(ctx).stream()
                .map(this::convertToExpression)
                .collect(Collectors.toList());
            return Expression.function(getFunctionName(ctx), args);
        }

        // 方法调用
        if (isMethodCall(ctx)) {
            List<Expression> args = getArguments(ctx).stream()
                .map(this::convertToExpression)
                .collect(Collectors.toList());
            return Expression.method(
                convertToExpression(getTarget(ctx)),
                getMethodName(ctx),
                args
            );
        }

        // 三元运算
        if (isTernary(ctx)) {
            return Expression.ternary(
                convertToExpression(getCondition(ctx)),
                convertToExpression(getThenExpr(ctx)),
                convertToExpression(getElseExpr(ctx))
            );
        }

        // 索引访问
        if (isIndexAccess(ctx)) {
            return Expression.index(
                convertToExpression(getTarget(ctx)),
                convertToExpression(getIndex(ctx))
            );
        }

        // 无法识别，返回原始代码
        return Expression.raw(ctx.getText());
    }

    /**
     * 验证表达式语法
     */
    public ValidationResult validate(Expression expr) {
        try {
            String ql = build(expr);
            // 尝试解析验证语法
            parseExpression(ql);
            return ValidationResult.valid();
        } catch (Exception e) {
            return ValidationResult.invalid(e.getMessage());
        }
    }
}
```

---

## 十一、性能优化建议

### 11.1 编译缓存

```java
/**
 * 基于脚本哈希的缓存策略
 */
@Service
public class OptimizedTranspileService {

    private final Cache<String, TranspileResult> transpileCache;
    private final Cache<String, QCompileCache> compileCache;

    public String transpileToQL(VisualFlowSchema flow) {
        // 计算流程的哈希值
        String flowHash = calculateHash(flow);

        // 尝试从缓存获取
        TranspileResult cached = transpileCache.getIfPresent(flowHash);
        if (cached != null) {
            return cached.getScript();
        }

        // 执行转译
        String script = doTranspile(flow);

        // 缓存结果
        transpileCache.put(flowHash, new TranspileResult(script));

        // 预编译并缓存
        compileCache.put(DigestUtils.md5Hex(script),
            runner.parseToDefinitionWithCache(script));

        return script;
    }

    private String calculateHash(VisualFlowSchema flow) {
        // 计算流程结构的哈希
        return DigestUtils.md5Hex(
            objectMapper.writeValueAsString(flow)
        );
    }
}
```

### 11.2 增量转译

```java
/**
 * 增量转译优化
 */
@Service
public class IncrementalTranspileService {

    private final Map<String, NodeTranspileCache> nodeCache = new ConcurrentHashMap<>();

    public String transpileIncremental(VisualFlowSchema flow,
                                        List<String> changedNodeIds) {
        // 如果没有变更，返回缓存结果
        if (changedNodeIds.isEmpty()) {
            return getCachedScript(flow);
        }

        // 找出受影响的节点
        Set<String> affectedNodes = findAffectedNodes(flow, changedNodeIds);

        // 只重新转译受影响的部分
        for (String nodeId : affectedNodes) {
            VisualNode node = findNode(flow, nodeId);
            NodeTranspileCache cached = transpileNode(node);
            nodeCache.put(nodeId, cached);
        }

        // 重新组装完整脚本
        return assembleScript(flow);
    }

    private Set<String> findAffectedNodes(VisualFlowSchema flow,
                                           List<String> changedNodeIds) {
        Set<String> affected = new HashSet<>(changedNodeIds);

        // 添加下游依赖节点
        for (String nodeId : changedNodeIds) {
            affected.addAll(findDownstreamNodes(flow, nodeId));
        }

        return affected;
    }
}
```

### 11.3 并行验证

```java
/**
 * 并行验证大型流程
 */
@Service
public class ParallelValidateService {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public ValidateFlowResponse validateParallel(VisualFlowSchema flow) {
        List<CompletableFuture<List<NodeError>>> nodeFutures = flow.getNodes().stream()
            .map(node -> CompletableFuture.supplyAsync(
                () -> validateNode(node), executor))
            .collect(Collectors.toList());

        List<CompletableFuture<List<EdgeError>>> edgeFutures = flow.getEdges().stream()
            .map(edge -> CompletableFuture.supplyAsync(
                () -> validateEdge(edge, flow), executor))
            .collect(Collectors.toList());

        // 等待所有验证完成
        List<NodeError> nodeErrors = nodeFutures.stream()
            .flatMap(f -> f.join().stream())
            .collect(Collectors.toList());

        List<EdgeError> edgeErrors = edgeFutures.stream()
            .flatMap(f -> f.join().stream())
            .collect(Collectors.toList());

        return ValidateFlowResponse.builder()
            .valid(nodeErrors.isEmpty() && edgeErrors.isEmpty())
            .nodeErrors(nodeErrors)
            .edgeErrors(edgeErrors)
            .build();
    }
}
```

---

## 十二、总结

### 12.1 关键扩展点

| 扩展点 | 类/接口 | 用途 |
|-------|--------|------|
| 语法树解析 | Express4Runner.parseToSyntaxTree() | QL→JSON转换 |
| 脚本执行 | Express4Runner.execute() | 流程执行 |
| 自定义函数 | CustomFunction | 业务节点功能 |
| 自定义操作符 | BinaryOperator | 自定义运算 |
| 表达式追踪 | TracePointTree | 调试支持 |
| 编译缓存 | QCompileCache | 性能优化 |
| 上下文管理 | QContext | 变量和作用域 |

### 12.2 实现要点

1. **转译器实现**
   - 使用访问者模式遍历可视化节点
   - 基于拓扑排序生成代码
   - 维护源映射用于调试

2. **反向解析**
   - 利用ANTLR4的语法树
   - 递归转换为可视化模型
   - 处理不支持的语法

3. **类型检查**
   - 运行时动态类型
   - 前端软类型检查
   - 基于元数据的验证

4. **安全执行**
   - 沙箱模式
   - 超时控制
   - 循环限制

5. **性能优化**
   - 编译缓存
   - 增量转译
   - 并行验证
