import { useRef, useEffect, useCallback } from 'react';
import { Graph } from '@antv/x6';
import { Selection } from '@antv/x6-plugin-selection';
import { Snapline } from '@antv/x6-plugin-snapline';
import { Keyboard } from '@antv/x6-plugin-keyboard';
import { History } from '@antv/x6-plugin-history';
import { Clipboard } from '@antv/x6-plugin-clipboard';
import { Scroller } from '@antv/x6-plugin-scroller';
import { registerNodes } from '../cells';
import { useFlowStore } from '../../../stores';

// 是否已注册节点
let nodesRegistered = false;

/**
 * Graph 配置选项
 */
export interface UseGraphOptions {
  container: HTMLElement | null;
  width?: number;
  height?: number;
  onNodeClick?: (nodeId: string) => void;
  onNodeDoubleClick?: (nodeId: string) => void;
  onSelectionChange?: (selectedIds: string[]) => void;
}

/**
 * X6 Graph 管理 Hook
 */
export function useGraph(options: UseGraphOptions) {
  const graphRef = useRef<Graph | null>(null);
  const { setGraph, selectNode, rootNode } = useFlowStore();

  /**
   * 初始化 Graph
   */
  const initGraph = useCallback(() => {
    if (!options.container || graphRef.current) return;

    // 确保节点已注���
    if (!nodesRegistered) {
      registerNodes();
      nodesRegistered = true;
    }

    const graph = new Graph({
      container: options.container,
      width: options.width || options.container.clientWidth,
      height: options.height || options.container.clientHeight,
      grid: {
        visible: true,
        type: 'doubleMesh',
        args: [
          {
            color: '#eee',
            thickness: 1,
          },
          {
            color: '#ddd',
            thickness: 1,
            factor: 4,
          },
        ],
      },
      background: {
        color: '#f8f9fa',
      },
      connecting: {
        router: 'manhattan',
        connector: {
          name: 'rounded',
          args: {
            radius: 8,
          },
        },
        anchor: 'center',
        connectionPoint: 'anchor',
        allowBlank: false,
        snap: {
          radius: 20,
        },
        createEdge() {
          return this.createEdge({
            shape: 'flow-edge',
            attrs: {
              line: {
                stroke: '#c0c0c0',
                strokeWidth: 2,
                targetMarker: {
                  name: 'block',
                  width: 12,
                  height: 8,
                },
              },
            },
          });
        },
        validateConnection({ targetMagnet }) {
          return !!targetMagnet;
        },
      },
      highlighting: {
        magnetAdsorbed: {
          name: 'stroke',
          args: {
            attrs: {
              fill: '#5F95FF',
              stroke: '#5F95FF',
            },
          },
        },
      },
      mousewheel: {
        enabled: true,
        modifiers: ['ctrl', 'meta'],
        zoomAtMousePosition: true,
        minScale: 0.5,
        maxScale: 2,
      },
      selecting: {
        enabled: true,
        rubberband: true,
        showNodeSelectionBox: true,
      },
      panning: {
        enabled: true,
        modifiers: 'shift',
      },
    });

    // 使用插件
    graph.use(
      new Selection({
        enabled: true,
        multiple: true,
        rubberband: true,
        movable: true,
        showNodeSelectionBox: true,
      })
    );

    graph.use(
      new Snapline({
        enabled: true,
      })
    );

    graph.use(
      new Keyboard({
        enabled: true,
      })
    );

    graph.use(
      new History({
        enabled: true,
      })
    );

    graph.use(
      new Clipboard({
        enabled: true,
      })
    );

    graph.use(
      new Scroller({
        enabled: true,
        pageVisible: false,
        pageBreak: false,
        pannable: true,
      })
    );

    // 绑定事件
    graph.on('node:click', ({ node }) => {
      options.onNodeClick?.(node.id);
    });

    graph.on('node:dblclick', ({ node }) => {
      options.onNodeDoubleClick?.(node.id);
    });

    graph.on('selection:changed', ({ selected }) => {
      const selectedIds = selected.map((cell) => cell.id);
      options.onSelectionChange?.(selectedIds);
    });

    // 快捷键
    graph.bindKey(['meta+z', 'ctrl+z'], () => {
      if (graph.canUndo()) {
        graph.undo();
      }
      return false;
    });

    graph.bindKey(['meta+shift+z', 'ctrl+y'], () => {
      if (graph.canRedo()) {
        graph.redo();
      }
      return false;
    });

    graph.bindKey(['meta+c', 'ctrl+c'], () => {
      const cells = graph.getSelectedCells();
      if (cells.length) {
        graph.copy(cells);
      }
      return false;
    });

    graph.bindKey(['meta+v', 'ctrl+v'], () => {
      if (!graph.isClipboardEmpty()) {
        const cells = graph.paste({ offset: 32 });
        graph.cleanSelection();
        graph.select(cells);
      }
      return false;
    });

    graph.bindKey('delete', () => {
      const cells = graph.getSelectedCells();
      if (cells.length) {
        graph.removeCells(cells);
      }
      return false;
    });

    graph.bindKey('backspace', () => {
      const cells = graph.getSelectedCells();
      if (cells.length) {
        graph.removeCells(cells);
      }
      return false;
    });

    graphRef.current = graph;
    setGraph(graph);

    return graph;
  }, [options.container, options.width, options.height, setGraph]);

  /**
   * 销毁 Graph
   */
  const disposeGraph = useCallback(() => {
    if (graphRef.current) {
      graphRef.current.dispose();
      graphRef.current = null;
      setGraph(null);
    }
  }, [setGraph]);

  /**
   * 适应画布
   */
  const fitView = useCallback(() => {
    if (graphRef.current) {
      graphRef.current.zoomToFit({ padding: 50, maxScale: 1 });
    }
  }, []);

  /**
   * 居中
   */
  const center = useCallback(() => {
    if (graphRef.current) {
      graphRef.current.centerContent();
    }
  }, []);

  /**
   * 缩放
   */
  const zoom = useCallback((factor: number) => {
    if (graphRef.current) {
      graphRef.current.zoom(factor);
    }
  }, []);

  /**
   * 设置缩放比例
   */
  const setZoom = useCallback((scale: number) => {
    if (graphRef.current) {
      graphRef.current.zoomTo(scale);
    }
  }, []);

  /**
   * 获取当前缩放比例
   */
  const getZoom = useCallback(() => {
    return graphRef.current?.zoom() || 1;
  }, []);

  /**
   * 撤销
   */
  const undo = useCallback(() => {
    if (graphRef.current?.canUndo()) {
      graphRef.current.undo();
    }
  }, []);

  /**
   * 重做
   */
  const redo = useCallback(() => {
    if (graphRef.current?.canRedo()) {
      graphRef.current.redo();
    }
  }, []);

  /**
   * 导出为 PNG
   */
  const exportPNG = useCallback(async (fileName = 'flow') => {
    if (!graphRef.current) return;

    const dataUri = await graphRef.current.toPNG({
      padding: 20,
      backgroundColor: '#fff',
    });

    const link = document.createElement('a');
    link.download = `${fileName}.png`;
    link.href = dataUri;
    link.click();
  }, []);

  /**
   * 导出为 SVG
   */
  const exportSVG = useCallback(async (fileName = 'flow') => {
    if (!graphRef.current) return;

    const svg = await graphRef.current.toSVG({
      preserveDimensions: true,
    });

    const blob = new Blob([svg], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.download = `${fileName}.svg`;
    link.href = url;
    link.click();

    URL.revokeObjectURL(url);
  }, []);

  // 初始化
  useEffect(() => {
    if (options.container) {
      initGraph();
    }

    return () => {
      disposeGraph();
    };
  }, [options.container, initGraph, disposeGraph]);

  return {
    graph: graphRef.current,
    initGraph,
    disposeGraph,
    fitView,
    center,
    zoom,
    setZoom,
    getZoom,
    undo,
    redo,
    exportPNG,
    exportSVG,
  };
}

export default useGraph;
