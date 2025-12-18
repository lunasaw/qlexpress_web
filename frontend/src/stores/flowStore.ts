import { create } from 'zustand';
import type { Graph } from '@antv/x6';
import type { CmpProperty, FlowDesign, FlowRequest } from '../types/flow';
import type { QLComponent } from '../types/component';
import { ELNode, ELBuilder, ChainOperator } from '../components/LiteFlowEditor/model';
import { flowApi } from '../api/flow';

interface FlowState {
  // ===== 流程元数据 =====
  flowId: string | null;
  flowName: string;
  flowDescription: string;
  flowCategory: string;

  // ===== ELNode 模型 (核心) =====
  rootNode: ELNode | null;
  selectedNode: ELNode | null;

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
  loadFlow: (flowId: string) => Promise<void>;
  saveFlow: () => Promise<string>;
  clearFlow: () => void;
  setFlowInfo: (info: { flowName?: string; flowDescription?: string; flowCategory?: string }) => void;

  // 组件数据
  setComponents: (components: QLComponent[]) => void;

  // 节点操作
  setRootNode: (node: ELNode) => void;
  selectNode: (node: ELNode | null) => void;
  addNode: (parent: ELNode, node: ELNode, index?: number) => void;
  removeNode: (node: ELNode) => void;
  updateNode: (node: ELNode, updates: Partial<ELNode>) => void;
  replaceNode: (oldNode: ELNode, newNode: ELNode) => void;

  // Graph 操作
  setGraph: (graph: Graph | null) => void;
  syncToGraph: () => void;
  refreshLayout: () => void;

  // EL 操作
  generateEL: () => string;
  previewEL: () => Promise<string>;

  // 验证与部署
  validateFlow: () => Promise<{ valid: boolean; errors: string[] }>;
  deployFlow: () => Promise<void>;
  executeFlow: (params?: Record<string, unknown>) => Promise<unknown>;
}

export const useFlowStore = create<FlowState & FlowActions>((set, get) => ({
  // ===== 初始状态 =====
  flowId: null,
  flowName: '未命名流程',
  flowDescription: '',
  flowCategory: '',
  rootNode: null,
  selectedNode: null,
  components: [],
  graph: null,
  elExpression: '',
  dirty: false,
  loading: false,
  error: null,

  // ===== 流程操作 =====
  newFlow: () => {
    const rootNode = ELBuilder.createChain();
    set({
      flowId: null,
      flowName: '未命名流程',
      flowDescription: '',
      flowCategory: '',
      rootNode,
      selectedNode: null,
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

      // 构建 ELNode 树
      const builder = new ELBuilder(get().components);
      const rootNode = builder.build(flowData.root);
      const elExpression = rootNode.toEL();

      set({
        flowId: flowData.flowId,
        flowName: flowData.flowName,
        flowDescription: flowData.description || '',
        flowCategory: flowData.category || '',
        rootNode,
        elExpression,
        dirty: false,
        loading: false,
      });

      // 同步到画布
      get().syncToGraph();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '加载流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  saveFlow: async () => {
    const { flowId, flowName, flowDescription, flowCategory, rootNode } = get();
    if (!rootNode) {
      throw new Error('流程为空');
    }

    set({ loading: true, error: null });
    try {
      const flowRequest: FlowRequest = {
        flowId: flowId || undefined,
        flowName,
        description: flowDescription,
        category: flowCategory,
        root: rootNode.toJSON(),
      };

      let savedFlowId: string;

      if (flowId) {
        await flowApi.update(flowId, flowRequest);
        savedFlowId = flowId;
      } else {
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
      rootNode: null,
      selectedNode: null,
      elExpression: '',
      dirty: false,
      error: null,
    });
  },

  setFlowInfo: (info) => {
    set((state) => ({
      ...info,
      dirty: true,
    }));
  },

  // ===== 组件数据 =====
  setComponents: (components) => {
    set({ components });
  },

  // ===== 节点操作 =====
  setRootNode: (node) => {
    set({ rootNode: node, dirty: true });
    get().syncToGraph();
    get().generateEL();
  },

  selectNode: (node) => {
    set({ selectedNode: node });
  },

  addNode: (parent, node, index) => {
    parent.appendChild(node, index);
    set({ dirty: true });
    get().syncToGraph();
    get().generateEL();
  },

  removeNode: (node) => {
    if (node.parent) {
      node.parent.removeChild(node);
      set({ dirty: true, selectedNode: null });
      get().syncToGraph();
      get().generateEL();
    }
  },

  updateNode: (node, updates) => {
    Object.assign(node, updates);
    set({ dirty: true });
    get().syncToGraph();
    get().generateEL();
  },

  replaceNode: (oldNode, newNode) => {
    oldNode.replace(newNode);
    set({ dirty: true, selectedNode: newNode });
    get().syncToGraph();
    get().generateEL();
  },

  // ===== Graph 操作 =====
  setGraph: (graph) => {
    set({ graph });
  },

  syncToGraph: () => {
    const { graph, rootNode } = get();
    if (!graph || !rootNode) return;

    // 清空画布
    graph.clearCells();

    // 从 ELNode 生成 X6 cells
    const cells = rootNode.toCells();

    // 添加到画布
    graph.addCell(cells);

    // 自动布局
    get().refreshLayout();
  },

  refreshLayout: () => {
    const { graph } = get();
    if (!graph) return;

    // TODO: 实现 Dagre 自动布局
    // 这里将在第三阶段实现
  },

  // ===== EL 操作 =====
  generateEL: () => {
    const { rootNode } = get();
    if (!rootNode) return '';

    try {
      const el = rootNode.toEL();
      set({ elExpression: el });
      return el;
    } catch (error) {
      console.error('生成 EL 表达式失败:', error);
      return '';
    }
  },

  previewEL: async () => {
    const { rootNode } = get();
    if (!rootNode) return '';

    try {
      const { data: response } = await flowApi.previewEL(rootNode.toJSON());
      const el = response.data.el;
      set({ elExpression: el });
      return el;
    } catch {
      // 使用本地生成
      return get().generateEL();
    }
  },

  // ===== 验证与部署 =====
  validateFlow: async () => {
    const { rootNode, flowName } = get();
    if (!rootNode) {
      return { valid: false, errors: ['流程为空'] };
    }

    try {
      const flowRequest: FlowRequest = {
        flowName,
        root: rootNode.toJSON(),
      };
      const { data: response } = await flowApi.validateDesign(flowRequest);
      const result = response.data;
      return {
        valid: result.valid,
        errors: result.errors?.map((e) => e.message) || [],
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
