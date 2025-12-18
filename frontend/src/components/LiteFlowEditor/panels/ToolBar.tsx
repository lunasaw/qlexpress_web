import React from 'react';
import { Button, Tooltip, Divider, Space, Dropdown } from 'antd';
import type { MenuProps } from 'antd';
import {
  ZoomInOutlined,
  ZoomOutOutlined,
  OneToOneOutlined,
  CompressOutlined,
  UndoOutlined,
  RedoOutlined,
  DeleteOutlined,
  CopyOutlined,
  SnippetsOutlined,
  DownloadOutlined,
  PlayCircleOutlined,
  SaveOutlined,
  ApartmentOutlined,
  ColumnWidthOutlined,
  ColumnHeightOutlined,
} from '@ant-design/icons';
import './ToolBar.css';

interface ToolBarProps {
  /** 缩放 */
  onZoomIn?: () => void;
  onZoomOut?: () => void;
  onZoomReset?: () => void;
  onFitView?: () => void;
  /** 撤销/重做 */
  onUndo?: () => void;
  onRedo?: () => void;
  canUndo?: boolean;
  canRedo?: boolean;
  /** 编辑操作 */
  onCopy?: () => void;
  onPaste?: () => void;
  onDelete?: () => void;
  /** 布局 */
  onVerticalLayout?: () => void;
  onHorizontalLayout?: () => void;
  onAutoLayout?: () => void;
  /** 导出 */
  onExportPNG?: () => void;
  onExportSVG?: () => void;
  onExportJSON?: () => void;
  /** 保存/执行 */
  onSave?: () => void;
  onExecute?: () => void;
  /** 当前缩放比例 */
  zoomLevel?: number;
  /** 是否有选中的节点 */
  hasSelection?: boolean;
  /** 是否正在保存 */
  saving?: boolean;
  /** 是否正在执行 */
  executing?: boolean;
}

/**
 * 工具栏
 */
const ToolBar: React.FC<ToolBarProps> = ({
  onZoomIn,
  onZoomOut,
  onZoomReset,
  onFitView,
  onUndo,
  onRedo,
  canUndo = false,
  canRedo = false,
  onCopy,
  onPaste,
  onDelete,
  onVerticalLayout,
  onHorizontalLayout,
  onAutoLayout,
  onExportPNG,
  onExportSVG,
  onExportJSON,
  onSave,
  onExecute,
  zoomLevel = 100,
  hasSelection = false,
  saving = false,
  executing = false,
}) => {
  /**
   * 导出菜单
   */
  const exportMenuItems: MenuProps['items'] = [
    {
      key: 'png',
      label: '导出为 PNG',
      icon: <DownloadOutlined />,
      onClick: onExportPNG,
    },
    {
      key: 'svg',
      label: '导出为 SVG',
      icon: <DownloadOutlined />,
      onClick: onExportSVG,
    },
    {
      key: 'json',
      label: '导出为 JSON',
      icon: <DownloadOutlined />,
      onClick: onExportJSON,
    },
  ];

  /**
   * 布局菜单
   */
  const layoutMenuItems: MenuProps['items'] = [
    {
      key: 'vertical',
      label: '垂直布局',
      icon: <ColumnHeightOutlined />,
      onClick: onVerticalLayout,
    },
    {
      key: 'horizontal',
      label: '水平布局',
      icon: <ColumnWidthOutlined />,
      onClick: onHorizontalLayout,
    },
    {
      key: 'auto',
      label: '自动布局',
      icon: <ApartmentOutlined />,
      onClick: onAutoLayout,
    },
  ];

  return (
    <div className="liteflow-toolbar">
      <div className="toolbar-left">
        {/* 缩放控制 */}
        <Space.Compact>
          <Tooltip title="缩小">
            <Button icon={<ZoomOutOutlined />} onClick={onZoomOut} />
          </Tooltip>
          <Tooltip title="当前缩放比例">
            <Button className="zoom-level" onClick={onZoomReset}>
              {Math.round(zoomLevel)}%
            </Button>
          </Tooltip>
          <Tooltip title="放大">
            <Button icon={<ZoomInOutlined />} onClick={onZoomIn} />
          </Tooltip>
        </Space.Compact>

        <Tooltip title="1:1">
          <Button icon={<OneToOneOutlined />} onClick={onZoomReset} />
        </Tooltip>

        <Tooltip title="适应画布">
          <Button icon={<CompressOutlined />} onClick={onFitView} />
        </Tooltip>

        <Divider type="vertical" />

        {/* 撤销/重做 */}
        <Tooltip title="撤销 (Ctrl+Z)">
          <Button icon={<UndoOutlined />} onClick={onUndo} disabled={!canUndo} />
        </Tooltip>

        <Tooltip title="重做 (Ctrl+Y)">
          <Button icon={<RedoOutlined />} onClick={onRedo} disabled={!canRedo} />
        </Tooltip>

        <Divider type="vertical" />

        {/* 编辑操作 */}
        <Tooltip title="复制 (Ctrl+C)">
          <Button icon={<CopyOutlined />} onClick={onCopy} disabled={!hasSelection} />
        </Tooltip>

        <Tooltip title="粘贴 (Ctrl+V)">
          <Button icon={<SnippetsOutlined />} onClick={onPaste} />
        </Tooltip>

        <Tooltip title="删除 (Delete)">
          <Button icon={<DeleteOutlined />} onClick={onDelete} disabled={!hasSelection} danger />
        </Tooltip>

        <Divider type="vertical" />

        {/* 布局 */}
        <Dropdown menu={{ items: layoutMenuItems }} placement="bottomLeft">
          <Tooltip title="自动布局">
            <Button icon={<ApartmentOutlined />} />
          </Tooltip>
        </Dropdown>
      </div>

      <div className="toolbar-right">
        {/* 导出 */}
        <Dropdown menu={{ items: exportMenuItems }} placement="bottomRight">
          <Button icon={<DownloadOutlined />}>导出</Button>
        </Dropdown>

        <Divider type="vertical" />

        {/* 保存/执行 */}
        <Tooltip title="保存流程">
          <Button
            type="primary"
            icon={<SaveOutlined />}
            onClick={onSave}
            loading={saving}
          >
            保存
          </Button>
        </Tooltip>

        <Tooltip title="执行流程">
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={onExecute}
            loading={executing}
            style={{ backgroundColor: '#52c41a', borderColor: '#52c41a' }}
          >
            执行
          </Button>
        </Tooltip>
      </div>
    </div>
  );
};

export default ToolBar;
