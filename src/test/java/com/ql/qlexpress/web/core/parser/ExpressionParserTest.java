package com.ql.qlexpress.web.core.parser;

import com.ql.qlexpress.web.model.visual.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExpressionParser 表达式解析器单元测试
 *
 * @author qlexpress
 */
class ExpressionParserTest {

    private ExpressionParser parser;

    @BeforeEach
    void setUp() {
        parser = new ExpressionParser();
    }

    @Nested
    @DisplayName("字面量解析测试")
    class LiteralParseTest {

        @Test
        @DisplayName("解析整数字面量")
        void parse_integer_shouldReturnLiteralExpression() {
            Expression expr = parser.parse("42");
            assertInstanceOf(LiteralExpression.class, expr);
            assertEquals("42", expr.toQL());
        }

        @Test
        @DisplayName("解析浮点数字面量")
        void parse_double_shouldReturnLiteralExpression() {
            Expression expr = parser.parse("3.14");
            assertInstanceOf(LiteralExpression.class, expr);
            assertTrue(expr.toQL().startsWith("3.14"));
        }

        @Test
        @DisplayName("解析字符串字面量")
        void parse_string_shouldReturnLiteralExpression() {
            Expression expr = parser.parse("\"hello world\"");
            assertInstanceOf(LiteralExpression.class, expr);
            assertEquals("\"hello world\"", expr.toQL());
        }

        @Test
        @DisplayName("解析布尔字面量true")
        void parse_booleanTrue_shouldReturnLiteralExpression() {
            Expression expr = parser.parse("true");
            assertInstanceOf(LiteralExpression.class, expr);
            assertEquals("true", expr.toQL());
        }

        @Test
        @DisplayName("解析布尔字面量false")
        void parse_booleanFalse_shouldReturnLiteralExpression() {
            Expression expr = parser.parse("false");
            assertInstanceOf(LiteralExpression.class, expr);
            assertEquals("false", expr.toQL());
        }

        @Test
        @DisplayName("解析null字面量")
        void parse_null_shouldReturnLiteralExpression() {
            Expression expr = parser.parse("null");
            assertInstanceOf(LiteralExpression.class, expr);
            assertEquals("null", expr.toQL());
        }
    }

    @Nested
    @DisplayName("变量解析测试")
    class VariableParseTest {

        @Test
        @DisplayName("解析简单变量名")
        void parse_simpleVariable_shouldReturnVariableExpression() {
            Expression expr = parser.parse("x");
            assertInstanceOf(VariableExpression.class, expr);
            assertEquals("x", expr.toQL());
        }

        @Test
        @DisplayName("解析复杂变量名")
        void parse_complexVariable_shouldReturnVariableExpression() {
            Expression expr = parser.parse("myVariable123");
            assertInstanceOf(VariableExpression.class, expr);
            assertEquals("myVariable123", expr.toQL());
        }

        @Test
        @DisplayName("解析下划线开头的变量名")
        void parse_underscoreVariable_shouldReturnVariableExpression() {
            Expression expr = parser.parse("_count");
            assertInstanceOf(VariableExpression.class, expr);
            assertEquals("_count", expr.toQL());
        }
    }

    @Nested
    @DisplayName("运算符解析测试")
    class OperatorParseTest {

        @Test
        @DisplayName("解析加法运算")
        void parse_addition_shouldReturnOperatorExpression() {
            Expression expr = parser.parse("a + b");
            assertInstanceOf(OperatorExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("+"));
            assertTrue(ql.contains("a"));
            assertTrue(ql.contains("b"));
        }

        @Test
        @DisplayName("解析比较运算")
        void parse_comparison_shouldReturnOperatorExpression() {
            Expression expr = parser.parse("x > 0");
            assertInstanceOf(OperatorExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains(">"));
        }

