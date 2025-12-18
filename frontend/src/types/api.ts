import type { TraceType } from './enums';

/**
 * 通用 API 响应格式 - 对应后端 ApiResponse<T>
 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  code: string;
  message: string;
  data: T;
  timestamp: number;
}

/**
 * 分页数据格式
 */
export interface PageData<T> {
  content: T[];
  total: number;
  page?: number;
  size?: number;
  totalPages?: number;
}

/**
 * 组件列表响应 - 后端返回的特殊结构
 */
export interface ComponentListData<T> {
  components: T[];
  categories: string[];
  categoryStats: Record<string, number>;
  total: number;
  page?: number;
  size?: number;
}

/**
 * 执行选项
 */
export interface ExecuteOptions {
  /** 超时时间 (毫秒) */
  timeout?: number;
  /** 是否启用沙箱 */
  sandbox?: boolean;
  /** 是否启用追踪 */
  enableTrace?: boolean;
  /** 导入类列表 */
  imports?: string[];
}

/**
 * 脚本执行请求
 */
export interface ExecuteScriptRequest {
  script: string;
  context?: Record<string, unknown>;
  options?: ExecuteOptions;
}

/**
 * 追踪点
 */
export interface TracePoint {
  lineNumber: number;
  statement: string;
  type: TraceType;
  value?: unknown;
  timestamp: number;
}

/**
 * 执行统计
 */
export interface ExecuteStats {
  startTime: number;
  endTime: number;
  duration: number;
  lineCount: number;
  stepCount: number;
}

/**
 * 执行错误
 */
export interface ExecuteError {
  message: string;
  errorCode?: string;
  stackTrace?: string;
  location?: {
    line: number;
    column?: number;
  };
}

/**
 * 执行响应
 */
export interface ExecuteResponse {
  success: boolean;
  result?: unknown;
  resultType?: string;
  traces?: TracePoint[];
  stats?: ExecuteStats;
  error?: ExecuteError;
  outputContext?: Record<string, unknown>;
}

/**
 * 语法错误
 */
export interface SyntaxError {
  line: number;
  column?: number;
  message: string;
  severity: 'error' | 'warning';
}

/**
 * 语义警告
 */
export interface SemanticWarning {
  line: number;
  message: string;
  suggestion?: string;
}

/**
 * 最佳实践建议
 */
export interface BestPracticeSuggestion {
  line: number;
  message: string;
  category: string;
}

/**
 * 复杂度分析
 */
export interface ComplexityAnalysis {
  cyclomatic: number;
  lines: number;
  nestingDepth: number;
  grade: 'A' | 'B' | 'C' | 'D' | 'F';
}

/**
 * 脚本验证响应
 */
export interface ValidateScriptResponse {
  valid: boolean;
  syntaxErrors?: SyntaxError[];
  semanticWarnings?: SemanticWarning[];
  suggestions?: BestPracticeSuggestion[];
  complexity?: ComplexityAnalysis;
}

/**
 * 验证请求
 */
export interface ValidateScriptRequest {
  script: string;
  checkSemantics?: boolean;
  checkBestPractices?: boolean;
}

/**
 * 操作符元数据
 */
export interface OperatorMetadata {
  name: string;
  symbol?: string;
  description: string;
  category: string;
  priority: number;
  examples?: string[];
}

/**
 * 函数元数据
 */
export interface FunctionMetadata {
  name: string;
  description: string;
  category: string;
  returnType: string;
  parameters: Array<{
    name: string;
    type: string;
    description?: string;
    optional?: boolean;
  }>;
  examples?: string[];
}

/**
 * 节点模板
 */
export interface NodeTemplate {
  id: string;
  name: string;
  category: string;
  description?: string;
  template: string;
}

/**
 * 元数据响应
 */
export interface MetadataResponse {
  operators?: OperatorMetadata[];
  functions?: FunctionMetadata[];
  nodeTemplates?: NodeTemplate[];
  keywords?: string[];
}

/**
 * LiteFlow 脚本节点请求
 */
export interface ScriptNodeRequest {
  nodeId: string;
  nodeName?: string;
  script: string;
  nodeType: 'script' | 'boolean_script' | 'switch_script';
  language?: string;
}

/**
 * LiteFlow 链请求
 */
export interface ChainRequest {
  chainName: string;
  el: string;
}

/**
 * LiteFlow 执行 EL 请求
 */
export interface ExecuteELRequest {
  el: string;
  params?: Record<string, unknown>;
}

/**
 * 流程定义请求 (批量加载)
 */
export interface FlowDefinitionRequest {
  nodes: ScriptNodeRequest[];
  chains: ChainRequest[];
}

/**
 * 执行轨迹 - 用于脚本执行器展示
 */
export interface ExecuteTrace {
  /** 表达式/步骤 */
  expression?: string;
  /** 执行结果 */
  result?: unknown;
  /** 错误信息 */
  error?: string;
  /** 耗时 (ms) */
  duration?: number;
}

/**
 * 执行结果 - 用于脚本执行器展示
 */
export interface ExecuteResult {
  /** 是否成功 */
  success: boolean;
  /** 执行结果 */
  result?: unknown;
  /** 输出上下文 */
  context?: Record<string, unknown>;
  /** 错误信息 */
  errorMessage?: string;
  /** 堆栈信息 */
  stackTrace?: string;
  /** 执行耗时 (ms) */
  duration?: number;
  /** 执行轨迹 */
  trace?: ExecuteTrace[];
}
