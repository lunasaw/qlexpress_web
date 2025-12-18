import React from 'react';
import { Form, Input, Select, Switch, InputNumber, Tabs, Empty, Button, Space } from 'antd';
import { DeleteOutlined, CopyOutlined } from '@ant-design/icons';
import type { Node } from '@antv/x6';
import { ConditionTypeEnum, ComponentType } from '../../../types/enums';
import './SettingBar.css';

const { TextArea } = Input;
const { Option } = Select;

interface SettingBarProps {
  /** 选中的节点 */
  selectedNode?: Node | null;
  /** 节点数据变化回调 */
  onNodeDataChange?: (nodeId: string, data: Record<string, unknown>) => void;
  /** 删除节点 */
  onDeleteNode?: (nodeId: string) => void;
  /** 复制节点 */
  onCopyNode?: (nodeId: string) => void;
}

/**
 * 获取节点类型名称
 */
function getNodeTypeName(type: string): string {
  const typeNames: Record<string, string> = {
    [ConditionTypeEnum.THEN]: '串行执行 (THEN)',
    [ConditionTypeEnum.WHEN]: '并行执行 (WHEN)',
    [ConditionTypeEnum.IF]: '条件判断 (IF)',
    [ConditionTypeEnum.SWITCH]: '多路选择 (SWITCH)',
    [ConditionTypeEnum.FOR]: 'FOR 循环',
    [ConditionTypeEnum.WHILE]: 'WHILE 循环',
    [ConditionTypeEnum.ITERATOR]: '迭代器循环',
    [ConditionTypeEnum.CATCH]: '异常捕获 (CATCH)',
    [ConditionTypeEnum.AND]: '逻辑与 (AND)',
    [ConditionTypeEnum.OR]: '逻辑或 (OR)',
    [ConditionTypeEnum.NOT]: '逻辑非 (NOT)',
    NODE: '业务组件',
  };
  return typeNames[type] || type;
}

/**
 * 属性配置面板
 */
