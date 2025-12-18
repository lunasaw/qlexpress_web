import {
  OrderedListOutlined,
  BranchesOutlined,
  SyncOutlined,
  ExclamationCircleOutlined,
  CalculatorOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import { ConditionTypeEnum, ComponentType } from '../../../types/enums';

/**
 * 节点分组定义
 */
export interface NodeGroupItem {
  type: ConditionTypeEnum | string;
  label: string;
  description: string;
  color: string;
  icon?: string;
}

export interface NodeGroup {
  key: string;
  label: string;
  icon: React.ComponentType;
  items?: NodeGroupItem[];
  dynamic?: boolean;
  subCategories?: Array<{
    key: string;
    label: string;
    componentType: ComponentType;
    color: string;
    description: string;
  }>;
}

/**
 * 节点分组配置
 * 参考 liteflow-editor-client 设计，增加 QLExpress 组件分类
 */
export const NODE_GROUPS: Record<string, NodeGroup> = {
  // ===== 串行/并行编排 =====
  SEQUENCE_GROUP: {
    key: 'sequence',
    label: '编排操作符',
    icon: OrderedListOutlined,
    items: [
      {
        type: ConditionTypeEnum.THEN,
        label: '串行 (THEN)',
        description: '顺序执行子节点',
        color: '#1890ff',
      },
      {
        type: ConditionTypeEnum.WHEN,
        label: '并行 (WHEN)',
        description: '并行执行子节点',
        color: '#52c41a',
      },
    ],
  },

  // ===== 条件分支 =====
  BRANCH_GROUP: {
    key: 'branch',
    label: '条件分支',
    icon: BranchesOutlined,
    items: [
      {
        type: ConditionTypeEnum.IF,
        label: '条件 (IF)',
        description: 'IF-ELIF-ELSE 条件分支',
        color: '#faad14',
      },
      {
        type: ConditionTypeEnum.SWITCH,
        label: '选择 (SWITCH)',
        description: '多路分支选择',
        color: '#722ed1',
      },
    ],
  },

  // ===== 循环控制 =====
  LOOP_GROUP: {
    key: 'loop',
    label: '循环控制',
    icon: SyncOutlined,
    items: [
      {
        type: ConditionTypeEnum.FOR,
        label: '次数循环 (FOR)',
        description: '固定次数循环执行',
        color: '#13c2c2',
      },
      {
        type: ConditionTypeEnum.WHILE,
        label: '条件循环 (WHILE)',
        description: '条件满足时循环',
        color: '#13c2c2',
      },
      {
        type: ConditionTypeEnum.ITERATOR,
        label: '迭代循环 (ITERATOR)',
        description: '集合迭代执行',
        color: '#13c2c2',
      },
    ],
  },

  // ===== 异常处理 =====
  EXCEPTION_GROUP: {
    key: 'exception',
    label: '异常处理',
    icon: ExclamationCircleOutlined,
    items: [
      {
        type: ConditionTypeEnum.CATCH,
        label: '异常捕获 (CATCH)',
        description: '捕获异常并处理',
        color: '#ff4d4f',
      },
    ],
  },

  // ===== 逻辑运算 =====
  LOGIC_GROUP: {
    key: 'logic',
    label: '逻辑运算',
    icon: CalculatorOutlined,
    items: [
      {
        type: ConditionTypeEnum.AND,
        label: '与 (AND)',
        description: '所有条件都为真',
        color: '#eb2f96',
      },
      {
        type: ConditionTypeEnum.OR,
        label: '或 (OR)',
        description: '任一条件为真',
        color: '#eb2f96',
      },
      {
        type: ConditionTypeEnum.NOT,
        label: '非 (NOT)',
        description: '条件取反',
        color: '#eb2f96',
      },
    ],
  },

  // ===== 业务组件 (动态加载) =====
  COMPONENT_GROUP: {
    key: 'component',
    label: '业务组件',
    icon: AppstoreOutlined,
    dynamic: true, // 从后端动态加载 QLComponent 列表
    // 按 ComponentType 分类显示
    subCategories: [
      {
        key: 'script',
        label: '脚本组件',
        componentType: ComponentType.SCRIPT,
        color: '#1890ff',
        description: '执行业务逻辑',
      },
      {
        key: 'boolean',
        label: '条件组件',
        componentType: ComponentType.BOOLEAN,
        color: '#52c41a',
        description: 'IF/WHILE 条件判断',
      },
      {
        key: 'switch',
        label: '路由组件',
        componentType: ComponentType.SWITCH,
        color: '#722ed1',
        description: 'SWITCH 多路分支',
      },
    ],
  },
};

/**
 * 获取所有分组的节点列表
 */
export function getAllNodeItems(): NodeGroupItem[] {
  const items: NodeGroupItem[] = [];
  Object.values(NODE_GROUPS).forEach((group) => {
    if (group.items) {
      items.push(...group.items);
    }
  });
  return items;
}

/**
 * 根据类型获取节点配置
 */
export function getNodeItemByType(type: string): NodeGroupItem | undefined {
  for (const group of Object.values(NODE_GROUPS)) {
    if (group.items) {
      const item = group.items.find((i) => i.type === type);
      if (item) return item;
    }
  }
  return undefined;
}

/**
 * 节点样式配置
 */
/**
 * 节点分组数组 (用于侧边栏渲染)
 */
export const nodeGroups = [
  {
    name: '编排操作',
    nodes: NODE_GROUPS.SEQUENCE_GROUP.items || [],
  },
  {
    name: '条件分支',
    nodes: NODE_GROUPS.BRANCH_GROUP.items || [],
  },
  {
    name: '循环控制',
    nodes: NODE_GROUPS.LOOP_GROUP.items || [],
  },
  {
    name: '异常处理',
    nodes: NODE_GROUPS.EXCEPTION_GROUP.items || [],
  },
  {
    name: '逻辑运算',
    nodes: NODE_GROUPS.LOGIC_GROUP.items || [],
  },
];

/**
 * 节点样式配置
 */
export const NODE_STYLES = {
  // 开始/结束节点
  START_END: {
    shape: 'circle',
    width: 60,
    height: 60,
    startColor: '#52c41a',
    endColor: '#ff4d4f',
  },

  // 容器节点 (THEN, WHEN)
  CONTAINER: {
    shape: 'rect',
    minWidth: 200,
    minHeight: 80,
    borderStyle: 'dashed',
  },

  // 条件节点 (IF, SWITCH)
  CONDITION: {
    shape: 'diamond',
    width: 120,
    height: 80,
  },

  // 循环节点 (FOR, WHILE, ITERATOR)
  LOOP: {
    shape: 'hexagon',
    width: 140,
    height: 80,
  },

  // 异常节点 (CATCH)
  EXCEPTION: {
    shape: 'rect',
    width: 160,
    height: 80,
    borderRadius: 8,
  },

  // 逻辑节点 (AND, OR, NOT)
  LOGIC: {
    shape: 'rect',
    width: 100,
    height: 60,
    borderRadius: 20,
  },

  // 业务组件节点
  COMPONENT: {
    shape: 'rect',
    width: 180,
    height: 60,
    borderRadius: 4,
  },
};

export default NODE_GROUPS;
