import React, { useRef, useState, useEffect, useCallback } from 'react';
import { message } from 'antd';
import type { Node } from '@antv/x6';
import { useGraph, useLayout, useDnd } from './hooks';
import { SideBar, ToolBar, SettingBar, ELPreview } from './panels';
import { useFlowStore } from '../../stores';
import { ConditionTypeEnum } from '../../types/enums';
import './LiteFlowEditor.css';

interface LiteFlowEditorProps {
  /** 流程ID */
  flowId?: string;
  /** 是否只读 */
  readonly?: boolean;
  /** 保存回调 */
  onSave?: () => void;
  /** 执行回调 */
  onExecute?: () => void;
}

/**
 * LiteFlow 可视化编辑器
 */
const LiteFlowEditor: React.FC<LiteFlowEditorProps> = ({
  flowId,
  readonly = false,
  onSave,
  onExecute,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const dndContainerRef = useRef<HTMLDivElement>(null);

  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [zoomLevel, setZoomLevel] = useState(100);
  const [canUndo, setCanUndo] = useState(false);
  const [canRedo, setCanRedo] = useState(false);
  const [saving, setSaving] = useState(false);
  const [executing, setExecuting] = useState(false);

  const {
    rootNode,
    elExpression,
    loadFlow,
    saveFlow,
    generateEL,
    validateFlow,
    deployFlow,
  } = useFlowStore();

  // 初始化 Graph
  const {
    graph,
    fitView,
    center,
    zoom,
    setZoom,
    getZoom,
    undo,
    redo,
    exportPNG,
    exportSVG,
  } = useGraph({
    container: containerRef.current,
    onNodeClick: (nodeId) => {
      if (graph) {
        const node = graph.getCellById(nodeId) as Node;
        setSelectedNode(node || null);
      }
    },
    onSelectionChange: (selectedIds) => {
      if (selectedIds.length === 1 && graph) {
        const node = graph.getCellById(selectedIds[0]) as Node;
        setSelectedNode(node || null);
      } else {
        setSelectedNode(null);
      }
    },
  });

  // 初始化布局
  const { verticalLayout, horizontalLayout, autoLayoutAndFit } = useLayout(graph);

  // 初始化拖拽
  const { initDnd, startDragFromSidebar, startDragControlNode } = useDnd(graph, {
    onNodeDropped: (node) => {
      message.success(`已添加节点: ${node.getAttrByPath('label') || node.id}`);
    },
  });

  // 监听 Graph 变化
  useEffect(() => {
    if (graph) {
      // 更新撤销/重做状态
      const updateHistory = () => {
        setCanUndo(graph.canUndo());
        setCanRedo(graph.canRedo());
      };

      // 更新缩放比例
      const updateZoom = () => {
        setZoomLevel(getZoom() * 100);
      };

      graph.on('history:change', updateHistory);
      graph.on('scale', updateZoom);

      // 初始化 Dnd
      if (dndContainerRef.current) {
        initDnd(dndContainerRef.current);
      }

      return () => {
        graph.off('history:change', updateHistory);
        graph.off('scale', updateZoom);
      };
    }
  }, [graph, getZoom, initDnd]);

  // 加载流程
  useEffect(() => {
    if (flowId) {
      loadFlow(flowId);
    }
  }, [flowId, loadFlow]);

  // 生成 EL 表达式
  useEffect(() => {
    if (rootNode) {
      generateEL();
    }
  }, [rootNode, generateEL]);

  /**
   * 缩放操作
   */
  const handleZoomIn = useCallback(() => {
    zoom(0.1);
    setZoomLevel(getZoom() * 100);
  }, [zoom, getZoom]);

  const handleZoomOut = useCallback(() => {
    zoom(-0.1);
    setZoomLevel(getZoom() * 100);
  }, [zoom, getZoom]);

  const handleZoomReset = useCallback(() => {
    setZoom(1);
    setZoomLevel(100);
  }, [setZoom]);

  const handleFitView = useCallback(() => {
    fitView();
    setZoomLevel(getZoom() * 100);
  }, [fitView, getZoom]);

  /**
   * 撤销/重做
   */
  const handleUndo = useCallback(() => {
    undo();
  }, [undo]);

  const handleRedo = useCallback(() => {
    redo();
  }, [redo]);

  /**
   * 复制/粘贴/删除
   */
  const handleCopy = useCallback(() => {
    if (graph) {
      const cells = graph.getSelectedCells();
      if (cells.length) {
        graph.copy(cells);
        message.success('已复制');
      }
    }
  }, [graph]);

  const handlePaste = useCallback(() => {
    if (graph && !graph.isClipboardEmpty()) {
      const cells = graph.paste({ offset: 32 });
      graph.cleanSelection();
      graph.select(cells);
      message.success('已粘贴');
    }
  }, [graph]);

  const handleDelete = useCallback(() => {
    if (graph) {
      const cells = graph.getSelectedCells();
      if (cells.length) {
        graph.removeCells(cells);
        setSelectedNode(null);
        message.success('已删除');
      }
    }
  }, [graph]);

  /**
   * 节点数据变化
   */
  const handleNodeDataChange = useCallback(
    (nodeId: string, data: Record<string, unknown>) => {
      if (graph) {
        const node = graph.getCellById(nodeId) as Node;
        if (node) {
          node.setData(data);
          if (data.label) {
            node.setAttrByPath('label', data.label);
          }
        }
      }
    },
    [graph]
  );

  /**
   * 删除节点
   */
  const handleDeleteNode = useCallback(
    (nodeId: string) => {
      if (graph) {
        const node = graph.getCellById(nodeId);
        if (node) {
          graph.removeCell(node);
          setSelectedNode(null);
          message.success('已删除节点');
        }
      }
    },
    [graph]
  );

  /**
   * 复制节点
   */
  const handleCopyNode = useCallback(
    (nodeId: string) => {
      if (graph) {
        const node = graph.getCellById(nodeId) as Node;
        if (node) {
          graph.copy([node]);
          const cells = graph.paste({ offset: 32 });
          graph.cleanSelection();
          graph.select(cells);
          message.success('已复制节点');
        }
      }
    },
    [graph]
  );

  /**
   * 导出
   */
  const handleExportPNG = useCallback(async () => {
    await exportPNG('liteflow-diagram');
    message.success('已导出 PNG');
  }, [exportPNG]);

  const handleExportSVG = useCallback(async () => {
    await exportSVG('liteflow-diagram');
    message.success('已导出 SVG');
  }, [exportSVG]);

  const handleExportJSON = useCallback(() => {
    if (graph) {
      const data = graph.toJSON();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.download = 'liteflow-diagram.json';
      link.href = url;
      link.click();
      URL.revokeObjectURL(url);
      message.success('已导出 JSON');
    }
  }, [graph]);

  /**
   * 保存流程
   */
  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      // 验证流程
      const validationResult = await validateFlow();
      if (!validationResult.valid) {
        message.error(validationResult.errors?.[0] || '流程验证失败');
        return;
      }

      await saveFlow();
      message.success('保存成功');
      onSave?.();
    } catch (error) {
      message.error('保存失败');
    } finally {
      setSaving(false);
    }
  }, [validateFlow, saveFlow, onSave]);

  /**
   * 执行流程
   */
  const handleExecute = useCallback(async () => {
    setExecuting(true);
    try {
      // 先部署
      await deployFlow();
      message.success('流程已部署');
      onExecute?.();
    } catch (error) {
      message.error('执行失败');
    } finally {
      setExecuting(false);
    }
  }, [deployFlow, onExecute]);

  /**
   * 刷新 EL 表达式
   */
  const handleRefreshEL = useCallback(() => {
    generateEL();
  }, [generateEL]);

  /**
   * 拖拽处理
   */
  const handleDragStart = useCallback(
    (componentId: string, componentName: string, componentType: string, e: React.MouseEvent) => {
      startDragFromSidebar(componentId, componentName, componentType, e);
    },
    [startDragFromSidebar]
  );

  const handleDragControlNode = useCallback(
    (type: ConditionTypeEnum, label: string, e: React.MouseEvent) => {
      startDragControlNode(type, label, e);
    },
    [startDragControlNode]
  );

  return (
    <div className="liteflow-editor" ref={dndContainerRef}>
      {/* 工具栏 */}
      <ToolBar
        zoomLevel={zoomLevel}
        canUndo={canUndo}
        canRedo={canRedo}
        hasSelection={!!selectedNode}
        saving={saving}
        executing={executing}
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onZoomReset={handleZoomReset}
        onFitView={handleFitView}
        onUndo={handleUndo}
        onRedo={handleRedo}
        onCopy={handleCopy}
        onPaste={handlePaste}
        onDelete={handleDelete}
        onVerticalLayout={verticalLayout}
        onHorizontalLayout={horizontalLayout}
        onAutoLayout={autoLayoutAndFit}
        onExportPNG={handleExportPNG}
        onExportSVG={handleExportSVG}
        onExportJSON={handleExportJSON}
        onSave={readonly ? undefined : handleSave}
        onExecute={handleExecute}
      />

      <div className="liteflow-editor-body">
        {/* 左侧边栏 */}
        {!readonly && (
          <SideBar
            onDragStart={handleDragStart}
            onDragControlNode={handleDragControlNode}
          />
        )}

        {/* 画布 */}
        <div className="liteflow-canvas-container">
          <div ref={containerRef} className="liteflow-canvas" />
        </div>

        {/* 右侧属性面板 */}
        <SettingBar
          selectedNode={selectedNode}
          onNodeDataChange={readonly ? undefined : handleNodeDataChange}
          onDeleteNode={readonly ? undefined : handleDeleteNode}
          onCopyNode={handleCopyNode}
        />
      </div>

      {/* EL 预览面板 */}
      <ELPreview
        elExpression={elExpression}
        jsonStructure={rootNode ? JSON.stringify(rootNode.toJSON()) : ''}
        isValid={true}
        onRefresh={handleRefreshEL}
      />
    </div>
  );
};

export default LiteFlowEditor;
