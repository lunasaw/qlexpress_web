import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum, NodeTypeEnum } from '../../../../types/enums';

/**
 * CHAIN 操作符 - 流程链
 * 作为根节点或子流程引用使用
 */
export class ChainOperator extends ELNode {
  /** 链名称 */
  chainName?: string;

  constructor(parent?: ELNode, chainName?: string) {
    super(parent, ConditionTypeEnum.CHAIN);
    this.chainName = chainName;
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    // 开始节点
    cells.push({
      id: `${this.id}_start`,
      shape: 'start-end-node',
      data: {
        type: NodeTypeEnum.START,
        label: 'START',
        isStart: true,
      },
    });

    // 子节点
    this.children.forEach((child) => {
      cells.push(...child.toCells());
    });

    // 结束节点
    cells.push({
      id: `${this.id}_end`,
      shape: 'start-end-node',
      data: {
        type: NodeTypeEnum.END,
        label: 'END',
        isEnd: true,
      },
    });

    // 连线：START -> 第一个子节点
    if (this.children.length > 0) {
      cells.push({
        id: `${this.id}_start_edge`,
        shape: 'edge',
        source: { cell: `${this.id}_start`, port: 'out' },
        target: { cell: this.children[0].id, port: 'in' },
      });

      // 连线：最后一个子节点 -> END
      const lastChild = this.children[this.children.length - 1];
      cells.push({
        id: `${this.id}_end_edge`,
        shape: 'edge',
        source: { cell: lastChild.id, port: 'out' },
        target: { cell: `${this.id}_end`, port: 'in' },
      });
    } else {
      // 无子节点时直接连接 START -> END
      cells.push({
        id: `${this.id}_direct_edge`,
        shape: 'edge',
        source: { cell: `${this.id}_start`, port: 'out' },
        target: { cell: `${this.id}_end`, port: 'in' },
      });
    }

    return cells;
  }

  toEL(prefix?: string): string {
    if (this.children.length === 0) {
      return '';
    }

    // 如果只有一个子节点，直接返回子节点的 EL
    if (this.children.length === 1) {
      return this.children[0].toEL(prefix);
    }

    // 多个子节点时，用 THEN 包装
    const childrenEL = this.children.map((child) => child.toEL(prefix));
    return `THEN(${childrenEL.join(', ')})`;
  }

  clone(): ChainOperator {
    const cloned = new ChainOperator(undefined, this.chainName);
    cloned.properties = this.properties ? { ...this.properties } : undefined;
    cloned.children = this.children.map((child) => {
      const childClone = child.clone();
      childClone.parent = cloned;
      return childClone;
    });
    return cloned;
  }
}

/**
 * 创建 CHAIN 操作符的工厂方法
 */
export function createChainOperator(
  parent?: ELNode,
  chainName?: string,
  children?: ELNode[]
): ChainOperator {
  const operator = new ChainOperator(parent, chainName);
  if (children) {
    children.forEach((child) => operator.appendChild(child));
  }
  return operator;
}

export default ChainOperator;
