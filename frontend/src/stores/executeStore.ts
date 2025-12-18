import { create } from 'zustand';
import { executeApi, validateApi } from '../api';
import type { ExecuteResult, ExecuteTrace, ExecuteResponse } from '../types/api';

/**
 * 执行历史记录
 */
interface ExecuteHistoryItem {
  /** 脚本内容 */
  script: string;
  /** 执行参数 */
  params: Record<string, unknown>;
  /** 执行结果 */
  result: unknown;
  /** 是否成功 */
  success: boolean;
  /** 错误信息 */
  errorMessage?: string;
  /** 执行耗时 (ms) */
  duration: number;
  /** 执行时间戳 */
  timestamp: number;
  /** 执行轨迹 */
  trace?: ExecuteTrace[];
}

/**
 * 执行状态
 */
interface ExecuteState {
  /** 执行中 */
  executing: boolean;
  /** 当前结果 */
  currentResult: ExecuteResult | null;
  /** 执行历史 */
  history: ExecuteHistoryItem[];
  /** 最大历史记录数 */
  maxHistorySize: number;
}

/**
 * 执行操作
 */
interface ExecuteActions {
  /** 执行脚本 */
  executeScript: (script: string, params?: Record<string, unknown>) => Promise<ExecuteResult>;
  /** 执行流程 (使用 CmpProperty) */
  executeFlow: (flowId: string, params?: Record<string, unknown>) => Promise<ExecuteResult>;
  /** 验证脚本 */
  validateScript: (script: string) => Promise<{ valid: boolean; errors?: string[] }>;
  /** 验证流程 */
  validateFlow: (flowId: string) => Promise<{ valid: boolean; errors?: string[] }>;
  /** 清除当前结果 */
  clearResult: () => void;
  /** 清除历史 */
  clearHistory: () => void;
  /** 设置最大历史记录数 */
  setMaxHistorySize: (size: number) => void;
}

type ExecuteStore = ExecuteState & ExecuteActions;

/**
 * 将 ExecuteResponse 转换为 ExecuteResult
 */
function convertResponse(response: ExecuteResponse): ExecuteResult {
  return {
    success: response.success,
    result: response.result,
    context: response.outputContext,
    errorMessage: response.error?.message,
    stackTrace: response.error?.stackTrace,
    duration: response.stats?.duration,
    trace: response.traces?.map(t => ({
      expression: t.statement,
      result: t.value,
      duration: 0,
    })),
  };
}

/**
 * 执行状态管理
 */
export const useExecuteStore = create<ExecuteStore>((set, get) => ({
  // 状态
  executing: false,
  currentResult: null,
  history: [],
  maxHistorySize: 50,

  // 执行脚本
  executeScript: async (script, params = {}) => {
    set({ executing: true });
    const startTime = Date.now();

    try {
      const axiosResponse = await executeApi.script({ script, context: params });
      const response = axiosResponse.data.data;
      const result = convertResponse(response);
      const duration = Date.now() - startTime;

      // 更新结果
      set({ currentResult: result, executing: false });

      // 添加历史记录
      const historyItem: ExecuteHistoryItem = {
        script,
        params,
        result: result.result,
        success: result.success,
        errorMessage: result.errorMessage,
        duration: result.duration || duration,
        timestamp: Date.now(),
        trace: result.trace,
      };

      const { history, maxHistorySize } = get();
      const newHistory = [historyItem, ...history].slice(0, maxHistorySize);
      set({ history: newHistory });

      return result;
    } catch (error) {
      const duration = Date.now() - startTime;
      const errorMessage = error instanceof Error ? error.message : '执行失败';

      const result: ExecuteResult = {
        success: false,
        errorMessage,
        duration,
      };

      set({ currentResult: result, executing: false });

      // 添加失败记录
      const historyItem: ExecuteHistoryItem = {
        script,
        params,
        result: null,
        success: false,
        errorMessage,
        duration,
        timestamp: Date.now(),
      };

      const { history, maxHistorySize } = get();
      const newHistory = [historyItem, ...history].slice(0, maxHistorySize);
      set({ history: newHistory });

      throw error;
    }
  },

  // 执行流程 - 注意: 当前实现仅作为占位，实际需要根据 flowId 获取流程定义
  executeFlow: async (flowId, params = {}) => {
    set({ executing: true });
    const startTime = Date.now();

    try {
      // 流程执行需要传入 CmpProperty 结构
      // 这里使用 script API 作为备选方案
      const axiosResponse = await executeApi.quick(`// Flow: ${flowId}\nreturn "Flow execution placeholder";`);
      const response = axiosResponse.data.data;
      const result = convertResponse(response);
      const duration = Date.now() - startTime;

      set({ currentResult: result, executing: false });

      // 添加历史记录
      const historyItem: ExecuteHistoryItem = {
        script: `[Flow: ${flowId}]`,
        params,
        result: result.result,
        success: result.success,
        errorMessage: result.errorMessage,
        duration: result.duration || duration,
        timestamp: Date.now(),
        trace: result.trace,
      };

      const { history, maxHistorySize } = get();
      const newHistory = [historyItem, ...history].slice(0, maxHistorySize);
      set({ history: newHistory });

      return result;
    } catch (error) {
      const duration = Date.now() - startTime;
      const errorMessage = error instanceof Error ? error.message : '执行失败';

      const result: ExecuteResult = {
        success: false,
        errorMessage,
        duration,
      };

      set({ currentResult: result, executing: false });
      throw error;
    }
  },

  // 验证脚本
  validateScript: async (script) => {
    try {
      const axiosResponse = await validateApi.script({ script });
      const result = axiosResponse.data.data;
      return {
        valid: result.valid,
        errors: result.syntaxErrors?.map((e: { line: number; message: string }) => `Line ${e.line}: ${e.message}`),
      };
    } catch (error) {
      return {
        valid: false,
        errors: [error instanceof Error ? error.message : '验证失败'],
      };
    }
  },

  // 验证流程
  validateFlow: async (flowId) => {
    try {
      // 流程验证需要获取流程定义后调用
      // 这里仅作为占位返回
      return { valid: true };
    } catch (error) {
      return {
        valid: false,
        errors: [error instanceof Error ? error.message : '验证失败'],
      };
    }
  },

  // 清除当前结果
  clearResult: () => {
    set({ currentResult: null });
  },

  // 清除历史
  clearHistory: () => {
    set({ history: [] });
  },

  // 设置最大历史记录数
  setMaxHistorySize: (size) => {
    set({ maxHistorySize: size });
    const { history } = get();
    if (history.length > size) {
      set({ history: history.slice(0, size) });
    }
  },
}));

export default useExecuteStore;
