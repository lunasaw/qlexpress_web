import type { DeployStatus } from './enums';

/**
 * 属性配置 - 对应后端 CmpProperty.Properties
 * 映射 LiteFlow EL 修饰符
 */
export interface CmpProperties {
  /** .id("xxx") - 表达式标识 */
  id?: string;
  /** .tag("xxx") - 节点标签 */
  tag?: string;
  /** .data("xxx") - 节点数据 */
  data?: string;

  /** SWITCH 分支标识 */
  branch?: string;

  /** WHEN 配置 */
  /** .maxWaitSeconds(n) */
  maxWaitSeconds?: number;
  /** .ignoreError(true) */
  ignoreError?: boolean;
  /** .must() */
  must?: boolean;
  /** .any(true) */
  any?: boolean;

  /** 循环配置 */
  /** FOR 循环次数 */
  loopCount?: number;
  /** BREAK 条件 */
  breakCondition?: CmpProperty;
}

/**
 * 可视化组件树节点 - 对应后端 CmpProperty
 * 用于前端画布与后端 EL 表达式双向转换
 *
 * 设计要点：
 * 1. 树形结构表示编排关系
 * 2. type 区分编排类型和节点类型
 * 3. componentRef 引用 QLComponent.componentId
 */
export interface CmpProperty {
  // ===== 节点标识 =====
  /** 节点 ID (自动生成或组件ID) */
  id?: string;

  /**
   * 节点类型
   * - 编排类型: THEN, WHEN, IF, SWITCH, FOR, WHILE, CATCH, AND, OR, NOT
   * - 节点类型: CommonNode, BooleanNode, SwitchNode, COMPONENT
   */
  type: string;

  // ===== 组件引用 (叶子节点) =====
  /** 引用 QLComponent.componentId */
  componentRef?: string;

  // ===== 属性配置 =====
  properties?: CmpProperties;

  // ===== 条件节点 (IF/SWITCH/FOR/WHILE) =====
  condition?: CmpProperty;

  // ===== 子节点 (树结构) =====
  children?: CmpProperty[];
}

/**
 * 流程设计 - 对应后端 FlowDesign
 */
export interface FlowDesign {
  /** 流程唯一标识 */
  flowId: string;
  /** 流程名称 */
  flowName: string;
  /** 流程描述 */
  description?: string;
  /** 分类 */
  category?: string;
  /** 编排定义根节点 */
  root: CmpProperty;
  /** 版本 */
  version?: string;
  /** 标签 */
  tags?: string[];
  /** 是否启用 */
  enabled?: boolean;
  /** 扩展属性 */
  metadata?: Record<string, unknown>;
  /** 使用的组件ID列表 */
  usedComponentIds?: string[];
  /** 部署状态 */
  deployStatus?: DeployStatus;
  /** 创建时间 */
  createTime?: string;
  /** 更新时间 */
  updateTime?: string;
  /** 最后部署时间 */
  lastDeployTime?: string;
}

/**
 * 流程创建/更新请求
 */
export interface FlowRequest {
  flowId?: string;
  flowName: string;
  description?: string;
  category?: string;
  version?: string;
  enabled?: boolean;
  root: CmpProperty | Record<string, unknown>;
  metadata?: Record<string, unknown>;
}

/**
 * 流程列表查询参数
 */
export interface FlowListParams {
  page?: number;
  size?: number;
  category?: string;
  enabled?: boolean;
  deployStatus?: DeployStatus;
  keyword?: string;
}

/**
 * 流程执行请求
 */
export interface FlowExecuteRequest {
  flowId?: string;
  params?: Record<string, unknown>;
  options?: {
    timeout?: number;
    traceEnabled?: boolean;
  };
}

/**
 * EL 预览结果
 */
export interface ELPreviewResult {
  el: string;
  nodes?: string[];
}

/**
 * 流程验证结果
 */
export interface FlowValidateResult {
  valid: boolean;
  errors?: Array<{
    nodeId?: string;
    message: string;
    type: 'error' | 'warning';
  }>;
  warnings?: Array<{
    nodeId?: string;
    message: string;
  }>;
}

/**
 * 流程部署状态信息
 */
export interface FlowStatusInfo {
  flowId: string;
  deployStatus: DeployStatus;
  lastDeployTime?: string;
  chainName?: string;
  nodeCount?: number;
}

/**
 * 流程列表数据 - 后端分页返回格式
 */
export interface FlowListData {
  flows: FlowDesign[];
  categories: string[];
  categoryStats: Record<string, number>;
  total: number;
  page?: number;
  size?: number;
}

/**
 * 流程筛选条件
 */
export interface FlowFilter {
  category?: string;
  deployStatus?: DeployStatus;
  keyword?: string;
  enabled?: boolean;
}

/**
 * 流程统计信息
 */
export interface FlowStats {
  total: number;
  deployed: number;
  notDeployed: number;
  modified: number;
  failed: number;
  categoryStats: Record<string, number>;
}
