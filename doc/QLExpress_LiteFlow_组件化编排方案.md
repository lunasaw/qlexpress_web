# QLExpress + LiteFlow 组件化编排方案

## 一、方案概述

### 1.1 核心思路

将 **QLExpress** 作为业务逻辑的实现载体（Node），**LiteFlow** 作为流程编排引擎，实现：

- **组件库**：预定义的 QLExpress 脚本组件，每个组件是一个可复用的业务逻辑单元
- **可视化编排**：基于 liteflow-editor-client 增强，支持从组件库拖拽组件进行流程编排
- **智能分支**：Switch 类型组件自动生成对应数量的分支

```
┌─────────────────────────────────────────────────────────────────────┐
│                         组件设计器 (新增)                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  组件元数据定义                                               │    │
│  │  - componentId: "ageCheck"                                   │    │
│  │  - componentType: BOOLEAN | SCRIPT | SWITCH                  │    │
│  │  - 输入参数: [{name: "age", type: "Integer"}]                │    │
│  │  - Switch分支: ["VIP", "GOLD", "NORMAL"] (仅SWITCH类型)       │    │
│  │  - QLExpress 脚本编辑器                                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓ 保存为组件
┌─────────────────────────────────────────────────────────────────────┐
│                     组件库 (Component Library)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │ 年龄检查  │ │ VIP检查   │ │ 金额计算  │ │ 会员路由  │  + 添加      │
│  │ BOOLEAN  │ │ BOOLEAN  │ │ SCRIPT   │ │SWITCH:3  │               │
│  │ in: age  │ │ in: level│ │ in/out   │ │VIP/GOLD/ │               │
│  │ out:T/F  │ │ out:T/F  │ │          │ │ NORMAL   │               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓ 拖拽组件到编排器
┌─────────────────────────────────────────────────────────────────────┐
│              LiteFlow 编排器 (liteflow-editor-client 增强)           │
│                                                                     │
│  选择 [会员路由] 组件后，自动创建 3 个分支：                           │
│                                                                     │
│                         ┌─ VIP    → [VIP折扣]    ─┐                 │
│  [开始] → [会员路由] → ─┼─ GOLD   → [黄金折扣]   ─┼→ [结算] → [结束] │
│                         └─ NORMAL → [普通价格]   ─┘                 │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 技术架构

```
┌────────────────────────────────────────────────────────────────┐
│                        前端层                                   │
│  ┌─────────────────┐  ┌─────────────────┐                      │
│  │  组件设计器      │  │  流程编排器      │                      │
│  │  (新增开发)      │  │  (增强现有)      │                      │
│  │                 │  │                 │                      │
│  │ - 脚本编辑      │  │ - 组件拖拽      │                      │
│  │ - 参数配置      │  │ - 流程连线      │                      │
│  │ - 分支定义      │  │ - 智能分支      │                      │
│  └─────────────────┘  └─────────────────┘                      │
└────────────────────────────────────────────────────────────────┘
                              ↓ REST API
┌────────────────────────────────────────────────────────────────┐
│                        后端层                                   │
│  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐  │
│  │  组件管理服务    │  │  编排转换服务    │  │  执行服务      │  │
│  │                 │  │                 │  │                │  │
│  │ - CRUD组件     │  │ - JSON→LiteFlow │  │ - 流程执行     │  │
│  │ - 版本管理     │  │ - 验证逻辑      │  │ - 结果返回     │  │
│  │ - 分类管理     │  │ - EL生成       │  │ - 日志追踪     │  │
│  └─────────────────┘  └─────────────────┘  └────────────────┘  │
└────────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────────┐
│                        引擎层                                   │
│  ┌─────────────────────────┐  ┌─────────────────────────────┐  │
│  │       LiteFlow          │  │        QLExpress            │  │
│  │                         │  │                             │  │
│  │  - 流程编排             │  │  - 脚本执行                  │  │
│  │  - 节点调度             │  │  - 规则计算                  │  │
│  │  - 并行/串行            │  │  - 上下文访问                │  │
│  │  - 条件分支             │  │                             │  │
│  └─────────────────────────┘  └─────────────────────────────┘  │
└────────────────────────────────────────────────────────────────┘
```

---

## 二、数据模型设计

### 2.1 组件定义模型 (QLComponent)

```java
/**
 * QLExpress 组件定义
 */
public class QLComponent {

    // ========== 基础信息 ==========
    /** 组件唯一标识 (对应 LiteFlow nodeId) */
    private String componentId;

