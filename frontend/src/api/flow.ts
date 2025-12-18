import { request } from './client';
import type {
  FlowDesign,
  FlowRequest,
  FlowListParams,
  FlowExecuteRequest,
  CmpProperty,
  PageData,
  ExecuteResponse,
  FlowValidateResult,
  FlowStatusInfo,
  ELPreviewResult,
} from '../types';

/**
 * 流程管理 API
 */
export const flowApi = {
  /**
   * 创建流程
   */
  create(data: FlowRequest) {
    return request.post<FlowDesign>('/api/flow', data);
  },

  /**
   * 获取流程详情
   */
  get(flowId: string) {
    return request.get<FlowDesign>(`/api/flow/${flowId}`);
  },

  /**
   * 更新流程
   */
  update(flowId: string, data: Partial<FlowRequest>) {
    return request.put<FlowDesign>(`/api/flow/${flowId}`, data);
  },

  /**
   * 删除流程
   */
  delete(flowId: string) {
    return request.delete(`/api/flow/${flowId}`);
  },

  /**
   * 获取流程列表
   */
  list(params?: FlowListParams) {
    return request.get<PageData<FlowDesign>>('/api/flow/list', { params });
  },

  /**
   * 搜索流程
   */
  search(keyword: string) {
    return request.get<FlowDesign[]>('/api/flow/search', { params: { keyword } });
  },

  /**
   * 转换流程为 LiteFlow 配置
   */
  convert(flowId: string) {
    return request.post<{ el: string; nodes: unknown[] }>(`/api/flow/${flowId}/convert`);
  },

  /**
   * 预览 EL 表达式 (不保存)
   */
  previewEL(root: CmpProperty) {
    return request.post<ELPreviewResult>('/api/flow/preview-el', { root });
  },

  /**
   * 验证流程 (已保存的流程)
   */
  validate(flowId: string) {
    return request.post<FlowValidateResult>(`/api/flow/${flowId}/validate`);
  },

  /**
   * 验证流程设计 (不保存)
   */
  validateDesign(flowDesign: FlowRequest) {
    return request.post<FlowValidateResult>('/api/flow/validate', flowDesign);
  },

  /**
   * 部署流程到 LiteFlow
   */
  deploy(flowId: string) {
    return request.post(`/api/flow/${flowId}/deploy`);
  },

  /**
   * 卸载流程
   */
  undeploy(flowId: string) {
    return request.post(`/api/flow/${flowId}/undeploy`);
  },

  /**
   * 获取流程部署状态
   */
  getStatus(flowId: string) {
    return request.get<FlowStatusInfo>(`/api/flow/${flowId}/status`);
  },

  /**
   * 重新部署所有流程
   */
  redeployAll() {
    return request.post('/api/flow/redeploy-all');
  },

  /**
   * 执行流程 (已部署的流程)
   */
  execute(flowId: string, params?: FlowExecuteRequest) {
    return request.post<ExecuteResponse>(`/api/flow/${flowId}/execute`, params);
  },

  /**
   * 直接执行流程设计 (不保存/不部署)
   */
  executeDirect(flowDesign: FlowRequest, params?: Record<string, unknown>) {
    return request.post<ExecuteResponse>('/api/flow/execute-direct', { flowDesign, params });
  },

  /**
   * 获取所有分类
   */
  getCategories() {
    return request.get<string[]>('/api/flow/category');
  },

  /**
   * 获取分类统计
   */
  getCategoryStats() {
    return request.get<Record<string, number>>('/api/flow/category/stats');
  },
};
