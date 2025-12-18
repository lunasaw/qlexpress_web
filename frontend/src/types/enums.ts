/**
 * 节点类型枚举 - 对应 liteflow-editor-client 设计
 * 叶子节点类型，对应 LiteFlow 的各种节点组件
 */
export enum NodeTypeEnum {
  // ===== 普通组件 =====
  COMMON = 'NodeComponent',                    // 普通节点

  // ===== 条件组件 =====
  BOOLEAN = 'NodeBooleanComponent',            // 布尔节点
  SWITCH = 'NodeSwitchComponent',              // 选择节点
  IF = 'NodeIfComponent',                      // IF 条件节点
  FOR = 'NodeForComponent',                    // FOR 循环次数节点
  WHILE = 'NodeWhileComponent',                // WHILE 循环条件节点
  BREAK = 'NodeBreakComponent',                // 循环跳出节点
  ITERATOR = 'NodeIteratorComponent',          // 迭代器节点

  // ===== 脚本组件 (QLExpress 核心使用) =====
  SCRIPT = 'ScriptCommonComponent',            // 普通脚本
  BOOLEAN_SCRIPT = 'ScriptBooleanComponent',   // 布尔脚本
  SWITCH_SCRIPT = 'ScriptSwitchComponent',     // 选择脚本
  IF_SCRIPT = 'ScriptIfComponent',             // IF 条件脚本
  FOR_SCRIPT = 'ScriptForComponent',           // FOR 循环次数脚本
  WHILE_SCRIPT = 'ScriptWhileComponent',       // WHILE 循环条件脚本
  BREAK_SCRIPT = 'ScriptBreakComponent',       // 循环跳出脚本

  // ===== 特殊类型 =====
  FALLBACK = 'fallback',                       // 降级节点
  VIRTUAL = 'NodeVirtualComponent',            // 虚节点 (占位)
  START = 'StartNode',                         // 开始节点
  END = 'EndNode',                             // 结束节点
}

/**
 * 编排类型枚举 - 容器/操作符节点类型
 * 对应 LiteFlow EL 表达式的各种操作符
 */
export enum ConditionTypeEnum {
  // ===== 根节点/子流程 =====
  CHAIN = 'CHAIN',           // 流程链 (根节点或子流程引用)

  // ===== 串行/并行编排 =====
  THEN = 'THEN',             // 串行编排
  SER = 'SER',               // 串行编排 (别名)
  WHEN = 'WHEN',             // 并行编排
  PAR = 'PAR',               // 并行编排 (别名)

  // ===== 条件分支 =====
  SWITCH = 'SWITCH',         // 选择编排
  IF = 'IF',                 // 条件编排

  // ===== 循环控制 =====
  FOR = 'FOR',               // 次数循环
  WHILE = 'WHILE',           // 条件循环
  ITERATOR = 'ITERATOR',     // 迭代循环
  BREAK = 'BREAK',           // 循环中断

  // ===== 异常处理 =====
  CATCH = 'CATCH',           // 异常捕获

  // ===== 逻辑运算 =====
  AND = 'AND',               // 逻辑与
  OR = 'OR',                 // 逻辑或
  NOT = 'NOT',               // 逻辑非

  // ===== 前置/后置 =====
  PRE = 'PRE',               // 前置组件
  FINALLY = 'FINALLY',       // 后置组件

  // ===== 其他 =====
  ABSTRACT = 'ABSTRACT',     // 抽象
  DEFAULT = 'DEFAULT',       // 默认分支
}

/**
 * 组件类型枚举 - 对应后端 QLComponent.ComponentType
 * 决定组件在 LiteFlow 中的节点类型和返回值约束
 */
export enum ComponentType {
  SCRIPT = 'SCRIPT',         // 普通脚本节点 - 执行业务逻辑
  BOOLEAN = 'BOOLEAN',       // 布尔判断节点 - IF/WHILE 条件
  SWITCH = 'SWITCH',         // 路由选择节点 - SWITCH 多路分支
}

/**
 * 流程部署状态
 */
export enum DeployStatus {
  NOT_DEPLOYED = 'NOT_DEPLOYED',   // 未部署
  DEPLOYED = 'DEPLOYED',           // 已部署
  MODIFIED = 'MODIFIED',           // 已修改 (需重新部署)
  FAILED = 'FAILED',               // 部署失败
}

/**
 * 追踪类型
 */
export enum TraceType {
  VARIABLE_ASSIGN = 'VARIABLE_ASSIGN',
  FUNCTION_CALL = 'FUNCTION_CALL',
  EXPRESSION = 'EXPRESSION',
  BRANCH = 'BRANCH',
  LOOP = 'LOOP',
  RETURN = 'RETURN',
}

/**
 * 节点类型到 LiteFlow 节点类型的映射
 */
export const COMPONENT_TYPE_TO_NODE_TYPE: Record<ComponentType, NodeTypeEnum> = {
  [ComponentType.SCRIPT]: NodeTypeEnum.SCRIPT,
  [ComponentType.BOOLEAN]: NodeTypeEnum.BOOLEAN_SCRIPT,
  [ComponentType.SWITCH]: NodeTypeEnum.SWITCH_SCRIPT,
};

/**
 * 节点类型到 LiteFlow 节点标识的映射
 */
export const COMPONENT_TYPE_TO_LITEFLOW_TYPE: Record<ComponentType, string> = {
  [ComponentType.SCRIPT]: 'script',
  [ComponentType.BOOLEAN]: 'boolean_script',
  [ComponentType.SWITCH]: 'switch_script',
};
