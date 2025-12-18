import React, { useEffect, useState } from 'react';
import {
  Table,
  Button,
  Input,
  Space,
  Tag,
  Dropdown,
  Modal,
  message,
  Tooltip,
  Select,
} from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  CopyOutlined,
  PlayCircleOutlined,
  MoreOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import { useComponentStore } from '../../stores';
import { ComponentType } from '../../types/enums';
import type { QLComponent } from '../../types/component';
import './ComponentList.css';

const { Option } = Select;

interface ComponentListProps {
  /** 选中组件回调 */
  onSelect?: (component: QLComponent) => void;
  /** 编辑组件回调 */
  onEdit?: (component: QLComponent) => void;
  /** 测试组件回调 */
  onTest?: (component: QLComponent) => void;
  /** 新建组件回调 */
  onCreate?: () => void;
}

/**
 * 获取组件类型标签颜色
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
 * 组件列表
 */
const ComponentList: React.FC<ComponentListProps> = ({
  onSelect,
  onEdit,
  onTest,
  onCreate,
}) => {
  const {
    components,
    categories,
    loading,
    pagination,
    filter,
    fetchComponents,
    fetchCategories,
    deleteComponent,
    setFilter,
    clearFilter,
    setPage,
    setPageSize,
    validateComponent,
    syncComponent,
  } = useComponentStore();

  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  useEffect(() => {
    fetchComponents();
    fetchCategories();
  }, [fetchComponents, fetchCategories]);

  /**
   * 处理搜索
   */
  const handleSearch = (value: string) => {
    setFilter({ keyword: value });
  };

  /**
   * 处理删除
   */
  const handleDelete = async (component: QLComponent) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除组件 "${component.componentName}" 吗？`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteComponent(component.componentId);
          message.success('删除成功');
        } catch {
          message.error('删除失败');
        }
      },
    });
  };

  /**
   * 处理验证
   */
  const handleValidate = async (component: QLComponent) => {
    const result = await validateComponent(component.componentId);
    if (result.valid) {
      message.success('组件验证通过');
    } else {
      message.error(`验证失败: ${result.errors?.join(', ')}`);
    }
  };

  /**
   * 处理同步
   */
  const handleSync = async (component: QLComponent) => {
    try {
      await syncComponent(component.componentId);
      message.success('同步成功');
    } catch {
      message.error('同步失败');
    }
  };

  /**
   * 操作菜单
   */
  const getActionMenuItems = (record: QLComponent): MenuProps['items'] => [
    {
      key: 'edit',
      label: '编辑',
      icon: <EditOutlined />,
      onClick: () => onEdit?.(record),
    },
    {
      key: 'copy',
      label: '复制',
      icon: <CopyOutlined />,
      onClick: () => {
        // 复制组件
        message.info('复制功能开发中');
      },
    },
    {
      key: 'test',
      label: '测试',
      icon: <PlayCircleOutlined />,
      onClick: () => onTest?.(record),
    },
    {
      key: 'validate',
      label: '验证',
      icon: <CheckCircleOutlined />,
      onClick: () => handleValidate(record),
    },
    {
      key: 'sync',
      label: '同步到 LiteFlow',
      icon: <SyncOutlined />,
      onClick: () => handleSync(record),
    },
    {
      type: 'divider',
    },
    {
      key: 'delete',
      label: '删除',
      icon: <DeleteOutlined />,
      danger: true,
      onClick: () => handleDelete(record),
    },
  ];

  /**
   * 表格列定义
   */
  const columns: TableColumnsType<QLComponent> = [
    {
      title: '组件名称',
      dataIndex: 'componentName',
      key: 'componentName',
      width: 200,
      ellipsis: true,
      render: (text, record) => (
        <a onClick={() => onSelect?.(record)}>{text}</a>
      ),
    },
    {
      title: '组件ID',
      dataIndex: 'componentId',
      key: 'componentId',
      width: 150,
      ellipsis: true,
      render: (text) => (
        <Tooltip title={text}>
          <code style={{ fontSize: 12 }}>{text}</code>
        </Tooltip>
      ),
    },
    {
      title: '类型',
      dataIndex: 'componentType',
      key: 'componentType',
      width: 100,
      render: (type: ComponentType) => (
        <Tag color={getComponentTypeColor(type)}>{type}</Tag>
      ),
      filters: [
        { text: '脚本组件', value: ComponentType.SCRIPT },
        { text: '布尔组件', value: ComponentType.BOOLEAN },
        { text: '选择组件', value: ComponentType.SWITCH },
      ],
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled: boolean) =>
        enabled ? (
          <Tag icon={<CheckCircleOutlined />} color="success">
            启用
          </Tag>
        ) : (
          <Tag icon={<CloseCircleOutlined />} color="default">
            禁用
          </Tag>
        ),
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Tooltip title="编辑">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => onEdit?.(record)}
            />
          </Tooltip>
          <Tooltip title="测试">
            <Button
              type="text"
              size="small"
              icon={<PlayCircleOutlined />}
              onClick={() => onTest?.(record)}
            />
          </Tooltip>
          <Dropdown menu={{ items: getActionMenuItems(record) }} trigger={['click']}>
            <Button type="text" size="small" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <div className="component-list">
      {/* 工具栏 */}
      <div className="component-list-toolbar">
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>
            新建组件
          </Button>
          <Input.Search
            placeholder="搜索组件..."
            allowClear
            style={{ width: 240 }}
            prefix={<SearchOutlined />}
            onSearch={handleSearch}
            onChange={(e) => {
              if (!e.target.value) {
                clearFilter();
              }
            }}
          />
          <Select
            placeholder="选择类型"
            allowClear
            style={{ width: 120 }}
            value={filter.componentType}
            onChange={(value) => setFilter({ componentType: value })}
          >
            <Option value={ComponentType.SCRIPT}>脚本组件</Option>
            <Option value={ComponentType.BOOLEAN}>布尔组件</Option>
            <Option value={ComponentType.SWITCH}>选择组件</Option>
          </Select>
          <Select
            placeholder="选择分类"
            allowClear
            style={{ width: 120 }}
            value={filter.category}
            onChange={(value) => setFilter({ category: value })}
          >
            {categories.map((cat) => (
              <Option key={cat} value={cat}>
                {cat}
              </Option>
            ))}
          </Select>
        </Space>
      </div>

      {/* 表格 */}
      <Table
        rowKey="componentId"
        columns={columns}
        dataSource={components}
        loading={loading}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{
          current: pagination.page,
          pageSize: pagination.size,
          total: pagination.total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 条`,
          onChange: (page, pageSize) => {
            if (pageSize !== pagination.size) {
              setPageSize(pageSize);
            } else {
              setPage(page);
            }
          },
        }}
        scroll={{ x: 1000 }}
        size="middle"
      />
    </div>
  );
};

export default ComponentList;
