package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.model.enums.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 元数据服务
 *
 * @author qlexpress
 */
@Slf4j
@Service
public class MetadataService {

    private static final String VERSION = "1.0.0";

    private List<OperatorMetadata> operators;
    private List<FunctionMetadata> builtInFunctions;
    private List<KeywordMetadata> keywords;
    private List<TypeMetadata> types;
    private List<NodeTemplate> nodeTemplates;
    private List<CategoryInfo> categories;

    @PostConstruct
    public void init() {
        log.info("初始化元数据服务...");
        this.operators = initOperators();
        this.builtInFunctions = initBuiltInFunctions();
        this.keywords = initKeywords();
        this.types = initTypes();
        this.nodeTemplates = initNodeTemplates();
        this.categories = initCategories();
        log.info("元数据服务初始化完成，操作符:{}个，内置函数:{}个，关键字:{}个，类型:{}个，节点模板:{}个",
                operators.size(), builtInFunctions.size(), keywords.size(), types.size(), nodeTemplates.size());
    }

    /**
     * 获取所有元数据
     */
    public MetadataResponse getAllMetadata() {
        return MetadataResponse.builder()
                .version(VERSION)
                .operators(operators)
                .builtInFunctions(builtInFunctions)
                .customFunctions(new ArrayList<>())
                .keywords(keywords)
                .types(types)
                .nodeTemplates(nodeTemplates)
                .categories(categories)
                .build();
    }

    /**
     * 获取操作符列表
     */
    public List<OperatorMetadata> getOperators(String category) {
        if (category == null || category.isEmpty()) {
            return operators;
        }
        List<OperatorMetadata> result = new ArrayList<>();
        for (OperatorMetadata op : operators) {
            if (op.getCategory() != null && op.getCategory().name().equalsIgnoreCase(category)) {
                result.add(op);
            }
        }
        return result;
    }

    /**
     * 获取函数列表
     */
    public List<FunctionMetadata> getFunctions(String category, String search) {
        List<FunctionMetadata> result = new ArrayList<>(builtInFunctions);

        if (category != null && !category.isEmpty()) {
            result.removeIf(f -> f.getCategory() == null || !f.getCategory().equalsIgnoreCase(category));
        }

        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            result.removeIf(f -> {
                boolean nameMatch = f.getPrimaryName() != null && f.getPrimaryName().toLowerCase().contains(lowerSearch);
                boolean displayNameMatch = f.getDisplayName() != null && f.getDisplayName().toLowerCase().contains(lowerSearch);
                boolean descMatch = f.getDescription() != null && f.getDescription().toLowerCase().contains(lowerSearch);
                return !(nameMatch || displayNameMatch || descMatch);
            });
        }

