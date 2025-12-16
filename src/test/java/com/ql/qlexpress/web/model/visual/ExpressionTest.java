package com.ql.qlexpress.web.model.visual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Expression 表达式模型单元测试
 *
 * @author qlexpress
 */
class ExpressionTest {

    @Nested
    @DisplayName("字面量表达式测试")
    class LiteralExpressionTest {

        @Test
        @DisplayName("null值应转换为'null'")
        void toQL_withNull_shouldReturnNull() {
            Expression expr = Expression.literal(null);
            assertEquals("null", expr.toQL());
        }

        @Test
        @DisplayName("整数应正确转换")
        void toQL_withInteger_shouldReturnNumber() {
            Expression expr = Expression.literal(42);
            assertEquals("42", expr.toQL());
        }

        @Test
        @DisplayName("浮点数应正确转换")
        void toQL_withDouble_shouldReturnNumber() {
            Expression expr = Expression.literal(3.14);
            assertEquals("3.14", expr.toQL());
        }

        @Test
        @DisplayName("布尔值应正确转换")
        void toQL_withBoolean_shouldReturnBoolean() {
            assertEquals("true", Expression.literal(true).toQL());
            assertEquals("false", Expression.literal(false).toQL());
        }

        @Test
        @DisplayName("字符串应添加引号")
        void toQL_withString_shouldAddQuotes() {
            Expression expr = Expression.literal("hello");
            assertEquals("\"hello\"", expr.toQL());
        }

        @Test
        @DisplayName("字符串应转义特殊字符")
        void toQL_withSpecialChars_shouldEscape() {
            Expression expr = Expression.literal("hello\nworld");
            assertEquals("\"hello\\nworld\"", expr.toQL());

            expr = Expression.literal("say \"hi\"");
            assertEquals("\"say \\\"hi\\\"\"", expr.toQL());

            expr = Expression.literal("path\\to\\file");
            assertEquals("\"path\\\\to\\\\file\"", expr.toQL());
        }

        @Test
        @DisplayName("字符应添加单引号")
        void toQL_withChar_shouldAddSingleQuotes() {
            Expression expr = Expression.literal('a');
            assertEquals("'a'", expr.toQL());
        }
    }

    @Nested
    @DisplayName("变量表达式测试")
    class VariableExpressionTest {

        @Test
        @DisplayName("变量名应直接输出")
        void toQL_shouldReturnVariableName() {
            Expression expr = Expression.variable("x");
            assertEquals("x", expr.toQL());
        }

        @Test
        @DisplayName("复杂变量名应正确处理")
        void toQL_withComplexName_shouldReturnCorrectly() {
            Expression expr = Expression.variable("myVariable");
            assertEquals("myVariable", expr.toQL());

            expr = Expression.variable("_count");
            assertEquals("_count", expr.toQL());
        }
    }

    @Nested
    @DisplayName("二元操作符表达式测试")
    class OperatorExpressionTest {

        @Test
        @DisplayName("加法运算应正确转换")
        void toQL_addition_shouldReturnCorrectly() {
            Expression expr = Expression.operator("+",
                    Expression.literal(1),
                    Expression.literal(2));
            assertEquals("(1 + 2)", expr.toQL());
        }

        @Test
        @DisplayName("减法运算应正确转换")
        void toQL_subtraction_shouldReturnCorrectly() {
            Expression expr = Expression.operator("-",
                    Expression.variable("a"),
                    Expression.literal(1));
            assertEquals("(a - 1)", expr.toQL());
        }

        @Test
        @DisplayName("比较运算应正确转换")
        void toQL_comparison_shouldReturnCorrectly() {
            Expression expr = Expression.operator(">",
                    Expression.variable("x"),
                    Expression.literal(0));
            assertEquals("(x > 0)", expr.toQL());
        }

        @Test
        @DisplayName("逻辑运算应正确转换")
        void toQL_logical_shouldReturnCorrectly() {
            Expression left = Expression.operator(">", Expression.variable("a"), Expression.literal(0));
            Expression right = Expression.operator("<", Expression.variable("a"), Expression.literal(10));
            Expression expr = Expression.operator("&&", left, right);
            assertEquals("((a > 0) && (a < 10))", expr.toQL());
        }

        @Test
        @DisplayName("嵌套运算应正确转换")
        void toQL_nested_shouldReturnCorrectly() {
            // (1 + 2) * 3
            Expression add = Expression.operator("+", Expression.literal(1), Expression.literal(2));
            Expression expr = Expression.operator("*", add, Expression.literal(3));
            assertEquals("((1 + 2) * 3)", expr.toQL());
        }
    }

    @Nested
    @DisplayName("一元操作符表达式测试")
    class UnaryOperatorExpressionTest {

