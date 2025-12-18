import React, { useEffect, useState } from 'react';
import { Input, Collapse, List, Tag, Spin, Empty, Tooltip } from 'antd';
import {
  SearchOutlined,
  ThunderboltOutlined,
  BranchesOutlined,
  RetweetOutlined,
  ApiOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import { useComponentStore } from '../../../stores';
import { ConditionTypeEnum, ComponentType } from '../../../types/enums';
import type { QLComponent } from '../../../types/component';
import { nodeGroups } from '../model/nodeGroups';
import './SideBar.css';

interface SideBarProps {
  /** 开始拖拽组件 */
  onDragStart?: (
    componentId: string,
    componentName: string,
    componentType: string,
    e: React.MouseEvent
  ) => void;
  /** 开始拖拽控制节点 */
  onDragControlNode?: (type: ConditionTypeEnum, label: string, e: React.MouseEvent) => void;
  /** 自定义样式 */
  style?: React.CSSProperties;
}

/**
 * 获取组件类型颜色
 */
function getComponentTypeColor(type: ComponentType): string {
  switch (type) {
    case ComponentType.SCRIPT:
      return 'blue';
    case ComponentType.BOOLEAN:
      return 'orange';
    case ComponentType.SWITCH:
      return 'purple';
    default:
      return 'default';
  }
}

/**
 * 获取控制节点图标
 */
function getControlNodeIcon(type: ConditionTypeEnum): React.ReactNode {
  switch (type) {
    case ConditionTypeEnum.THEN:
    case ConditionTypeEnum.WHEN:
      return <RetweetOutlined />;
    case ConditionTypeEnum.IF:
    case ConditionTypeEnum.SWITCH:
      return <BranchesOutlined />;
    case ConditionTypeEnum.FOR:
    case ConditionTypeEnum.WHILE:
    case ConditionTypeEnum.ITERATOR:
      return <RetweetOutlined />;
    case ConditionTypeEnum.CATCH:
      return <ExclamationCircleOutlined />;
    case ConditionTypeEnum.AND:
    case ConditionTypeEnum.OR:
    case ConditionTypeEnum.NOT:
      return <ThunderboltOutlined />;
    default:
      return <ApiOutlined />;
  }
}

/**
 * 侧边栏 - 组件面板
 */
const SideBar: React.FC<SideBarProps> = ({ onDragStart, onDragControlNode, style }) => {
  const { components, loading, fetchComponents, searchComponents } = useComponentStore();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [activeKeys, setActiveKeys] = useState<string[]>(['control', 'components']);

  useEffect(() => {
    fetchComponents();
  }, [fetchComponents]);

  /**
   * 处理搜索
   */
  const handleSearch = (value: string) => {
    setSearchKeyword(value);
    if (value.trim()) {
      searchComponents(value);
    } else {
      fetchComponents();
    }
  };

  /**
   * 处理拖拽开始
   */
  const handleDragStart = (component: QLComponent, e: React.MouseEvent) => {
    e.preventDefault();
    onDragStart?.(component.componentId, component.componentName, component.componentType, e);
  };

  /**
   * 处理控制节点拖拽
   */
  const handleControlNodeDrag = (type: ConditionTypeEnum, label: string, e: React.MouseEvent) => {
    e.preventDefault();
    onDragControlNode?.(type, label, e);
  };

  /**
   * 渲染控制节点分组
   */
  const renderControlNodeGroups = () => {
    return nodeGroups.map((group) => (
      <div key={group.name} className="control-node-group">
        <div className="group-title">{group.name}</div>
        <div className="group-nodes">
          {group.nodes.map((node) => (
            <Tooltip key={node.type} title={node.description}>
              <div
                className="control-node-item"
                style={{ borderColor: node.color }}
                onMouseDown={(e) => handleControlNodeDrag(node.type as ConditionTypeEnum, node.label, e)}
              >
                <span className="node-icon" style={{ color: node.color }}>
                  {getControlNodeIcon(node.type as ConditionTypeEnum)}
                </span>
                <span className="node-label">{node.label}</span>
              </div>
            </Tooltip>
          ))}
        </div>
      </div>
    ));
  };

  /**
   * 渲染组件列表
   */
  const renderComponentList = () => {
    if (loading) {
      return (
        <div className="loading-container">
          <Spin />
        </div>
      );
    }

    // 安全检查：确保 components 是数组
    const componentList = Array.isArray(components) ? components : [];

    if (componentList.length === 0) {
      return <Empty description="暂无组件" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
    }

    return (
      <List
        size="small"
        dataSource={componentList}
        renderItem={(component) => (
          <List.Item
            className="component-item"
            onMouseDown={(e) => handleDragStart(component, e)}
          >
            <div className="component-info">
              <div className="component-name">{component.componentName}</div>
              <div className="component-meta">
                <Tag color={getComponentTypeColor(component.componentType)}>
                  {component.componentType}
                </Tag>
                {component.category && (
                  <span className="component-category">{component.category}</span>
                )}
              </div>
            </div>
          </List.Item>
        )}
      />
    );
  };

  // Collapse items 配置 (使用新 API)
  const collapseItems = [
    {
      key: 'control',
      label: '控制节点',
      children: <div className="control-nodes-container">{renderControlNodeGroups()}</div>,
    },
    {
      key: 'components',
      label: '业务组件',
      children: <div className="components-container">{renderComponentList()}</div>,
    },
  ];

  return (
    <div className="liteflow-sidebar" style={style}>
      <div className="sidebar-header">
        <Input
          placeholder="搜索组件..."
          prefix={<SearchOutlined />}
          value={searchKeyword}
          onChange={(e) => handleSearch(e.target.value)}
          allowClear
        />
      </div>

      <div className="sidebar-content">
        <Collapse
          activeKey={activeKeys}
          onChange={(keys) => setActiveKeys(keys as string[])}
          ghost
          expandIconPlacement="end"
          items={collapseItems}
        />
      </div>
    </div>
  );
};

export default SideBar;
