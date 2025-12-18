import { create } from 'zustand';
import type { Graph, Node } from '@antv/x6';
import type { FlowRequest } from '../types/flow';
import type { QLComponent } from '../types/component';
import { ELNode, ELBuilder } from '../components/LiteFlowEditor/model';
import { NodeOperator, ThenOperator, ChainOperator } from '../components/LiteFlowEditor/model/operators';
import { ConditionTypeEnum } from '../types/enums';
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
  saveFlow: (newFlowId?: string) => Promise<string>;
  clearFlow: () => void;
  setFlowInfo: (info: { flowName?: string; flowDescription?: string; flowCategory?: string }) => void;

  // Graph 到 ELNode 同步
  buildELFromGraph: (chainName?: string) => ELNode | null;

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
  validateFlow: (flowId?: string) => Promise<{ valid: boolean; errors: string[] }>;
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

      // 同步到画布 - 延迟执行以确保 graph 已初始化
      // 使用 setTimeout 确保在下一个事件循环中执行
      setTimeout(() => {
        get().syncToGraph();
      }, 0);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : '加载流程失败';
      set({ error: message, loading: false });
      throw error;
    }
  },

  saveFlow: async (newFlowId?: string) => {
    const { flowId: existingFlowId, flowName, flowDescription, flowCategory } = get();

    // 使用传入的 flowId 或已存在的 flowId
    const targetFlowId = newFlowId || existingFlowId;

    if (!targetFlowId) {
      throw new Error('流程ID不能为空');
    }

    // 先从 Graph 构建 ELNode，传入 flowId 作为 chainName
    let rootNode = get().rootNode;
    if (!rootNode) {
      rootNode = get().buildELFromGraph(targetFlowId);
    }

    if (!rootNode) {
      throw new Error('流程为空，请先添加节点');
    }

    set({ loading: true, error: null });
    try {
      const flowRequest: FlowRequest = {
        flowId: targetFlowId,
        flowName,
        description: flowDescription,
        category: flowCategory,
        root: rootNode.toJSON(),
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

      set({ dirty: false, loading: false, rootNode });
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
    set({
      ...info,
      dirty: true,
    });
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

  /**
   * 从 X6 Graph 构建 ELNode 树
   * 将画布上的节点转换为 ELNode 模型
   */
  buildELFromGraph: (chainName?: string) => {
    const { graph, components, flowId } = get();
    if (!graph) return null;

    // 获取所有节点 (不包括边)
    const nodes = graph.getNodes();
    if (nodes.length === 0) return null;

    // 创建组件映射
    const componentMap = new Map<string, QLComponent>();
    components.forEach((comp) => componentMap.set(comp.componentId, comp));

    // 创建 Chain 作为根节点，使用 flowId 或传入的 chainName 作为标识
    const chain = new ChainOperator(undefined, chainName || flowId || undefined);

    // 简化处理：将所有节点按 Y 坐标排序，放入 THEN 序列中
    // TODO: 后续需要根据边的连接关系构建更复杂的结构
    const sortedNodes = [...nodes].sort((a, b) => {
      const posA = a.getPosition();
      const posB = b.getPosition();
      // 先按 Y 排序，再按 X 排序
      if (Math.abs(posA.y - posB.y) < 50) {
        return posA.x - posB.x;
      }
      return posA.y - posB.y;
    });

    // 遍历节点，根据类型创建对应的 ELNode
    sortedNodes.forEach((x6Node: Node) => {
      const data = x6Node.getData() || {};
      const nodeType = data.type || data.nodeType;

      // 根据节点类型创建 ELNode
      if (nodeType === 'NODE' || data.componentId) {
        // 组件节点
        const componentId = data.componentId;
        const componentData = componentId ? componentMap.get(componentId) : undefined;
        const elNode = new NodeOperator(chain, componentId, componentData);
        chain.appendChild(elNode);
      } else if (nodeType === ConditionTypeEnum.THEN || nodeType === 'THEN') {
        // THEN 节点 - 暂时跳过，简化处理
        const thenNode = new ThenOperator(chain);
        chain.appendChild(thenNode);
      } else if (nodeType === ConditionTypeEnum.WHEN || nodeType === 'WHEN') {
        // WHEN 节点 - 暂时作为叶子节点处理
        const elNode = new NodeOperator(chain, x6Node.id);
        elNode.type = 'WHEN';
        chain.appendChild(elNode);
      } else if (
        nodeType === ConditionTypeEnum.IF ||
        nodeType === ConditionTypeEnum.SWITCH ||
        nodeType === ConditionTypeEnum.FOR ||
        nodeType === ConditionTypeEnum.WHILE
      ) {
        // 控制节点 - 简化处理为叶子节点
        const elNode = new NodeOperator(chain, x6Node.id);
        elNode.type = nodeType;
        chain.appendChild(elNode);
      } else {
        // 其他节点作为普通组件节点
        const label = x6Node.getAttrByPath('label/text') as string || x6Node.id;
        const elNode = new NodeOperator(chain, label);
        chain.appendChild(elNode);
      }
    });

    return chain;
  },

  syncToGraph: () => {
    const { graph, rootNode } = get();
    if (!graph || !rootNode) return;

    // 从 ELNode 生成 X6 cells metadata
    const cellsMetadata = rootNode.toCells();

    // 清空画布
    graph.clearCells();

    if (cellsMetadata.length > 0) {
      // 从 metadata 创建实际的 Cell 实例
      const cells = cellsMetadata.map((metadata) => {
        // 根据 shape 判断是节点还是边
        if (metadata.shape?.includes('edge') || metadata.source || metadata.target) {
          return graph.createEdge(metadata);
        } else {
          return graph.createNode(metadata);
        }
      });
      // 使用 resetCells 添加实际的 Cell 实例
      graph.resetCells(cells);
    }

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
  validateFlow: async (newFlowId?: string) => {
    const { flowId: existingFlowId, flowName } = get();

    // 使用传入的 flowId 或已存在的 flowId
    const targetFlowId = newFlowId || existingFlowId;

    // 先从 Graph 构建 ELNode，传入 flowId 作为 chainName
    let rootNode = get().rootNode;
    if (!rootNode) {
      rootNode = get().buildELFromGraph(targetFlowId || undefined);
    }

    if (!rootNode) {
      return { valid: false, errors: ['流程为空，请先添加节点'] };
    }

    // 如果没有 flowId，跳过后端验证，只做本地验证
    if (!targetFlowId) {
      // 简单的本地验证：至少有一个子节点
      if (rootNode.children.length === 0) {
        return { valid: false, errors: ['流程至少需要一个节点'] };
      }
      return { valid: true, errors: [] };
    }

    try {
      const flowRequest: FlowRequest = {
        flowId: targetFlowId,
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
