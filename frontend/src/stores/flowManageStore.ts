import { create } from 'zustand';
import type { FlowDesign, FlowFilter, FlowListParams } from '../types/flow';
import { flowApi } from '../api/flow';

interface FlowManageState {
  // 列表数据
  flows: FlowDesign[];
  total: number;

  // 分类数据
  categories: string[];
  categoryStats: Record<string, number>;

  // 分页
  page: number;
  pageSize: number;

  // 筛选
  filter: FlowFilter;

  // 选中项
  selectedRowKeys: string[];

  // 状态
  loading: boolean;
  error: string | null;
}

interface FlowManageActions {
  // 列表操作
  fetchFlows: (params?: FlowListParams) => Promise<void>;
  refreshFlows: () => Promise<void>;
  searchFlows: (keyword: string) => Promise<void>;

  // 分页操作
  setPage: (page: number) => void;
  setPageSize: (size: number) => void;

  // 筛选操作
  setFilter: (filter: Partial<FlowFilter>) => void;
  resetFilter: () => void;

  // 选中操作
  setSelectedRowKeys: (keys: string[]) => void;
  clearSelection: () => void;

  // 单个流程操作
  deleteFlow: (flowId: string) => Promise<void>;
  deployFlow: (flowId: string) => Promise<void>;
  undeployFlow: (flowId: string) => Promise<void>;
  copyFlow: (flowId: string, newFlowId: string, newFlowName: string) => Promise<FlowDesign>;
  toggleEnabled: (flowId: string, enabled: boolean) => Promise<void>;

  // 批量操作
  batchDeleteFlows: () => Promise<void>;
  batchDeployFlows: () => Promise<void>;
  batchUndeployFlows: () => Promise<void>;

  // 导入导出
  exportFlow: (flowId: string) => Promise<FlowDesign>;
  batchExportFlows: () => Promise<FlowDesign[]>;
  importFlow: (flowDesign: FlowDesign) => Promise<FlowDesign>;

  // 分类操作
  fetchCategories: () => Promise<void>;

  // 清理
  reset: () => void;
}

const initialState: FlowManageState = {
  flows: [],
  total: 0,
  categories: [],
  categoryStats: {},
  page: 1,
  pageSize: 10,
  filter: {},
  selectedRowKeys: [],
  loading: false,
  error: null,
};