    /** 组件名称 (显示用) */
    private String componentName;

    /** 组件描述 */
    private String description;

    /** 组件分类 (如: 风控、营销、支付) */
    private String category;

    /** 组件图标 */
    private String icon;

    /** 组件版本 */
    private String version;

    /** 是否启用 */
    private boolean enabled;

    // ========== 类型定义 ==========
    /**
     * 组件类型：
     * - SCRIPT: 普通脚本节点，执行逻辑，可修改上下文
     * - BOOLEAN: 条件判断节点，返回 true/false，用于 IF 分支
     * - SWITCH: 路由选择节点，返回分支标识，用于 SWITCH 多路分支
     */
    private ComponentType componentType;

    /** 脚本语言 (默认 qlexpress) */
    private String language = "qlexpress";

    // ========== 脚本内容 ==========
    /** QLExpress 脚本 */
    private String script;

    // ========== 参数定义 ==========
    /** 输入参数列表 */
    private List<ParameterDef> inputs;

    /** 输出参数列表 (SCRIPT类型) */
    private List<ParameterDef> outputs;

    // ========== Switch 分支定义 (仅 SWITCH 类型) ==========
    /** 分支列表 */
    private List<SwitchBranch> switchBranches;

    /** 默认分支 */
    private String defaultBranch;

    // ========== 扩展 ==========
    /** 元数据 */
    private Map<String, Object> metadata;
}

/**
 * 参数定义
 */
public class ParameterDef {
    private String name;        // 参数名
    private String type;        // 类型: String, Integer, Double, Boolean, List, Map
    private String description; // 描述
    private boolean required;   // 是否必填
    private Object defaultValue;// 默认值
}

/**
 * Switch 分支定义
 */
public class SwitchBranch {
    private String branchId;    // 分支标识 (脚本返回值)
    private String branchName;  // 分支名称 (显示用)
    private String description; // 分支描述
}
```

### 2.2 编排流程模型 (FlowDesign)

```java
/**
 * 可视化编排流程定义
 * 兼容 liteflow-editor-client 的 JSON 格式
 */
public class FlowDesign {

    /** 流程ID */
    private String flowId;

    /** 流程名称 */
    private String flowName;

    /** 流程描述 */
    private String description;

    /** 流程版本 */
    private String version;

    /** 编排结构 (树形，与 liteflow-editor 兼容) */
    private FlowNode root;

    /** 使用的组件ID列表 */
    private List<String> usedComponentIds;
}

/**
 * 流程节点 (编排树节点)
 */
public class FlowNode {

    /**
     * 节点类型：
     * 编排类型: THEN, WHEN, IF, SWITCH, FOR, WHILE, CATCH
     * 组件类型: 对应 QLComponent 的 componentId
     */
    private String type;

    /** 节点ID (运行时生成或指定) */
    private String id;

    /** 子节点列表 (编排类型使用) */
    private List<FlowNode> children;

    /** 条件节点 (IF/SWITCH/FOR/WHILE 使用) */
    private FlowNode condition;

    /** 引用的组件ID (叶子节点使用) */
    private String componentRef;

    /** 节点属性 */
    private Map<String, Object> properties;
}
```

### 2.3 JSON 数据格式示例

#### 组件定义 JSON

```json
{
  "componentId": "memberLevelRouter",
  "componentName": "会员等级路由",
  "description": "根据会员等级路由到不同处理分支",
  "category": "营销",
  "icon": "route",
  "componentType": "SWITCH",
  "language": "qlexpress",
  "script": "level = flowContext.getString('memberLevel'); return level != null ? level : 'NORMAL';",
  "inputs": [
    {
      "name": "memberLevel",
      "type": "String",
      "description": "会员等级",
      "required": true
    }
  ],
  "switchBranches": [
    { "branchId": "VIP", "branchName": "VIP会员", "description": "VIP专属通道" },
    { "branchId": "GOLD", "branchName": "黄金会员", "description": "黄金会员通道" },
    { "branchId": "NORMAL", "branchName": "普通会员", "description": "普通会员通道" }
  ],
  "defaultBranch": "NORMAL",
  "version": "1.0.0",
  "enabled": true
}
```

#### 编排流程 JSON

```json
{
  "flowId": "orderDiscount",
  "flowName": "订单折扣计算流程",
  "description": "根据会员等级计算不同折扣",
  "version": "1.0.0",
  "root": {
    "type": "THEN",
    "children": [
      {
        "type": "component",
        "componentRef": "initOrder",
        "id": "init"
      },
      {
        "type": "SWITCH",
        "condition": {
          "type": "component",
          "componentRef": "memberLevelRouter",
          "id": "router"
        },
        "children": [
          {
            "type": "component",
            "componentRef": "vipDiscount",
            "id": "vip",
            "properties": { "branch": "VIP" }
          },
          {
            "type": "component",
            "componentRef": "goldDiscount",
            "id": "gold",
            "properties": { "branch": "GOLD" }
          },
          {
            "type": "component",
            "componentRef": "normalPrice",
            "id": "normal",
            "properties": { "branch": "NORMAL" }
          }
        ]
      },
      {
        "type": "component",
        "componentRef": "finalCalc",
        "id": "calc"
      }
    ]
  },
  "usedComponentIds": ["initOrder", "memberLevelRouter", "vipDiscount", "goldDiscount", "normalPrice", "finalCalc"]
}
```

---

## 三、API 接口设计

### 3.1 组件管理接口

```yaml
# 组件 CRUD
POST   /api/component                    # 创建组件
GET    /api/component/{componentId}      # 获取组件详情
PUT    /api/component/{componentId}      # 更新组件
DELETE /api/component/{componentId}      # 删除组件
GET    /api/component/list               # 获��组件列表 (支持分类筛选)
GET    /api/component/category           # 获取所有分类

