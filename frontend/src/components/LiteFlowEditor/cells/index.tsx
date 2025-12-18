import { Graph, Node } from '@antv/x6';
import { register } from '@antv/x6-react-shape';
import {
  NODE_HEIGHT,
  NODE_WIDTH,
  NODE_TYPE_INTERMEDIATE_END,
  LITEFLOW_EDGE,
  LINE_COLOR,
  ConditionTypeEnum,
  NodeTypeEnum,
} from '../constant';

// AntV X6 自定义节点配置
import Start from './nodes/start';
import End from './nodes/end';
import Then from './nodes/then';
import When from './nodes/when';
import Common from './nodes/common';
import IntermediateEnd from './nodes/intermediate-end';
import Switch from './nodes/switch';
import If from './nodes/if';
import For from './nodes/for';
import While from './nodes/while';
import Iterator from './nodes/iterator';
import Catch from './nodes/catch';
import And from './nodes/and';
import Or from './nodes/or';
import Not from './nodes/not';
import Chain from './nodes/chain';
import Virtual from './nodes/virtual';

// AntV X6 自定义节点的视图：使用 React 组件
import { NodeBadge, NodeToolBar, NodeView } from '../components';

/**
 * 注册所有自定义节点到 AntV X6
 */
export function registerNodes() {
  const nodes: LiteFlowNode[] = [
    Start,
    End,
    Then,
    When,
    Common,
    IntermediateEnd,
    If,
    Switch,
    For,
    While,
    Iterator,
    Catch,
    And,
    Or,
    Not,
    Virtual,
    Chain,
  ];

  nodes.forEach((cell: LiteFlowNode) => {
    const { type, label, icon, node = {} } = cell;
    register({
      shape: type,
      inherit: 'react-shape',
      component: ({ node }: { node: Node }) => (
        <NodeView node={node} icon={icon}>
          <NodeBadge node={node} />
          <NodeToolBar node={node} />
        </NodeView>
      ),
      width: NODE_WIDTH,
      height: NODE_HEIGHT,
      attrs: {
        label: {
          refX: 0.5,
          refY: '100%',
          refY2: 20,
          text: label,
          fill: '#333',
          fontSize: 13,
          textAnchor: 'middle',
          textVerticalAnchor: 'middle',
          textWrap: {
            width: 80,
            height: 60,
            ellipsis: true,
            breakWord: true,
          },
        },
      },
      ...node,
    });
  });

  // 注册边样式
  Graph.registerEdge(
    LITEFLOW_EDGE,
    {
      inherit: 'edge',
      attrs: {
        line: {
          stroke: LINE_COLOR,
          strokeWidth: 1,
          targetMarker: null,
        },
      },
      router: {
        name: 'manhattan',
        args: {
          padding: 10,
        },
      },
      connector: {
        name: 'rounded',
        args: {
          radius: 4,
        },
      },
    },
    true
  );

  // 兼容旧的 flow-edge
  Graph.registerEdge(
    'flow-edge',
    {
      inherit: LITEFLOW_EDGE,
    },
    true
  );

  console.log('LiteFlow 节点注册完成');
}

// 导出节点配置
export {
  Start,
  End,
  Common,
  Then,
  When,
  IntermediateEnd,
  Switch,
  If,
  For,
  While,
  Iterator,
  Catch,
  And,
  Or,
  Not,
  Virtual,
  Chain,
};

// 节点分组定义
export interface IGroupItem {
  key: string;
  name: string;
  cellTypes: LiteFlowNode[];
}

export const NODE_GROUP: IGroupItem = {
  key: 'node',
  name: '节点类',
  cellTypes: [{ ...Common, type: NodeTypeEnum.COMMON, shape: Common.type }],
};

export const SEQUENCE_GROUP: IGroupItem = {
  key: 'sequence',
  name: '顺序类',
  cellTypes: [
    { ...Then, type: ConditionTypeEnum.THEN, shape: Then.type },
    { ...When, type: ConditionTypeEnum.WHEN, shape: When.type },
  ],
};

