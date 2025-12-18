package com.ql.qlexpress.web.model.visual;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 新增Expression类型单元测试
 *
 * @author qlexpress
 */
class NewExpressionTypesTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("StaticMethodExpression测试")
    class StaticMethodExpressionTest {

        @Test
        @DisplayName("无参数静态方法调用应正确生成QL")
        void toQL_noArgs_shouldReturnCorrectly() {
            Expression expr = Expression.staticMethod("System", "currentTimeMillis", Collections.emptyList());
            assertEquals("System.currentTimeMillis()", expr.toQL());
        }

        @Test
        @DisplayName("带参数静态方法调用应正确生成QL")
        void toQL_withArgs_shouldReturnCorrectly() {
            Expression expr = Expression.staticMethod("Math", "max",
                    Arrays.asList(Expression.literal(1), Expression.literal(2)));
            assertEquals("Math.max(1, 2)", expr.toQL());
        }

        @Test
        @DisplayName("完整类名的静态方法调用应正确生成QL")
        void toQL_fullyQualified_shouldReturnCorrectly() {
            Expression expr = Expression.staticMethod("java.lang.Math", "abs",
                    Collections.singletonList(Expression.literal(-5)));
            assertEquals("java.lang.Math.abs(-5)", expr.toQL());
        }

        @Test
        @DisplayName("静态方法表达式应能正确序列化和反序列化")
        void serialization_shouldPreserveData() throws Exception {
            Expression original = Expression.staticMethod("Math", "max",
                    Arrays.asList(Expression.literal(1), Expression.literal(2)));

            String json = objectMapper.writeValueAsString(original);
            Expression deserialized = objectMapper.readValue(json, Expression.class);

            assertInstanceOf(StaticMethodExpression.class, deserialized);
            assertEquals(original.toQL(), deserialized.toQL());
        }
    }

    @Nested
    @DisplayName("NewExpression测试")
    class NewExpressionTest {

        @Test
        @DisplayName("无参数new表达式应正确生成QL")
        void toQL_noArgs_shouldReturnCorrectly() {
            Expression expr = Expression.newInstance("ArrayList", Collections.emptyList());
            assertEquals("new ArrayList()", expr.toQL());
        }

        @Test
        @DisplayName("带参数new表达式应正确生成QL")
        void toQL_withArgs_shouldReturnCorrectly() {
            Expression expr = Expression.newInstance("StringBuilder",
                    Collections.singletonList(Expression.literal("hello")));
            assertEquals("new StringBuilder(\"hello\")", expr.toQL());
        }

        @Test
        @DisplayName("多参数new表达式应正确生成QL")
        void toQL_multiArgs_shouldReturnCorrectly() {
            Expression expr = Expression.newInstance("HashMap",
                    Arrays.asList(Expression.literal(16), Expression.literal(0.75)));
            String ql = expr.toQL();
            assertTrue(ql.contains("new HashMap(16"));
        }

        @Test
        @DisplayName("带泛型的new表达式应正确生成QL")
        void toQL_withGeneric_shouldReturnCorrectly() {
            Expression expr = Expression.newInstance("ArrayList<String>", Collections.emptyList());
            assertEquals("new ArrayList<String>()", expr.toQL());
        }

        @Test
        @DisplayName("new表达式应能正确序列化和反序列化")
        void serialization_shouldPreserveData() throws Exception {
            Expression original = Expression.newInstance("ArrayList", Collections.emptyList());

            String json = objectMapper.writeValueAsString(original);
            Expression deserialized = objectMapper.readValue(json, Expression.class);

            assertInstanceOf(NewExpression.class, deserialized);
            assertEquals(original.toQL(), deserialized.toQL());
        }
    }

    @Nested
    @DisplayName("CastExpression测试")
    class CastExpressionTest {

        @Test
        @DisplayName("基本类型转换应正确生成QL")
        void toQL_primitiveType_shouldReturnCorrectly() {
            Expression expr = Expression.cast("int", Expression.variable("x"));
            assertEquals("(int)x", expr.toQL());
        }

        @Test
        @DisplayName("对象类型转换应正确生成QL")
        void toQL_objectType_shouldReturnCorrectly() {
            Expression expr = Expression.cast("String", Expression.variable("obj"));
            assertEquals("(String)obj", expr.toQL());
        }

        @Test
        @DisplayName("泛型类型转换应正确生成QL")
        void toQL_genericType_shouldReturnCorrectly() {
            Expression expr = Expression.cast("List<String>", Expression.variable("collection"));
            assertEquals("(List<String>)collection", expr.toQL());
        }

        @Test
        @DisplayName("嵌套表达式的类型转换应正确生成QL")
        void toQL_nestedExpression_shouldReturnCorrectly() {
            Expression inner = Expression.method(Expression.variable("obj"), "getValue", Collections.emptyList());
            Expression expr = Expression.cast("Integer", inner);
            assertEquals("(Integer)obj.getValue()", expr.toQL());
        }

        @Test
        @DisplayName("类型转换表达式应能正确序列化和反序列化")
        void serialization_shouldPreserveData() throws Exception {
            Expression original = Expression.cast("String", Expression.variable("obj"));

            String json = objectMapper.writeValueAsString(original);
            Expression deserialized = objectMapper.readValue(json, Expression.class);

            assertInstanceOf(CastExpression.class, deserialized);
            assertEquals(original.toQL(), deserialized.toQL());
        }
    }

    @Nested
    @DisplayName("FieldAccessExpression测试")
    class FieldAccessExpressionTest {

        @Test
        @DisplayName("简单字段访问应正确生成QL")
        void toQL_simpleField_shouldReturnCorrectly() {
            Expression expr = Expression.fieldAccess(Expression.variable("obj"), "name");
            assertEquals("obj.name", expr.toQL());
        }

        @Test
        @DisplayName("链式字段访问应正确生成QL")
        void toQL_chainedField_shouldReturnCorrectly() {
            Expression inner = Expression.fieldAccess(Expression.variable("obj"), "data");
            Expression expr = Expression.fieldAccess(inner, "value");
            assertEquals("obj.data.value", expr.toQL());
        }

        @Test
        @DisplayName("方法返回值的字段访问应正确生成QL")
        void toQL_methodResultField_shouldReturnCorrectly() {
            Expression method = Expression.method(Expression.variable("list"), "get",
                    Collections.singletonList(Expression.literal(0)));
            Expression expr = Expression.fieldAccess(method, "name");
            assertEquals("list.get(0).name", expr.toQL());
        }

        @Test
        @DisplayName("字段访问表达式应能正确序列化和反序列化")
        void serialization_shouldPreserveData() throws Exception {
            Expression original = Expression.fieldAccess(Expression.variable("obj"), "name");

            String json = objectMapper.writeValueAsString(original);
            Expression deserialized = objectMapper.readValue(json, Expression.class);

            assertInstanceOf(FieldAccessExpression.class, deserialized);
            assertEquals(original.toQL(), deserialized.toQL());
        }
    }

    @Nested
    @DisplayName("ArrayAccessExpression测试")
    class ArrayAccessExpressionTest {

        @Test
        @DisplayName("简单数组访问应正确生成QL")
        void toQL_simpleIndex_shouldReturnCorrectly() {
            Expression expr = Expression.arrayAccess(Expression.variable("arr"), Expression.literal(0));
            assertEquals("arr[0]", expr.toQL());
        }

        @Test
        @DisplayName("变量索引的数组访问应正确生成QL")
        void toQL_variableIndex_shouldReturnCorrectly() {
            Expression expr = Expression.arrayAccess(Expression.variable("arr"), Expression.variable("i"));
            assertEquals("arr[i]", expr.toQL());
        }

        @Test
        @DisplayName("表达式索引的数组访问应正确生成QL")
        void toQL_expressionIndex_shouldReturnCorrectly() {
            Expression index = Expression.operator("-", Expression.variable("len"), Expression.literal(1));
            Expression expr = Expression.arrayAccess(Expression.variable("arr"), index);
            String ql = expr.toQL();
            assertTrue(ql.contains("arr["));
            assertTrue(ql.contains("len"));
        }

        @Test
        @DisplayName("多维数组访问应正确生成QL")
        void toQL_multiDimensional_shouldReturnCorrectly() {
            Expression firstAccess = Expression.arrayAccess(Expression.variable("matrix"), Expression.literal(0));
            Expression expr = Expression.arrayAccess(firstAccess, Expression.literal(1));
            assertEquals("matrix[0][1]", expr.toQL());
        }

        @Test
        @DisplayName("数组访问表达式应能正确序列化和反序列化")
        void serialization_shouldPreserveData() throws Exception {
            Expression original = Expression.arrayAccess(Expression.variable("arr"), Expression.literal(0));

            String json = objectMapper.writeValueAsString(original);
            Expression deserialized = objectMapper.readValue(json, Expression.class);

            assertInstanceOf(ArrayAccessExpression.class, deserialized);
            assertEquals(original.toQL(), deserialized.toQL());
        }
    }

    @Nested
    @DisplayName("TernaryExpression测试")
    class TernaryExpressionTest {

        @Test
        @DisplayName("简单三元表达式应正确生成QL")
        void toQL_simple_shouldReturnCorrectly() {
            Expression expr = Expression.ternary(
                    Expression.operator(">", Expression.variable("x"), Expression.literal(0)),
                    Expression.literal("positive"),
                    Expression.literal("non-positive"));
            String ql = expr.toQL();
            assertTrue(ql.contains("?"));
            assertTrue(ql.contains(":"));
            assertTrue(ql.contains("positive"));
        }

        @Test
        @DisplayName("嵌套条件的三元表达式应正确生成QL")
        void toQL_nestedCondition_shouldReturnCorrectly() {
            Expression cond = Expression.operator("&&",
                    Expression.operator(">", Expression.variable("x"), Expression.literal(0)),
                    Expression.operator("<", Expression.variable("x"), Expression.literal(100)));
            Expression expr = Expression.ternary(cond,
                    Expression.literal("in range"),
                    Expression.literal("out of range"));
            String ql = expr.toQL();
            assertTrue(ql.contains("?"));
            assertTrue(ql.contains(":"));
        }

        @Test
        @DisplayName("复杂分支的三元表达式应正确生成QL")
        void toQL_complexBranches_shouldReturnCorrectly() {
            Expression expr = Expression.ternary(
                    Expression.variable("flag"),
                    Expression.method(Expression.variable("str"), "toUpperCase", Collections.emptyList()),
                    Expression.method(Expression.variable("str"), "toLowerCase", Collections.emptyList()));
            String ql = expr.toQL();
            assertTrue(ql.contains("?"));
            assertTrue(ql.contains(":"));
            assertTrue(ql.contains("toUpperCase"));
            assertTrue(ql.contains("toLowerCase"));
        }

        @Test
        @DisplayName("三元表达式应能正确序列化和反序列化")
        void serialization_shouldPreserveData() throws Exception {
            Expression original = Expression.ternary(
                    Expression.variable("flag"),
                    Expression.literal("yes"),
                    Expression.literal("no"));

            String json = objectMapper.writeValueAsString(original);
            Expression deserialized = objectMapper.readValue(json, Expression.class);

            assertInstanceOf(TernaryExpression.class, deserialized);
            assertEquals(original.toQL(), deserialized.toQL());
        }
    }

    @Nested
    @DisplayName("复杂组合表达式测试")
    class ComplexCombinationTest {

        @Test
        @DisplayName("new表达式作为方法调用目标应正确生成QL")
        void toQL_newAsMethodTarget_shouldReturnCorrectly() {
            Expression newExpr = Expression.newInstance("StringBuilder",
                    Collections.singletonList(Expression.literal("hello")));
            Expression expr = Expression.method(newExpr, "toString", Collections.emptyList());
            String ql = expr.toQL();
            assertTrue(ql.contains("new StringBuilder"));
            assertTrue(ql.contains("toString"));
        }

        @Test
        @DisplayName("类型转换后的方法调用应正确生成QL")
        void toQL_castThenMethod_shouldReturnCorrectly() {
            Expression cast = Expression.cast("String", Expression.variable("obj"));
            Expression expr = Expression.method(cast, "length", Collections.emptyList());
            String ql = expr.toQL();
            assertTrue(ql.contains("(String)"));
            assertTrue(ql.contains("length"));
        }

        @Test
        @DisplayName("数组访问后的方法调用应正确生成QL")
        void toQL_arrayAccessThenMethod_shouldReturnCorrectly() {
            Expression arrayAccess = Expression.arrayAccess(Expression.variable("list"), Expression.literal(0));
            Expression expr = Expression.method(arrayAccess, "toString", Collections.emptyList());
            String ql = expr.toQL();
            assertTrue(ql.contains("list[0]"));
            assertTrue(ql.contains("toString"));
        }

        @Test
        @DisplayName("三元表达式作为参数应正确生成QL")
        void toQL_ternaryAsArgument_shouldReturnCorrectly() {
            Expression ternary = Expression.ternary(
                    Expression.variable("flag"),
                    Expression.literal(1),
                    Expression.literal(0));
            Expression expr = Expression.function("process", Collections.singletonList(ternary));
            String ql = expr.toQL();
            assertTrue(ql.contains("process"));
            assertTrue(ql.contains("?"));
        }
    }
}
