import React, { useState } from 'react';
import { Layout, Modal } from 'antd';
import { ComponentList, ComponentForm } from '../components/ComponentDesigner';
import { ScriptExecutor } from '../components/ScriptExecutor';
import type { QLComponent } from '../types/component';
import './pages.css';

const { Content, Sider } = Layout;

/**
 * 组件设计器页面
 */
const ComponentDesignerPage: React.FC = () => {
  const [selectedComponent, setSelectedComponent] = useState<QLComponent | null>(null);
  const [editingComponent, setEditingComponent] = useState<QLComponent | null>(null);
  const [isFormVisible, setIsFormVisible] = useState(false);
  const [isTestVisible, setIsTestVisible] = useState(false);
  const [testComponent, setTestComponent] = useState<QLComponent | null>(null);

  /**
   * 创建新组件
   */
  const handleCreate = () => {
    setEditingComponent(null);
    setIsFormVisible(true);
  };

  /**
   * 编辑组件
   */
  const handleEdit = (component: QLComponent) => {
    setEditingComponent(component);
    setIsFormVisible(true);
  };

  /**
   * 测试组件
   */
  const handleTest = (component: QLComponent) => {
    setTestComponent(component);
    setIsTestVisible(true);
  };

  /**
   * 选中组件
   */
  const handleSelect = (component: QLComponent) => {
    setSelectedComponent(component);
  };

  /**
   * 保存组件
   */
  const handleSave = () => {
    setIsFormVisible(false);
    setEditingComponent(null);
  };

  /**
   * 取消编辑
   */
  const handleCancel = () => {
    setIsFormVisible(false);
    setEditingComponent(null);
  };

  return (
    <Layout className="page-layout">
      <Content className="page-content">
        {isFormVisible ? (
          <ComponentForm
            component={editingComponent}
            onSave={handleSave}
            onCancel={handleCancel}
            onTest={handleTest}
          />
        ) : (
          <ComponentList
            onSelect={handleSelect}
            onEdit={handleEdit}
            onTest={handleTest}
            onCreate={handleCreate}
          />
        )}
      </Content>

      {/* 组件详情侧边栏 */}
      {selectedComponent && !isFormVisible && (
        <Sider width={300} className="page-sider" theme="light">
          <div className="component-detail">
            <h3>{selectedComponent.componentName}</h3>
            <p className="component-id">{selectedComponent.componentId}</p>
            <p className="component-desc">{selectedComponent.description}</p>
            <pre className="component-script">{selectedComponent.script}</pre>
          </div>
        </Sider>
      )}

      {/* 测试弹窗 */}
      <Modal
        title={`测试组件: ${testComponent?.componentName}`}
        open={isTestVisible}
        onCancel={() => setIsTestVisible(false)}
        footer={null}
        width={800}
        destroyOnClose
      >
        {testComponent && (
          <ScriptExecutor
            script={testComponent.script || ''}
            parameters={testComponent.inputs || []}
            showEditor={false}
            title="组件测试"
          />
        )}
      </Modal>
    </Layout>
  );
};

export default ComponentDesignerPage;
