package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.model.enums.NodeCategory;
import com.ql.qlexpress.web.model.enums.OperatorCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetadataService单元测试
 *
 * @author qlexpress
 */
class MetadataServiceTest {

    private MetadataService metadataService;

    @BeforeEach
    void setUp() {
        metadataService = new MetadataService();
        metadataService.init();
    }

    @Test
    @DisplayName("获取所有元数据应返回完整数据")
    void getAllMetadata_shouldReturnCompleteData() {
        MetadataResponse response = metadataService.getAllMetadata();

        assertNotNull(response);
        assertEquals("1.0.0", response.getVersion());
        assertNotNull(response.getOperators());
        assertFalse(response.getOperators().isEmpty());
        assertNotNull(response.getBuiltInFunctions());
        assertFalse(response.getBuiltInFunctions().isEmpty());
        assertNotNull(response.getKeywords());
        assertFalse(response.getKeywords().isEmpty());
        assertNotNull(response.getTypes());
        assertFalse(response.getTypes().isEmpty());
        assertNotNull(response.getNodeTemplates());
        assertFalse(response.getNodeTemplates().isEmpty());
        assertNotNull(response.getCategories());
        assertFalse(response.getCategories().isEmpty());
    }

    @Test
    @DisplayName("获取操作符列表-无过滤")
    void getOperators_withoutFilter_shouldReturnAll() {
        List<OperatorMetadata> operators = metadataService.getOperators(null);

        assertNotNull(operators);
        assertFalse(operators.isEmpty());
        assertTrue(operators.size() >= 10);
    }

    @Test
    @DisplayName("获取操作符列表-按分类过滤")
    void getOperators_withCategoryFilter_shouldReturnFiltered() {
        List<OperatorMetadata> arithmeticOperators = metadataService.getOperators("ARITHMETIC");

        assertNotNull(arithmeticOperators);
        assertFalse(arithmeticOperators.isEmpty());
        for (OperatorMetadata op : arithmeticOperators) {
            assertEquals(OperatorCategory.ARITHMETIC, op.getCategory());
        }
    }

    @Test
    @DisplayName("获取操作符列表-包含基本算术运算符")
    void getOperators_shouldContainBasicArithmetic() {
        List<OperatorMetadata> operators = metadataService.getOperators(null);

        assertTrue(operators.stream().anyMatch(op -> "+".equals(op.getSymbol())));
        assertTrue(operators.stream().anyMatch(op -> "-".equals(op.getSymbol())));
        assertTrue(operators.stream().anyMatch(op -> "*".equals(op.getSymbol())));
        assertTrue(operators.stream().anyMatch(op -> "/".equals(op.getSymbol())));
    }

    @Test
    @DisplayName("获取操作符列表-包含比较运算符")
    void getOperators_shouldContainComparisonOperators() {
        List<OperatorMetadata> operators = metadataService.getOperators(null);

        assertTrue(operators.stream().anyMatch(op -> "==".equals(op.getSymbol())));
        assertTrue(operators.stream().anyMatch(op -> "!=".equals(op.getSymbol())));
        assertTrue(operators.stream().anyMatch(op -> ">".equals(op.getSymbol())));
        assertTrue(operators.stream().anyMatch(op -> "<".equals(op.getSymbol())));
    }

    @Test
    @DisplayName("获取操作符列表-包含逻辑运算符")
    void getOperators_shouldContainLogicalOperators() {
        List<OperatorMetadata> operators = metadataService.getOperators(null);

        assertTrue(operators.stream().anyMatch(op -> "&&".equals(op.getSymbol())));
        assertTrue(operators.stream().anyMatch(op -> "||".equals(op.getSymbol())));
        assertTrue(operators.stream().anyMatch(op -> "!".equals(op.getSymbol())));
    }

    @Test
    @DisplayName("获取函数列表-无过滤")
    void getFunctions_withoutFilter_shouldReturnAll() {
        List<FunctionMetadata> functions = metadataService.getFunctions(null, null);

        assertNotNull(functions);
        assertFalse(functions.isEmpty());
    }

    @Test
    @DisplayName("获取函数列表-按分类过滤")
    void getFunctions_withCategoryFilter_shouldReturnFiltered() {
        List<FunctionMetadata> mathFunctions = metadataService.getFunctions("数学函数", null);

        assertNotNull(mathFunctions);
        assertFalse(mathFunctions.isEmpty());
        for (FunctionMetadata func : mathFunctions) {
            assertEquals("数学函数", func.getCategory());
        }
    }

    @Test
    @DisplayName("获取函数列表-按关键字搜索")
    void getFunctions_withSearchFilter_shouldReturnFiltered() {
        List<FunctionMetadata> functions = metadataService.getFunctions(null, "max");

        assertNotNull(functions);
        assertFalse(functions.isEmpty());
        assertTrue(functions.stream().anyMatch(f -> "max".equals(f.getPrimaryName())));
    }

