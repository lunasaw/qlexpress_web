import { create } from 'zustand';
import type { Graph } from '@antv/x6';
import type { FlowRequest } from '../types/flow';
import type { QLComponent } from '../types/component';
import { flowApi } from '../api/flow';

interface FlowState {
  // ===== 流程元数据 =====
  flowId: string | null;
  flowName: string;
  flowDescription: string;
  flowCategory: string;

  // ===== 组件数据 =====
  components: QLComponent[];

  // ===== X6 Graph 引用 =====
  graph: Graph | null;

  // ===== EL 表达式 =====
  elExpression: string;

  // ===== 状态标记 =====
  dirty: boolean;
  loading: boolean;
  error: string | null;
}

interface FlowActions {
  // 流程操作
  newFlow: () => void;
  loadFlow: (flowId: string) => Promise<any>;
  saveFlow: (flowData: Record<string, any>, newFlowId?: string) => Promise<string>;
  clearFlow: () => void;
  setFlowInfo: (info: { flowName?: string; flowDescription?: string; flowCategory?: string }) => void;

  // 组件数据
  setComponents: (components: QLComponent[]) => void;

  // Graph 操作
  setGraph: (graph: Graph | null) => void;

  // 验证与部署
  validateFlow: (flowData: Record<string, any>, flowId?: string) => Promise<{ valid: boolean; errors: string[] }>;
  deployFlow: () => Promise<void>;
  executeFlow: (params?: Record<string, unknown>) => Promise<unknown>;
}

export const useFlowStore = create<FlowState & FlowActions>((set, get) => ({
  // ===== 初始状态 =====
  flowId: null,
  flowName: '未命名流程',
  flowDescription: '',
  flowCategory: '',
  components: [],
  graph: null,
  elExpression: '',
  dirty: false,
  loading: false,
  error: null,

  // ===== 流程操作 =====
  newFlow: () => {
    set({
      flowId: null,
      flowName: '未命名流程',
      flowDescription: '',
      flowCategory: '',
      elExpression: '',
      dirty: false,
      error: null,
    });
  },

  loadFlow: async (flowId: string) => {
    set({ loading: true, error: null });
    try {
      const { data: response } = await flowApi.get(flowId);
      const flowData = response.data;

      set({
        flowId: flowData.flowId,
        flowName: flowData.flowName,
        flowDescription: flowData.description || '',
        flowCategory: flowData.category || '',
        dirty: false,
        loading: false,
      });

      return flowData;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '加载流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  saveFlow: async (flowData: Record<string, any>, newFlowId?: string) => {
    const { flowId: existingFlowId, flowName, flowDescription, flowCategory } = get();

    // 使用传入的 flowId 或已存在的 flowId
    const targetFlowId = newFlowId || existingFlowId;

    if (!targetFlowId) {
      throw new Error('流程ID不能为空');
    }

    set({ loading: true, error: null });
    try {
      const flowRequest: FlowRequest = {
        flowId: targetFlowId,
        flowName,
        description: flowDescription,
        category: flowCategory,
        root: flowData,
      };

      let savedFlowId: string;

      if (existingFlowId) {
        // 更新已存在的流程
        await flowApi.update(existingFlowId, flowRequest);
        savedFlowId = existingFlowId;
      } else {
        // 创建新流程
        const { data: response } = await flowApi.create(flowRequest);
        savedFlowId = response.data.flowId;
        set({ flowId: savedFlowId });
      }

      set({ dirty: false, loading: false });
      return savedFlowId;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '保存流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  clearFlow: () => {
    set({
      flowId: null,
      flowName: '未命名流程',
      flowDescription: '',
      flowCategory: '',
      elExpression: '',
      dirty: false,
      error: null,
    });
  },

  setFlowInfo: (info) => {
    set({
      ...info,
      dirty: true,
    });
  },

  // ===== 组件数据 =====
  setComponents: (components) => {
    set({ components });
  },

  // ===== Graph 操作 =====
  setGraph: (graph) => {
    set({ graph });
  },

  // ===== 验证与部署 =====
  validateFlow: async (flowData: Record<string, any>, newFlowId?: string) => {
    const { flowId: existingFlowId, flowName } = get();

    // 使用传入的 flowId 或已存在的 flowId
    const targetFlowId = newFlowId || existingFlowId;

    // 如果没有 flowId，跳过后端验证
    if (!targetFlowId) {
      return { valid: true, errors: [] };
    }

    try {
      const flowRequest: FlowRequest = {
        flowId: targetFlowId,
        flowName,
        root: flowData,
      };
      const { data: response } = await flowApi.validateDesign(flowRequest);
      const result = response.data;
      return {
        valid: result.valid,
        errors: result.errors?.map((e: { message: string }) => e.message) || [],
      };
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '验证失败';
      return { valid: false, errors: [message] };
    }
  },

  deployFlow: async () => {
    const { flowId } = get();
    if (!flowId) {
      throw new Error('请先保存流程');
    }

    set({ loading: true, error: null });
    try {
      await flowApi.deploy(flowId);
      set({ loading: false });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '部署失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  executeFlow: async (params) => {
    const { flowId } = get();
    if (!flowId) {
      throw new Error('请先保存并部署流程');
    }

    set({ loading: true, error: null });
    try {
      const { data: response } = await flowApi.execute(flowId, { params });
      set({ loading: false });
      return response.data;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '执行失败';
      set({ error: message, loading: false });
      throw error;
    }
  },
}));

export default useFlowStore;