# 组件验证
POST   /api/component/validate           # 验证组件脚本
POST   /api/component/test               # 测试组件执行
```

#### 创建组件请求示例

```bash
POST /api/component
Content-Type: application/json

{
  "componentId": "ageCheck",
  "componentName": "年龄检查",
  "description": "检查用户是否成年",
  "category": "风控",
  "componentType": "BOOLEAN",
  "script": "age = flowContext.getInt('age', 0); return age >= 18;",
  "inputs": [
    { "name": "age", "type": "Integer", "description": "用户年龄", "required": true }
  ]
}
```

#### 组件列表响应示例

```json
{
  "code": 200,
  "data": {
    "total": 15,
    "categories": ["风控", "营销", "支付", "通用"],
    "components": [
      {
        "componentId": "ageCheck",
        "componentName": "年龄检查",
        "category": "风控",
        "componentType": "BOOLEAN",
        "description": "检查用户是否成年",
        "inputCount": 1,
        "version": "1.0.0"
      },
      {
        "componentId": "memberLevelRouter",
        "componentName": "会员等级路由",
        "category": "营销",
        "componentType": "SWITCH",
        "description": "根据会员等级路由",
        "inputCount": 1,
        "switchBranchCount": 3,
        "version": "1.0.0"
      }
    ]
  }
}
```

### 3.2 流程编排接口

```yaml
# 流程 CRUD
POST   /api/flow                         # 创建流程
GET    /api/flow/{flowId}                # 获取流程详情
PUT    /api/flow/{flowId}                # 更新流程
DELETE /api/flow/{flowId}                # 删除流程
GET    /api/flow/list                    # 获取流程列表

# 流程转换
POST   /api/flow/convert                 # JSON → LiteFlow (节点+链)
POST   /api/flow/validate                # 验证流程定义
POST   /api/flow/preview-el              # 预览生成的 EL 表达式

# 流程部署
POST   /api/flow/{flowId}/deploy         # 部署流程到 LiteFlow
POST   /api/flow/{flowId}/undeploy       # 卸载流程
GET    /api/flow/{flowId}/status         # 获取部署状态
```

#### 流程转换请求示例

```bash
POST /api/flow/convert
Content-Type: application/json

{
  "flowId": "orderDiscount",
  "flowName": "订单折扣流程",
  "root": {
    "type": "THEN",
    "children": [
      { "type": "component", "componentRef": "initOrder" },
      {
        "type": "IF",
        "condition": { "type": "component", "componentRef": "ageCheck" },
        "children": [
          { "type": "component", "componentRef": "adultProcess" },
          { "type": "component", "componentRef": "minorReject" }
        ]
      }
    ]
  }
}
```

#### 流程转换响应示例

```json
{
  "code": 200,
  "data": {
    "flowId": "orderDiscount",
    "chainName": "orderDiscount",
    "el": "THEN(initOrder, IF(ageCheck, adultProcess, minorReject))",
    "nodes": [
      {
        "nodeId": "initOrder",
        "nodeType": "script",
        "language": "qlexpress",
        "script": "..."
      },
      {
        "nodeId": "ageCheck",
        "nodeType": "boolean_script",
        "language": "qlexpress",
        "script": "..."
      }
    ],
    "valid": true
  }
}
```

### 3.3 流程执行接口

```yaml
# 执行流程
POST   /api/flow/{flowId}/execute        # 执行已部署流程
POST   /api/flow/execute-direct          # 直接执行流程定义 (无需部署)

