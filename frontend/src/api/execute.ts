import { request } from './client';
import type {
  ExecuteScriptRequest,
  ExecuteResponse,
} from '../types';
import type { CmpProperty } from '../types/flow';

/**
 * 脚本执行 API
 */
export const executeApi = {
  /**
   * 执行 QL 脚本
   */
  script(data: ExecuteScriptRequest) {
    return request.post<ExecuteResponse>('/api/v1/execute/script', data);
  },

  /**
   * 执行可视化流程
   */
  flow(data: {
    flow: { root: CmpProperty };
    params?: Record<string, unknown>;
    options?: {
      timeout?: number;
      traceEnabled?: boolean;
    };
  }) {
    return request.post<ExecuteResponse>('/api/v1/execute/flow', data);
  },

  /**
   * 快速执行 (简化参数)
   */
  quick(script: string) {
    return request.post<ExecuteResponse>('/api/v1/execute/quick', script, {
      headers: { 'Content-Type': 'text/plain' },
    });
  },

  /**
   * 执行并返回追踪信息
   */
  trace(data: ExecuteScriptRequest) {
    return request.post<ExecuteResponse>('/api/v1/execute/trace', data);
  },
};
