import React, { useRef, useState, useEffect, useCallback } from 'react';
import { message, Modal, Form, Input } from 'antd';
import type { Node } from '@antv/x6';
import { useGraph, useLayout, useDnd } from './hooks';
import { SideBar, ToolBar, SettingBar, ELPreview } from './panels';
import { Resizer } from './components';
import { useFlowStore } from '../../stores';
import { ConditionTypeEnum } from '../../types/enums';
import './LiteFlowEditor.css';

const { TextArea } = Input;

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

  // 使用 state 跟踪容器元素，确保 Graph 在 DOM 挂载后初始化
  const [container, setContainer] = useState<HTMLDivElement | null>(null);

  const [selectedNode, setSelectedNode] = useState<Node | null>(null);
  const [zoomLevel, setZoomLevel] = useState(100);
  const [canUndo, setCanUndo] = useState(false);
  const [canRedo, setCanRedo] = useState(false);
  const [saving, setSaving] = useState(false);
  const [executing, setExecuting] = useState(false);
  const [saveModalVisible, setSaveModalVisible] = useState(false);
  const [saveForm] = Form.useForm();

  // 面板尺寸状态
  const [sidebarWidth, setSidebarWidth] = useState(280);
  const [settingBarWidth, setSettingBarWidth] = useState(300);
  const [previewHeight, setPreviewHeight] = useState(280);

  const {
    flowId: storeFlowId,
    flowName,
    flowDescription,
    rootNode,
    elExpression,
    loadFlow,
    saveFlow,
    generateEL,
    validateFlow,
    deployFlow,
    setFlowInfo,
  } = useFlowStore();

  // 容器挂载后更新 state，触发 useGraph 初始化
  useEffect(() => {
    if (containerRef.current) {
      setContainer(containerRef.current);
    }
  }, []);

  // 初始化 Graph - 使用 state 中的 container 确保 DOM 已挂载
  const {
    graph,
    fitView,
    zoom,
    setZoom,
    getZoom,
    undo,
    redo,
    exportPNG,
    exportSVG,
  } = useGraph({
    container,
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

  // 加载流程 - 确保 graph 初始化后再加载
  useEffect(() => {
    if (flowId && graph) {
      loadFlow(flowId);
    }
  }, [flowId, graph, loadFlow]);

  // 当 rootNode 变化且 graph 存在时，同步到画布
  useEffect(() => {
    if (rootNode && graph) {
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

      // 自动适应视图
      setTimeout(() => {
        graph.zoomToFit({ padding: 50, maxScale: 1 });
      }, 100);
    }
  }, [rootNode, graph]);

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
            node.setAttrByPath('label/text', data.label as string);
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
   * 打开保存对话框
   */
  const handleSave = useCallback(() => {
    // 设置表单初始值
    saveForm.setFieldsValue({
      flowId: storeFlowId || `flow_${Date.now()}`,
      flowName: flowName || '未命名流程',
      description: flowDescription || '',
    });
    setSaveModalVisible(true);
  }, [saveForm, storeFlowId, flowName, flowDescription]);

  /**
   * 确认保存
   */
  const handleSaveConfirm = useCallback(async () => {
    try {
      const values = await saveForm.validateFields();
      const targetFlowId = values.flowId;

      // 更新流程信息到 store
      setFlowInfo({
        flowName: values.flowName,
        flowDescription: values.description,
      });

      setSaving(true);
      setSaveModalVisible(false);

      // 验证流程 - 传递 flowId
      const validationResult = await validateFlow(targetFlowId);
      if (!validationResult.valid) {
        message.error(validationResult.errors?.[0] || '流程验证失败');
        setSaving(false);
        return;
      }

      // 保存时需要带上 flowId
      await saveFlow(targetFlowId);
      message.success('保存成功');
      onSave?.();
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '保存失败');
      }
    } finally {
      setSaving(false);
    }
  }, [saveForm, setFlowInfo, validateFlow, saveFlow, onSave]);

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

      <div className="liteflow-editor-body" style={{ paddingBottom: previewHeight }}>
        {/* 左侧边栏 */}
        {!readonly && (
          <>
            <SideBar
              onDragStart={handleDragStart}
              onDragControlNode={handleDragControlNode}
              style={{ width: sidebarWidth }}
            />
            <Resizer
              direction="horizontal"
              size={sidebarWidth}
              minSize={200}
              maxSize={500}
              onResize={setSidebarWidth}
            />
          </>
        )}

        {/* 画布 */}
        <div className="liteflow-canvas-container">
          <div ref={containerRef} className="liteflow-canvas" />
        </div>

        {/* 右侧属性面板 */}
        <Resizer
          direction="horizontal"
          size={settingBarWidth}
          minSize={200}
          maxSize={500}
          onResize={setSettingBarWidth}
          reverse
        />
        <SettingBar
          selectedNode={selectedNode}
          onNodeDataChange={readonly ? undefined : handleNodeDataChange}
          onDeleteNode={readonly ? undefined : handleDeleteNode}
          onCopyNode={handleCopyNode}
          style={{ width: settingBarWidth }}
        />
      </div>

      {/* EL 预览面板 */}
      <div
        className="preview-resizer-wrapper"
        style={{ position: 'absolute', bottom: previewHeight, left: 0, right: 0, zIndex: 101 }}
      >
        <Resizer
          direction="vertical"
          size={previewHeight}
          minSize={100}
          maxSize={500}
          onResize={setPreviewHeight}
          reverse
        />
      </div>
      <ELPreview
        elExpression={elExpression}
        jsonStructure={rootNode ? JSON.stringify(rootNode.toJSON()) : ''}
        isValid={true}
        onRefresh={handleRefreshEL}
        style={{ height: previewHeight, left: readonly ? 0 : sidebarWidth, right: settingBarWidth }}
      />

      {/* 保存对话框 */}
      <Modal
        title="保存流程"
        open={saveModalVisible}
        onOk={handleSaveConfirm}
        onCancel={() => setSaveModalVisible(false)}
        confirmLoading={saving}
        okText="保存"
        cancelText="取消"
      >
        <Form form={saveForm} layout="vertical">
          <Form.Item
            name="flowId"
            label="流程ID"
            rules={[
              { required: true, message: '请输入流程ID' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '只能包含字母、数字、下划线，且以字母开头' },
            ]}
          >
            <Input placeholder="如: order_process" disabled={!!storeFlowId} />
          </Form.Item>

          <Form.Item
            name="flowName"
            label="流程名称"
            rules={[{ required: true, message: '请输入流程名称' }]}
          >
            <Input placeholder="如: 订单处理流程" />
          </Form.Item>

          <Form.Item name="description" label="流程描述">
            <TextArea rows={3} placeholder="请输入流程描述" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default LiteFlowEditor;