# 执行历史
GET    /api/flow/{flowId}/history        # 获取执行历史
GET    /api/flow/execution/{executionId} # 获取执行详情
```

#### 执行请求示例

```bash
POST /api/flow/orderDiscount/execute
Content-Type: application/json

{
  "params": {
    "userId": "U12345",
    "memberLevel": "VIP",
    "orderAmount": 500.0
  },
  "options": {
    "timeout": 30000,
    "traceEnabled": true
  }
}
```

#### 执行响应示例

```json
{
  "code": 200,
  "data": {
    "success": true,
    "flowId": "orderDiscount",
    "executionId": "exec_20231217_001",
    "context": {
      "userId": "U12345",
      "memberLevel": "VIP",
      "orderAmount": 500.0,
      "discount": 0.8,
      "finalAmount": 400.0
    },
    "executeSteps": "initOrder(2ms) -> memberLevelRouter(1ms) -> vipDiscount(3ms) -> finalCalc(1ms)",
    "totalTime": 15,
    "trace": [
      { "node": "initOrder", "time": 2, "status": "success" },
      { "node": "memberLevelRouter", "time": 1, "status": "success", "result": "VIP" },
      { "node": "vipDiscount", "time": 3, "status": "success" },
      { "node": "finalCalc", "time": 1, "status": "success" }
    ]
  }
}
```

---

## 四、核心转换逻辑

### 4.1 JSON → LiteFlow 转换服务

```java
@Service
public class FlowConvertService {

    @Autowired
    private QLComponentRepository componentRepo;

    /**
     * 将编排 JSON 转换为 LiteFlow 配置
     */
    public ConvertResult convert(FlowDesign flowDesign) {
        ConvertResult result = new ConvertResult();
        result.setFlowId(flowDesign.getFlowId());
        result.setChainName(flowDesign.getFlowId());

        // 1. 收集所有使用的组件
        Set<String> componentIds = collectComponentIds(flowDesign.getRoot());

        // 2. 加载组件定义，生成 LiteFlow 节点
        List<NodeDefinition> nodes = new ArrayList<>();
        for (String componentId : componentIds) {
            QLComponent component = componentRepo.findById(componentId)
                .orElseThrow(() -> new BusinessException("组件不存在: " + componentId));

            nodes.add(NodeDefinition.builder()
                .nodeId(componentId)
                .nodeName(component.getComponentName())
                .nodeType(component.toLiteFlowNodeType())
                .language(component.getLanguage())
                .script(component.getScript())
                .build());
        }
        result.setNodes(nodes);

        // 3. 递归生成 EL 表达式
        String el = generateEL(flowDesign.getRoot());
        result.setEl(el);

        return result;
    }

    /**
     * 递归生成 EL 表达式
     */
    private String generateEL(FlowNode node) {
        switch (node.getType().toUpperCase()) {
            case "THEN":
                return "THEN(" + childrenToEL(node.getChildren()) + ")";

            case "WHEN":
                return "WHEN(" + childrenToEL(node.getChildren()) + ")";

            case "IF":
                String condition = generateEL(node.getCondition());
                List<FlowNode> children = node.getChildren();
                if (children.size() == 1) {
                    return "IF(" + condition + ", " + generateEL(children.get(0)) + ")";
                } else {
                    return "IF(" + condition + ", " + generateEL(children.get(0))
                         + ", " + generateEL(children.get(1)) + ")";
                }

            case "SWITCH":
                String switchCond = generateEL(node.getCondition());
                String branches = node.getChildren().stream()
                    .map(this::generateEL)
                    .collect(Collectors.joining(", "));
                // 提取分支ID
                String branchIds = node.getChildren().stream()
                    .map(n -> "'" + n.getProperties().get("branch") + "'")
                    .collect(Collectors.joining(", "));
                return "SWITCH(" + switchCond + ").to(" + branches + ").id(" + branchIds + ")";

            case "FOR":
                String forCond = generateEL(node.getCondition());
                return "FOR(" + forCond + ").DO(" + generateEL(node.getChildren().get(0)) + ")";

            case "WHILE":
                String whileCond = generateEL(node.getCondition());
                return "WHILE(" + whileCond + ").DO(" + generateEL(node.getChildren().get(0)) + ")";

            case "CATCH":
                String tryBlock = generateEL(node.getChildren().get(0));
                String catchBlock = node.getChildren().size() > 1
                    ? generateEL(node.getChildren().get(1)) : "";
                return "CATCH(" + tryBlock + ").DO(" + catchBlock + ")";

            case "COMPONENT":
            default:
                // 叶子节点，返回组件ID
                return node.getComponentRef() != null ? node.getComponentRef() : node.getId();
        }
    }

