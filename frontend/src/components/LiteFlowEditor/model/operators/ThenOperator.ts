import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';

/**
 * THEN 操作符 - 串行执行
 * 对应 LiteFlow EL: THEN(a, b, c)
 */
export class ThenOperator extends ELNode {
  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.THEN);
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    // 生成容器节点
    cells.push({
      id: this.id,
      shape: 'then-node',
      data: {
        type: this.type,
        label: '串行 (THEN)',
        properties: this.properties,
        childCount: this.children.length,
      },
    });

    // 生成子节点
    this.children.forEach((child) => {
      cells.push(...child.toCells());
    });

    // 生成连线
    for (let i = 0; i < this.children.length; i++) {
      const child = this.children[i];

      // 从容器到第一个子节点
      if (i === 0) {
        cells.push({
          id: `${this.id}_to_${child.id}`,
          shape: 'edge',
          source: { cell: this.id, port: 'out' },
          target: { cell: child.id, port: 'in' },
        });
      }

      // 子节点之间的连线
      if (i < this.children.length - 1) {
        const nextChild = this.children[i + 1];
        cells.push({
          id: `${child.id}_to_${nextChild.id}`,
          shape: 'edge',
          source: { cell: child.id, port: 'out' },
          target: { cell: nextChild.id, port: 'in' },
        });
      }
    }

    return cells;
  }

  toEL(prefix?: string): string {
    if (this.children.length === 0) {
      return '';
    }

    const childrenEL = this.children.map((child) => child.toEL(prefix)).filter(Boolean);
    const modifiers = this.propertiesToEL();

    if (prefix !== undefined) {
      const indent = prefix + '  ';
      return `${prefix}THEN(\n${childrenEL.map((el) => `${indent}${el}`).join(',\n')}\n${prefix})${modifiers}`;
    }

    return `THEN(${childrenEL.join(', ')})${modifiers}`;
  }

  clone(): ThenOperator {
    const cloned = new ThenOperator();
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
 * 创建 THEN 操作符的工厂方法
 */
export function createThenOperator(parent?: ELNode, children?: ELNode[]): ThenOperator {
  const operator = new ThenOperator(parent);
  if (children) {
    children.forEach((child) => operator.appendChild(child));
  }
  return operator;
}

export default ThenOperator;
