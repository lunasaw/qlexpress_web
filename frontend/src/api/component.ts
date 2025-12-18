import { request } from './client';
import type {
  QLComponent,
  ComponentRequest,
  ComponentListParams,
  ComponentTestRequest,
  ScriptValidateRequest,
  ComponentListData,
  ExecuteResponse,
  ValidateScriptResponse,
} from '../types';

/**
 * 组件管理 API
 */
export const componentApi = {
  /**
   * 创建组件
   */
  create(data: ComponentRequest) {
    return request.post<QLComponent>('/api/component', data);
  },

  /**
   * 获取组件详情
   */
  get(componentId: string) {
    return request.get<QLComponent>(`/api/component/${componentId}`);
  },

  /**
   * 更新组件
   */
  update(componentId: string, data: Partial<ComponentRequest>) {
    return request.put<QLComponent>(`/api/component/${componentId}`, data);
  },

  /**
   * 删除组件
   */
  delete(componentId: string) {
    return request.delete(`/api/component/${componentId}`);
  },

  /**
   * 获取组件列表
   */
  list(params?: ComponentListParams) {
    return request.get<ComponentListData<QLComponent>>('/api/component/list', { params });
  },

  /**
   * 搜索组件
   */
  search(keyword: string) {
    return request.get<QLComponent[]>('/api/component/search', { params: { keyword } });
  },

  /**
   * 批量创建组件
   */
  batchCreate(components: ComponentRequest[]) {
    return request.post<QLComponent[]>('/api/component/batch', components);
  },

  /**
   * 获取所有分类
   */
  getCategories() {
    return request.get<string[]>('/api/component/category');
  },

  /**
   * 获取分类统计
   */
  getCategoryStats() {
    return request.get<Record<string, number>>('/api/component/category/stats');
  },

  /**
   * 验证组件 (已保存的组件)
   */
  validate(componentId: string) {
    return request.post<ValidateScriptResponse>(`/api/component/${componentId}/validate`);
  },

  /**
   * 验证脚本语法 (不保存)
   */
  validateScript(data: ScriptValidateRequest) {
    return request.post<ValidateScriptResponse>('/api/component/validate', data);
  },

  /**
   * 测试组件执行 (已保存的组件)
   */
  test(componentId: string, params?: ComponentTestRequest) {
    return request.post<ExecuteResponse>(`/api/component/${componentId}/test`, params);
  },

  /**
   * 直接测试脚本 (不保存)
   */
  testScript(data: ComponentTestRequest) {
    return request.post<ExecuteResponse>('/api/component/test', data);
  },

  /**
   * 同步组件到 LiteFlow
   */
  sync(componentId: string) {
    return request.post(`/api/component/${componentId}/sync`);
  },

  /**
   * 同步所有组件到 LiteFlow
   */
  syncAll() {
    return request.post('/api/component/sync-all');
  },
};
