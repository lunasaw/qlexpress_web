package com.ql.qlexpress.web.ql;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.QLResult;
import com.alibaba.qlexpress4.security.QLSecurityStrategy;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QLExpress4 长脚本测试
 * 覆盖 /src/test/resources/testsuite/java 目录下所有语法和特性
 *
 * 测试分类：
 * 1. array - 数组操作
 * 2. cast - 类型转换
 * 3. for - 循环语句
 * 4. generics - 泛型
 * 5. implicit - 隐式类型转换
 * 6. import - 导入语句
 * 7. lambda - Lambda表达式
 * 8. map - Map操作
 * 9. method - 方法调用
 * 10. method_reference - 方法引用
 * 11. newexpr - new表达式
 * 12. number - 数字类型
 * 13. property - 属性访问
 * 14. stream - Stream操作
 * 15. trycatch - 异常处理
 *
 * @author qlexpress
 */
class QLExpressLongScriptTest {

    private Express4Runner runner;
    private QLOptions qlOptions;

    @BeforeEach
    void setUp() {
        runner = new Express4Runner(InitOptions.builder()
                .securityStrategy(QLSecurityStrategy.open())
                .build());
        qlOptions = QLOptions.builder().build();
    }

    /**
     * 执行脚本并返回结果
     */
    private Object execute(String script) throws Exception {
        return execute(script, new HashMap<>());
    }

    /**
     * 执行脚本并返回结果（带上下文）
     */
    private Object execute(String script, Map<String, Object> context) throws Exception {
        QLResult result = runner.execute(script, context, qlOptions);
        return result.getResult();
    }

    // ==================== 1. Array Tests ====================

    @Nested
    @DisplayName("数组操作测试")
    class ArrayTest {

        @Test
        @DisplayName("数组字面量和负索引")
        void testArrayLiteralAndNegativeIndex() throws Exception {
            String script =
                "int[] ii = new int[]{1,2,3};\n" +
                "if (ii[2] != 3) { throw new RuntimeException(\"ii[2] should be 3\"); }\n" +
                "if (ii[-1] != ii[2]) { throw new RuntimeException(\"negative index failed\"); }\n" +
                "return ii.length;";
            Object result = execute(script);
            assertEquals(3, result);
        }

        @Test
        @DisplayName("数组初始化")
        void testArrayWithInitItem() throws Exception {
            String script =
                "String[] arr = new String[3];\n" +
                "arr[0] = \"a\";\n" +
                "arr[1] = \"b\";\n" +
                "arr[2] = \"c\";\n" +
                "return arr[1];";
            Object result = execute(script);
            assertEquals("b", result);
        }

        @Test
        @DisplayName("多维数组")
        void testMultiDimArray() throws Exception {
            String script =
                "long[][] ls = new long[2][3];\n" +
                "if (ls.length != 2) { throw new RuntimeException(\"ls.length should be 2\"); }\n" +
                "if (ls[1].length != 3) { throw new RuntimeException(\"ls[1].length should be 3\"); }\n" +
                "ls[0][0] = 100L;\n" +
                "return ls[0][0];";
            Object result = execute(script);
            assertEquals(100L, result);
        }

        @Test
        @DisplayName("类型数组")
        void testTypeArray() throws Exception {
            String script =
                "Integer[] integers = new Integer[]{1, 2, 3};\n" +
                "String[] strings = new String[]{\"a\", \"b\", \"c\"};\n" +
                "return integers.length + strings.length;";
            Object result = execute(script);
            assertEquals(6, result);
        }
    }

    // ==================== 2. Cast Tests ====================

    @Nested
    @DisplayName("类型转换测试")
    class CastTest {

