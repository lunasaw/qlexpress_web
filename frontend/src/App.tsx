import React from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link, useLocation } from 'react-router-dom';
import { Layout, Menu, ConfigProvider } from 'antd';
import {
  AppstoreOutlined,
  ApartmentOutlined,
  CodeOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import zhCN from 'antd/locale/zh_CN';
import { ComponentDesignerPage, FlowEditorPage, FlowManagePage, ScriptExecutorPage } from './pages';
import './App.css';

const { Header, Content } = Layout;

/**
 * 应用主布局
 */
const AppLayout: React.FC = () => {
  const location = useLocation();

  const menuItems = [
    {
      key: '/components',
      icon: <AppstoreOutlined />,
      label: <Link to="/components">组件设计器</Link>,
    },
    {
      key: '/flows',
      icon: <UnorderedListOutlined />,
      label: <Link to="/flows">流程管理</Link>,
    },
    {
      key: '/flow',
      icon: <ApartmentOutlined />,
      label: <Link to="/flow">流程编排器</Link>,
    },
    {
      key: '/executor',
      icon: <CodeOutlined />,
      label: <Link to="/executor">脚本执行器</Link>,
    },
  ];

  // 获取当前选中的菜单项
  const getSelectedKey = () => {
    const path = location.pathname;
    if (path.startsWith('/components')) return '/components';
    if (path === '/flows') return '/flows';
    if (path.startsWith('/flow')) return '/flow';
    if (path.startsWith('/executor')) return '/executor';
    return '/components';
  };

  return (
    <Layout className="app-layout">
      <Header className="app-header">
        <div className="app-logo">
          <ApartmentOutlined />
          <span>QLExpress 可视化编排</span>
        </div>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[getSelectedKey()]}
          items={menuItems}
          className="app-menu"
        />
      </Header>
      <Content className="app-content">
        <Routes>
          <Route path="/" element={<Navigate to="/flows" replace />} />
          <Route path="/components" element={<ComponentDesignerPage />} />
          <Route path="/flows" element={<FlowManagePage />} />
          <Route path="/flow" element={<FlowEditorPage />} />
          <Route path="/flow/new" element={<FlowEditorPage />} />
          <Route path="/flow/:flowId" element={<FlowEditorPage />} />
          <Route path="/executor" element={<ScriptExecutorPage />} />
        </Routes>
      </Content>
    </Layout>
  );
};

/**
 * 应用入口
 */
const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <AppLayout />
      </BrowserRouter>
    </ConfigProvider>
  );
};

export default App;
