import React, { useRef, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Graph } from '@antv/x6';
import LiteFlowEditor from '../components/LiteFlowEditor';
import './pages.css';

interface EditorRef {
  getGraphInstance: () => Graph | undefined;
  toJSON: () => Record<string, any>;
  fromJSON: (data: Record<string, any>) => void;
}

/**
 * 流程编排器页面
 */
const FlowEditorPage: React.FC = () => {
  const { flowId } = useParams<{ flowId: string }>();
  const editorRef = useRef<EditorRef>(null);

  useEffect(() => {
    // 如果有 flowId，加载流程数据
    if (flowId && editorRef.current) {
      // TODO: 从后端加载流程数据
      console.log('Load flow:', flowId);
    }
  }, [flowId]);

  /**
   * 图准备就绪
   */
  const handleReady = (graph: Graph) => {
    console.log('Graph ready:', graph);
  };

  return (
    <div className="flow-editor-page">
      <LiteFlowEditor
        ref={editorRef as any}
        onReady={handleReady}
      />
    </div>
  );
};

export default FlowEditorPage;
