import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Table,
  Button,
  Input,
  Select,
  Space,
  Tag,
  Tooltip,
  Modal,
  message,
  Card,
  Row,
  Col,
  Statistic,
  Dropdown,
  Form,
  Upload,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { MenuProps } from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  CopyOutlined,
  DownloadOutlined,
  UploadOutlined,
  ReloadOutlined,
  MoreOutlined,
  CloudUploadOutlined,
  CloudDownloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { FlowDesign, FlowRequest } from '../types/flow';
import type { QLComponent } from '../types/component';
import { DeployStatus } from '../types/enums';
import { useFlowManageStore } from '../stores/flowManageStore';
import { componentApi } from '../api/component';
import { flowApi } from '../api/flow';
import './FlowManagePage.css';

/**
 * 完整规则编排导入格式
 * 支持同时导入组件和流程
 */
interface FlowPackageImport {
  components?: QLComponent[];
  flow?: FlowRequest;
  // 兼容单独的流程导入
  flowId?: string;
  flowName?: string;
  root?: unknown;
}

const { Search } = Input;
const { Option } = Select;

/**
 * 部署状态标签
 */
const DeployStatusTag: React.FC<{ status?: DeployStatus }> = ({ status }) => {
  const config: Record<DeployStatus, { color: string; icon: React.ReactNode; text: string }> = {
    [DeployStatus.NOT_DEPLOYED]: { color: 'default', icon: <CloseCircleOutlined />, text: '未部署' },
    [DeployStatus.DEPLOYED]: { color: 'success', icon: <CheckCircleOutlined />, text: '已部署' },
    [DeployStatus.MODIFIED]: { color: 'warning', icon: <SyncOutlined />, text: '已修改' },
    [DeployStatus.FAILED]: { color: 'error', icon: <ExclamationCircleOutlined />, text: '部署失败' },
  };

  const { color, icon, text } = config[status || DeployStatus.NOT_DEPLOYED];

  return (
    <Tag color={color} icon={icon}>
      {text}
    </Tag>
  );
};

/**
 * 流程管理页面
 */