export const BRANCH_GROUP: IGroupItem = {
  key: 'branch',
  name: '分支类',
  cellTypes: [
    { ...Switch, type: ConditionTypeEnum.SWITCH, shape: Switch.type },
    { ...If, type: ConditionTypeEnum.IF, shape: If.type },
  ],
};

export const CONTROL_GROUP: IGroupItem = {
  key: 'control',
  name: '循环类',
  cellTypes: [
    { ...For, type: ConditionTypeEnum.FOR, shape: For.type },
    { ...While, type: ConditionTypeEnum.WHILE, shape: While.type },
    { ...Iterator, type: ConditionTypeEnum.ITERATOR, shape: Iterator.type },
  ],
};

export const OTHER_GROUP: IGroupItem = {
  key: 'other',
  name: '其他类',
  cellTypes: [
    { ...Catch, type: ConditionTypeEnum.CATCH, shape: Catch.type },
    { ...And, type: ConditionTypeEnum.AND, shape: And.type },
    { ...Or, type: ConditionTypeEnum.OR, shape: Or.type },
    { ...Not, type: ConditionTypeEnum.NOT, shape: Not.type },
    { ...Chain, type: ConditionTypeEnum.CHAIN, shape: Chain.type },
  ],
};

/**
 * 根据类型获取图标
 */
export const getIconByType = (
  nodeType: ConditionTypeEnum | NodeTypeEnum | string
): string => {
  switch (nodeType) {
    case ConditionTypeEnum.THEN:
      return Then.icon;
    case ConditionTypeEnum.WHEN:
      return When.icon;
    case ConditionTypeEnum.SWITCH:
    case NodeTypeEnum.SWITCH:
      return Switch.icon;
    case ConditionTypeEnum.IF:
    case NodeTypeEnum.IF:
      return If.icon;
    case ConditionTypeEnum.FOR:
    case NodeTypeEnum.FOR:
      return For.icon;
    case ConditionTypeEnum.WHILE:
    case NodeTypeEnum.WHILE:
      return While.icon;
    case ConditionTypeEnum.ITERATOR:
    case NodeTypeEnum.ITERATOR:
      return Iterator.icon;
    case ConditionTypeEnum.CHAIN:
      return Chain.icon;
    case ConditionTypeEnum.CATCH:
      return Catch.icon;
    case ConditionTypeEnum.AND:
      return And.icon;
    case ConditionTypeEnum.OR:
      return Or.icon;
    case ConditionTypeEnum.NOT:
      return Not.icon;
    case NodeTypeEnum.COMMON:
    default:
      return Common.icon;
  }
};

/**
 * 根据节点类型获取对��的形状名称
 */
export function getNodeShapeByType(nodeType: NodeTypeEnum | string): string {
  switch (nodeType) {
    case NodeTypeEnum.BOOLEAN:
    case NodeTypeEnum.BOOLEAN_SCRIPT:
    case NodeTypeEnum.IF:
    case NodeTypeEnum.IF_SCRIPT:
      return NodeTypeEnum.IF;
    case NodeTypeEnum.SWITCH:
    case NodeTypeEnum.SWITCH_SCRIPT:
      return NodeTypeEnum.SWITCH;
    case NodeTypeEnum.FOR:
    case NodeTypeEnum.FOR_SCRIPT:
      return NodeTypeEnum.FOR;
    case NodeTypeEnum.WHILE:
    case NodeTypeEnum.WHILE_SCRIPT:
      return NodeTypeEnum.WHILE;
    case NodeTypeEnum.ITERATOR:
      return NodeTypeEnum.ITERATOR;
    case NodeTypeEnum.BREAK:
    case NodeTypeEnum.BREAK_SCRIPT:
      return NODE_TYPE_INTERMEDIATE_END;
    case NodeTypeEnum.VIRTUAL:
      return NodeTypeEnum.VIRTUAL;
    case NodeTypeEnum.COMMON:
    case NodeTypeEnum.FALLBACK:
    case NodeTypeEnum.SCRIPT:
    default:
      return NodeTypeEnum.COMMON;
  }
}

export default registerNodes;
