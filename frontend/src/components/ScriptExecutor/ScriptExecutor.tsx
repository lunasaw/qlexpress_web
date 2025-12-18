import React, { useState, useCallback } from 'react';
import {
  Card,
  Button,
  Space,
  Tabs,
  Splitter,
  message,
  Tooltip,
} from 'antd';
import {
  PlayCircleOutlined,
  ClearOutlined,
  SettingOutlined,
  CodeOutlined,
  ProfileOutlined,
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';
import ParameterInput from './ParameterInput';
import ResultDisplay from './ResultDisplay';
import { useExecuteStore } from '../../stores';
import type { ParameterDef } from '../../types/component';
import type { ExecuteResult } from '../../types/api';
import './ScriptExecutor.css';

interface ScriptExecutorProps {
  /** 初始脚本 */
  script?: string;
  /** 预定义参数 */
  parameters?: ParameterDef[];
  /** 执行回调 */
  onExecute?: (script: string, params: Record<string, unknown>) => Promise<ExecuteResult>;
  /** 是否显示脚本编辑器 */
  showEditor?: boolean;
  /** 标题 */
  title?: string;
}

/**
 * 脚本执行器
 */
const ScriptExecutor: React.FC<ScriptExecutorProps> = ({
  script: initialScript = '',
  parameters = [],
  onExecute,
  showEditor = true,
  title = '脚本执行器',
}) => {
  const [script, setScript] = useState(initialScript);
  const [params, setParams] = useState<Record<string, unknown>>({});
  const [result, setResult] = useState<ExecuteResult | null>(null);
  const [executing, setExecuting] = useState(false);

  const { executeScript, history, clearHistory } = useExecuteStore();

  /**
   * 执行脚本
   */
  const handleExecute = useCallback(async () => {
    if (!script.trim()) {
      message.warning('请输入脚本内容');
      return;
    }

    setExecuting(true);
    try {
      let execResult: ExecuteResult;

      if (onExecute) {
        execResult = await onExecute(script, params);
      } else {
        // 处理自由参数格式
        let execParams = { ...params };
        if (params.__freeParams && Array.isArray(params.__freeParams)) {
          const freeParams: Record<string, unknown> = {};
          (params.__freeParams as { key: string; type: string; value: string }[]).forEach(
            (item) => {
              if (item.key) {
                // 根据类型转换值
                let value: unknown = item.value;
                switch (item.type) {
                  case 'Integer':
                  case 'Long':
                    value = parseInt(item.value, 10);
                    break;
                  case 'Double':
                    value = parseFloat(item.value);
                    break;
                  case 'Boolean':
                    value = item.value === 'true';
                    break;
                  case 'Object':
                    try {
                      value = JSON.parse(item.value);
                    } catch {
                      // 保持字符串
                    }
                    break;
                }
                freeParams[item.key] = value;
              }
            }
          );
          execParams = freeParams;
        }

        execResult = await executeScript(script, execParams);
      }

      setResult(execResult);

      if (execResult.success) {
        message.success('执行成功');
      } else {
        message.error(execResult.errorMessage || '执行失败');
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '执行失败';
      message.error(errorMessage);
      setResult({
        success: false,
        errorMessage,
        duration: 0,
      });
    } finally {
      setExecuting(false);
    }
  }, [script, params, onExecute, executeScript]);

  /**
   * 清除结果
   */
  const handleClear = () => {
    setResult(null);
    clearHistory();
  };

  /**
   * 渲染历史记录
   */
  const renderHistory = () => (
    <div className="execution-history">
      {history.length === 0 ? (
        <div className="empty-history">暂无执行历史</div>
      ) : (
        history.map((item, index) => (
          <Card
            key={index}
            size="small"
            className={`history-item ${item.success ? 'success' : 'failed'}`}
            title={
              <span>
                <CodeOutlined /> {new Date(item.timestamp).toLocaleTimeString()}
              </span>
            }
            extra={
              <span className="history-duration">
                {item.duration}ms
              </span>
            }
          >
            <div className="history-script">
              {item.script.substring(0, 100)}
              {item.script.length > 100 && '...'}
            </div>
            {item.errorMessage && (
              <div className="history-error">{item.errorMessage}</div>
            )}
          </Card>
        ))
      )}
    </div>
  );

  const tabItems = [
    {
      key: 'params',
      label: (
        <span>
          <SettingOutlined /> 参数
        </span>
      ),
      children: (
        <ParameterInput
          parameters={parameters}
          values={params}
          onChange={setParams}
        />
      ),
    },
    {
      key: 'history',
      label: (
        <span>
          <ProfileOutlined /> 历史
        </span>
      ),
      children: renderHistory(),
    },
  ];

  return (
    <div className="script-executor">
      <div className="executor-header">
        <span className="executor-title">{title}</span>
        <Space>
          <Tooltip title="清除结果">
            <Button icon={<ClearOutlined />} onClick={handleClear} />
          </Tooltip>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            loading={executing}
            onClick={handleExecute}
          >
            执行
          </Button>
        </Space>
      </div>

      <div className="executor-body">
        <Splitter layout="vertical">
          {/* 上部：脚本编辑与参数 */}
          <Splitter.Panel defaultSize="50%" min="30%">
            <Splitter>
              {showEditor && (
                <Splitter.Panel defaultSize="60%" min="40%">
                  <Card
                    title={
                      <span>
                        <CodeOutlined /> 脚本编辑
                      </span>
                    }
                    size="small"
                    className="editor-card"
                  >
                    <Editor
                      height="100%"
                      language="java"
                      theme="vs"
                      value={script}
                      onChange={(value) => setScript(value || '')}
                      options={{
                        minimap: { enabled: false },
                        fontSize: 14,
                        lineNumbers: 'on',
                        scrollBeyondLastLine: false,
                        automaticLayout: true,
                        tabSize: 2,
                        wordWrap: 'on',
                      }}
                    />
                  </Card>
                </Splitter.Panel>
              )}
              <Splitter.Panel min="30%">
                <Card size="small" className="params-card">
                  <Tabs items={tabItems} size="small" />
                </Card>
              </Splitter.Panel>
            </Splitter>
          </Splitter.Panel>

          {/* 下部：执行结果 */}
          <Splitter.Panel min="20%">
            <Card
              title={
                <span>
                  <ProfileOutlined /> 执行结果
                </span>
              }
              size="small"
              className="result-card"
            >
              <ResultDisplay
                result={result}
                showTrace={true}
                loading={executing}
              />
            </Card>
          </Splitter.Panel>
        </Splitter>
      </div>
    </div>
  );
};

export default ScriptExecutor;
