package com.ql.qlexpress.web.core.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ql.qlexpress.web.core.transpiler.TranspileResult;
import com.ql.qlexpress.web.core.transpiler.VisualToQLTranspiler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 调试转译测试
 */
class DebugTranspileTest {

    private QLToVisualParser parser;
    private VisualToQLTranspiler transpiler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        parser = new QLToVisualParser();
        transpiler = new VisualToQLTranspiler();
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    @DisplayName("调试嵌套if-else")
    void debugNestedIfElse() throws Exception {
        String script = "if (level == 1) {\n" +
                "    if (score > 80) {\n" +
                "        result = \"good\";\n" +
                "    } else {\n" +
                "        result = \"normal\";\n" +
                "    }\n" +
                "} else if (level == 2) {\n" +
                "    result = \"high\";\n" +
                "} else {\n" +
                "    result = \"unknown\";\n" +
                "}";

        System.out.println("=== Input Script ===");
        System.out.println(script);

        QLToVisualParser.ParseResult parseResult = parser.parse(script);
        assertTrue(parseResult.isSuccess(), "Parse should succeed: " + parseResult.getErrorMessage());

        System.out.println("\n=== JSON Schema ===");
        System.out.println(objectMapper.writeValueAsString(parseResult.getFlow()));

        TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
        assertTrue(transpileResult.isSuccess(), "Transpile should succeed");

        System.out.println("\n=== Transpiled Output ===");
        System.out.println(transpileResult.getScript());

        System.out.println("\n=== Checks ===");
        System.out.println("Contains 'if': " + transpileResult.getScript().contains("if"));
        System.out.println("Contains 'else': " + transpileResult.getScript().contains("else"));
    }

    @Test
    @DisplayName("调试带break/continue的循环")
    void debugLoopBreakContinue() throws Exception {
        String script = "for (i = 0; i < 100; i = i + 1) {\n" +
                "    if (i % 2 == 0) {\n" +
                "        continue;\n" +
                "    }\n" +
                "    if (i > 50) {\n" +
                "        break;\n" +
                "    }\n" +
                "    sum = sum + i;\n" +
                "}";

        System.out.println("=== Input Script ===");
        System.out.println(script);

        QLToVisualParser.ParseResult parseResult = parser.parse(script);
        assertTrue(parseResult.isSuccess(), "Parse should succeed: " + parseResult.getErrorMessage());

        System.out.println("\n=== JSON Schema ===");
        System.out.println(objectMapper.writeValueAsString(parseResult.getFlow()));

        TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
        assertTrue(transpileResult.isSuccess(), "Transpile should succeed");

        System.out.println("\n=== Transpiled Output ===");
        System.out.println(transpileResult.getScript());

        System.out.println("\n=== Checks ===");
        System.out.println("Contains 'for': " + transpileResult.getScript().contains("for"));
        System.out.println("Contains 'break': " + transpileResult.getScript().contains("break"));
        System.out.println("Contains 'continue': " + transpileResult.getScript().contains("continue"));
    }
}