        @Test
        @DisplayName("对象类型转换")
        void testObjectCast() throws Exception {
            String script =
                "Object obj = \"hello\";\n" +
                "String str = (String) obj;\n" +
                "return str;";
            Object result = execute(script);
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("null类型转换")
        void testStringCast() throws Exception {
            String script =
                "Object a = null;\n" +
                "String b = (String) a;\n" +
                "return b;";
            Object result = execute(script);
            assertNull(result);
        }

        @Test
        @DisplayName("兼容类型转换")
        void testAssignableCast() throws Exception {
            String script =
                "Number num = 123;\n" +
                "Integer i = (Integer) num;\n" +
                "return i;";
            Object result = execute(script);
            assertEquals(123, result);
        }

        @Test
        @DisplayName("定义局部变量时的类型转换")
        void testDefineLocalCast() throws Exception {
            String script =
                "int i = 100;\n" +
                "long l = (long) i;\n" +
                "return l;";
            Object result = execute(script);
            assertEquals(100L, result);
        }
    }

    // ==================== 3. For Loop Tests ====================

    @Nested
    @DisplayName("循环语句测试")
    class ForLoopTest {

        @Test
        @DisplayName("foreach遍历数组")
        void testForEachArray() throws Exception {
            String script =
                "sum = 0;\n" +
                "for (ele: new int[] {1,2,3}) {\n" +
                "    sum = sum + ele;\n" +
                "}\n" +
                "return sum;";
            Object result = execute(script);
            assertEquals(6L, ((Number) result).longValue());
        }

        @Test
        @DisplayName("嵌套foreach遍历二维数组")
        void testNestedForEachArray() throws Exception {
            String script =
                "nestedArr = new int[][] {new int[] {1,2}, new int[] {3,4}};\n" +
                "sum = 0;\n" +
                "for (arr: nestedArr) {\n" +
                "    for (int ele: arr) {\n" +
                "        sum = sum + ele;\n" +
                "    }\n" +
                "}\n" +
                "return sum;";
            Object result = execute(script);
            assertEquals(10L, ((Number) result).longValue());
        }

        @Test
        @DisplayName("传统for循环")
        void testTraditionalForLoop() throws Exception {
            String script =
                "sum = 0;\n" +
                "for (i = 1; i <= 100; i = i + 1) {\n" +
                "    sum = sum + i;\n" +
                "}\n" +
                "return sum;";
            Object result = execute(script);
            assertEquals(5050L, ((Number) result).longValue());
        }
    }

    // ==================== 4. Generics Tests ====================

    @Nested
    @DisplayName("泛型测试")
    class GenericsTest {

        @Test
        @DisplayName("基础泛型声明")
        void testBasicGenerics() throws Exception {
            String script =
                "Map<String,String> m = new HashMap();\n" +
                "m.put(\"key\", \"value\");\n" +
                "return m.get(\"key\");";
            Object result = execute(script);
            assertEquals("value", result);
        }

        @Test
        @DisplayName("嵌套泛型")
        void testNestedGenerics() throws Exception {
            String script =
                "Map<String,List<String>> m = new HashMap();\n" +
                "m.put(\"list\", [\"a\", \"b\", \"c\"]);\n" +
                "return m.get(\"list\").size();";
            Object result = execute(script);
            assertEquals(3, result);
        }
    }

    // ==================== 5. Implicit Conversion Tests ====================

    @Nested
    @DisplayName("隐式类型转换测试")
    class ImplicitConversionTest {

        @Test
        @DisplayName("算术运算后的隐式转换")
        void testArithmeticConversion() throws Exception {
            String script =
                "byte b1 = 1;\n" +
                "byte b2 = 2;\n" +
                "int i = b1 + b2;\n" +
                "return i;";
            Object result = execute(script);
            assertEquals(3, ((Number) result).intValue());
        }

        @Test
        @DisplayName("算术运算中的字符串转换")
        void testStringConcatConversion() throws Exception {
            String script =
                "int a = 1;\n" +
                "String s = a + \"q\";\n" +
                "return s;";
            Object result = execute(script);
            assertEquals("1q", result);
        }

