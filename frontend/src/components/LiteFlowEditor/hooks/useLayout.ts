import { useCallback } from 'react';
import { Graph, Node, Edge } from '@antv/x6';
import dagre from 'dagre';

/**
 * 布局配置选项
 */
export interface LayoutOptions {
  /** 布局方向: TB(上到下), BT(下到上), LR(左到右), RL(右到左) */
  rankdir?: 'TB' | 'BT' | 'LR' | 'RL';
  /** 节点之间的水平间距 */
  nodesep?: number;
  /** 层级之间的垂直间距 */
  ranksep?: number;
  /** 是否居中对齐 */
  align?: 'UL' | 'UR' | 'DL' | 'DR';
}

/**
 * 默认布局配置
 */
const defaultLayoutOptions: LayoutOptions = {
  rankdir: 'TB',
  nodesep: 50,
  ranksep: 80,
  align: 'UL',
};

/**
 * Dagre 自动布局 Hook
 */
export function useLayout(graph: Graph | null) {
  /**
   * 执行自动布局
   */
  const doLayout = useCallback(
    (options: LayoutOptions = {}) => {
      if (!graph) return;

      const nodes = graph.getNodes();
      const edges = graph.getEdges();

      if (nodes.length === 0) return;

      // 合并配置
      const layoutOptions = { ...defaultLayoutOptions, ...options };

      // 创建 dagre 图
      const g = new dagre.graphlib.Graph();
      g.setGraph({
        rankdir: layoutOptions.rankdir,
        nodesep: layoutOptions.nodesep,
        ranksep: layoutOptions.ranksep,
        align: layoutOptions.align,
      });
      g.setDefaultEdgeLabel(() => ({}));

      // 添加节点到 dagre
      nodes.forEach((node: Node) => {
        const size = node.getSize();
        g.setNode(node.id, {
          width: size.width,
          height: size.height,
        });
      });

      // 添加边到 dagre
      edges.forEach((edge: Edge) => {
        const source = edge.getSourceCellId();
        const target = edge.getTargetCellId();
        if (source && target) {
          g.setEdge(source, target);
        }
      });

      // 执行布局计算
      dagre.layout(g);

      // 开始批量更新
      graph.startBatch('layout');

      // 应用布局结果到节点
      nodes.forEach((node: Node) => {
        const dagreNode = g.node(node.id);
        if (dagreNode) {
          node.setPosition({
            x: dagreNode.x - dagreNode.width / 2,
            y: dagreNode.y - dagreNode.height / 2,
          });
        }
      });

      // 结束批量更新
      graph.stopBatch('layout');
    },
    [graph]
  );

  /**
   * 水平布局 (从左到右)
   */
  const horizontalLayout = useCallback(
    (options: Omit<LayoutOptions, 'rankdir'> = {}) => {
      doLayout({ ...options, rankdir: 'LR' });
    },
    [doLayout]
  );

  /**
   * 垂直布局 (从上到下)
   */
  const verticalLayout = useCallback(
    (options: Omit<LayoutOptions, 'rankdir'> = {}) => {
      doLayout({ ...options, rankdir: 'TB' });
    },
    [doLayout]
  );

  /**
   * 逆向水平布局 (从右到左)
   */
  const reverseHorizontalLayout = useCallback(
    (options: Omit<LayoutOptions, 'rankdir'> = {}) => {
      doLayout({ ...options, rankdir: 'RL' });
    },
    [doLayout]
  );

  /**
   * 逆向垂直布局 (从下到上)
   */
  const reverseVerticalLayout = useCallback(
    (options: Omit<LayoutOptions, 'rankdir'> = {}) => {
      doLayout({ ...options, rankdir: 'BT' });
    },
    [doLayout]
  );

  /**
   * 基于 ELNode 树结构进行布局
   * 用于从 EL 表达式结构生成的节点树
   */
  const treeLayout = useCallback(
    (rootNodeId: string, options: LayoutOptions = {}) => {
      if (!graph) return;

      const nodes = graph.getNodes();
      const edges = graph.getEdges();

      if (nodes.length === 0) return;

      // 找到根节点
      const rootNode = nodes.find((n) => n.id === rootNodeId);
      if (!rootNode) {
        // 如果找不到指定的根节点，使用默认布局
        doLayout(options);
        return;
      }

      // 合并配置，树布局默认使用较大间距
      const layoutOptions = {
        ...defaultLayoutOptions,
        nodesep: 60,
        ranksep: 100,
        ...options,
      };

      // 创建 dagre 图
      const g = new dagre.graphlib.Graph();
      g.setGraph({
        rankdir: layoutOptions.rankdir,
        nodesep: layoutOptions.nodesep,
        ranksep: layoutOptions.ranksep,
        align: layoutOptions.align,
      });
      g.setDefaultEdgeLabel(() => ({}));

      // 添加节点
      nodes.forEach((node: Node) => {
        const size = node.getSize();
        g.setNode(node.id, {
          width: size.width,
          height: size.height,
        });
      });

      // 添加边
      edges.forEach((edge: Edge) => {
        const source = edge.getSourceCellId();
        const target = edge.getTargetCellId();
        if (source && target) {
          g.setEdge(source, target);
        }
      });

      // 执行布局
      dagre.layout(g);

      // 应用布局
      graph.startBatch('tree-layout');
      nodes.forEach((node: Node) => {
        const dagreNode = g.node(node.id);
        if (dagreNode) {
          node.setPosition({
            x: dagreNode.x - dagreNode.width / 2,
            y: dagreNode.y - dagreNode.height / 2,
          });
        }
      });
      graph.stopBatch('tree-layout');
    },
    [graph, doLayout]
  );

  /**
   * 自动布局并居中
   */
  const autoLayoutAndCenter = useCallback(
    (options: LayoutOptions = {}) => {
      doLayout(options);
      if (graph) {
        // 等待布局完成后居中
        setTimeout(() => {
          graph.centerContent();
        }, 100);
      }
    },
    [graph, doLayout]
  );

  /**
   * 自动布局并适应画布
   */
  const autoLayoutAndFit = useCallback(
    (options: LayoutOptions = {}) => {
      doLayout(options);
      if (graph) {
        // 等待布局完成后适应画布
        setTimeout(() => {
          graph.zoomToFit({ padding: 50, maxScale: 1 });
        }, 100);
      }
    },
    [graph, doLayout]
  );

  return {
    doLayout,
    horizontalLayout,
    verticalLayout,
    reverseHorizontalLayout,
    reverseVerticalLayout,
    treeLayout,
    autoLayoutAndCenter,
    autoLayoutAndFit,
  };
}

export default useLayout;
