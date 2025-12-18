import React from 'react';
import {
  Form,
  Input,
  InputNumber,
  Switch,
  Select,
  Button,
  Space,
  Tooltip,
} from 'antd';
import {
  PlusOutlined,
  MinusCircleOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';
import type { ParameterDef } from '../../types/component';
import './ParameterInput.css';

const { Option } = Select;

interface ParameterInputProps {
  /** 参数定义 */
  parameters?: ParameterDef[];
  /** 当前参数值 */
  values?: Record<string, unknown>;
  /** 值变化回调 */
  onChange?: (values: Record<string, unknown>) => void;
  /** 是否只读 */
  readonly?: boolean;
}

/**
 * 参数输入组件
 */
const ParameterInput: React.FC<ParameterInputProps> = ({
  parameters = [],
  values = {},
  onChange,
  readonly = false,
}) => {
  const [form] = Form.useForm();

  /**
   * 表单值变化
   */
  const handleValuesChange = (_: unknown, allValues: Record<string, unknown>) => {
    onChange?.(allValues);
  };

  /**
   * 根据类型渲染输入控件
   */
  const renderInput = (param: ParameterDef) => {
    const { name, type, description } = param;

    switch (type) {
      case 'Integer':
      case 'Long':
        return (
          <InputNumber
            placeholder={description || `请输入 ${name}`}
            style={{ width: '100%' }}
            disabled={readonly}
          />
        );

      case 'Double':
        return (
          <InputNumber
            placeholder={description || `请输入 ${name}`}
            style={{ width: '100%' }}
            step={0.01}
            disabled={readonly}
          />
        );

      case 'Boolean':
        return <Switch disabled={readonly} />;

      case 'List':
        return (
          <Form.List name={name}>
            {(fields, { add, remove }) => (
              <div className="list-input">
                {fields.map(({ key, name: fieldName, ...restField }) => (
                  <Space key={key} className="list-item">
                    <Form.Item {...restField} name={fieldName} noStyle>
                      <Input placeholder="列表项" disabled={readonly} />
                    </Form.Item>
                    {!readonly && (
                      <MinusCircleOutlined onClick={() => remove(fieldName)} />
                    )}
                  </Space>
                ))}
                {!readonly && (
                  <Button
                    type="dashed"
                    onClick={() => add()}
                    icon={<PlusOutlined />}
                    size="small"
                  >
                    添加项
                  </Button>
                )}
              </div>
            )}
          </Form.List>
        );

      case 'Map':
      case 'Object':
        return (
          <div className="json-input">
            <Editor
              height="120px"
              language="json"
              theme="vs"
              defaultValue="{}"
              options={{
                minimap: { enabled: false },
                fontSize: 12,
                lineNumbers: 'off',
                scrollBeyondLastLine: false,
                automaticLayout: true,
                readOnly: readonly,
              }}
              onChange={(value) => {
                try {
                  const parsed = JSON.parse(value || '{}');
                  form.setFieldValue(name, parsed);
                  const allValues = form.getFieldsValue();
                  onChange?.(allValues);
                } catch {
                  // JSON 解析失败,忽略
                }
              }}
            />
          </div>
        );

      case 'String':
      default:
        return (
          <Input
            placeholder={description || `请输入 ${name}`}
            disabled={readonly}
          />
        );
    }
  };

  /**
   * 渲染自由参数输入 (无预定义参数时)
   */
  const renderFreeParams = () => (
    <Form.List name="__freeParams">
      {(fields, { add, remove }) => (
        <div className="free-params">
          {fields.map(({ key, name: fieldName, ...restField }) => (
            <Space key={key} className="free-param-item" align="baseline">
              <Form.Item
                {...restField}
                name={[fieldName, 'key']}
                rules={[{ required: true, message: '请输入参数名' }]}
              >
                <Input placeholder="参数名" style={{ width: 120 }} />
              </Form.Item>
              <Form.Item {...restField} name={[fieldName, 'type']}>
                <Select style={{ width: 100 }} defaultValue="String">
                  <Option value="String">String</Option>
                  <Option value="Integer">Integer</Option>
                  <Option value="Long">Long</Option>
                  <Option value="Double">Double</Option>
                  <Option value="Boolean">Boolean</Option>
                  <Option value="Object">Object</Option>
                </Select>
              </Form.Item>
              <Form.Item
                {...restField}
                name={[fieldName, 'value']}
                rules={[{ required: true, message: '请输入参数值' }]}
              >
                <Input placeholder="参数值" style={{ width: 200 }} />
              </Form.Item>
              {!readonly && (
                <MinusCircleOutlined onClick={() => remove(fieldName)} />
              )}
            </Space>
          ))}
          {!readonly && (
            <Button
              type="dashed"
              onClick={() => add({ key: '', type: 'String', value: '' })}
              icon={<PlusOutlined />}
              block
            >
              添加参数
            </Button>
          )}
        </div>
      )}
    </Form.List>
  );

  return (
    <div className="parameter-input">
      <Form
        form={form}
        layout="vertical"
        initialValues={values}
        onValuesChange={handleValuesChange}
      >
        {parameters.length > 0 ? (
          parameters.map((param) => (
            <Form.Item
              key={param.name}
              name={param.name}
              label={
                <span>
                  {param.name}
                  {param.required && <span className="required">*</span>}
                  {param.description && (
                    <Tooltip title={param.description}>
                      <QuestionCircleOutlined className="param-hint" />
                    </Tooltip>
                  )}
                </span>
              }
              rules={
                param.required
                  ? [{ required: true, message: `请输入 ${param.name}` }]
                  : undefined
              }
              valuePropName={param.type === 'Boolean' ? 'checked' : 'value'}
            >
              {renderInput(param)}
            </Form.Item>
          ))
        ) : (
          <div className="free-params-section">
            <div className="section-header">
              <span>执行参数</span>
              <span className="section-hint">无预定义参数，请自定义输入</span>
            </div>
            {renderFreeParams()}
          </div>
        )}
      </Form>
    </div>
  );
};

export default ParameterInput;