        @Test
        @DisplayName("逻辑非应正确转换")
        void toQL_not_shouldReturnCorrectly() {
            Expression expr = Expression.unaryOperator("!", Expression.variable("flag"));
            assertEquals("!flag", expr.toQL());
        }

        @Test
        @DisplayName("负号应正确转换")
        void toQL_negative_shouldReturnCorrectly() {
            Expression expr = Expression.unaryOperator("-", Expression.variable("x"));
            assertEquals("-x", expr.toQL());
        }
    }

    @Nested
    @DisplayName("函数调用表达式测试")
    class FunctionExpressionTest {

        @Test
        @DisplayName("无参函数调用应正确转换")
        void toQL_noArgs_shouldReturnCorrectly() {
            Expression expr = Expression.function("now", Collections.emptyList());
            assertEquals("now()", expr.toQL());
        }

        @Test
        @DisplayName("单参数函数调用应正确转换")
        void toQL_oneArg_shouldReturnCorrectly() {
            Expression expr = Expression.function("abs",
                    Collections.singletonList(Expression.literal(-5)));
            assertEquals("abs(-5)", expr.toQL());
        }

        @Test
        @DisplayName("多参数函数调用应正确转换")
        void toQL_multiArgs_shouldReturnCorrectly() {
            Expression expr = Expression.function("max",
                    Arrays.asList(
                            Expression.literal(1),
                            Expression.literal(2),
                            Expression.literal(3)
                    ));
            assertEquals("max(1, 2, 3)", expr.toQL());
        }

        @Test
        @DisplayName("嵌套函数调用应正确转换")
        void toQL_nested_shouldReturnCorrectly() {
            Expression inner = Expression.function("abs", Collections.singletonList(Expression.literal(-5)));
            Expression expr = Expression.function("max",
                    Arrays.asList(inner, Expression.literal(10)));
            assertEquals("max(abs(-5), 10)", expr.toQL());
        }
    }

    @Nested
    @DisplayName("方法调用表达式测试")
    class MethodExpressionTest {

        @Test
        @DisplayName("无参方法调用应正确转换")
        void toQL_noArgs_shouldReturnCorrectly() {
            Expression expr = Expression.method(
                    Expression.variable("str"),
                    "length",
                    Collections.emptyList());
            assertEquals("str.length()", expr.toQL());
        }

        @Test
        @DisplayName("有参方法调用应正确转换")
        void toQL_withArgs_shouldReturnCorrectly() {
            Expression expr = Expression.method(
                    Expression.variable("str"),
                    "substring",
                    Arrays.asList(Expression.literal(0), Expression.literal(5)));
            assertEquals("str.substring(0, 5)", expr.toQL());
        }

        @Test
        @DisplayName("链式方法调用应正确转换")
        void toQL_chained_shouldReturnCorrectly() {
            Expression trim = Expression.method(Expression.variable("str"), "trim", Collections.emptyList());
            Expression expr = Expression.method(trim, "toLowerCase", Collections.emptyList());
            assertEquals("str.trim().toLowerCase()", expr.toQL());
        }
    }

    @Nested
    @DisplayName("原始表达式测试")
    class RawExpressionTest {

        @Test
        @DisplayName("原始代码应直接输出")
        void toQL_shouldReturnRawCode() {
            Expression expr = Expression.raw("a[0]");
            assertEquals("a[0]", expr.toQL());
        }

        @Test
        @DisplayName("复杂原始代码应正确处理")
        void toQL_complex_shouldReturnCorrectly() {
            Expression expr = Expression.raw("map.get(\"key\")?.value");
            assertEquals("map.get(\"key\")?.value", expr.toQL());
        }
    }

    @Nested
    @DisplayName("复杂表达式组合测试")
    class ComplexExpressionTest {

        @Test
        @DisplayName("复杂嵌套表达式应正确转换")
        void toQL_complexNested_shouldReturnCorrectly() {
            // max(a + b, min(c, d))
            Expression addExpr = Expression.operator("+",
                    Expression.variable("a"),
                    Expression.variable("b"));
            Expression minExpr = Expression.function("min",
                    Arrays.asList(Expression.variable("c"), Expression.variable("d")));
            Expression expr = Expression.function("max", Arrays.asList(addExpr, minExpr));
            assertEquals("max((a + b), min(c, d))", expr.toQL());
        }

        @Test
        @DisplayName("带方法调用的复杂表达式应正确转换")
        void toQL_withMethodCall_shouldReturnCorrectly() {
            // str.trim().length() > 0
            Expression trim = Expression.method(Expression.variable("str"), "trim", Collections.emptyList());
            Expression length = Expression.method(trim, "length", Collections.emptyList());
            Expression expr = Expression.operator(">", length, Expression.literal(0));
            assertEquals("(str.trim().length() > 0)", expr.toQL());
        }
    }
}
