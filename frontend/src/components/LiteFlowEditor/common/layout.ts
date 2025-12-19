import { Graph, Node } from '@antv/x6';
import dagre from 'dagre';
import { NODE_WIDTH, RANK_SEP, NODE_SEP } from '../constant';

const rankdir = 'LR';
const nodeSize: number = NODE_WIDTH;
const ranksep: number = RANK_SEP;
const nodesep: number = NODE_SEP;
const begin: [number, number] = [40, 40];

export const forceLayout = (flowGraph: Graph, _cfg: Record<string, unknown> = {}): void => {
  dagreLayout(flowGraph);
};

/**
 * 使用 dagre 实现布局
 * @param flowGraph 图实例
 */
function dagreLayout(flowGraph: Graph): void {
  const g = new dagre.graphlib.Graph();
  g.setGraph({
    rankdir,
    nodesep,
    ranksep,
    marginx: begin[0],
    marginy: begin[1],
  });
  g.setDefaultEdgeLabel(() => ({}));

  // 添加节点
  flowGraph.getNodes().forEach((node) => {
    node.setZIndex(1);
    const size = node.getSize();
    g.setNode(node.id, {
      width: size.width || nodeSize,
      height: size.height || nodeSize
    });
  });

  // 添加边
  flowGraph.getEdges().forEach((edge) => {
    edge.setZIndex(0);
    const source = edge.getSourceNode();
    const target = edge.getTargetNode();
    if (source && target) {
      g.setEdge(source.id, target.id);
    }
  });

  // 执行布局
  dagre.layout(g);

  // 应用布局结果
  flowGraph.startBatch('layout');
  g.nodes().forEach((id) => {
    const node = flowGraph.getCellById(id) as Node;
    if (node) {
      const pos = g.node(id);
      if (pos) {
        const size = node.getSize();
        node.position(pos.x - (size.width || nodeSize) / 2, pos.y - (size.height || nodeSize) / 2);
      }
    }
  });
  flowGraph.stopBatch('layout');
}