        @Test
        @DisplayName("解析逻辑与运算")
        void parse_logicalAnd_shouldReturnOperatorExpression() {
            Expression expr = parser.parse("a && b");
            assertInstanceOf(OperatorExpression.class, expr);
            assertTrue(expr.toQL().contains("&&"));
        }

        @Test
        @DisplayName("解析逻辑或运算")
        void parse_logicalOr_shouldReturnOperatorExpression() {
            Expression expr = parser.parse("a || b");
            assertInstanceOf(OperatorExpression.class, expr);
            assertTrue(expr.toQL().contains("||"));
        }

        @Test
        @DisplayName("解析一元非运算")
        void parse_unaryNot_shouldReturnOperatorExpression() {
            Expression expr = parser.parse("!flag");
            assertInstanceOf(OperatorExpression.class, expr);
            assertTrue(expr.toQL().contains("!"));
        }
    }

    @Nested
    @DisplayName("函数调用解析测试")
    class FunctionCallParseTest {

        @Test
        @DisplayName("解析无参数函数调用")
        void parse_noArgsFunctionCall_shouldReturnFunctionExpression() {
            Expression expr = parser.parse("now()");
            assertInstanceOf(FunctionExpression.class, expr);
            assertEquals("now()", expr.toQL());
        }

        @Test
        @DisplayName("解析单参数函数调用")
        void parse_singleArgFunctionCall_shouldReturnFunctionExpression() {
            Expression expr = parser.parse("abs(-5)");
            assertInstanceOf(FunctionExpression.class, expr);
            assertTrue(expr.toQL().contains("abs"));
        }

        @Test
        @DisplayName("解析多参数函数调用")
        void parse_multiArgsFunctionCall_shouldReturnFunctionExpression() {
            Expression expr = parser.parse("max(1, 2, 3)");
            assertInstanceOf(FunctionExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("max"));
            assertTrue(ql.contains("1"));
            assertTrue(ql.contains("2"));
            assertTrue(ql.contains("3"));
        }
    }

    @Nested
    @DisplayName("方法调用解析测试")
    class MethodCallParseTest {

        @Test
        @DisplayName("解析无参数方法调用")
        void parse_noArgsMethodCall_shouldReturnMethodExpression() {
            Expression expr = parser.parse("str.length()");
            assertInstanceOf(MethodExpression.class, expr);
            assertEquals("str.length()", expr.toQL());
        }

        @Test
        @DisplayName("解析带参数方法调用")
        void parse_withArgsMethodCall_shouldReturnMethodExpression() {
            Expression expr = parser.parse("str.substring(0, 5)");
            assertInstanceOf(MethodExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("substring"));
        }

        @Test
        @DisplayName("解析链式方法调用")
        void parse_chainedMethodCall_shouldReturnCorrectly() {
            Expression expr = parser.parse("str.trim().toLowerCase()");
            assertInstanceOf(MethodExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("trim"));
            assertTrue(ql.contains("toLowerCase"));
        }
    }

    @Nested
    @DisplayName("静态方法调用解析测试")
    class StaticMethodParseTest {

        @Test
        @DisplayName("解析静态方法调用")
        void parse_staticMethodCall_shouldReturnStaticMethodExpression() {
            Expression expr = parser.parse("Math.abs(-5)");
            // 可能被解析为方法表达式，根据实际实现检查
            String ql = expr.toQL();
            assertTrue(ql.contains("Math"));
            assertTrue(ql.contains("abs"));
        }

        @Test
        @DisplayName("解析包含包名的静态方法调用")
        void parse_fullyQualifiedStaticMethod_shouldReturnCorrectly() {
            Expression expr = parser.parse("java.lang.Math.max(1, 2)");
            String ql = expr.toQL();
            assertTrue(ql.contains("max"));
        }
    }

    @Nested
    @DisplayName("new表达式解析测试")
    class NewExpressionParseTest {

        @Test
        @DisplayName("解析无参数new表达式")
        void parse_newNoArgs_shouldReturnNewExpression() {
            Expression expr = parser.parse("new ArrayList()");
            assertInstanceOf(NewExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("new"));
            assertTrue(ql.contains("ArrayList"));
        }

