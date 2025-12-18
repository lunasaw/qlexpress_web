import React, { useState } from 'react';
import { Button, Tooltip, message, Tabs, Tag, Empty } from 'antd';
import {
  CopyOutlined,
  ExpandOutlined,
  CompressOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';
import './ELPreview.css';

interface ELPreviewProps {
  /** EL 表达式 */
  elExpression?: string;
  /** JSON 结构 */
  jsonStructure?: string;
  /** 是否有效 */
  isValid?: boolean;
  /** 验证错误信息 */
  validationErrors?: string[];
  /** 刷新 EL 表达式 */
  onRefresh?: () => void;
  /** 是否正在加载 */
  loading?: boolean;
}

/**
 * EL 表达式预览面板
 */
const ELPreview: React.FC<ELPreviewProps> = ({
  elExpression = '',
  jsonStructure = '',
  isValid = true,
  validationErrors = [],
  onRefresh,
  loading = false,
}) => {
  const [expanded, setExpanded] = useState(false);

  /**
   * 复制到剪贴板
   */
  const handleCopy = async (text: string, type: string) => {
    try {
      await navigator.clipboard.writeText(text);
      message.success(`${type}已复制到剪贴板`);
    } catch {
      message.error('复制失败');
    }
  };

  /**
   * 格式化 JSON
   */
  const formatJSON = (json: string): string => {
    try {
      return JSON.stringify(JSON.parse(json), null, 2);
    } catch {
      return json;
    }
  };

  /**
   * 渲染 EL 表达式预览
   */
  const renderELPreview = () => {
    if (!elExpression) {
      return (
        <Empty
          description="暂无 EL 表达式"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          style={{ padding: '40px 0' }}
        />
      );
    }

    return (
      <div className="el-preview-content">
        <div className="el-preview-header">
          <div className="el-status">
            {isValid ? (
              <Tag icon={<CheckCircleOutlined />} color="success">
                有效
              </Tag>
            ) : (
              <Tag icon={<CloseCircleOutlined />} color="error">
                无效
              </Tag>
            )}
          </div>
          <div className="el-actions">
            <Tooltip title="刷新">
              <Button
                type="text"
                size="small"
                icon={<ReloadOutlined spin={loading} />}
                onClick={onRefresh}
              />
            </Tooltip>
            <Tooltip title="复制">
              <Button
                type="text"
                size="small"
                icon={<CopyOutlined />}
                onClick={() => handleCopy(elExpression, 'EL 表达式')}
              />
            </Tooltip>
          </div>
        </div>

        <div className="el-expression-box">
          <Editor
            height={expanded ? '300px' : '150px'}
            language="plaintext"
            value={elExpression}
            options={{
              readOnly: true,
              minimap: { enabled: false },
              lineNumbers: 'off',
              scrollBeyondLastLine: false,
              wordWrap: 'on',
              fontSize: 13,
              fontFamily: 'Consolas, Monaco, monospace',
              padding: { top: 8, bottom: 8 },
            }}
            theme="vs"
          />
        </div>

        {!isValid && validationErrors.length > 0 && (
          <div className="el-errors">
            {validationErrors.map((error, index) => (
              <div key={index} className="el-error-item">
                <CloseCircleOutlined style={{ color: '#ff4d4f', marginRight: 8 }} />
                {error}
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  /**
   * 渲染 JSON 结构预览
   */
  const renderJSONPreview = () => {
    if (!jsonStructure) {
      return (
        <Empty
          description="暂无 JSON 结构"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          style={{ padding: '40px 0' }}
        />
      );
    }

    const formattedJSON = formatJSON(jsonStructure);

    return (
      <div className="json-preview-content">
        <div className="json-preview-header">
          <div className="json-actions">
            <Tooltip title="复制">
              <Button
                type="text"
                size="small"
                icon={<CopyOutlined />}
                onClick={() => handleCopy(formattedJSON, 'JSON 结构')}
              />
            </Tooltip>
          </div>
        </div>

        <div className="json-structure-box">
          <Editor
            height={expanded ? '300px' : '200px'}
            language="json"
            value={formattedJSON}
            options={{
              readOnly: true,
              minimap: { enabled: false },
              lineNumbers: 'on',
              scrollBeyondLastLine: false,
              wordWrap: 'on',
              fontSize: 12,
              fontFamily: 'Consolas, Monaco, monospace',
              padding: { top: 8, bottom: 8 },
              folding: true,
            }}
            theme="vs"
          />
        </div>
      </div>
    );
  };

  const tabItems = [
    {
      key: 'el',
      label: 'EL 表达式',
      children: renderELPreview(),
    },
    {
      key: 'json',
      label: 'JSON 结构',
      children: renderJSONPreview(),
    },
  ];

  return (
    <div className={`liteflow-el-preview ${expanded ? 'expanded' : ''}`}>
      <div className="el-preview-panel-header">
        <span>表达式预览</span>
        <Tooltip title={expanded ? '收起' : '展开'}>
          <Button
            type="text"
            size="small"
            icon={expanded ? <CompressOutlined /> : <ExpandOutlined />}
            onClick={() => setExpanded(!expanded)}
          />
        </Tooltip>
      </div>
      <div className="el-preview-panel-content">
        <Tabs items={tabItems} size="small" />
      </div>
    </div>
  );
};

export default ELPreview;
