import React, { useEffect, useState } from 'react';
import {
  Form,
  Input,
  Select,
  Button,
  Space,
  Divider,
  Switch,
  message,
  Tabs,
} from 'antd';
import {
  SaveOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  MinusCircleOutlined,
} from '@ant-design/icons';
import Editor from '@monaco-editor/react';
import { ComponentType } from '../../types/enums';
import type { QLComponent, ComponentRequest } from '../../types/component';
import { useComponentStore } from '../../stores';
import './ComponentForm.css';

const { Option } = Select;
const { TextArea } = Input;

interface ComponentFormProps {
  /** 编辑的组件 (为空则为新建模式) */
  component?: QLComponent | null;
  /** 保存回调 */
  onSave?: (component: QLComponent) => void;
  /** 取消回调 */
  onCancel?: () => void;
  /** 测试回调 */
  onTest?: (component: QLComponent) => void;
}

/**
 * 默认脚本模板
 */
const scriptTemplates: Record<ComponentType, string> = {
  [ComponentType.SCRIPT]: `// 普通脚本组件
// 从 flowContext 获取输入参数
// input1 = flowContext.get("input1");

// 执行业务逻辑
result = "处理结果";

// 设置输出到 flowContext
flowContext.put("output1", result);
`,
  [ComponentType.BOOLEAN]: `// 布尔判断组件
// 用于 IF/WHILE 条件判断
// 从 flowContext 获取参数
// value = flowContext.get("checkValue");

// 返回布尔值
return true;
`,
  [ComponentType.SWITCH]: `// 路由选择组件
// 用于 SWITCH 多路分支
// 从 flowContext 获取参数
// routeKey = flowContext.get("routeKey");

// 返回分支标识
return "branch1";
`,
};

/**
 * 组件表单
 */