export const useFlowManageStore = create<FlowManageState & FlowManageActions>((set, get) => ({
  ...initialState,

  // ===== 列表操作 =====
  fetchFlows: async (params?: FlowListParams) => {
    const { page, pageSize, filter } = get();
    set({ loading: true, error: null });

    try {
      const queryParams: FlowListParams = {
        page: params?.page ?? page,
        size: params?.size ?? pageSize,
        ...filter,
        ...params,
      };

      const { data: response } = await flowApi.list(queryParams);
      const pageData = response.data;

      // 后端返回 flows 字段而不是 content
      const flowList = (pageData as any).flows || pageData.content || [];

      set({
        flows: flowList,
        total: pageData.total,
        categories: (pageData as any).categories || [],
        categoryStats: (pageData as any).categoryStats || {},
        page: pageData.page ?? page,
        loading: false,
      });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '获取流程列表失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  refreshFlows: async () => {
    await get().fetchFlows();
  },

  searchFlows: async (keyword: string) => {
    set({ filter: { ...get().filter, keyword }, page: 1 });
    await get().fetchFlows({ page: 1 });
  },

  // ===== 分页操作 =====
  setPage: (page) => {
    set({ page });
    get().fetchFlows({ page });
  },

  setPageSize: (size) => {
    set({ pageSize: size, page: 1 });
    get().fetchFlows({ page: 1, size });
  },

  // ===== 筛选操作 =====
  setFilter: (filter) => {
    set({ filter: { ...get().filter, ...filter }, page: 1 });
    get().fetchFlows({ page: 1 });
  },

  resetFilter: () => {
    set({ filter: {}, page: 1 });
    get().fetchFlows({ page: 1 });
  },

  // ===== 选中操作 =====
  setSelectedRowKeys: (keys) => {
    set({ selectedRowKeys: keys });
  },

  clearSelection: () => {
    set({ selectedRowKeys: [] });
  },

  // ===== 单个流程操作 =====
  deleteFlow: async (flowId: string) => {
    set({ loading: true, error: null });
    try {
      await flowApi.delete(flowId);
      await get().refreshFlows();
      set({ loading: false });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '删除流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  deployFlow: async (flowId: string) => {
    set({ loading: true, error: null });
    try {
      await flowApi.deploy(flowId);
      await get().refreshFlows();
      set({ loading: false });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '部署流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  undeployFlow: async (flowId: string) => {
    set({ loading: true, error: null });
    try {
      await flowApi.undeploy(flowId);
      await get().refreshFlows();
      set({ loading: false });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '卸载流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  copyFlow: async (flowId: string, newFlowId: string, newFlowName: string) => {
    set({ loading: true, error: null });
    try {
      const { data: response } = await flowApi.copy(flowId, newFlowId, newFlowName);
      await get().refreshFlows();
      set({ loading: false });
      return response.data;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '复制流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  toggleEnabled: async (flowId: string, enabled: boolean) => {
    set({ loading: true, error: null });
    try {
      await flowApi.toggleEnabled(flowId, enabled);
      // 更新本地状态
      set((state) => ({
        flows: state.flows.map((f) => (f.flowId === flowId ? { ...f, enabled } : f)),
        loading: false,
      }));
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '切换状态失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  // ===== 批量操作 =====
  batchDeleteFlows: async () => {
    const { selectedRowKeys } = get();
    if (selectedRowKeys.length === 0) return;

    set({ loading: true, error: null });
    try {
      await flowApi.batchDelete(selectedRowKeys);
      set({ selectedRowKeys: [] });
      await get().refreshFlows();
      set({ loading: false });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '批量删除失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  batchDeployFlows: async () => {
    const { selectedRowKeys } = get();
    if (selectedRowKeys.length === 0) return;

    set({ loading: true, error: null });
    try {
      await flowApi.batchDeploy(selectedRowKeys);
      await get().refreshFlows();
      set({ loading: false });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '批量部署失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  batchUndeployFlows: async () => {
    const { selectedRowKeys } = get();
    if (selectedRowKeys.length === 0) return;

    set({ loading: true, error: null });
    try {
      await flowApi.batchUndeploy(selectedRowKeys);
      await get().refreshFlows();
      set({ loading: false });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '批量卸载失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  // ===== 导入导出 =====
  exportFlow: async (flowId: string) => {
    try {
      const { data: response } = await flowApi.exportFlow(flowId);
      return response.data;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '导出流程失败';
      set({ error: message });
      throw error;
    }
  },

  batchExportFlows: async () => {
    const { selectedRowKeys } = get();
    if (selectedRowKeys.length === 0) return [];

    try {
      const { data: response } = await flowApi.batchExport(selectedRowKeys);
      return response.data;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '批量导出失败';
      set({ error: message });
      throw error;
    }
  },

  importFlow: async (flowDesign: FlowDesign) => {
    set({ loading: true, error: null });
    try {
      const { data: response } = await flowApi.importFlow(flowDesign);
      await get().refreshFlows();
      set({ loading: false });
      return response.data;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '导入流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  // ===== 分类操作 =====
  fetchCategories: async () => {
    try {
      const [categoriesRes, statsRes] = await Promise.all([
        flowApi.getCategories(),
        flowApi.getCategoryStats(),
      ]);
      set({
        categories: categoriesRes.data.data,
        categoryStats: statsRes.data.data,
      });
    } catch (error: unknown) {
      console.error('获取分类失败:', error);
    }
  },

  // ===== 清理 =====
  reset: () => {
    set(initialState);
  },
}));

export default useFlowManageStore;
