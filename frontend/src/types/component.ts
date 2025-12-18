import type { ComponentType } from './enums';

/**
 * 参数定义 - 对应后端 ParameterDefinition
 * 定义组件的输入/输出参数
 */
export interface ParameterDef {
  /** 参数名 (对应 flowContext key) */
  name: string;
  /** 类型 (String, Integer, Boolean, List, Map 等) */
  type: string;
  /** 描述 */
  description?: string;
  /** 是否必填 */
  required?: boolean;
  /** 默认值 */
  defaultValue?: unknown;
  /** 验证规则 */
  validation?: string;
}

/**
 * Switch 分支定义 - 对应后端 SwitchBranch
 * 定义 SWITCH 类型组件的分支选项
 */
export interface SwitchBranch {
  /** 分支标识 (脚本返回值) */
  branchId: string;
  /** 分支显示名 */
  branchName: string;
  /** 分支描述 */
  description?: string;
  /** 分支样式/颜色 */
  style?: string;
}

/**
 * QLExpress 组件定义 - 对应后端 QLComponent
 * 每个组件对应一个 LiteFlow 脚本节点
 */
export interface QLComponent {
  // ===== 标识信息 =====
  /** 全局唯一标识 (对应 LiteFlow nodeId) */
  componentId: string;
  /** 显示名称 */
  componentName: string;
  /** 组件描述 */
  description?: string;
  /** 分类 */
  category?: string;
  /** 图标 */
  icon?: string;

  // ===== 类型与行为 =====
  /** 组件类型: SCRIPT | BOOLEAN | SWITCH */
  componentType: ComponentType;
  /** 脚本语言 (默认 qlexpress) */
  language?: string;

  // ===== 脚本内容 =====
  /** QLExpress 脚本 */
  script: string;

  // ===== 参数契约 =====
  /** 输入参数 (从 flowContext 获取) */
  inputs?: ParameterDef[];
  /** 输出参数 (设置到 flowContext) */
  outputs?: ParameterDef[];

  // ===== SWITCH 专用 =====
  /** 分支定义 */
  switchBranches?: SwitchBranch[];
  /** 默认分支 */
  defaultBranch?: string;

  // ===== 版本与状态 =====
  /** 版本 */
  version?: string;
  /** 是否启用 */
  enabled?: boolean;

  // ===== 扩展 =====
  /** 元数据 */
  metadata?: Record<string, unknown>;
}

/**
 * 组件创建/更新请求
 */
export interface ComponentRequest {
  componentId: string;
  componentName: string;
  description?: string;
  category?: string;
  icon?: string;
  componentType: ComponentType;
  script: string;
  language?: string;
  inputs?: ParameterDef[];
  outputs?: ParameterDef[];
  switchBranches?: SwitchBranch[];
  defaultBranch?: string;
  version?: string;
  enabled?: boolean;
  metadata?: Record<string, unknown>;
}

/**
 * 组件列表查询参数
 */
export interface ComponentListParams {
  page?: number;
  size?: number;
  category?: string;
  componentType?: ComponentType;
  enabled?: boolean;
  keyword?: string;
}

/**
 * 组件测试请求
 */
export interface ComponentTestRequest {
  script?: string;
  language?: string;
  params?: Record<string, unknown>;
}

/**
 * 脚本验证请求
 */
export interface ScriptValidateRequest {
  script: string;
  language?: string;
}
