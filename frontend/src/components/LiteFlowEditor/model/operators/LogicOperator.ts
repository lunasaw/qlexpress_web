import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';

/**
 * AND 操作符 - 逻辑与
 * 对应 LiteFlow EL: AND(a, b, c)
 * 所有子条件都为 true 时返回 true
 */
export class AndOperator extends ELNode {
  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.AND);
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    cells.push({
      id: this.id,
      shape: 'logic-node',
      data: {
        type: this.type,
        label: '与 (AND)',
        properties: this.properties,
        childCount: this.children.length,
        operator: '&&',
      },
    });

    this.children.forEach((child, index) => {
      cells.push(...child.toCells());
      cells.push({
        id: `${this.id}_to_${child.id}`,
        shape: 'edge',
        source: { cell: this.id, port: `out_${index}` },
        target: { cell: child.id, port: 'in' },
      });
    });

    return cells;
  }

  toEL(prefix?: string): string {
    if (this.children.length < 2) {
      throw new Error('AND 节点至少需要两个子节点');
    }

    const childrenEL = this.children.map((child) => child.toEL(prefix));
    return `AND(${childrenEL.join(', ')})`;
  }

  clone(): AndOperator {
    const cloned = new AndOperator();
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
 * OR 操作符 - 逻辑或
 * 对应 LiteFlow EL: OR(a, b, c)
 * 任一子条件为 true 时返回 true
 */
export class OrOperator extends ELNode {
  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.OR);
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    cells.push({
      id: this.id,
      shape: 'logic-node',
      data: {
        type: this.type,
        label: '或 (OR)',
        properties: this.properties,
        childCount: this.children.length,
        operator: '||',
      },
    });

    this.children.forEach((child, index) => {
      cells.push(...child.toCells());
      cells.push({
        id: `${this.id}_to_${child.id}`,
        shape: 'edge',
        source: { cell: this.id, port: `out_${index}` },
        target: { cell: child.id, port: 'in' },
      });
    });

    return cells;
  }

  toEL(prefix?: string): string {
    if (this.children.length < 2) {
      throw new Error('OR 节点至少需要两个子节点');
    }

    const childrenEL = this.children.map((child) => child.toEL(prefix));
    return `OR(${childrenEL.join(', ')})`;
  }

  clone(): OrOperator {
    const cloned = new OrOperator();
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
 * NOT 操作符 - 逻辑非
 * 对应 LiteFlow EL: NOT(a)
 * 对子条件取反
 */
export class NotOperator extends ELNode {
  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.NOT);
  }

  /** 获取被取反的条件 */
  get operand(): ELNode | undefined {
    return this.children[0];
  }

  set operand(node: ELNode | undefined) {
    if (node) {
      node.parent = this;
      this.children[0] = node;
    }
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    cells.push({
      id: this.id,
      shape: 'logic-node',
      data: {
        type: this.type,
        label: '非 (NOT)',
        properties: this.properties,
        operator: '!',
      },
    });

    if (this.operand) {
      cells.push(...this.operand.toCells());
      cells.push({
        id: `${this.id}_to_${this.operand.id}`,
        shape: 'edge',
        source: { cell: this.id, port: 'out' },
        target: { cell: this.operand.id, port: 'in' },
      });
    }

    return cells;
  }

  toEL(prefix?: string): string {
    if (!this.operand) {
      throw new Error('NOT 节点必须有子节点');
    }

    const childEL = this.operand.toEL(prefix);
    return `NOT(${childEL})`;
  }

  clone(): NotOperator {
    const cloned = new NotOperator();
    cloned.properties = this.properties ? { ...this.properties } : undefined;
    cloned.children = this.children.map((child) => {
      const childClone = child.clone();
      childClone.parent = cloned;
      return childClone;
    });
    return cloned;
  }
}

// 工厂方法
export function createAndOperator(parent?: ELNode, children?: ELNode[]): AndOperator {
  const operator = new AndOperator(parent);
  if (children) {
    children.forEach((child) => operator.appendChild(child));
  }
  return operator;
}

export function createOrOperator(parent?: ELNode, children?: ELNode[]): OrOperator {
  const operator = new OrOperator(parent);
  if (children) {
    children.forEach((child) => operator.appendChild(child));
  }
  return operator;
}

export function createNotOperator(parent?: ELNode, operand?: ELNode): NotOperator {
  const operator = new NotOperator(parent);
  if (operand) {
    operator.operand = operand;
  }
  return operator;
}

