import { request } from './client';
import type {
  ValidateScriptRequest,
  ValidateScriptResponse,
  ComplexityAnalysis,
} from '../types';
import type { CmpProperty } from '../types/flow';

/**
 * 脚本验证 API
 */
export const validateApi = {
  /**
   * 完整验证 (语法 + 语义 + 最佳实践)
   */
  script(data: ValidateScriptRequest) {
    return request.post<ValidateScriptResponse>('/api/v1/validate/script', data);
  },

  /**
   * 验证可视化流程
   */
  flow(data: { flow: { root: CmpProperty } }) {
    return request.post<{
      valid: boolean;
      errors?: Array<{ nodeId?: string; message: string }>;
      warnings?: Array<{ nodeId?: string; message: string }>;
    }>('/api/v1/validate/flow', data);
  },

  /**
   * 快速验证 (仅语法)
   */
  quick(script: string) {
    return request.post<{
      valid: boolean;
      errors?: Array<{ line: number; message: string }>;
    }>('/api/v1/validate/quick', script, {
      headers: { 'Content-Type': 'text/plain' },
    });
  },

  /**
   * 分析脚本复杂度
   */
  complexity(script: string) {
    return request.post<ComplexityAnalysis>('/api/v1/validate/complexity', script, {
      headers: { 'Content-Type': 'text/plain' },
    });
  },

  /**
   * 完整验证
   */
  full(script: string) {
    return request.post<ValidateScriptResponse>('/api/v1/validate/full', script, {
      headers: { 'Content-Type': 'text/plain' },
    });
  },
};