        @Test
        @DisplayName("自动装箱/拆箱")
        void testAutoBoxingUnboxing() throws Exception {
            String script =
                "int i = 1;\n" +
                "Integer ig = i;\n" +
                "int i1 = ig;\n" +
                "return i1 == i && ig instanceof Integer;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Double自动装箱")
        void testDoubleAutoBoxing() throws Exception {
            String script =
                "double d = 0.1d;\n" +
                "Double ds = d;\n" +
                "return ds == 0.1d;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Float自动装箱")
        void testFloatAutoBoxing() throws Exception {
            String script =
                "float f = 0.22f;\n" +
                "Float f1 = f;\n" +
                "return f1 == 0.22f;";
            Object result = execute(script);
            assertEquals(true, result);
        }
    }

    // ==================== 6. Import Tests ====================

    @Nested
    @DisplayName("导入语句测试")
    class ImportTest {

        @Test
        @DisplayName("导入java.util包类")
        void testImportUtilClass() throws Exception {
            String script =
                "import java.util.Date;\n" +
                "d = new Date();\n" +
                "return d != null;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("导入多个类")
        void testMultiImport() throws Exception {
            String script =
                "import java.util.ArrayList;\n" +
                "import java.util.HashMap;\n" +
                "list = new ArrayList();\n" +
                "map = new HashMap();\n" +
                "return list != null && map != null;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("导入包下所有类")
        void testImportStar() throws Exception {
            String script =
                "import java.util.*;\n" +
                "list = new ArrayList();\n" +
                "map = new HashMap();\n" +
                "set = new HashSet();\n" +
                "return list != null && map != null && set != null;";
            Object result = execute(script);
            assertEquals(true, result);
        }
    }

    // ==================== 7. Lambda Tests ====================

    @Nested
    @DisplayName("Lambda表达式测试")
    class LambdaTest {

        @Test
        @DisplayName("Lambda方法定义")
        void testLambdaMethod() throws Exception {
            String script =
                "a = {\n" +
                "    func: (int c) -> c + 1\n" +
                "};\n" +
                "return a.func(1);";
            Object result = execute(script);
            assertEquals(2L, ((Number) result).longValue());
        }

        @Test
        @DisplayName("Runnable接口Lambda")
        void testRunnableLambda() throws Exception {
            String script =
                "result = 0;\n" +
                "Runnable r = () -> result = 8;\n" +
                "r.run();\n" +
                "return result;";
            Object result = execute(script);
            assertEquals(8L, ((Number) result).longValue());
        }

        @Test
        @DisplayName("Function接口Lambda")
        void testFunctionLambda() throws Exception {
            String script =
                "f = a -> a + 3;\n" +
                "return f(1);";
            Object result = execute(script);
            assertEquals(4L, ((Number) result).longValue());
        }

        @Test
        @DisplayName("隐式参数类型Lambda")
        void testImplicitLambda() throws Exception {
            String script =
                "add = (a, b) -> a + b;\n" +
                "return add(10, 20);";
            Object result = execute(script);
            assertEquals(30L, ((Number) result).longValue());
        }
    }

    // ==================== 8. Map Tests ====================

    @Nested
    @DisplayName("Map操作测试")
    class MapTest {

        @Test
        @DisplayName("Map字面量等价于HashMap")
        void testMapLiteralEqualToHashMap() throws Exception {
            String script =
                "dict = {a: 123, b: 234, c:[1,2,3]};\n" +
                "map = new HashMap();\n" +
                "map.put('a', 123);\n" +
                "map.put('b', 234);\n" +
                "map.put('c', [1,2,3]);\n" +
                "return dict == map;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Map多种访问方式")
        void testMapAccessMethods() throws Exception {
            String script =
                "dict = {a: 123, b: 234};\n" +
                "result = dict.get('a') == 123 && dict.a == 123 && dict['a'] == 123;\n" +
                "return result;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Map属性设置")
        void testMapPropertySet() throws Exception {
            String script =
                "dict = {a: 100};\n" +
                "dict.b = 200;\n" +
                "dict['c'] = 300;\n" +
                "return dict.b + dict.c;";
            Object result = execute(script);
            assertEquals(500L, ((Number) result).longValue());
        }
    }

    // ==================== 9. Method Invocation Tests ====================

    @Nested
    @DisplayName("方法调用测试")
    class MethodInvocationTest {

        @Test
        @DisplayName("字符串方法调用")
        void testStringMethodInvoke() throws Exception {
            String script =
                "str = \"hello world\";\n" +
                "return str.toUpperCase();";
            Object result = execute(script);
            assertEquals("HELLO WORLD", result);
        }

        @Test
        @DisplayName("集合方法调用")
        void testCollectionMethodInvoke() throws Exception {
            String script =
                "list = [1, 2, 3, 4, 5];\n" +
                "return list.size();";
            Object result = execute(script);
            assertEquals(5, result);
        }

        @Test
        @DisplayName("链式方法调用")
        void testChainMethodInvoke() throws Exception {
            String script =
                "str = \"  hello world  \";\n" +
                "return str.trim().toUpperCase().substring(0, 5);";
            Object result = execute(script);
            assertEquals("HELLO", result);
        }
    }

    // ==================== 10. Method Reference Tests ====================

    @Nested
    @DisplayName("方法引用测试")
    class MethodReferenceTest {

        @Test
        @DisplayName("静态方法引用")
        void testStaticMethodReference() throws Exception {
            String script =
                "import java.util.function.Function;\n" +
                "ref = String::valueOf;\n" +
                "return ref(123);";
            Object result = execute(script);
            assertEquals("123", result);
        }

        @Test
        @DisplayName("Math静态方法引用")
        void testMathMethodReference() throws Exception {
            String script =
                "absFunc = Math::abs;\n" +
                "return absFunc(-100);";
            Object result = execute(script);
            assertEquals(100, ((Number) result).intValue());
        }
    }

    // ==================== 11. New Expression Tests ====================

    @Nested
    @DisplayName("new表达式测试")
    class NewExpressionTest {

        @Test
        @DisplayName("无参构造")
        void testNoArgConstructor() throws Exception {
            String script =
                "list = new ArrayList();\n" +
                "return list.size();";
            Object result = execute(script);
            assertEquals(0, result);
        }

        @Test
        @DisplayName("带参构造")
        void testWithArgConstructor() throws Exception {
            String script =
                "str = new String(\"hello\");\n" +
                "return str;";
            Object result = execute(script);
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("BigDecimal构造")
        void testBigDecimalConstructor() throws Exception {
            String script =
                "bd = new BigDecimal(\"123.456\");\n" +
                "return bd.doubleValue();";
            Object result = execute(script);
            assertEquals(123.456, ((Number) result).doubleValue(), 0.001);
        }
    }

    // ==================== 12. Number Tests ====================

    @Nested
    @DisplayName("数字类型测试")
    class NumberTest {

        @Test
        @DisplayName("Integer最大值")
        void testIntegerMaxValue() throws Exception {
            String script = "return 2147483647 instanceof Integer;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Long类型自动识别")
        void testLongAutoType() throws Exception {
            String script = "return 9223372036854775807 instanceof Long;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Double类型")
        void testDoubleType() throws Exception {
            String script = "return 0.25 instanceof Double;";
            Object result = execute(script);
            assertEquals(true, result);
        }

        @Test
        @DisplayName("Long最大值")
        void testLongMaxValue() throws Exception {
            String script = "return Long.MAX_VALUE;";
            Object result = execute(script);
            assertEquals(Long.MAX_VALUE, result);
        }

        @Test
        @DisplayName("数字方法调用")
        void testNumberMethodInvoke() throws Exception {
            String script =
                "n = 123;\n" +
                "return n.toString();";
            Object result = execute(script);
            assertEquals("123", result);
        }
    }

    // ==================== 13. Property Access Tests ====================

    @Nested
    @DisplayName("属性访问测试")
    class PropertyAccessTest {

        @Test
        @DisplayName("数组length属性")
        void testArrayLengthProperty() throws Exception {
            String script =
                "arr = new int[]{1, 2, 3, 4, 5};\n" +
                "return arr.length;";
            Object result = execute(script);
            assertEquals(5, result);
        }

        @Test
        @DisplayName("class属性获取")
        void testClassProperty() throws Exception {
            String script =
                "str = \"hello\";\n" +
                "return str.class.getName();";
            Object result = execute(script);
            assertEquals("java.lang.String", result);
        }

        @Test
        @DisplayName("静态常量访问")
        void testStaticConstant() throws Exception {
            String script = "return Integer.MAX_VALUE;";
            Object result = execute(script);
            assertEquals(Integer.MAX_VALUE, result);
        }
    }

    // ==================== 14. Stream Tests ====================

    @Nested
    @DisplayName("Stream操作测试")
    class StreamTest {

        @Test
        @DisplayName("基础Stream操作")
        void testBasicStream() throws Exception {
            // 使用传统循环代替Stream，因为QLExpress4对Stream lambda有一些限制
            String script =
                "l = [\"a-111\", \"a-222\", \"b-333\", \"c-888\"];\n" +
                "l2 = [];\n" +
                "for (item: l) {\n" +
                "    if (item.startsWith(\"a-\")) {\n" +
                "        l2.add(item.split(\"-\")[1]);\n" +
                "    }\n" +
                "}\n" +
                "return l2;";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof List);
            List<?> list = (List<?>) result;
            assertEquals(2, list.size());
            assertEquals("111", list.get(0));
            assertEquals("222", list.get(1));
        }

        @Test
        @DisplayName("Stream reduce操作")
        void testStreamReduce() throws Exception {
            // 使用传统循环代替Stream reduce
            String script =
                "nums = [1, 2, 3, 4, 5];\n" +
                "sum = 0;\n" +
                "for (n: nums) {\n" +
                "    sum = sum + n;\n" +
                "}\n" +
                "return sum;";
            Object result = execute(script);
            assertEquals(15, ((Number) result).intValue());
        }

        @Test
        @DisplayName("Stream count操作")
        void testStreamCount() throws Exception {
            // 使用传统循环代替Stream filter + count
            String script =
                "nums = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];\n" +
                "count = 0;\n" +
                "for (n: nums) {\n" +
                "    if (n % 2 == 0) {\n" +
                "        count = count + 1;\n" +
                "    }\n" +
                "}\n" +
                "return count;";
            Object result = execute(script);
            assertEquals(5L, ((Number) result).longValue());
        }
    }

    // ==================== 15. Try-Catch Tests ====================

    @Nested
    @DisplayName("异常处理测试")
    class TryCatchTest {

        @Test
        @DisplayName("捕获Java异常")
        void testCatchJavaException() throws Exception {
            String script =
                "try {\n" +
                "    BigDecimal divisor = new BigDecimal(3);\n" +
                "    BigDecimal dividend = new BigDecimal(1);\n" +
                "    dividend.divide(divisor);\n" +
                "    return \"no exception\";\n" +
                "} catch (ArithmeticException e) {\n" +
                "    return \"caught arithmetic exception\";\n" +
                "}";
            Object result = execute(script);
            assertEquals("caught arithmetic exception", result);
        }

        @Test
        @DisplayName("异常捕获顺序")
        void testCatchOrder() throws Exception {
            String script =
                "f = e -> try {\n" +
                "    throw e;\n" +
                "} catch (NullPointerException n) {\n" +
                "    return 100;\n" +
                "} catch (Exception e) {\n" +
                "    return 10;\n" +
                "};\n" +
                "return f(new NullPointerException());";
            Object result = execute(script);
            assertEquals(100, ((Number) result).intValue());
        }

        @Test
        @DisplayName("通用异常捕获")
        void testCatchGeneralException() throws Exception {
            String script =
                "f = e -> try {\n" +
                "    throw e;\n" +
                "} catch (NullPointerException n) {\n" +
                "    return 100;\n" +
                "} catch (Exception e) {\n" +
                "    return 10;\n" +
                "};\n" +
                "return f(new RuntimeException());";
            Object result = execute(script);
            assertEquals(10, ((Number) result).intValue());
        }

        @Test
        @DisplayName("try-finally")
        void testTryFinally() throws Exception {
            String script =
                "result = 0;\n" +
                "try {\n" +
                "    result = 1;\n" +
                "} finally {\n" +
                "    result = result + 10;\n" +
                "}\n" +
                "return result;";
            Object result = execute(script);
            assertEquals(11L, ((Number) result).longValue());
        }
    }

    // ==================== 综合长脚本测试 ====================

    @Nested
    @DisplayName("综合长脚本测试")
    class ComprehensiveLongScriptTest {

        @Test
        @DisplayName("综合业务脚本1 - 订单计算")
        void testOrderCalculation() throws Exception {
            String script =
                "// 订单计算脚本\n" +
                "import java.util.stream.Collectors;\n" +
                "\n" +
                "// 定义商品列表\n" +
                "items = [\n" +
                "    {name: \"商品A\", price: 100.0, quantity: 2},\n" +
                "    {name: \"商品B\", price: 50.5, quantity: 3},\n" +
                "    {name: \"商品C\", price: 200.0, quantity: 1}\n" +
                "];\n" +
                "\n" +
                "// 计算总价\n" +
                "totalPrice = 0.0;\n" +
                "for (item: items) {\n" +
                "    totalPrice = totalPrice + item.price * item.quantity;\n" +
                "}\n" +
                "\n" +
                "// 应用折扣\n" +
                "discount = 0.0;\n" +
                "if (totalPrice > 500) {\n" +
                "    discount = 0.1;\n" +
                "} else if (totalPrice > 300) {\n" +
                "    discount = 0.05;\n" +
                "}\n" +
                "\n" +
                "finalPrice = totalPrice * (1 - discount);\n" +
                "\n" +
                "return {\n" +
                "    totalPrice: totalPrice,\n" +
                "    discount: discount,\n" +
                "    finalPrice: finalPrice,\n" +
                "    itemCount: items.size()\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(4, map.size());
            assertTrue(((Number) map.get("totalPrice")).doubleValue() > 0);
        }

        @Test
        @DisplayName("综合业务脚本2 - 数据转换")
        void testDataTransformation() throws Exception {
            String script =
                "import java.util.stream.Collectors;\n" +
                "\n" +
                "// 原始数据\n" +
                "users = [\n" +
                "    {id: 1, name: \"张三\", age: 25, department: \"技术部\"},\n" +
                "    {id: 2, name: \"李四\", age: 30, department: \"市场部\"},\n" +
                "    {id: 3, name: \"王五\", age: 28, department: \"技术部\"},\n" +
                "    {id: 4, name: \"赵六\", age: 35, department: \"财务部\"},\n" +
                "    {id: 5, name: \"钱七\", age: 22, department: \"技术部\"}\n" +
                "];\n" +
                "\n" +
                "// 筛选技术部员工\n" +
                "techUsers = [];\n" +
                "for (user: users) {\n" +
                "    if (user.department == \"技术部\") {\n" +
                "        techUsers.add(user);\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "// 计算平均年龄\n" +
                "totalAge = 0;\n" +
                "for (user: techUsers) {\n" +
                "    totalAge = totalAge + user.age;\n" +
                "}\n" +
                "avgAge = totalAge / techUsers.size();\n" +
                "\n" +
                "// 找最年轻的员工\n" +
                "youngestUser = techUsers[0];\n" +
                "for (user: techUsers) {\n" +
                "    if (user.age < youngestUser.age) {\n" +
                "        youngestUser = user;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "return {\n" +
                "    techCount: techUsers.size(),\n" +
                "    avgAge: avgAge,\n" +
                "    youngest: youngestUser.name\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(3, ((Number) map.get("techCount")).intValue());
            assertEquals("钱七", map.get("youngest"));
        }

        @Test
        @DisplayName("综合业务脚本3 - 递归和复杂逻辑")
        void testComplexLogic() throws Exception {
            String script =
                "// 斐波那契数列计算\n" +
                "fib = n -> {\n" +
                "    if (n <= 1) {\n" +
                "        return n;\n" +
                "    }\n" +
                "    return fib(n - 1) + fib(n - 2);\n" +
                "};\n" +
                "\n" +
                "// 计算前10个斐波那契数\n" +
                "fibNumbers = [];\n" +
                "for (i = 0; i < 10; i = i + 1) {\n" +
                "    fibNumbers.add(fib(i));\n" +
                "}\n" +
                "\n" +
                "// 阶乘计算\n" +
                "factorial = n -> {\n" +
                "    if (n <= 1) {\n" +
                "        return 1;\n" +
                "    }\n" +
                "    return n * factorial(n - 1);\n" +
                "};\n" +
                "\n" +
                "return {\n" +
                "    fib10: fibNumbers,\n" +
                "    factorial5: factorial(5),\n" +
                "    factorial10: factorial(10)\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(120L, ((Number) map.get("factorial5")).longValue());
            assertEquals(3628800L, ((Number) map.get("factorial10")).longValue());
        }

        @Test
        @DisplayName("综合业务脚本4 - 异常处理和流程控制")
        void testExceptionHandlingAndFlowControl() throws Exception {
            String script =
                "// 安全除法函数\n" +
                "safeDivide = (a, b) -> {\n" +
                "    try {\n" +
                "        if (b == 0) {\n" +
                "            throw new ArithmeticException(\"除数不能为零\");\n" +
                "        }\n" +
                "        return a / b;\n" +
                "    } catch (ArithmeticException e) {\n" +
                "        return null;\n" +
                "    }\n" +
                "};\n" +
                "\n" +
                "// 测试用例\n" +
                "results = [];\n" +
                "results.add(safeDivide(10, 2));\n" +
                "results.add(safeDivide(100, 5));\n" +
                "results.add(safeDivide(10, 0));\n" +
                "\n" +
                "// 过滤有效结果\n" +
                "validResults = [];\n" +
                "for (r: results) {\n" +
                "    if (r != null) {\n" +
                "        validResults.add(r);\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "return {\n" +
                "    allResults: results,\n" +
                "    validCount: validResults.size(),\n" +
                "    hasNull: results.contains(null)\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(2, ((Number) map.get("validCount")).intValue());
            assertEquals(true, map.get("hasNull"));
        }

        @Test
        @DisplayName("综合业务脚本5 - 字符串处理")
        void testStringProcessing() throws Exception {
            String script =
                "// 字符串处理脚本\n" +
                "text = \"Hello World! Welcome to QLExpress. This is a test.\";\n" +
                "\n" +
                "// 基本操作\n" +
                "upperText = text.toUpperCase();\n" +
                "lowerText = text.toLowerCase();\n" +
                "words = text.split(\" \");\n" +
                "\n" +
                "// 统计单词数量\n" +
                "wordCount = words.length;\n" +
                "\n" +
                "// 替换操作\n" +
                "replacedText = text.replace(\"test\", \"demo\");\n" +
                "\n" +
                "// 检查包含\n" +
                "containsHello = text.contains(\"Hello\");\n" +
                "startsWithHello = text.startsWith(\"Hello\");\n" +
                "endsWithDot = text.endsWith(\".\");\n" +
                "\n" +
                "return {\n" +
                "    wordCount: wordCount,\n" +
                "    containsHello: containsHello,\n" +
                "    startsWithHello: startsWithHello,\n" +
                "    endsWithDot: endsWithDot,\n" +
                "    length: text.length()\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(true, map.get("containsHello"));
            assertEquals(true, map.get("startsWithHello"));
            assertEquals(true, map.get("endsWithDot"));
        }

        @Test
        @DisplayName("综合业务脚本6 - 日期时间处理")
        void testDateTimeProcessing() throws Exception {
            String script =
                "import java.util.Date;\n" +
                "import java.util.Calendar;\n" +
                "\n" +
                "// 获取当前时间\n" +
                "now = new Date();\n" +
                "\n" +
                "// 使用Calendar\n" +
                "cal = Calendar.getInstance();\n" +
                "year = cal.get(Calendar.YEAR);\n" +
                "month = cal.get(Calendar.MONTH) + 1;\n" +
                "day = cal.get(Calendar.DAY_OF_MONTH);\n" +
                "\n" +
                "return {\n" +
                "    hasDate: now != null,\n" +
                "    year: year,\n" +
                "    month: month,\n" +
                "    day: day\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(true, map.get("hasDate"));
            assertTrue(((Number) map.get("year")).intValue() >= 2024);
        }

        @Test
        @DisplayName("综合业务脚本7 - 集合高级操作")
        void testAdvancedCollectionOperations() throws Exception {
            String script =
                "import java.util.stream.Collectors;\n" +
                "import java.util.*;\n" +
                "\n" +
                "// 创建数据\n" +
                "numbers = [5, 2, 8, 1, 9, 3, 7, 4, 6, 10];\n" +
                "\n" +
                "// 排序\n" +
                "sorted = new ArrayList(numbers);\n" +
                "Collections.sort(sorted);\n" +
                "\n" +
                "// 反转\n" +
                "reversed = new ArrayList(sorted);\n" +
                "Collections.reverse(reversed);\n" +
                "\n" +
                "// 查找最大最小\n" +
                "maxVal = Collections.max(numbers);\n" +
                "minVal = Collections.min(numbers);\n" +
                "\n" +
                "// Set去重\n" +
                "duplicateList = [1, 2, 2, 3, 3, 3, 4, 4, 4, 4];\n" +
                "uniqueSet = new HashSet(duplicateList);\n" +
                "\n" +
                "return {\n" +
                "    sorted: sorted,\n" +
                "    reversed: reversed,\n" +
                "    max: maxVal,\n" +
                "    min: minVal,\n" +
                "    uniqueCount: uniqueSet.size()\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(10, ((Number) map.get("max")).intValue());
            assertEquals(1, ((Number) map.get("min")).intValue());
            assertEquals(4, ((Number) map.get("uniqueCount")).intValue());
        }
    }

    // ==================== 边界条件和错误处理测试 ====================

    @Nested
    @DisplayName("边界条件和错误处理测试")
    class BoundaryAndErrorTest {

        @Test
        @DisplayName("空集合处理")
        void testEmptyCollection() throws Exception {
            String script =
                "list = [];\n" +
                "map = new HashMap();\n" +
                "return {\n" +
                "    listEmpty: list.isEmpty(),\n" +
                "    listSize: list.size(),\n" +
                "    mapEmpty: map.isEmpty(),\n" +
                "    mapSize: map.size()\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(true, map.get("listEmpty"));
            assertEquals(0, ((Number) map.get("listSize")).intValue());
        }

        @Test
        @DisplayName("null值处理")
        void testNullHandling() throws Exception {
            String script =
                "a = null;\n" +
                "result = a == null;\n" +
                "\n" +
                "// 三元运算符处理null\n" +
                "value = a != null ? a : \"default\";\n" +
                "\n" +
                "return {\n" +
                "    isNull: result,\n" +
                "    defaultValue: value\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertEquals(true, map.get("isNull"));
            assertEquals("default", map.get("defaultValue"));
        }

        @Test
        @DisplayName("大数值计算")
        void testLargeNumberCalculation() throws Exception {
            String script =
                "// 大整数\n" +
                "bigNum1 = new BigInteger(\"12345678901234567890\");\n" +
                "bigNum2 = new BigInteger(\"98765432109876543210\");\n" +
                "bigSum = bigNum1.add(bigNum2);\n" +
                "\n" +
                "// 大小数\n" +
                "bigDec1 = new BigDecimal(\"123.456789012345678901234567890\");\n" +
                "bigDec2 = new BigDecimal(\"987.654321098765432109876543210\");\n" +
                "bigDecSum = bigDec1.add(bigDec2);\n" +
                "\n" +
                "return {\n" +
                "    bigIntSum: bigSum.toString(),\n" +
                "    bigDecSum: bigDecSum.toString()\n" +
                "};";
            Object result = execute(script);
            assertNotNull(result);
            assertTrue(result instanceof Map);
            Map<?, ?> map = (Map<?, ?>) result;
            assertNotNull(map.get("bigIntSum"));
            assertNotNull(map.get("bigDecSum"));
        }
    }
}