const ComponentForm: React.FC<ComponentFormProps> = ({
  component,
  onSave,
  onCancel,
  onTest,
}) => {
  const [form] = Form.useForm();
  const [scriptContent, setScriptContent] = useState('');
  const [componentType, setComponentType] = useState<ComponentType>(ComponentType.SCRIPT);
  const [saving, setSaving] = useState(false);

  const { createComponent, updateComponent, categories } = useComponentStore();

  const isEdit = !!component;

  // 初始化表单
  useEffect(() => {
    if (component) {
      form.setFieldsValue({
        componentId: component.componentId,
        componentName: component.componentName,
        description: component.description,
        category: component.category,
        componentType: component.componentType,
        enabled: component.enabled ?? true,
        inputs: component.inputs || [],
        outputs: component.outputs || [],
        switchBranches: component.switchBranches || [],
        defaultBranch: component.defaultBranch,
      });
      setScriptContent(component.script || '');
      setComponentType(component.componentType);
    } else {
      form.resetFields();
      setScriptContent(scriptTemplates[ComponentType.SCRIPT]);
      setComponentType(ComponentType.SCRIPT);
    }
  }, [component, form]);

  /**
   * 组件类型变化
   */
  const handleTypeChange = (type: ComponentType) => {
    setComponentType(type);
    if (!isEdit && !scriptContent.trim()) {
      setScriptContent(scriptTemplates[type]);
    }
  };

  /**
   * 保存组件
   */
  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);

      const request: ComponentRequest = {
        ...values,
        script: scriptContent,
      };

      let savedComponent: QLComponent;

      if (isEdit && component) {
        savedComponent = await updateComponent(component.componentId, request);
        message.success('更新成功');
      } else {
        savedComponent = await createComponent(request);
        message.success('创建成功');
      }

      onSave?.(savedComponent);
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message);
      }
    } finally {
      setSaving(false);
    }
  };

  /**
   * 测试组件
   */
  const handleTest = () => {
    form.validateFields().then((values) => {
      const testComponent: QLComponent = {
        ...values,
        script: scriptContent,
      };
      onTest?.(testComponent);
    });
  };

  /**
   * 渲染参数列表
   */
  const renderParameterList = (name: string, label: string) => (
    <Form.List name={name}>
      {(fields, { add, remove }) => (
        <div className="parameter-list">
          <div className="parameter-list-header">
            <span>{label}</span>
            <Button
              type="link"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => add({ name: '', type: 'String', required: false })}
            >
              添加
            </Button>
          </div>
          {fields.map(({ key, name: fieldName, ...restField }) => (
            <Space key={key} className="parameter-item" align="baseline">
              <Form.Item
                {...restField}
                name={[fieldName, 'name']}
                rules={[{ required: true, message: '参数名必填' }]}
              >
                <Input placeholder="参数名" style={{ width: 120 }} />
              </Form.Item>
              <Form.Item {...restField} name={[fieldName, 'type']}>
                <Select style={{ width: 100 }}>
                  <Option value="String">String</Option>
                  <Option value="Integer">Integer</Option>
                  <Option value="Long">Long</Option>
                  <Option value="Double">Double</Option>
                  <Option value="Boolean">Boolean</Option>
                  <Option value="List">List</Option>
                  <Option value="Map">Map</Option>
                  <Option value="Object">Object</Option>
                </Select>
              </Form.Item>
              <Form.Item {...restField} name={[fieldName, 'description']}>
                <Input placeholder="描述" style={{ width: 150 }} />
              </Form.Item>
              <Form.Item {...restField} name={[fieldName, 'required']} valuePropName="checked">
                <Switch size="small" checkedChildren="必填" unCheckedChildren="可选" />
              </Form.Item>
              <MinusCircleOutlined onClick={() => remove(fieldName)} />
            </Space>
          ))}
        </div>
      )}
    </Form.List>
  );

  /**
   * 渲染 Switch 分支配置
   */
  const renderSwitchBranches = () => (
    <Form.List name="switchBranches">
      {(fields, { add, remove }) => (
        <div className="parameter-list">
          <div className="parameter-list-header">
            <span>分支配置</span>
            <Button
              type="link"
              size="small"
              icon={<PlusOutlined />}
              onClick={() => add({ branchId: '', branchName: '' })}
            >
              添加分支
            </Button>
          </div>
          {fields.map(({ key, name: fieldName, ...restField }) => (
            <Space key={key} className="parameter-item" align="baseline">
              <Form.Item
                {...restField}
                name={[fieldName, 'branchId']}
                rules={[{ required: true, message: '分支ID必填' }]}
              >
                <Input placeholder="分支ID" style={{ width: 120 }} />
              </Form.Item>
              <Form.Item
                {...restField}
                name={[fieldName, 'branchName']}
                rules={[{ required: true, message: '分支名必填' }]}
              >
                <Input placeholder="分支名称" style={{ width: 150 }} />
              </Form.Item>
              <Form.Item {...restField} name={[fieldName, 'description']}>
                <Input placeholder="描述" style={{ width: 200 }} />
              </Form.Item>
              <MinusCircleOutlined onClick={() => remove(fieldName)} />
            </Space>
          ))}
          {componentType === ComponentType.SWITCH && (
            <Form.Item name="defaultBranch" label="默认分支">
              <Input placeholder="默认分支ID" style={{ width: 200 }} />
            </Form.Item>
          )}
        </div>
      )}
    </Form.List>
  );

  const tabItems = [
    {
      key: 'basic',
      label: '基本信息',
      children: (
        <div className="form-section">
          <Form.Item
            name="componentId"
            label="组件ID"
            rules={[
              { required: true, message: '请输入组件ID' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '只能包含字母、数字、下划线，且以字母开头' },
            ]}
          >
            <Input placeholder="如: checkUserStatus" disabled={isEdit} />
          </Form.Item>

          <Form.Item
            name="componentName"
            label="组件名称"
            rules={[{ required: true, message: '请输入组件名称' }]}
          >
            <Input placeholder="如: 检查用户状态" />
          </Form.Item>

          <Form.Item name="description" label="组件描述">
            <TextArea rows={2} placeholder="请输入组件描述" />
          </Form.Item>

          <Form.Item
            name="componentType"
            label="组件类型"
            rules={[{ required: true, message: '请选择组件类型' }]}
          >
            <Select onChange={handleTypeChange} disabled={isEdit}>
              <Option value={ComponentType.SCRIPT}>脚本组件 (SCRIPT)</Option>
              <Option value={ComponentType.BOOLEAN}>布尔组件 (BOOLEAN)</Option>
              <Option value={ComponentType.SWITCH}>选择组件 (SWITCH)</Option>
            </Select>
          </Form.Item>

          <Form.Item name="category" label="分类">
            <Select
              placeholder="选择或输入分类"
              allowClear
              showSearch
              optionFilterProp="children"
            >
              {categories.map((cat) => (
                <Option key={cat} value={cat}>
                  {cat}
                </Option>
              ))}
              {/* 预设一些常用分类 */}
              <Option value="通用">通用</Option>
              <Option value="业务逻辑">业务逻辑</Option>
              <Option value="数据处理">数据处理</Option>
              <Option value="条件判断">条件判断</Option>
              <Option value="外部调用">外部调用</Option>
            </Select>
          </Form.Item>

          <Form.Item name="enabled" label="启用状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </div>
      ),
    },
    {
      key: 'script',
      label: '脚本编辑',
      children: (
        <div className="script-editor-container">
          <Editor
            height="400px"
            language="java"
            theme="vs"
            value={scriptContent}
            onChange={(value) => setScriptContent(value || '')}
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
        </div>
      ),
    },
    {
      key: 'params',
      label: '参数定义',
      children: (
        <div className="form-section">
          {renderParameterList('inputs', '输入参数')}
          <Divider />
          {renderParameterList('outputs', '输出参数')}
          {componentType === ComponentType.SWITCH && (
            <>
              <Divider />
              {renderSwitchBranches()}
            </>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="component-form">
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          componentType: ComponentType.SCRIPT,
          enabled: true,
          inputs: [],
          outputs: [],
          switchBranches: [],
        }}
      >
        <Tabs items={tabItems} />

        <Divider />

        <div className="form-actions">
          <Space>
            <Button onClick={onCancel}>取消</Button>
            <Button icon={<PlayCircleOutlined />} onClick={handleTest}>
              测试
            </Button>
            <Button
              type="primary"
              icon={<SaveOutlined />}
              loading={saving}
              onClick={handleSave}
            >
              {isEdit ? '保存' : '创建'}
            </Button>
          </Space>
        </div>
      </Form>
    </div>
  );
};

export default ComponentForm;
