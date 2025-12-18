import React, { useMemo } from 'react';
import {
  Card,
  Descriptions,
  Tag,
  Timeline,
  Collapse,
  Empty,
  Typography,
  Tooltip,
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  CodeOutlined,
  BugOutlined,
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';
import type { ExecuteResult, ExecuteTrace } from '../../types/api';
import './ResultDisplay.css';

const { Text, Paragraph } = Typography;

interface ResultDisplayProps {
  /** 执行结果 */
  result?: ExecuteResult | null;
  /** 是否显示执行轨迹 */
  showTrace?: boolean;
  /** 加载状态 */
  loading?: boolean;
}

/**
 * 格式化执行时间
 */
function formatDuration(ms: number): string {
  if (ms < 1) return '<1ms';
  if (ms < 1000) return `${ms.toFixed(2)}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
}

/**
 * 结果展示组件
 */
const ResultDisplay: React.FC<ResultDisplayProps> = ({
  result,
  showTrace = true,
  loading = false,
}) => {
  /**
   * 格式化结果值
   */
  const formattedResult = useMemo(() => {
    if (!result?.result) return null;

    try {
      if (typeof result.result === 'object') {
        return JSON.stringify(result.result, null, 2);
      }
      return String(result.result);
    } catch {
      return String(result.result);
    }
  }, [result?.result]);

  /**
   * 格式化上下文输出
   */
  const formattedContext = useMemo(() => {
    if (!result?.context) return null;

    try {
      return JSON.stringify(result.context, null, 2);
    } catch {
      return null;
    }
  }, [result?.context]);

  /**
   * 渲染执行轨迹
   */
  const renderTrace = (traces: ExecuteTrace[]) => (
    <Timeline
      className="execution-trace"
      items={traces.map((trace, index) => ({
        key: index,
        color: trace.error ? 'red' : 'green',
        dot: trace.error ? <BugOutlined /> : <CodeOutlined />,
        children: (
          <div className="trace-item">
            <div className="trace-header">
              <Text strong>{trace.expression || `Step ${index + 1}`}</Text>
              <Text type="secondary" className="trace-time">
                {formatDuration(trace.duration || 0)}
              </Text>
            </div>
            {trace.result !== undefined && (
              <div className="trace-result">
                <Text type="secondary">结果: </Text>
                <Text code>
                  {typeof trace.result === 'object'
                    ? JSON.stringify(trace.result)
                    : String(trace.result)}
                </Text>
              </div>
            )}
            {trace.error && (
              <Paragraph type="danger" className="trace-error">
                {trace.error}
              </Paragraph>
            )}
          </div>
        ),
      }))}
    />
  );

  if (!result && !loading) {
    return (
      <div className="result-display">
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="暂无执行结果"
        />
      </div>
    );
  }

  const collapseItems = [];

  if (formattedResult) {
    collapseItems.push({
      key: 'result',
      label: (
        <span>
          <CodeOutlined /> 返回值
        </span>
      ),
      children: (
        <div className="result-value">
          <Editor
            height="150px"
            language="json"
            theme="vs"
            value={formattedResult}
            options={{
              readOnly: true,
              minimap: { enabled: false },
              fontSize: 13,
              lineNumbers: 'off',
              scrollBeyondLastLine: false,
              automaticLayout: true,
            }}
          />
        </div>
      ),
    });
  }

  if (formattedContext) {
    collapseItems.push({
      key: 'context',
      label: (
        <span>
          <CodeOutlined /> 上下文输出
        </span>
      ),
      children: (
        <div className="context-output">
          <Editor
            height="200px"
            language="json"
            theme="vs"
            value={formattedContext}
            options={{
              readOnly: true,
              minimap: { enabled: false },
              fontSize: 13,
              lineNumbers: 'off',
              scrollBeyondLastLine: false,
              automaticLayout: true,
            }}
          />
        </div>
      ),
    });
  }

  if (showTrace && result?.trace && result.trace.length > 0) {
    collapseItems.push({
      key: 'trace',
      label: (
        <span>
          <ClockCircleOutlined /> 执行轨迹 ({result.trace.length} 步)
        </span>
      ),
      children: renderTrace(result.trace),
    });
  }

  return (
    <div className="result-display">
      {/* 执行状态卡片 */}
      <Card size="small" className="status-card">
        <Descriptions column={3} size="small">
          <Descriptions.Item label="状态">
            {result?.success ? (
              <Tag icon={<CheckCircleOutlined />} color="success">
                成功
              </Tag>
            ) : (
              <Tag icon={<CloseCircleOutlined />} color="error">
                失败
              </Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="耗时">
            <Tooltip title={`${result?.duration || 0}ms`}>
              <Tag icon={<ClockCircleOutlined />} color="processing">
                {formatDuration(result?.duration || 0)}
              </Tag>
            </Tooltip>
          </Descriptions.Item>
          <Descriptions.Item label="结果类型">
            <Tag>
              {result?.result === null
                ? 'null'
                : typeof result?.result}
            </Tag>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* 错误信息 */}
      {result?.errorMessage && (
        <Card
          size="small"
          className="error-card"
          title={
            <span>
              <CloseCircleOutlined /> 错误信息
            </span>
          }
        >
          <Paragraph
            type="danger"
            copyable={{ text: result.errorMessage }}
            className="error-message"
          >
            {result.errorMessage}
          </Paragraph>
          {result.stackTrace && (
            <Collapse
              ghost
              items={[
                {
                  key: 'stacktrace',
                  label: '堆栈信息',
                  children: (
                    <pre className="stack-trace">{result.stackTrace}</pre>
                  ),
                },
              ]}
            />
          )}
        </Card>
      )}

      {/* 结果详情 */}
      {collapseItems.length > 0 && (
        <Collapse
          defaultActiveKey={['result']}
          items={collapseItems}
          className="result-collapse"
        />
      )}
    </div>
  );
};

export default ResultDisplay;
