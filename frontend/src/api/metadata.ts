import { request } from './client';
import type {
  MetadataResponse,
  OperatorMetadata,
  FunctionMetadata,
  NodeTemplate,
} from '../types';

/**
 * 元数据 API
 */
export const metadataApi = {
  /**
   * 获取所有元数据
   */
  getAll() {
    return request.get<MetadataResponse>('/api/v1/metadata');
  },

  /**
   * 获取操作符列表
   */
  getOperators(category?: string) {
    return request.get<OperatorMetadata[]>('/api/v1/metadata/operators', {
      params: category ? { category } : undefined,
    });
  },

  /**
   * 获取函数列表
   */
  getFunctions(params?: { category?: string; search?: string }) {
    return request.get<FunctionMetadata[]>('/api/v1/metadata/functions', { params });
  },

  /**
   * 获取节点模板列表
   */
  getNodeTemplates(category?: string) {
    return request.get<NodeTemplate[]>('/api/v1/metadata/node-templates', {
      params: category ? { category } : undefined,
    });
  },
};
