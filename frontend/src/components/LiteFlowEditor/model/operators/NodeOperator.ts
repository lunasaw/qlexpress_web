import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { NodeTypeEnum, ComponentType, COMPONENT_TYPE_TO_NODE_TYPE } from '../../../../types/enums';
import type { QLComponent } from '../../../../types/component';

/**
 * 叶子节点 - 表示一个具体的组件节点
 */
export class NodeOperator extends ELNode {
  constructor(parent?: ELNode, componentRef?: string, componentData?: QLComponent) {
    // 根据组件类型确定节点类型
    let nodeType: NodeTypeEnum = NodeTypeEnum.SCRIPT;
    if (componentData?.componentType) {
      nodeType = COMPONENT_TYPE_TO_NODE_TYPE[componentData.componentType] || NodeTypeEnum.SCRIPT;
    }

    super(parent, nodeType);
    this.componentRef = componentRef;
    this.componentData = componentData;

    // 如果有 componentRef，使用它作为 ID
    if (componentRef) {
      this.id = componentRef;
    }
  }

  /**
   * 获取节点显示名称
   */
  get displayName(): string {
    return this.componentData?.componentName || this.componentRef || this.id;
  }

  /**
   * 获取节点类型描述
   */
  get typeLabel(): string {
    switch (this.componentData?.componentType) {
      case ComponentType.BOOLEAN:
        return '条件';
      case ComponentType.SWITCH:
        return '路由';
      case ComponentType.SCRIPT:
      default:
        return '脚本';
    }
  }

  /**
   * 获取节点颜色
   */
  get color(): string {
    switch (this.componentData?.componentType) {
      case ComponentType.BOOLEAN:
        return '#52c41a';
      case ComponentType.SWITCH:
        return '#722ed1';
      case ComponentType.SCRIPT:
      default:
        return '#1890ff';
    }
  }

  toCells(): Cell.Metadata[] {
    return [
      {
        id: this.id,
        shape: 'common-node',
        data: {
          type: this.type,
          componentRef: this.componentRef,
          componentData: this.componentData,
          label: this.displayName,
          color: this.color,
          properties: this.properties,
        },
      },
    ];
  }

  toEL(_prefix?: string): string {
    const nodeId = this.componentRef || this.id;
    const modifiers = this.propertiesToEL();
    return `${nodeId}${modifiers}`;
  }

  clone(): NodeOperator {
    const cloned = new NodeOperator(undefined, this.componentRef, this.componentData);
    cloned.properties = this.properties ? { ...this.properties } : undefined;
    return cloned;
  }
}

/**
 * 创建叶子节点的工厂方法
 */
export function createNodeOperator(
  componentRef: string,
  componentData?: QLComponent,
  parent?: ELNode
): NodeOperator {
  return new NodeOperator(parent, componentRef, componentData);
}

export default NodeOperator;
