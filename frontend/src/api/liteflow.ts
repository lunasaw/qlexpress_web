import { request } from './client';
import type {
  ScriptNodeRequest,
  ChainRequest,
  ExecuteELRequest,
  FlowDefinitionRequest,
  ExecuteResponse,
} from '../types';

/**
 * LiteFlow 动态控制 API
 */
export const liteflowApi = {
  /**
   * 动态添加脚本节点
   */
  addNode(data: ScriptNodeRequest) {
    return request.post('/api/liteflow/dynamic/node', data);
  },

  /**
   * 动态刷新脚本节点
   */
  updateNode(nodeId: string, script: string) {
    return request.put(`/api/liteflow/dynamic/node/${nodeId}`, script, {
      headers: { 'Content-Type': 'text/plain' },
    });
  },

  /**
   * 动态添加/更新流程链
   */
  addChain(data: ChainRequest) {
    return request.post('/api/liteflow/dynamic/chain', data);
  },

  /**
   * 刷新指定流程链
   */
  updateChain(chainId: string, el: string) {
    return request.put(`/api/liteflow/dynamic/chain/${chainId}`, el, {
      headers: { 'Content-Type': 'text/plain' },
    });
  },

  /**
   * 移除流程链
   */
  deleteChain(chainId: string) {
    return request.delete(`/api/liteflow/dynamic/chain/${chainId}`);
  },

  /**
   * 批量加载完整流程定义
   */
  batchLoad(data: FlowDefinitionRequest) {
    return request.post('/api/liteflow/dynamic/load', data);
  },

  /**
   * 执行指定流程链
   */
  execute(chainName: string, params?: Record<string, unknown>) {
    return request.post<ExecuteResponse>(`/api/liteflow/execute/${chainName}`, params);
  },

  /**
   * 直接执行 EL 表达式
   */
  executeEL(data: ExecuteELRequest) {
    return request.post<ExecuteResponse>('/api/liteflow/dynamic/execute-el', data);
  },

  /**
   * 刷新所有规则
   */
  reloadAll() {
    return request.post('/api/liteflow/dynamic/reload-all');
  },
};