    @Test
    @DisplayName("获取函数列表-应包含数学函数")
    void getFunctions_shouldContainMathFunctions() {
        List<FunctionMetadata> functions = metadataService.getFunctions(null, null);

        assertTrue(functions.stream().anyMatch(f -> "max".equals(f.getPrimaryName())));
        assertTrue(functions.stream().anyMatch(f -> "min".equals(f.getPrimaryName())));
        assertTrue(functions.stream().anyMatch(f -> "abs".equals(f.getPrimaryName())));
    }

    @Test
    @DisplayName("获取函数列表-应包含字符串函数")
    void getFunctions_shouldContainStringFunctions() {
        List<FunctionMetadata> functions = metadataService.getFunctions(null, null);

        assertTrue(functions.stream().anyMatch(f -> "length".equals(f.getPrimaryName())));
        assertTrue(functions.stream().anyMatch(f -> "trim".equals(f.getPrimaryName())));
    }

    @Test
    @DisplayName("获取节点模板-无过滤")
    void getNodeTemplates_withoutFilter_shouldReturnAll() {
        List<NodeTemplate> templates = metadataService.getNodeTemplates(null);

        assertNotNull(templates);
        assertFalse(templates.isEmpty());
        assertTrue(templates.size() >= 5);
    }

    @Test
    @DisplayName("获取节点模板-按分类过滤")
    void getNodeTemplates_withCategoryFilter_shouldReturnFiltered() {
        List<NodeTemplate> controlFlowTemplates = metadataService.getNodeTemplates("CONTROL_FLOW");

        assertNotNull(controlFlowTemplates);
        assertFalse(controlFlowTemplates.isEmpty());
        for (NodeTemplate template : controlFlowTemplates) {
            assertEquals(NodeCategory.CONTROL_FLOW, template.getCategory());
        }
    }

    @Test
    @DisplayName("获取节点模板-应包含开始和结束节点")
    void getNodeTemplates_shouldContainStartAndEndNodes() {
        List<NodeTemplate> templates = metadataService.getNodeTemplates(null);

        assertTrue(templates.stream().anyMatch(t -> "start".equals(t.getType())));
        assertTrue(templates.stream().anyMatch(t -> "end".equals(t.getType())));
    }

    @Test
    @DisplayName("获取节点模板-应包含IF节点")
    void getNodeTemplates_shouldContainIfNode() {
        List<NodeTemplate> templates = metadataService.getNodeTemplates(null);

        NodeTemplate ifNode = templates.stream()
                .filter(t -> "if".equals(t.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(ifNode);
        assertEquals("条件判断", ifNode.getDisplayName());
        assertEquals(NodeCategory.CONTROL_FLOW, ifNode.getCategory());
        assertNotNull(ifNode.getInputHandles());
        assertEquals(1, ifNode.getInputHandles().size());
        assertNotNull(ifNode.getOutputHandles());
        assertEquals(2, ifNode.getOutputHandles().size()); // true and false branches
    }

    @Test
    @DisplayName("获取节点模板-应包含FOR循环节点")
    void getNodeTemplates_shouldContainForNode() {
        List<NodeTemplate> templates = metadataService.getNodeTemplates(null);

        NodeTemplate forNode = templates.stream()
                .filter(t -> "for".equals(t.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(forNode);
        assertEquals("FOR循环", forNode.getDisplayName());
        assertNotNull(forNode.getProperties());
        assertTrue(forNode.getProperties().size() >= 3); // init, condition, update
    }

    @Test
    @DisplayName("获取节点模板-应包含表达式节点")
    void getNodeTemplates_shouldContainExpressionNode() {
        List<NodeTemplate> templates = metadataService.getNodeTemplates(null);

        NodeTemplate exprNode = templates.stream()
                .filter(t -> "expression".equals(t.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(exprNode);
        assertEquals("表达式", exprNode.getDisplayName());
        assertEquals(NodeCategory.EXPRESSION, exprNode.getCategory());
    }

    @Test
    @DisplayName("获取节点模板-应包含函数调用节点")
    void getNodeTemplates_shouldContainFunctionCallNode() {
        List<NodeTemplate> templates = metadataService.getNodeTemplates(null);

        NodeTemplate funcNode = templates.stream()
                .filter(t -> "function_call".equals(t.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(funcNode);
        assertEquals("函数调用", funcNode.getDisplayName());
        assertEquals(NodeCategory.FUNCTION_CALL, funcNode.getCategory());
    }

    @Test
    @DisplayName("开始节点不应该有输入连接点")
    void startNode_shouldNotHaveInputHandles() {
        List<NodeTemplate> templates = metadataService.getNodeTemplates(null);

        NodeTemplate startNode = templates.stream()
                .filter(t -> "start".equals(t.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(startNode);
        assertTrue(startNode.getInputHandles().isEmpty());
        assertFalse(startNode.isDeletable());
    }

    @Test
    @DisplayName("结束节点不应该有输出连接点")
    void endNode_shouldNotHaveOutputHandles() {
        List<NodeTemplate> templates = metadataService.getNodeTemplates(null);

        NodeTemplate endNode = templates.stream()
                .filter(t -> "end".equals(t.getType()))
                .findFirst()
                .orElse(null);

        assertNotNull(endNode);
        assertTrue(endNode.getOutputHandles().isEmpty());
        assertFalse(endNode.isDeletable());
    }
}