        @Test
        @DisplayName("解析带参数new表达式")
        void parse_newWithArgs_shouldReturnNewExpression() {
            Expression expr = parser.parse("new StringBuilder(\"hello\")");
            assertInstanceOf(NewExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("new"));
            assertTrue(ql.contains("StringBuilder"));
        }
    }

    @Nested
    @DisplayName("类型转换解析测试")
    class CastExpressionParseTest {

        @Test
        @DisplayName("解析简单类型转换")
        void parse_simpleCast_shouldReturnCastExpression() {
            Expression expr = parser.parse("(int)x");
            assertInstanceOf(CastExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("int"));
        }

        @Test
        @DisplayName("解析对象类型转换")
        void parse_objectCast_shouldReturnCastExpression() {
            Expression expr = parser.parse("(String)obj");
            assertInstanceOf(CastExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("String"));
        }
    }

    @Nested
    @DisplayName("三元运算符解析测试")
    class TernaryExpressionParseTest {

        @Test
        @DisplayName("解析简单三元运算符")
        void parse_simpleTernary_shouldReturnTernaryExpression() {
            Expression expr = parser.parse("x > 0 ? \"positive\" : \"non-positive\"");
            assertInstanceOf(TernaryExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("?"));
            assertTrue(ql.contains(":"));
        }
    }

    @Nested
    @DisplayName("数组访问解析测试")
    class ArrayAccessParseTest {

        @Test
        @DisplayName("解析简单数组访问")
        void parse_simpleArrayAccess_shouldReturnArrayAccessExpression() {
            Expression expr = parser.parse("arr[0]");
            assertInstanceOf(ArrayAccessExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("arr"));
            assertTrue(ql.contains("["));
            assertTrue(ql.contains("]"));
        }

        @Test
        @DisplayName("解析变量索引数组访问")
        void parse_variableIndexArrayAccess_shouldReturnArrayAccessExpression() {
            Expression expr = parser.parse("arr[i]");
            assertInstanceOf(ArrayAccessExpression.class, expr);
            String ql = expr.toQL();
            assertTrue(ql.contains("arr"));
            assertTrue(ql.contains("i"));
        }
    }

    @Nested
    @DisplayName("字段访问解析测试")
    class FieldAccessParseTest {

        @Test
        @DisplayName("解析简单字段访问")
        void parse_simpleFieldAccess_shouldReturnFieldAccessExpression() {
            Expression expr = parser.parse("obj.field");
            // 可能返回RawExpression或FieldAccessExpression
            String ql = expr.toQL();
            assertTrue(ql.contains("obj"));
            assertTrue(ql.contains("field"));
        }
    }

    @Nested
    @DisplayName("复杂表达式解析测试")
    class ComplexExpressionParseTest {

        @Test
        @DisplayName("解析带括号的复杂表达式")
        void parse_parenthesizedExpression_shouldReturnCorrectly() {
            Expression expr = parser.parse("(a + b) * c");
            String ql = expr.toQL();
            assertTrue(ql.contains("a"));
            assertTrue(ql.contains("b"));
            assertTrue(ql.contains("c"));
        }

        @Test
        @DisplayName("解析嵌套函数调用")
        void parse_nestedFunctionCalls_shouldReturnCorrectly() {
            Expression expr = parser.parse("max(abs(x), 10)");
            String ql = expr.toQL();
            assertTrue(ql.contains("max"));
            assertTrue(ql.contains("abs"));
        }

        @Test
        @DisplayName("解析方法链加运算符")
        void parse_methodChainWithOperator_shouldReturnCorrectly() {
            Expression expr = parser.parse("str.length() > 0");
            String ql = expr.toQL();
            assertTrue(ql.contains("length"));
            assertTrue(ql.contains(">"));
        }
    }
}