const FlowManagePage: React.FC = () => {
  const navigate = useNavigate();
  const [copyModalVisible, setCopyModalVisible] = useState(false);
  const [copyingFlow, setCopyingFlow] = useState<FlowDesign | null>(null);
  const [copyForm] = Form.useForm();

  const {
    flows,
    total,
    page,
    pageSize,
    filter,
    selectedRowKeys,
    loading,
    categories,
    fetchFlows,
    fetchCategories,
    setPage,
    setPageSize,
    setFilter,
    resetFilter,
    setSelectedRowKeys,
    clearSelection,
    deleteFlow,
    deployFlow,
    undeployFlow,
    copyFlow,
    toggleEnabled,
    batchDeleteFlows,
    batchDeployFlows,
    batchUndeployFlows,
    exportFlow,
    batchExportFlows,
    importFlow,
  } = useFlowManageStore();

  // 初始化加载
  useEffect(() => {
    fetchFlows();
    fetchCategories();
  }, [fetchFlows, fetchCategories]);

  // 搜索处理
  const handleSearch = useCallback(
    (value: string) => {
      setFilter({ keyword: value });
    },
    [setFilter]
  );

  // 分类筛选
  const handleCategoryChange = useCallback(
    (value: string | undefined) => {
      setFilter({ category: value });
    },
    [setFilter]
  );

  // 部署状态筛选
  const handleDeployStatusChange = useCallback(
    (value: DeployStatus | undefined) => {
      setFilter({ deployStatus: value });
    },
    [setFilter]
  );

  // 新建流程
  const handleCreate = useCallback(() => {
    navigate('/flow/new');
  }, [navigate]);

  // 编辑流程
  const handleEdit = useCallback(
    (flowId: string) => {
      navigate(`/flow/${flowId}`);
    },
    [navigate]
  );

  // 删除流程
  const handleDelete = useCallback(
    async (flowId: string) => {
      try {
        await deleteFlow(flowId);
        message.success('删除成功');
      } catch {
        message.error('删除失败');
      }
    },
    [deleteFlow]
  );

  // 部署流程
  const handleDeploy = useCallback(
    async (flowId: string) => {
      try {
        await deployFlow(flowId);
        message.success('部署成功');
      } catch {
        message.error('部署失败');
      }
    },
    [deployFlow]
  );

  // 卸载流程
  const handleUndeploy = useCallback(
    async (flowId: string) => {
      try {
        await undeployFlow(flowId);
        message.success('卸载成功');
      } catch {
        message.error('卸载失败');
      }
    },
    [undeployFlow]
  );

  // 打开复制对话框
  const handleOpenCopyModal = useCallback((flow: FlowDesign) => {
    setCopyingFlow(flow);
    copyForm.setFieldsValue({
      newFlowId: `${flow.flowId}_copy`,
      newFlowName: `${flow.flowName} (副本)`,
    });
    setCopyModalVisible(true);
  }, [copyForm]);

  // 确认复制
  const handleConfirmCopy = useCallback(async () => {
    if (!copyingFlow) return;

    try {
      const values = await copyForm.validateFields();
      await copyFlow(copyingFlow.flowId, values.newFlowId, values.newFlowName);
      message.success('复制成功');
      setCopyModalVisible(false);
      setCopyingFlow(null);
      copyForm.resetFields();
    } catch {
      message.error('复制失败');
    }
  }, [copyingFlow, copyFlow, copyForm]);

  // 导出单个流程
  const handleExport = useCallback(
    async (flowId: string, flowName: string) => {
      try {
        const flowData = await exportFlow(flowId);
        const blob = new Blob([JSON.stringify(flowData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${flowName || flowId}.json`;
        link.click();
        URL.revokeObjectURL(url);
        message.success('导出成功');
      } catch {
        message.error('导出失败');
      }
    },
    [exportFlow]
  );

  // 批量导出
  const handleBatchExport = useCallback(async () => {
    try {
      const flowsData = await batchExportFlows();
      const blob = new Blob([JSON.stringify(flowsData, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `flows_export_${Date.now()}.json`;
      link.click();
      URL.revokeObjectURL(url);
      message.success(`导出 ${flowsData.length} 个流程成功`);
    } catch {
      message.error('批量导出失败');
    }
  }, [batchExportFlows]);

  // 导入流程（支持完整规则编排包）
  const handleImport = useCallback(
    async (file: File) => {
      try {
        const text = await file.text();
        const data = JSON.parse(text) as FlowPackageImport;

        let componentsImported = 0;
        let flowImported = false;

        // 1. 如果包含组件定义，先批量创建组件
        if (data.components && data.components.length > 0) {
          try {
            await componentApi.batchCreate(data.components);
            componentsImported = data.components.length;
          } catch (err) {
            console.warn('部分组件可能已存在:', err);
            // 尝试逐个创建
            for (const comp of data.components) {
              try {
                await componentApi.create(comp);
                componentsImported++;
              } catch {
                // 组件已存在，跳过
              }
            }
          }
        }

        // 2. 创建流程
        if (data.flow) {
          // 完整格式：{ components: [...], flow: {...} }
          await flowApi.create(data.flow);
          flowImported = true;
        } else if (data.flowId && data.root) {
          // 简单格式：直接的 FlowDesign
          await importFlow(data as unknown as FlowDesign);
          flowImported = true;
        }

        // 3. 显示结果
        const messages: string[] = [];
        if (componentsImported > 0) {
          messages.push(`${componentsImported} 个组件`);
        }
        if (flowImported) {
          messages.push('1 个流程');
        }

        if (messages.length > 0) {
          message.success(`导入成功: ${messages.join(', ')}`);
          fetchFlows(); // 刷新列表
        } else {
          message.warning('未找到可导入的内容');
        }
      } catch (err) {
        console.error('导入失败:', err);
        message.error('导入失败，请检查文件格式');
      }
      return false; // 阻止默认上传行为
    },
    [importFlow, fetchFlows]
  );

  // 批量删除
  const handleBatchDelete = useCallback(async () => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除选中的 ${selectedRowKeys.length} 个流程吗？此操作不可恢复。`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await batchDeleteFlows();
          message.success('批量删除成功');
        } catch {
          message.error('批量删除失败');
        }
      },
    });
  }, [selectedRowKeys, batchDeleteFlows]);

  // 批量部署
  const handleBatchDeploy = useCallback(async () => {
    try {
      await batchDeployFlows();
      message.success('批量部署成功');
    } catch {
      message.error('批量部署失败');
    }
  }, [batchDeployFlows]);

  // 批量卸载
  const handleBatchUndeploy = useCallback(async () => {
    try {
      await batchUndeployFlows();
      message.success('批量卸载成功');
    } catch {
      message.error('批量卸载失败');
    }
  }, [batchUndeployFlows]);

  // 切换启用状态
  const handleToggleEnabled = useCallback(
    async (flowId: string, enabled: boolean) => {
      try {
        await toggleEnabled(flowId, enabled);
        message.success(enabled ? '已启用' : '已禁用');
      } catch {
        message.error('操作失败');
      }
    },
    [toggleEnabled]
  );

  // 操作菜单
  const getActionMenu = useCallback(
    (record: FlowDesign): MenuProps => ({
      items: [
        {
          key: 'edit',
          icon: <EditOutlined />,
          label: '编辑',
          onClick: () => handleEdit(record.flowId),
        },
        {
          key: 'copy',
          icon: <CopyOutlined />,
          label: '复制',
          onClick: () => handleOpenCopyModal(record),
        },
        { type: 'divider' },
        {
          key: 'deploy',
          icon: <CloudUploadOutlined />,
          label: '部署',
          disabled: record.deployStatus === DeployStatus.DEPLOYED,
          onClick: () => handleDeploy(record.flowId),
        },
        {
          key: 'undeploy',
          icon: <CloudDownloadOutlined />,
          label: '卸载',
          disabled:
            record.deployStatus === DeployStatus.NOT_DEPLOYED ||
            record.deployStatus === DeployStatus.FAILED,
          onClick: () => handleUndeploy(record.flowId),
        },
        { type: 'divider' },
        {
          key: 'export',
          icon: <DownloadOutlined />,
          label: '导出',
          onClick: () => handleExport(record.flowId, record.flowName),
        },
        { type: 'divider' },
        {
          key: 'delete',
          icon: <DeleteOutlined />,
          label: '删除',
          danger: true,
          onClick: () => {
            Modal.confirm({
              title: '确认删除',
              icon: <ExclamationCircleOutlined />,
              content: `确定要删除流程 "${record.flowName}" 吗？`,
              okText: '删除',
              okType: 'danger',
              cancelText: '取消',
              onOk: () => handleDelete(record.flowId),
            });
          },
        },
      ],
    }),
    [handleEdit, handleOpenCopyModal, handleDeploy, handleUndeploy, handleExport, handleDelete]
  );

  // 表格列定义
  const columns: ColumnsType<FlowDesign> = [
    {
      title: '流程ID',
      dataIndex: 'flowId',
      key: 'flowId',
      width: 180,
      ellipsis: true,
      render: (text: string) => (
        <Tooltip title={text}>
          <span className="flow-id">{text}</span>
        </Tooltip>
      ),
    },
    {
      title: '流程名称',
      dataIndex: 'flowName',
      key: 'flowName',
      width: 200,
      ellipsis: true,
      render: (text: string, record: FlowDesign) => (
        <a onClick={() => handleEdit(record.flowId)}>{text}</a>
      ),
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      render: (text: string) => (text ? <Tag>{text}</Tag> : '-'),
    },
    {
      title: '部署状态',
      dataIndex: 'deployStatus',
      key: 'deployStatus',
      width: 120,
      render: (status: DeployStatus) => <DeployStatusTag status={status} />,
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 80,
      render: (enabled: boolean, record: FlowDesign) => (
        <Tooltip title={enabled ? '点击禁用' : '点击启用'}>
          <Button
            type="text"
            size="small"
            icon={enabled ? <PlayCircleOutlined style={{ color: '#52c41a' }} /> : <PauseCircleOutlined style={{ color: '#999' }} />}
            onClick={() => handleToggleEnabled(record.flowId, !enabled)}
          />
        </Tooltip>
      ),
    },
    {
      title: '版本',
      dataIndex: 'version',
      key: 'version',
      width: 80,
      render: (text: string) => text || 'v1.0',
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 180,
      render: (text: string) => text || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      fixed: 'right',
      render: (_: unknown, record: FlowDesign) => (
        <Space size="small">
          <Tooltip title="编辑">
            <Button
              type="text"
              size="small"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record.flowId)}
            />
          </Tooltip>
          <Tooltip title="部署">
            <Button
              type="text"
              size="small"
              icon={<CloudUploadOutlined />}
              disabled={record.deployStatus === DeployStatus.DEPLOYED}
              onClick={() => handleDeploy(record.flowId)}
            />
          </Tooltip>
          <Dropdown menu={getActionMenu(record)} trigger={['click']}>
            <Button type="text" size="small" icon={<MoreOutlined />} />
          </Dropdown>
        </Space>
      ),
    },
  ];

  // 统计卡片
  const flowList = flows || [];
  const statsCards = [
    {
      title: '总流程数',
      value: total,
      icon: <CheckCircleOutlined />,
      color: '#1890ff',
    },
    {
      title: '已部署',
      value: flowList.filter((f) => f.deployStatus === DeployStatus.DEPLOYED).length,
      icon: <CloudUploadOutlined />,
      color: '#52c41a',
    },
    {
      title: '未部署',
      value: flowList.filter((f) => f.deployStatus === DeployStatus.NOT_DEPLOYED).length,
      icon: <CloudDownloadOutlined />,
      color: '#faad14',
    },
    {
      title: '已修改',
      value: flowList.filter((f) => f.deployStatus === DeployStatus.MODIFIED).length,
      icon: <SyncOutlined />,
      color: '#722ed1',
    },
  ];

  return (
    <div className="flow-manage-page">
      {/* 统计卡片 */}
      <Row gutter={16} className="stats-row">
        {statsCards.map((stat) => (
          <Col span={6} key={stat.title}>
            <Card className="stat-card">
              <Statistic
                title={stat.title}
                value={stat.value}
                prefix={<span style={{ color: stat.color }}>{stat.icon}</span>}
              />
            </Card>
          </Col>
        ))}
      </Row>

      {/* 工具栏 */}
      <Card className="toolbar-card">
        <div className="toolbar">
          <div className="toolbar-left">
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              新建流程
            </Button>
            <Upload
              accept=".json"
              showUploadList={false}
              beforeUpload={handleImport}
            >
              <Button icon={<UploadOutlined />}>导入</Button>
            </Upload>
            {selectedRowKeys.length > 0 && (
              <Space>
                <span className="selected-count">已选 {selectedRowKeys.length} 项</span>
                <Button onClick={handleBatchDeploy} icon={<CloudUploadOutlined />}>
                  批量部署
                </Button>
                <Button onClick={handleBatchUndeploy} icon={<CloudDownloadOutlined />}>
                  批量卸载
                </Button>
                <Button onClick={handleBatchExport} icon={<DownloadOutlined />}>
                  批量导出
                </Button>
                <Button danger onClick={handleBatchDelete} icon={<DeleteOutlined />}>
                  批量删除
                </Button>
                <Button type="link" onClick={clearSelection}>
                  取消选择
                </Button>
              </Space>
            )}
          </div>
          <div className="toolbar-right">
            <Search
              placeholder="搜索流程名称/ID"
              allowClear
              style={{ width: 240 }}
              prefix={<SearchOutlined />}
              onSearch={handleSearch}
              defaultValue={filter.keyword}
            />
            <Select
              placeholder="分类"
              allowClear
              style={{ width: 140 }}
              value={filter.category}
              onChange={handleCategoryChange}
            >
              {categories.map((cat) => (
                <Option key={cat} value={cat}>
                  {cat}
                </Option>
              ))}
            </Select>
            <Select
              placeholder="部署状态"
              allowClear
              style={{ width: 140 }}
              value={filter.deployStatus}
              onChange={handleDeployStatusChange}
            >
              <Option value={DeployStatus.NOT_DEPLOYED}>未部署</Option>
              <Option value={DeployStatus.DEPLOYED}>已部署</Option>
              <Option value={DeployStatus.MODIFIED}>已修改</Option>
              <Option value={DeployStatus.FAILED}>部署失败</Option>
            </Select>
            <Tooltip title="重置筛选">
              <Button icon={<ReloadOutlined />} onClick={resetFilter} />
            </Tooltip>
          </div>
        </div>
      </Card>

      {/* 流程列表 */}
      <Card className="table-card">
        <Table<FlowDesign>
          rowKey="flowId"
          columns={columns}
          dataSource={flowList}
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys as (keys: React.Key[]) => void,
          }}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: setPage,
            onShowSizeChange: (_, size) => setPageSize(size),
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 复制对话框 */}
      <Modal
        title="复制流程"
        open={copyModalVisible}
        onOk={handleConfirmCopy}
        onCancel={() => {
          setCopyModalVisible(false);
          setCopyingFlow(null);
          copyForm.resetFields();
        }}
        okText="确认复制"
        cancelText="取消"
      >
        <Form form={copyForm} layout="vertical">
          <Form.Item
            name="newFlowId"
            label="新流程ID"
            rules={[
              { required: true, message: '请输入流程ID' },
              { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '只能包含字母、数字、下划线，且以字母或下划线开头' },
            ]}
          >
            <Input placeholder="请输入新流程ID" />
          </Form.Item>
          <Form.Item
            name="newFlowName"
            label="新流程名称"
            rules={[{ required: true, message: '请输入流程名称' }]}
          >
            <Input placeholder="请输入新流程名称" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default FlowManagePage;
