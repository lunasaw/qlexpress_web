import React from 'react';
import { useParams } from 'react-router-dom';
import { LiteFlowEditor } from '../components/LiteFlowEditor';
import './pages.css';

/**
 * 流程编排器页面
 */
const FlowEditorPage: React.FC = () => {
  const { flowId } = useParams<{ flowId: string }>();

  /**
   * 保存流程
   */
  const handleSave = () => {
    console.log('Save flow');
    // TODO: 调用 API 保存流程
  };

  /**
   * 执行流程
   */
  const handleExecute = () => {
    console.log('Execute flow');
    // TODO: 调用 API 执行流程
  };

  return (
    <div className="flow-editor-page">
      <LiteFlowEditor
        flowId={flowId}
        onSave={handleSave}
        onExecute={handleExecute}
      />
    </div>
  );
};

export default FlowEditorPage;
