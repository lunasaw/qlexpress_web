import { useCallback, useRef } from 'react';
import { Graph, Node } from '@antv/x6';
import { Dnd } from '@antv/x6-plugin-dnd';
import type { ConditionTypeEnum } from '../../../types/enums';

/**
 * 拖拽节点配置
 */
export interface DragNodeConfig {
  /** 节点类型 */
  type: ConditionTypeEnum | string;
  /** 节点标签 */
  label: string;
  /** X6 节点形状 */
  shape: string;
  /** 节点宽度 */
  width?: number;
  /** 节点高度 */
  height?: number;
  /** 额外数据 */
  data?: Record<string, unknown>;
}

/**
 * Dnd 配置选项
 */
export interface UseDndOptions {
  /** 是否启用 */
  enabled?: boolean;
  /** 拖拽时节点缩放比例 */
  scaled?: boolean;
  /** 拖拽时是否显示节点动画 */
  animation?: boolean;
  /** 节��放置回调 */
  onNodeDropped?: (node: Node) => void;
}

/**
 * 获取节点形状配置
 */
function getNodeShapeConfig(type: string): { shape: string; width: number; height: number } {
  switch (type) {
    case 'THEN':
      return { shape: 'then-node', width: 240, height: 80 };
    case 'WHEN':
      return { shape: 'when-node', width: 240, height: 80 };
    case 'IF':
      return { shape: 'if-node', width: 120, height: 80 };
    case 'SWITCH':
      return { shape: 'switch-node', width: 120, height: 80 };
    case 'FOR':
      return { shape: 'for-node', width: 140, height: 80 };
    case 'WHILE':
    case 'ITERATOR':
      return { shape: 'while-node', width: 140, height: 80 };
    case 'CATCH':
      return { shape: 'catch-node', width: 160, height: 80 };
    case 'AND':
    case 'OR':
    case 'NOT':
      return { shape: 'logic-node', width: 100, height: 60 };
    case 'START':
    case 'END':
      return { shape: 'start-end-node', width: 60, height: 60 };
    default:
      return { shape: 'common-node', width: 180, height: 60 };
  }
}

/**
 * 拖拽交互 Hook
 */
export function useDnd(graph: Graph | null, options: UseDndOptions = {}) {
  const dndRef = useRef<Dnd | null>(null);
  const { enabled = true, scaled = false, animation = true, onNodeDropped } = options;

  /**
   * 初始化 Dnd 插件
   */
  const initDnd = useCallback(
    (dndContainer: HTMLElement) => {
      if (!graph || !enabled) return null;

      // 如果已存在则先销毁
      if (dndRef.current) {
        dndRef.current.dispose?.();
      }

      const dnd = new Dnd({
        target: graph,
        scaled,
        animation,
        getDragNode: (node) => node.clone(),
        getDropNode: (node) => {
          const cloned = node.clone();
          onNodeDropped?.(cloned);
          return cloned;
        },
        validateNode: () => true,
      });

      dndRef.current = dnd;
      return dnd;
    },
    [graph, enabled, scaled, animation, onNodeDropped]
  );

  /**
   * 开始拖拽节点
   */
  const startDrag = useCallback(
    (config: DragNodeConfig, e: React.MouseEvent | MouseEvent) => {
      if (!graph || !dndRef.current) return;

      const shapeConfig = getNodeShapeConfig(config.type);

      // 创建拖拽节点
      const node = graph.createNode({
        shape: config.shape || shapeConfig.shape,
        width: config.width || shapeConfig.width,
        height: config.height || shapeConfig.height,
        label: config.label,
        data: {
          type: config.type,
          ...config.data,
        },
      });

      dndRef.current.start(node, e as MouseEvent);
    },
    [graph]
  );

  /**
   * 创建拖拽元素的事件处理器
   */
  const createDragHandler = useCallback(
    (config: DragNodeConfig) => {
      return (e: React.MouseEvent) => {
        startDrag(config, e);
      };
    },
    [startDrag]
  );

  /**
   * 销毁 Dnd 插件
   */
  const disposeDnd = useCallback(() => {
    if (dndRef.current) {
      dndRef.current.dispose?.();
      dndRef.current = null;
    }
  }, []);

  /**
   * 从侧边栏组件列表拖拽
   */
  const startDragFromSidebar = useCallback(
    (
      componentId: string,
      componentName: string,
      componentType: string,
      e: React.MouseEvent | MouseEvent
    ) => {
      startDrag(
        {
          type: 'NODE',
          label: componentName,
          shape: 'common-node',
          data: {
            componentId,
            componentName,
            componentType,
          },
        },
        e
      );
    },
    [startDrag]
  );

  /**
   * 从控制节点面板拖拽
   */
  const startDragControlNode = useCallback(
    (type: ConditionTypeEnum, label: string, e: React.MouseEvent | MouseEvent) => {
      const shapeConfig = getNodeShapeConfig(type);
      startDrag(
        {
          type,
          label,
          shape: shapeConfig.shape,
          width: shapeConfig.width,
          height: shapeConfig.height,
          data: {
            nodeType: type,
          },
        },
        e
      );
    },
    [startDrag]
  );

  return {
    dnd: dndRef.current,
    initDnd,
    startDrag,
    createDragHandler,
    disposeDnd,
    startDragFromSidebar,
    startDragControlNode,
  };
}

export default useDnd;
