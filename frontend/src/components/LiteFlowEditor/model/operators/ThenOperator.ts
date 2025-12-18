import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';
import { LITEFLOW_EDGE } from '../../constant';

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

    // THEN 操作符不生成自己的节点，只生成子节点和它们之间的连线
    // 生成子节点
    this.children.forEach((child) => {
      cells.push(...child.toCells());
    });

    // 生成子节点之间的连线
    // 使用 getExitId() 和 getEntryId() 获取正确的连接点
    for (let i = 0; i < this.children.length - 1; i++) {
      const child = this.children[i];
      const nextChild = this.children[i + 1];
      const sourceId = child.getExitId();
      const targetId = nextChild.getEntryId();
      cells.push({
        id: `${sourceId}_to_${targetId}`,
        shape: LITEFLOW_EDGE,
        source: sourceId,
        target: targetId,
      });
    }

    return cells;
  }

  /**
   * 获取 THEN 的入口点 - 第一个子节点的入口
   */
  override getEntryId(): string {
    if (this.children.length > 0) {
      return this.children[0].getEntryId();
    }
    return this.id;
  }

  /**
   * 获取 THEN 的出口点 - 最后一个子节点的出口
   */
  override getExitId(): string {
    if (this.children.length > 0) {
      return this.children[this.children.length - 1].getExitId();
    }
    return this.id;
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