    private String childrenToEL(List<FlowNode> children) {
        return children.stream()
            .map(this::generateEL)
            .collect(Collectors.joining(", "));
    }

    private Set<String> collectComponentIds(FlowNode node) {
        Set<String> ids = new HashSet<>();
        collectComponentIdsRecursive(node, ids);
        return ids;
    }

    private void collectComponentIdsRecursive(FlowNode node, Set<String> ids) {
        if ("COMPONENT".equalsIgnoreCase(node.getType()) || node.getComponentRef() != null) {
            ids.add(node.getComponentRef());
        }
        if (node.getCondition() != null) {
            collectComponentIdsRecursive(node.getCondition(), ids);
        }
        if (node.getChildren() != null) {
            node.getChildren().forEach(child -> collectComponentIdsRecursive(child, ids));
        }
    }
}
```

### 4.2 流程部署服务

```java
@Service
public class FlowDeployService {

    @Autowired
    private LiteFlowDynamicService liteFlowService;

    @Autowired
    private FlowConvertService convertService;

    /**
     * 部署流程
     */
    public DeployResult deploy(FlowDesign flowDesign) {
        // 1. 转换
        ConvertResult convertResult = convertService.convert(flowDesign);

        // 2. 注册所有节点
        for (NodeDefinition node : convertResult.getNodes()) {
            liteFlowService.addScriptNode(
                node.getNodeId(),
                node.getNodeName(),
                node.getScript(),
                node.getNodeType(),
                node.getLanguage()
            );
        }

        // 3. 注册流程链
        liteFlowService.addOrUpdateChain(
            convertResult.getChainName(),
            convertResult.getEl()
        );

        return DeployResult.success(flowDesign.getFlowId(), convertResult.getEl());
    }

    /**
     * 卸载流程
     */
    public void undeploy(String flowId) {
        liteFlowService.removeChain(flowId);
    }
}
```

---

## 五、前端增强方案

### 5.1 liteflow-editor-client 扩展点

#### 5.1.1 扩展节点类型

在 `constant/index.ts` 中添加：

```typescript
// 新增：QL组件节点类型
export enum QLComponentTypeEnum {
  QL_SCRIPT = 'QLScriptComponent',
  QL_BOOLEAN = 'QLBooleanComponent',
  QL_SWITCH = 'QLSwitchComponent',
}

// 扩展节点分组
export const QL_COMPONENT_GROUP: IGroupItem = {
  key: 'qlcomponent',
  name: 'QL组件',
  cellTypes: [], // 动态从后端加载
};
```

#### 5.1.2 组件侧边栏改造

```typescript
// panels/sideBar/ComponentPanel.tsx

interface ComponentPanelProps {
  components: QLComponent[];  // 从后端加载的组件列表
  onDragStart: (component: QLComponent) => void;
}