const SettingBar: React.FC<SettingBarProps> = ({
  selectedNode,
  onNodeDataChange,
  onDeleteNode,
  onCopyNode,
}) => {
  const [form] = Form.useForm();

  // 获取节点数据
  const nodeData = selectedNode?.getData() || {};
  const nodeType = nodeData.type || nodeData.nodeType || 'NODE';

  // 表单值变化时更新节点
  const handleValuesChange = (changedValues: Record<string, unknown>) => {
    if (selectedNode) {
      onNodeDataChange?.(selectedNode.id, {
        ...nodeData,
        ...changedValues,
      });
    }
  };

  /**
   * 渲染基础属性表单
   */
  const renderBasicForm = () => {
    return (
      <Form
        form={form}
        layout="vertical"
        size="small"
        initialValues={nodeData}
        onValuesChange={handleValuesChange}
      >
        <Form.Item label="节点ID">
          <Input value={selectedNode?.id} disabled />
        </Form.Item>

        <Form.Item label="节点类型">
          <Input value={getNodeTypeName(nodeType)} disabled />
        </Form.Item>

        <Form.Item label="节点标签" name="label">
          <Input placeholder="请输入节点标签" />
        </Form.Item>

        <Form.Item label="节点描述" name="description">
          <TextArea rows={3} placeholder="请输入节点描述" />
        </Form.Item>
      </Form>
    );
  };

  /**
   * 渲染组件节点配置
   */
  const renderComponentConfig = () => {
    return (
      <Form
        form={form}
        layout="vertical"
        size="small"
        initialValues={nodeData}
        onValuesChange={handleValuesChange}
      >
        <Form.Item label="组件ID" name="componentId">
          <Input placeholder="请选择或输入组件ID" />
        </Form.Item>

        <Form.Item label="组件类型" name="componentType">
          <Select placeholder="请选择组件类型">
            <Option value={ComponentType.SCRIPT}>脚本组件</Option>
            <Option value={ComponentType.BOOLEAN}>布尔组件</Option>
            <Option value={ComponentType.SWITCH}>选择组件</Option>
          </Select>
        </Form.Item>

        <Form.Item label="重试次数" name="retryCount">
          <InputNumber min={0} max={10} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item label="超时时间(ms)" name="timeout">
          <InputNumber min={0} step={1000} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    );
  };

  /**
   * 渲染条件节点配置
   */
  const renderConditionConfig = () => {
    return (
      <Form
        form={form}
        layout="vertical"
        size="small"
        initialValues={nodeData}
        onValuesChange={handleValuesChange}
      >
        <Form.Item label="条件组件" name="conditionComponent">
          <Input placeholder="请输入条件组件ID" />
        </Form.Item>

        {nodeType === ConditionTypeEnum.IF && (
          <>
            <Form.Item label="真分支标签" name="trueBranchLabel">
              <Input placeholder="条件为真时的分支" />
            </Form.Item>
            <Form.Item label="假分支标签" name="falseBranchLabel">
              <Input placeholder="条件为假时的分支" />
            </Form.Item>
          </>
        )}

        {nodeType === ConditionTypeEnum.SWITCH && (
          <Form.Item label="默认分支" name="defaultBranch">
            <Input placeholder="请输入默认分支标识" />
          </Form.Item>
        )}
      </Form>
    );
  };

  /**
   * 渲染循环节点配置
   */
  const renderLoopConfig = () => {
    return (
      <Form
        form={form}
        layout="vertical"
        size="small"
        initialValues={nodeData}
        onValuesChange={handleValuesChange}
      >
        {nodeType === ConditionTypeEnum.FOR && (
          <Form.Item label="循环次数" name="loopCount">
            <InputNumber min={1} max={1000} style={{ width: '100%' }} />
          </Form.Item>
        )}

        {(nodeType === ConditionTypeEnum.WHILE ||
          nodeType === ConditionTypeEnum.ITERATOR) && (
          <Form.Item label="条件组件" name="conditionComponent">
            <Input placeholder="请输入条件组件ID" />
          </Form.Item>
        )}

        <Form.Item label="并行执行" name="parallel" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    );
  };

  /**
   * 渲染异常处理配置
   */
  const renderCatchConfig = () => {
    return (
      <Form
        form={form}
        layout="vertical"
        size="small"
        initialValues={nodeData}
        onValuesChange={handleValuesChange}
      >
        <Form.Item label="异常处理组件" name="catchComponent">
          <Input placeholder="请输入异常处理组件ID" />
        </Form.Item>
      </Form>
    );
  };

  /**
   * 根据节点类型渲染配置表单
   */
  const renderConfigForm = () => {
    switch (nodeType) {
      case ConditionTypeEnum.IF:
      case ConditionTypeEnum.SWITCH:
        return renderConditionConfig();
      case ConditionTypeEnum.FOR:
      case ConditionTypeEnum.WHILE:
      case ConditionTypeEnum.ITERATOR:
        return renderLoopConfig();
      case ConditionTypeEnum.CATCH:
        return renderCatchConfig();
      case 'NODE':
      default:
        return renderComponentConfig();
    }
  };

  // 无选中节点时显示空状态
  if (!selectedNode) {
    return (
      <div className="liteflow-setting-bar">
        <div className="setting-bar-header">
          <span>属性配置</span>
        </div>
        <div className="setting-bar-empty">
          <Empty description="请选择一个节点" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        </div>
      </div>
    );
  }

  const tabItems = [
    {
      key: 'basic',
      label: '基础',
      children: renderBasicForm(),
    },
    {
      key: 'config',
      label: '配置',
      children: renderConfigForm(),
    },
  ];

  return (
    <div className="liteflow-setting-bar">
      <div className="setting-bar-header">
        <span>属性配置</span>
        <Space>
          <Button
            type="text"
            size="small"
            icon={<CopyOutlined />}
            onClick={() => onCopyNode?.(selectedNode.id)}
          />
          <Button
            type="text"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => onDeleteNode?.(selectedNode.id)}
          />
        </Space>
      </div>
      <div className="setting-bar-content">
        <Tabs items={tabItems} size="small" />
      </div>
    </div>
  );
};

export default SettingBar;
