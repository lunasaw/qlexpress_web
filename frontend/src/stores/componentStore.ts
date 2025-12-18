import { create } from 'zustand';
import type { QLComponent, ComponentRequest, ComponentListParams } from '../types/component';
import type { ComponentType } from '../types/enums';
import { componentApi } from '../api/component';

interface ComponentState {
  // ===== 组件列表 =====
  components: QLComponent[];
  categories: string[];
  categoryStats: Record<string, number>;

  // ===== 当前编辑的组件 =====
  currentComponent: QLComponent | null;

  // ===== 筛选条件 =====
  filter: {
    category?: string;
    componentType?: ComponentType;
    keyword?: string;
    enabled?: boolean;
  };

  // ===== 分页 =====
  pagination: {
    page: number;
    size: number;
    total: number;
  };

  // ===== 状态标记 =====
  loading: boolean;
  error: string | null;
}

interface ComponentActions {
  // 列表操作
  fetchComponents: (params?: ComponentListParams) => Promise<void>;
  searchComponents: (keyword: string) => Promise<void>;
  fetchCategories: () => Promise<void>;
  fetchCategoryStats: () => Promise<void>;

  // 筛选
  setFilter: (filter: Partial<ComponentState['filter']>) => void;
  clearFilter: () => void;

  // 分页
  setPage: (page: number) => void;
  setPageSize: (size: number) => void;

  // CRUD
  createComponent: (data: ComponentRequest) => Promise<QLComponent>;
  updateComponent: (componentId: string, data: Partial<ComponentRequest>) => Promise<QLComponent>;
  deleteComponent: (componentId: string) => Promise<void>;
  getComponent: (componentId: string) => Promise<QLComponent>;

  // 当前组件
  setCurrentComponent: (component: QLComponent | null) => void;

  // 验证与测试
  validateComponent: (componentId: string) => Promise<{ valid: boolean; errors?: string[] }>;
  testComponent: (componentId: string, params?: Record<string, unknown>) => Promise<unknown>;

  // 同步到 LiteFlow
  syncComponent: (componentId: string) => Promise<void>;
  syncAllComponents: () => Promise<void>;

  // 工具方法
  getComponentById: (componentId: string) => QLComponent | undefined;
  getComponentsByType: (type: ComponentType) => QLComponent[];
}

export const useComponentStore = create<ComponentState & ComponentActions>((set, get) => ({
  // ===== 初始状态 =====
  components: [],
  categories: [],
  categoryStats: {},
  currentComponent: null,
  filter: {},
  pagination: {
    page: 1,
    size: 20,
    total: 0,
  },
  loading: false,
  error: null,

  // ===== 列表操作 =====
  fetchComponents: async (params) => {
    const { filter, pagination } = get();
    set({ loading: true, error: null });

    try {
      const queryParams: ComponentListParams = {
        page: pagination.page,
        size: pagination.size,
        ...filter,
        ...params,
      };

      const { data: response } = await componentApi.list(queryParams);
      const result = response.data;

      // 后端返回 ComponentListData 结构
      set({
        components: result?.components || [],
        categories: result?.categories || [],
        categoryStats: result?.categoryStats || {},
        pagination: {
          ...pagination,
          total: result?.total || 0,
        },
        loading: false,
      });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '获取组件列表失败';
      set({ error: message, loading: false, components: [] });
    }
  },

  searchComponents: async (keyword) => {
    set({ loading: true, error: null });

    try {
      const { data: response } = await componentApi.search(keyword);
      set({
        components: response.data || [],
        loading: false,
      });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '搜索组件失败';
      set({ error: message, loading: false, components: [] });
    }
  },

  fetchCategories: async () => {
    try {
      const { data: response } = await componentApi.getCategories();
      set({ categories: response.data });
    } catch (error: unknown) {
      console.error('获取分类失败:', error);
    }
  },

  fetchCategoryStats: async () => {
    try {
      const { data: response } = await componentApi.getCategoryStats();
      set({ categoryStats: response.data });
    } catch (error: unknown) {
      console.error('获取分类统计失败:', error);
    }
  },

  // ===== 筛选 =====
  setFilter: (filter) => {
    set((state) => ({
      filter: { ...state.filter, ...filter },
      pagination: { ...state.pagination, page: 1 },
    }));
    get().fetchComponents();
  },

  clearFilter: () => {
    set({
      filter: {},
      pagination: { ...get().pagination, page: 1 },
    });
    get().fetchComponents();
  },

  // ===== 分页 =====
  setPage: (page) => {
    set((state) => ({
      pagination: { ...state.pagination, page },
    }));
    get().fetchComponents();
  },

  setPageSize: (size) => {
    set((state) => ({
      pagination: { ...state.pagination, size, page: 1 },
    }));
    get().fetchComponents();
  },

  // ===== CRUD =====
  createComponent: async (data) => {
    set({ loading: true, error: null });

    try {
      const { data: response } = await componentApi.create(data);
      const newComponent = response.data;

      set((state) => ({
        components: [newComponent, ...state.components],
        loading: false,
      }));

      return newComponent;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '创建组件失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  updateComponent: async (componentId, data) => {
    set({ loading: true, error: null });

    try {
      const { data: response } = await componentApi.update(componentId, data);
      const updatedComponent = response.data;

      set((state) => ({
        components: state.components.map((c) =>
          c.componentId === componentId ? updatedComponent : c
        ),
        currentComponent:
          state.currentComponent?.componentId === componentId
            ? updatedComponent
            : state.currentComponent,
        loading: false,
      }));

      return updatedComponent;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '更新组件失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  deleteComponent: async (componentId) => {
    set({ loading: true, error: null });

    try {
      await componentApi.delete(componentId);

      set((state) => ({
        components: state.components.filter((c) => c.componentId !== componentId),
        currentComponent:
          state.currentComponent?.componentId === componentId ? null : state.currentComponent,
        loading: false,
      }));
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '删除组件失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  getComponent: async (componentId) => {
    set({ loading: true, error: null });

    try {
      const { data: response } = await componentApi.get(componentId);
      const component = response.data;
      set({ currentComponent: component, loading: false });
      return component;
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '获取组件详情失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  // ===== 当前组件 =====
  setCurrentComponent: (component) => {
    set({ currentComponent: component });
  },

  // ===== 验证与测试 =====
  validateComponent: async (componentId) => {
    try {
      const { data: response } = await componentApi.validate(componentId);
      const result = response.data;
      return {
        valid: result.valid,
        errors: result.syntaxErrors?.map((e) => e.message),
      };
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '验证失败';
      return { valid: false, errors: [message] };
    }
  },

  testComponent: async (componentId, params) => {
    try {
      const { data: response } = await componentApi.test(componentId, { params });
      return response.data;
    } catch (error: unknown) {
      throw error;
    }
  },

  // ===== 同步 =====
  syncComponent: async (componentId) => {
    try {
      await componentApi.sync(componentId);
    } catch (error: unknown) {
      throw error;
    }
  },

  syncAllComponents: async () => {
    try {
      await componentApi.syncAll();
    } catch (error: unknown) {
      throw error;
    }
  },

  // ===== 工具方法 =====
  getComponentById: (componentId) => {
    return get().components.find((c) => c.componentId === componentId);
  },

  getComponentsByType: (type) => {
    return get().components.filter((c) => c.componentType === type);
  },
}));

export default useComponentStore;