const ComponentPanel: React.FC<ComponentPanelProps> = ({ components, onDragStart }) => {
  // 按分类分组
  const categorizedComponents = useMemo(() => {
    return components.reduce((acc, comp) => {
      const category = comp.category || '未分类';
      if (!acc[category]) acc[category] = [];
      acc[category].push(comp);
      return acc;
    }, {} as Record<string, QLComponent[]>);
  }, [components]);

  return (
    <div className="component-panel">
      {Object.entries(categorizedComponents).map(([category, comps]) => (
        <div key={category} className="component-category">
          <div className="category-title">{category}</div>
          <div className="component-list">
            {comps.map(comp => (
              <ComponentCard
                key={comp.componentId}
                component={comp}
                onDragStart={() => onDragStart(comp)}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

const ComponentCard: React.FC<{ component: QLComponent; onDragStart: () => void }> = ({
  component,
  onDragStart
}) => {
  const getTypeIcon = () => {
    switch (component.componentType) {
      case 'BOOLEAN': return '⚡';
      case 'SWITCH': return `🔀 (${component.switchBranches?.length || 0})`;
      case 'SCRIPT': return '📝';
    }
  };

  return (
    <div
      className="component-card"
      draggable
      onDragStart={onDragStart}
    >
      <div className="component-icon">{component.icon || '📦'}</div>
      <div className="component-info">
        <div className="component-name">{component.componentName}</div>
        <div className="component-type">{getTypeIcon()}</div>
      </div>
    </div>
  );
};
```

#### 5.1.3 智能分支处理

```typescript
// hooks/useSmartBranch.ts

/**
 * 处理 SWITCH 组件的智能分支创建
 */
export const useSmartBranch = () => {
  const { graph, model, setModel } = useGraphContext();

  const handleComponentDrop = useCallback((
    component: QLComponent,
    targetNode: ELNode,
    position: 'condition' | 'child'
  ) => {
    if (component.componentType === 'SWITCH' && position === 'condition') {
      // SWITCH 组件作为条件节点时，自动创建分支
      const branches = component.switchBranches || [];

      // 为每个分支创建占位节点
      const branchNodes = branches.map(branch =>
        NodeOperator.create(targetNode, NodeTypeEnum.COMMON)
          .setProperties({
            id: `${component.componentId}_${branch.branchId}`,
            branch: branch.branchId,
            branchName: branch.branchName
          })
      );

      // 设置子节点
      targetNode.children = branchNodes;

      // 触发更新
      refreshGraph();
    }
  }, [graph, model]);

  return { handleComponentDrop };
};
```

#### 5.1.4 组件属性面板

```typescript
// panels/settingBar/ComponentSettingPanel.tsx

const ComponentSettingPanel: React.FC<{ node: ELNode }> = ({ node }) => {
  const [component, setComponent] = useState<QLComponent | null>(null);
  const componentRef = node.properties?.componentRef;

  useEffect(() => {
    if (componentRef) {
      fetchComponent(componentRef).then(setComponent);
    }
  }, [componentRef]);

  if (!component) return null;

  return (
    <div className="component-setting-panel">
      <div className="panel-header">
        <span>{component.componentName}</span>
        <Tag color={getTypeColor(component.componentType)}>
          {component.componentType}
        </Tag>
      </div>

      <div className="panel-section">
        <div className="section-title">输入参数</div>
        {component.inputs?.map(input => (
          <div key={input.name} className="param-item">
            <span className="param-name">{input.name}</span>
            <span className="param-type">{input.type}</span>
            {input.required && <Tag color="red">必填</Tag>}
          </div>
        ))}
      </div>

      {component.componentType === 'SWITCH' && (
        <div className="panel-section">
          <div className="section-title">分支定义</div>
          {component.switchBranches?.map(branch => (
            <div key={branch.branchId} className="branch-item">
              <span className="branch-id">{branch.branchId}</span>
              <span className="branch-name">{branch.branchName}</span>
            </div>
          ))}
        </div>
      )}

      <div className="panel-section">
        <div className="section-title">脚本预览</div>
        <pre className="script-preview">
          {component.script}
        </pre>
      </div>
    </div>
  );
};
```

### 5.2 组件设计器 (新增页面)

```typescript
// pages/ComponentDesigner.tsx

const ComponentDesigner: React.FC = () => {
  const [form] = Form.useForm();
  const [componentType, setComponentType] = useState<ComponentType>('SCRIPT');
  const [switchBranches, setSwitchBranches] = useState<SwitchBranch[]>([]);

  const handleSave = async () => {
    const values = await form.validateFields();
    const component: QLComponent = {
      ...values,
      componentType,
      switchBranches: componentType === 'SWITCH' ? switchBranches : undefined,
    };
    await saveComponent(component);
    message.success('组件保存成功');
  };

  return (
    <div className="component-designer">
      <div className="designer-header">
        <h2>组件设计器</h2>
        <Button type="primary" onClick={handleSave}>保存组件</Button>
      </div>

      <div className="designer-body">
        <div className="form-section">
          <Form form={form} layout="vertical">
            <Form.Item name="componentId" label="组件ID" rules={[{ required: true }]}>
              <Input placeholder="唯一标识，如: ageCheck" />
            </Form.Item>

            <Form.Item name="componentName" label="组件名称" rules={[{ required: true }]}>
              <Input placeholder="显示名称，如: 年龄检查" />
            </Form.Item>

            <Form.Item name="category" label="分类">
              <Select placeholder="选择分类">
                <Option value="风控">风控</Option>
                <Option value="营销">营销</Option>
                <Option value="支付">支付</Option>
                <Option value="通用">通用</Option>
              </Select>
            </Form.Item>

            <Form.Item label="组件类型" required>
              <Radio.Group value={componentType} onChange={e => setComponentType(e.target.value)}>
                <Radio value="SCRIPT">普通脚本</Radio>
                <Radio value="BOOLEAN">条件判断</Radio>
                <Radio value="SWITCH">路由选择</Radio>
              </Radio.Group>
            </Form.Item>

            {/* 输入参数 */}
            <ParameterEditor
              title="输入参数"
              value={form.getFieldValue('inputs')}
              onChange={inputs => form.setFieldsValue({ inputs })}
            />

            {/* SWITCH 分支定义 */}
            {componentType === 'SWITCH' && (
              <SwitchBranchEditor
                branches={switchBranches}
                onChange={setSwitchBranches}
              />
            )}
          </Form>
        </div>

        <div className="script-section">
          <div className="section-header">
            <span>QLExpress 脚本</span>
            <Button size="small" onClick={() => {}}>测试执行</Button>
          </div>
          <MonacoEditor
            language="javascript"
            theme="vs-dark"
            value={form.getFieldValue('script')}
            onChange={script => form.setFieldsValue({ script })}
            options={{
              minimap: { enabled: false },
              lineNumbers: 'on',
            }}
          />
          <div className="script-help">
            <p>可用变量: <code>flowContext</code> - 流程上下文</p>
            <p>获取数据: <code>flowContext.getString('key')</code>, <code>flowContext.getInt('key')</code></p>
            <p>设置数据: <code>flowContext.setData('key', value)</code></p>
            {componentType === 'BOOLEAN' && <p>返回值: <code>return true/false;</code></p>}
            {componentType === 'SWITCH' && <p>返回值: <code>return "分支ID";</code></p>}
          </div>
        </div>
      </div>
    </div>
  );
};
```

---

## 六、完整使用示例

### 6.1 场景：会员订单折扣计算

#### Step 1: 创建组件

```bash
# 1. 创建初始化组件
POST /api/component
{
  "componentId": "initOrder",
  "componentName": "订单初始化",
  "category": "订单",
  "componentType": "SCRIPT",
  "script": "flowContext.setData('startTime', System.currentTimeMillis()); flowContext.setData('processed', false);",
  "inputs": []
}

# 2. 创建会员路由组件
POST /api/component
{
  "componentId": "memberRouter",
  "componentName": "会员等级路由",
  "category": "营销",
  "componentType": "SWITCH",
  "script": "level = flowContext.getString('memberLevel', 'NORMAL'); return level;",
  "inputs": [
    { "name": "memberLevel", "type": "String", "description": "会员等级", "required": false }
  ],
  "switchBranches": [
    { "branchId": "VIP", "branchName": "VIP会员" },
    { "branchId": "GOLD", "branchName": "黄金会员" },
    { "branchId": "NORMAL", "branchName": "普通会员" }
  ],
  "defaultBranch": "NORMAL"
}

# 3. 创建VIP折扣组件
POST /api/component
{
  "componentId": "vipDiscount",
  "componentName": "VIP折扣计算",
  "category": "营销",
  "componentType": "SCRIPT",
  "script": "amount = flowContext.getDouble('orderAmount'); flowContext.setData('discount', 0.7); flowContext.setData('finalAmount', amount * 0.7);",
  "inputs": [
    { "name": "orderAmount", "type": "Double", "description": "订单金额", "required": true }
  ],
  "outputs": [
    { "name": "discount", "type": "Double", "description": "折扣率" },
    { "name": "finalAmount", "type": "Double", "description": "最终金额" }
  ]
}

# 4. 创建黄金会员折扣组件
POST /api/component
{
  "componentId": "goldDiscount",
  "componentName": "黄金会员折扣",
  "category": "营销",
  "componentType": "SCRIPT",
  "script": "amount = flowContext.getDouble('orderAmount'); flowContext.setData('discount', 0.85); flowContext.setData('finalAmount', amount * 0.85);"
}

# 5. 创建普通价格组件
POST /api/component
{
  "componentId": "normalPrice",
  "componentName": "普通价格",
  "category": "营销",
  "componentType": "SCRIPT",
  "script": "amount = flowContext.getDouble('orderAmount'); flowContext.setData('discount', 1.0); flowContext.setData('finalAmount', amount);"
}

# 6. 创建结算组件
POST /api/component
{
  "componentId": "settlement",
  "componentName": "订单结算",
  "category": "订单",
  "componentType": "SCRIPT",
  "script": "startTime = flowContext.getLong('startTime'); flowContext.setData('processTime', System.currentTimeMillis() - startTime); flowContext.setData('processed', true);"
}
```

#### Step 2: 创建并部署流程

```bash
# 创建流程编排
POST /api/flow
{
  "flowId": "memberOrderDiscount",
  "flowName": "会员订单折扣流程",
  "description": "根据会员等级计算不同折扣",
  "root": {
    "type": "THEN",
    "children": [
      {
        "type": "component",
        "componentRef": "initOrder"
      },
      {
        "type": "SWITCH",
        "condition": {
          "type": "component",
          "componentRef": "memberRouter"
        },
        "children": [
          { "type": "component", "componentRef": "vipDiscount", "properties": { "branch": "VIP" } },
          { "type": "component", "componentRef": "goldDiscount", "properties": { "branch": "GOLD" } },
          { "type": "component", "componentRef": "normalPrice", "properties": { "branch": "NORMAL" } }
        ]
      },
      {
        "type": "component",
        "componentRef": "settlement"
      }
    ]
  }
}

# 部署流程
POST /api/flow/memberOrderDiscount/deploy
```

#### Step 3: 执行流程

```bash
# 执行 VIP 会员订单
POST /api/flow/memberOrderDiscount/execute
{
  "params": {
    "userId": "U001",
    "memberLevel": "VIP",
    "orderAmount": 1000.0
  }
}

# 响应
{
  "code": 200,
  "data": {
    "success": true,
    "context": {
      "userId": "U001",
      "memberLevel": "VIP",
      "orderAmount": 1000.0,
      "discount": 0.7,
      "finalAmount": 700.0,
      "processTime": 12,
      "processed": true
    },
    "executeSteps": "initOrder(1ms) -> memberRouter(1ms) -> vipDiscount(2ms) -> settlement(1ms)"
  }
}
```

### 6.2 生成的 LiteFlow EL 表达式

```
THEN(
  initOrder,
  SWITCH(memberRouter).to(vipDiscount, goldDiscount, normalPrice).id('VIP', 'GOLD', 'NORMAL'),
  settlement
)
```

---

## 七、实施计划

### 第一阶段：后端核心功能 (1-2周)

1. **组��模型与存储**
   - [ ] QLComponent 实体类
   - [ ] 组件 Repository (可用 MySQL/MongoDB/内存)
   - [ ] 组件 CRUD Service

2. **组件管理 API**
   - [ ] 创建/更新/删除/查询组件
   - [ ] 组件脚本验证
   - [ ] 组件测试执行

3. **流程转换服务**
   - [ ] JSON → LiteFlow 转换器
   - [ ] EL 表达式生成器
   - [ ] 流程验证器

4. **流程部署服务**
   - [ ] 流程部署/卸载
   - [ ] 热更新支持

### 第二阶段：前端组件库 (1-2周)

1. **组件设计器页面**
   - [ ] 基础信息表单
   - [ ] 参数配置器
   - [ ] Switch 分支编辑器
   - [ ] QLExpress 脚本编辑器 (Monaco)
   - [ ] 脚本测试功能

2. **组件库面板**
   - [ ] 组件列表展示
   - [ ] 分类筛选
   - [ ] 搜索功能
   - [ ] 拖拽支持

### 第三阶段：编排器增强 (1-2周)

1. **liteflow-editor-client 扩展**
   - [ ] 集成组件库侧边栏
   - [ ] 组件拖拽处理
   - [ ] 智能分支创建 (SWITCH)
   - [ ] 组件属性面板

2. **流程管理**
   - [ ] 流程保存/加载
   - [ ] 流程部署
   - [ ] 执行测试

### 第四阶段：完善与优化 (1周)

1. **功能完善**
   - [ ] 组件版本管理
   - [ ] 流程版本管理
   - [ ] 执行历史记录
   - [ ] 错误追踪

2. **性能优化**
   - [ ] 组件缓存
   - [ ] 流程缓存
   - [ ] 并发执行优化

---

## 八、总结

本方案通过将 **QLExpress 脚本封装为可复用组件**，结合 **LiteFlow 的可视化编排能力**，实现了：

1. **组件化**：业务逻辑模块化，可复用、可维护
2. **可视化**：拖拽式流程编排，降低使用门槛
3. **智能化**：Switch 组件自动创建对应分支
4. **动态化**：支持运行时热更新组件和流程

这种架构既保留了 QLExpress 强大的表达式能力，又利用了 LiteFlow 成熟的流程编排功能，是企业级规则引擎的理想方案。