        return result;
    }

    /**
     * 获取节点模板列表
     */
    public List<NodeTemplate> getNodeTemplates(String category) {
        if (category == null || category.isEmpty()) {
            return nodeTemplates;
        }
        List<NodeTemplate> result = new ArrayList<>();
        for (NodeTemplate template : nodeTemplates) {
            if (template.getCategory() != null && template.getCategory().name().equalsIgnoreCase(category)) {
                result.add(template);
            }
        }
        return result;
    }

    private List<OperatorMetadata> initOperators() {
        List<OperatorMetadata> list = new ArrayList<>();

        // 算术运算符
        list.add(OperatorMetadata.builder()
                .symbol("+")
                .displayName("加法")
                .description("两个操作数相加，或字符串拼接")
                .precedence(12)
                .category(OperatorCategory.ARITHMETIC)
                .leftType("any")
                .rightType("any")
                .resultType("any")
                .examples(Arrays.asList("1 + 2", "\"hello\" + \"world\""))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("-")
                .displayName("减法")
                .description("两个操作数相减")
                .precedence(12)
                .category(OperatorCategory.ARITHMETIC)
                .leftType("number")
                .rightType("number")
                .resultType("number")
                .examples(Collections.singletonList("5 - 3"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("*")
                .displayName("乘法")
                .description("两个操作数相乘")
                .precedence(13)
                .category(OperatorCategory.ARITHMETIC)
                .leftType("number")
                .rightType("number")
                .resultType("number")
                .examples(Collections.singletonList("2 * 3"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("/")
                .displayName("除法")
                .description("两个操作数相除")
                .precedence(13)
                .category(OperatorCategory.ARITHMETIC)
                .leftType("number")
                .rightType("number")
                .resultType("number")
                .examples(Collections.singletonList("10 / 2"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("%")
                .displayName("取模")
                .description("取两个操作数相除的余数")
                .precedence(13)
                .category(OperatorCategory.ARITHMETIC)
                .leftType("number")
                .rightType("number")
                .resultType("number")
                .examples(Collections.singletonList("10 % 3"))
                .build());

        // 比较运算符
        list.add(OperatorMetadata.builder()
                .symbol("==")
                .displayName("等于")
                .description("判断两个操作数是否相等")
                .precedence(9)
                .category(OperatorCategory.COMPARISON)
                .leftType("any")
                .rightType("any")
                .resultType("boolean")
                .examples(Arrays.asList("a == b", "1 == 1"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("!=")
                .displayName("不等于")
                .description("判断两个操作数是否不相等")
                .precedence(9)
                .category(OperatorCategory.COMPARISON)
                .leftType("any")
                .rightType("any")
                .resultType("boolean")
                .examples(Collections.singletonList("a != b"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol(">")
                .displayName("大于")
                .description("判断左操作数是否大于右操作数")
                .precedence(10)
                .category(OperatorCategory.COMPARISON)
                .leftType("number")
                .rightType("number")
                .resultType("boolean")
                .examples(Collections.singletonList("5 > 3"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("<")
                .displayName("小于")
                .description("判断左操作数是否小于右操作数")
                .precedence(10)
                .category(OperatorCategory.COMPARISON)
                .leftType("number")
                .rightType("number")
                .resultType("boolean")
                .examples(Collections.singletonList("3 < 5"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol(">=")
                .displayName("大于等于")
                .description("判断左操作数是否大于等于右操作数")
                .precedence(10)
                .category(OperatorCategory.COMPARISON)
                .leftType("number")
                .rightType("number")
                .resultType("boolean")
                .examples(Collections.singletonList("5 >= 5"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("<=")
                .displayName("小于等于")
                .description("判断左操作数是否小于等于右操作数")
                .precedence(10)
                .category(OperatorCategory.COMPARISON)
                .leftType("number")
                .rightType("number")
                .resultType("boolean")
                .examples(Collections.singletonList("3 <= 5"))
                .build());

        // 逻辑运算符
        list.add(OperatorMetadata.builder()
                .symbol("&&")
                .displayName("逻辑与")
                .description("两个条件都为真时返回真")
                .precedence(5)
                .category(OperatorCategory.LOGICAL)
                .leftType("boolean")
                .rightType("boolean")
                .resultType("boolean")
                .examples(Arrays.asList("a && b", "x > 0 && x < 10"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("||")
                .displayName("逻辑或")
                .description("两个条件至少一个为真时返回真")
                .precedence(4)
                .category(OperatorCategory.LOGICAL)
                .leftType("boolean")
                .rightType("boolean")
                .resultType("boolean")
                .examples(Arrays.asList("a || b", "x < 0 || x > 10"))
                .build());

        list.add(OperatorMetadata.builder()
                .symbol("!")
                .displayName("逻辑非")
                .description("取反，真变假，假变真")
                .precedence(15)
                .category(OperatorCategory.LOGICAL)
                .rightType("boolean")
                .resultType("boolean")
                .unary(true)
                .examples(Collections.singletonList("!flag"))
                .build());

        // 赋值运算符
        list.add(OperatorMetadata.builder()
                .symbol("=")
                .displayName("赋值")
                .description("将右边的值赋给左边的变量")
                .precedence(2)
                .category(OperatorCategory.ASSIGNMENT)
                .leftType("variable")
                .rightType("any")
                .resultType("any")
                .examples(Collections.singletonList("a = 10"))
                .build());

        return list;
    }

    private List<FunctionMetadata> initBuiltInFunctions() {
        List<FunctionMetadata> list = new ArrayList<>();

        // 数学函数
        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("max"))
                .primaryName("max")
                .displayName("最大值")
                .description("返回参数中的最大值")
                .category("数学函数")
                .returnType("number")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("values")
                                .type("number")
                                .required(true)
                                .description("数值列表")
                                .build()
                ))
                .varargs(true)
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("max(1, 2, 3)")
                                .expectedOutput(3)
                                .description("返回3")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("min"))
                .primaryName("min")
                .displayName("最小值")
                .description("返回参数中的最小值")
                .category("数学函数")
                .returnType("number")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("values")
                                .type("number")
                                .required(true)
                                .description("数值列表")
                                .build()
                ))
                .varargs(true)
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("min(1, 2, 3)")
                                .expectedOutput(1)
                                .description("返回1")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("abs"))
                .primaryName("abs")
                .displayName("绝对值")
                .description("返回数值的绝对值")
                .category("数学函数")
                .returnType("number")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("value")
                                .type("number")
                                .required(true)
                                .description("数值")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("abs(-5)")
                                .expectedOutput(5)
                                .description("返回5")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("round"))
                .primaryName("round")
                .displayName("四舍五入")
                .description("对数值进行四舍五入")
                .category("数学函数")
                .returnType("long")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("value")
                                .type("number")
                                .required(true)
                                .description("数值")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("round(3.6)")
                                .expectedOutput(4L)
                                .description("返回4")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        // 字符串函数
        list.add(FunctionMetadata.builder()
                .names(Arrays.asList("length", "len"))
                .primaryName("length")
                .displayName("长度")
                .description("返回字符串或集合的长度")
                .category("字符串函数")
                .returnType("int")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("value")
                                .type("any")
                                .required(true)
                                .description("字符串或集合")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("length(\"hello\")")
                                .expectedOutput(5)
                                .description("返回5")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("trim"))
                .primaryName("trim")
                .displayName("去除空白")
                .description("去除字符串两端的空白字符")
                .category("字符串函数")
                .returnType("String")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("value")
                                .type("String")
                                .required(true)
                                .description("字符串")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("trim(\"  hello  \")")
                                .expectedOutput("hello")
                                .description("返回\"hello\"")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("substring"))
                .primaryName("substring")
                .displayName("截取子串")
                .description("截取字符串的一部分")
                .category("字符串函数")
                .returnType("String")
                .parameters(Arrays.asList(
                        ParameterMetadata.builder()
                                .name("str")
                                .type("String")
                                .required(true)
                                .description("原字符串")
                                .build(),
                        ParameterMetadata.builder()
                                .name("beginIndex")
                                .type("int")
                                .required(true)
                                .description("起始索引")
                                .build(),
                        ParameterMetadata.builder()
                                .name("endIndex")
                                .type("int")
                                .required(false)
                                .description("结束索引")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("substring(\"hello\", 1, 3)")
                                .expectedOutput("el")
                                .description("返回\"el\"")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        // 类型转换函数
        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("toInt"))
                .primaryName("toInt")
                .displayName("转整数")
                .description("将值转换为整数")
                .category("类型转换")
                .returnType("int")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("value")
                                .type("any")
                                .required(true)
                                .description("要转换的值")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("toInt(\"123\")")
                                .expectedOutput(123)
                                .description("返回123")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("toDouble"))
                .primaryName("toDouble")
                .displayName("转浮点数")
                .description("将值转换为浮点数")
                .category("类型转换")
                .returnType("double")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("value")
                                .type("any")
                                .required(true)
                                .description("要转换的值")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("toDouble(\"3.14\")")
                                .expectedOutput(3.14)
                                .description("返回3.14")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("toString"))
                .primaryName("toString")
                .displayName("转字符串")
                .description("将值转换为字符串")
                .category("类型转换")
                .returnType("String")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("value")
                                .type("any")
                                .required(true)
                                .description("要转换的值")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("toString(123)")
                                .expectedOutput("123")
                                .description("返回\"123\"")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        // 集合函数
        list.add(FunctionMetadata.builder()
                .names(Collections.singletonList("size"))
                .primaryName("size")
                .displayName("集合大小")
                .description("返回集合的元素个数")
                .category("集合函数")
                .returnType("int")
                .parameters(Collections.singletonList(
                        ParameterMetadata.builder()
                                .name("collection")
                                .type("Collection")
                                .required(true)
                                .description("集合")
                                .build()
                ))
                .examples(Collections.singletonList(
                        FunctionExample.builder()
                                .code("size([1,2,3])")
                                .expectedOutput(3)
                                .description("返回3")
                                .build()
                ))
                .source(FunctionSource.BUILT_IN)
                .build());

        return list;
    }

    private List<KeywordMetadata> initKeywords() {
        List<KeywordMetadata> list = new ArrayList<>();

        list.add(KeywordMetadata.builder()
                .keyword("if")
                .displayName("条件判断")
                .description("条件判断语句")
                .category("控制流")
                .examples(Arrays.asList("if (a > b) { return a; }", "if (x) { ... } else { ... }"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("else")
                .displayName("否则")
                .description("条件判断的否则分支")
                .category("控制流")
                .examples(Collections.singletonList("if (a > b) { ... } else { ... }"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("for")
                .displayName("循环")
                .description("for循环语句")
                .category("控制流")
                .examples(Collections.singletonList("for (i = 0; i < 10; i = i + 1) { ... }"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("while")
                .displayName("while循环")
                .description("while循环语句")
                .category("控制流")
                .examples(Collections.singletonList("while (condition) { ... }"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("return")
                .displayName("返回")
                .description("返回语句")
                .category("控制流")
                .examples(Collections.singletonList("return result;"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("break")
                .displayName("跳出循环")
                .description("跳出当前循环")
                .category("控制流")
                .examples(Collections.singletonList("break;"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("continue")
                .displayName("继续循环")
                .description("跳过当前循环迭代，继续下一次")
                .category("控制流")
                .examples(Collections.singletonList("continue;"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("function")
                .displayName("函数定义")
                .description("定义函数")
                .category("定义")
                .examples(Collections.singletonList("function add(a, b) { return a + b; }"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("var")
                .displayName("变量声明")
                .description("声明变量")
                .category("定义")
                .examples(Collections.singletonList("var x = 10;"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("true")
                .displayName("真")
                .description("布尔值真")
                .category("字面量")
                .examples(Collections.singletonList("flag = true;"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("false")
                .displayName("假")
                .description("布尔值假")
                .category("字面量")
                .examples(Collections.singletonList("flag = false;"))
                .build());

        list.add(KeywordMetadata.builder()
                .keyword("null")
                .displayName("空")
                .description("空值")
                .category("字面量")
                .examples(Collections.singletonList("obj = null;"))
                .build());

        return list;
    }

    private List<TypeMetadata> initTypes() {
        List<TypeMetadata> list = new ArrayList<>();

        list.add(TypeMetadata.builder()
                .name("int")
                .displayName("整数")
                .description("32位整数类型")
                .primitive(true)
                .methods(Arrays.asList("toString()", "compareTo()"))
                .convertibleTo(Arrays.asList("long", "double", "String"))
                .build());

        list.add(TypeMetadata.builder()
                .name("long")
                .displayName("长整数")
                .description("64位整数类型")
                .primitive(true)
                .methods(Arrays.asList("toString()", "compareTo()"))
                .convertibleTo(Arrays.asList("double", "String"))
                .build());

        list.add(TypeMetadata.builder()
                .name("double")
                .displayName("浮点数")
                .description("64位浮点数类型")
                .primitive(true)
                .methods(Arrays.asList("toString()", "compareTo()"))
                .convertibleTo(Collections.singletonList("String"))
                .build());

        list.add(TypeMetadata.builder()
                .name("boolean")
                .displayName("布尔")
                .description("布尔类型")
                .primitive(true)
                .methods(Collections.singletonList("toString()"))
                .convertibleTo(Collections.singletonList("String"))
                .build());

        list.add(TypeMetadata.builder()
                .name("String")
                .displayName("字符串")
                .description("字符串类型")
                .primitive(false)
                .methods(Arrays.asList("length()", "substring()", "trim()", "toUpperCase()", "toLowerCase()"))
                .convertibleTo(Collections.emptyList())
                .build());

        list.add(TypeMetadata.builder()
                .name("List")
                .displayName("列表")
                .description("列表类型")
                .primitive(false)
                .methods(Arrays.asList("size()", "get()", "add()", "remove()", "contains()"))
                .convertibleTo(Collections.emptyList())
                .build());

        list.add(TypeMetadata.builder()
                .name("Map")
                .displayName("映射")
                .description("键值对映射类型")
                .primitive(false)
                .methods(Arrays.asList("size()", "get()", "put()", "remove()", "containsKey()"))
                .convertibleTo(Collections.emptyList())
                .build());

        return list;
    }

    private List<NodeTemplate> initNodeTemplates() {
        List<NodeTemplate> list = new ArrayList<>();

        // 开始节点
        list.add(NodeTemplate.builder()
                .type("start")
                .displayName("开始")
                .description("流程开始节点")
                .category(NodeCategory.CONTROL_FLOW)
                .icon("play")
                .color("#52c41a")
                .defaultWidth(80)
                .defaultHeight(40)
                .inputHandles(Collections.emptyList())
                .outputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("out")
                                .type("source")
                                .position("bottom")
                                .label("输出")
                                .maxConnections(1)
                                .build()
                ))
                .properties(Collections.emptyList())
                .deletable(false)
                .copyable(false)
                .build());

        // 结束节点
        list.add(NodeTemplate.builder()
                .type("end")
                .displayName("结束")
                .description("流程结束节点")
                .category(NodeCategory.CONTROL_FLOW)
                .icon("stop")
                .color("#f5222d")
                .defaultWidth(80)
                .defaultHeight(40)
                .inputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("in")
                                .type("target")
                                .position("top")
                                .label("输入")
                                .maxConnections(-1)
                                .build()
                ))
                .outputHandles(Collections.emptyList())
                .properties(Collections.emptyList())
                .deletable(false)
                .copyable(false)
                .build());

        // IF条件节点
        list.add(NodeTemplate.builder()
                .type("if")
                .displayName("条件判断")
                .description("IF条件分支节点")
                .category(NodeCategory.CONTROL_FLOW)
                .icon("branch")
                .color("#1890ff")
                .defaultWidth(160)
                .defaultHeight(80)
                .inputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("in")
                                .type("target")
                                .position("top")
                                .label("输入")
                                .maxConnections(1)
                                .build()
                ))
                .outputHandles(Arrays.asList(
                        HandleTemplate.builder()
                                .id("true")
                                .type("source")
                                .position("bottom")
                                .label("是")
                                .maxConnections(1)
                                .build(),
                        HandleTemplate.builder()
                                .id("false")
                                .type("source")
                                .position("right")
                                .label("否")
                                .maxConnections(1)
                                .build()
                ))
                .properties(Collections.singletonList(
                        PropertyTemplate.builder()
                                .name("condition")
                                .displayName("条件")
                                .type(PropertyType.EXPRESSION)
                                .required(true)
                                .helpText("输入判断条件表达式")
                                .build()
                ))
                .deletable(true)
                .copyable(true)
                .build());

        // FOR循环节点
        list.add(NodeTemplate.builder()
                .type("for")
                .displayName("FOR循环")
                .description("FOR循环节点")
                .category(NodeCategory.CONTROL_FLOW)
                .icon("sync")
                .color("#722ed1")
                .defaultWidth(180)
                .defaultHeight(100)
                .inputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("in")
                                .type("target")
                                .position("top")
                                .label("输入")
                                .maxConnections(1)
                                .build()
                ))
                .outputHandles(Arrays.asList(
                        HandleTemplate.builder()
                                .id("body")
                                .type("source")
                                .position("bottom")
                                .label("循环体")
                                .maxConnections(1)
                                .build(),
                        HandleTemplate.builder()
                                .id("complete")
                                .type("source")
                                .position("right")
                                .label("完成")
                                .maxConnections(1)
                                .build()
                ))
                .properties(Arrays.asList(
                        PropertyTemplate.builder()
                                .name("init")
                                .displayName("初始化")
                                .type(PropertyType.EXPRESSION)
                                .helpText("循环初始化表达式，如: i = 0")
                                .build(),
                        PropertyTemplate.builder()
                                .name("condition")
                                .displayName("条件")
                                .type(PropertyType.EXPRESSION)
                                .required(true)
                                .helpText("循环条件，如: i < 10")
                                .build(),
                        PropertyTemplate.builder()
                                .name("update")
                                .displayName("更新")
                                .type(PropertyType.EXPRESSION)
                                .helpText("循环更新表达式，如: i = i + 1")
                                .build()
                ))
                .deletable(true)
                .copyable(true)
                .build());

        // 表达式节点
        list.add(NodeTemplate.builder()
                .type("expression")
                .displayName("表达式")
                .description("通用表达式节点")
                .category(NodeCategory.EXPRESSION)
                .icon("code")
                .color("#fa8c16")
                .defaultWidth(200)
                .defaultHeight(60)
                .inputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("in")
                                .type("target")
                                .position("top")
                                .label("输入")
                                .maxConnections(1)
                                .build()
                ))
                .outputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("out")
                                .type("source")
                                .position("bottom")
                                .label("输出")
                                .maxConnections(1)
                                .build()
                ))
                .properties(Arrays.asList(
                        PropertyTemplate.builder()
                                .name("expression")
                                .displayName("表达式")
                                .type(PropertyType.EXPRESSION)
                                .required(true)
                                .helpText("输入表达式")
                                .build(),
                        PropertyTemplate.builder()
                                .name("resultVariable")
                                .displayName("结果变量")
                                .type(PropertyType.VARIABLE)
                                .helpText("将结果赋值给变量（可选）")
                                .build()
                ))
                .deletable(true)
                .copyable(true)
                .build());

        // 函数调用节点
        list.add(NodeTemplate.builder()
                .type("function_call")
                .displayName("函数调用")
                .description("调用函数节点")
                .category(NodeCategory.FUNCTION_CALL)
                .icon("function")
                .color("#13c2c2")
                .defaultWidth(200)
                .defaultHeight(80)
                .inputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("in")
                                .type("target")
                                .position("top")
                                .label("输入")
                                .maxConnections(1)
                                .build()
                ))
                .outputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("out")
                                .type("source")
                                .position("bottom")
                                .label("输出")
                                .maxConnections(1)
                                .build()
                ))
                .properties(Arrays.asList(
                        PropertyTemplate.builder()
                                .name("functionName")
                                .displayName("函数名")
                                .type(PropertyType.STRING)
                                .required(true)
                                .helpText("输入要调用的函数名")
                                .build(),
                        PropertyTemplate.builder()
                                .name("arguments")
                                .displayName("参数")
                                .type(PropertyType.EXPRESSION)
                                .helpText("函数参数，多个参数用逗号分隔")
                                .build(),
                        PropertyTemplate.builder()
                                .name("resultVariable")
                                .displayName("结果变量")
                                .type(PropertyType.VARIABLE)
                                .helpText("将结果赋值给变量（可选）")
                                .build()
                ))
                .deletable(true)
                .copyable(true)
                .build());

        // 赋值节点
        list.add(NodeTemplate.builder()
                .type("assignment")
                .displayName("赋值")
                .description("变量赋值节点")
                .category(NodeCategory.VARIABLE)
                .icon("edit")
                .color("#eb2f96")
                .defaultWidth(180)
                .defaultHeight(60)
                .inputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("in")
                                .type("target")
                                .position("top")
                                .label("输入")
                                .maxConnections(1)
                                .build()
                ))
                .outputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("out")
                                .type("source")
                                .position("bottom")
                                .label("输出")
                                .maxConnections(1)
                                .build()
                ))
                .properties(Arrays.asList(
                        PropertyTemplate.builder()
                                .name("variable")
                                .displayName("变量名")
                                .type(PropertyType.VARIABLE)
                                .required(true)
                                .helpText("要赋值的变量名")
                                .build(),
                        PropertyTemplate.builder()
                                .name("value")
                                .displayName("值")
                                .type(PropertyType.EXPRESSION)
                                .required(true)
                                .helpText("赋给变量的值")
                                .build()
                ))
                .deletable(true)
                .copyable(true)
                .build());

        // 返回节点
        list.add(NodeTemplate.builder()
                .type("return")
                .displayName("返回")
                .description("返回语句节点")
                .category(NodeCategory.CONTROL_FLOW)
                .icon("logout")
                .color("#f5222d")
                .defaultWidth(140)
                .defaultHeight(50)
                .inputHandles(Collections.singletonList(
                        HandleTemplate.builder()
                                .id("in")
                                .type("target")
                                .position("top")
                                .label("输入")
                                .maxConnections(1)
                                .build()
                ))
                .outputHandles(Collections.emptyList())
                .properties(Collections.singletonList(
                        PropertyTemplate.builder()
                                .name("value")
                                .displayName("返回值")
                                .type(PropertyType.EXPRESSION)
                                .helpText("返回的值（可选）")
                                .build()
                ))
                .deletable(true)
                .copyable(true)
                .build());

        return list;
    }

    private List<CategoryInfo> initCategories() {
        List<CategoryInfo> list = new ArrayList<>();

        list.add(CategoryInfo.builder()
                .id("control_flow")
                .name("控制流")
                .description("流程控制相关节点")
                .icon("branch")
                .order(1)
                .build());

        list.add(CategoryInfo.builder()
                .id("expression")
                .name("表达式")
                .description("表达式相关节点")
                .icon("code")
                .order(2)
                .build());

        list.add(CategoryInfo.builder()
                .id("function_call")
                .name("函数调用")
                .description("函数调用相关节点")
                .icon("function")
                .order(3)
                .build());

        list.add(CategoryInfo.builder()
                .id("variable")
                .name("变量")
                .description("变量操作相关节点")
                .icon("edit")
                .order(4)
                .build());

        list.add(CategoryInfo.builder()
                .id("data_operation")
                .name("数据操作")
                .description("数据处理相关节点")
                .icon("database")
                .order(5)
                .build());

        return list;
    }
}
