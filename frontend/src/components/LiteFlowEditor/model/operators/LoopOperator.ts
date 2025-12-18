import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';

/**
 * FOR 操作符 - 次数循环
 * 对应 LiteFlow EL: FOR(5).DO(body).BREAK(breakCondition)
 */
export class ForOperator extends ELNode {
  /** 循环体 */
  get body(): ELNode | undefined {
    return this.children[0];
  }

  set body(node: ELNode | undefined) {
    if (node) {
      node.parent = this;
      this.children[0] = node;
    }
  }

  /** 中断条件 */
  get breakCondition(): ELNode | undefined {
    return this.properties?.breakCondition
      ? (this as any)._breakConditionNode
      : undefined;
  }

  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.FOR);
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    cells.push({
      id: this.id,
      shape: 'for-node',
      data: {
        type: this.type,
        label: `循环 (FOR ${this.properties?.loopCount || '?'})`,
        properties: this.properties,
        hasBody: !!this.body,
      },
    });

    if (this.body) {
      cells.push(...this.body.toCells());
      cells.push({
        id: `${this.id}_body`,
        shape: 'edge',
        source: { cell: this.id, port: 'out' },
        target: { cell: this.body.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'DO' } } }],
      });
    }

    return cells;
  }

  toEL(prefix?: string): string {
    const loopCount = this.properties?.loopCount;
    if (!loopCount) {
      throw new Error('FOR 节点必须指定循环次数');
    }
    if (!this.body) {
      throw new Error('FOR 节点必须有循环体');
    }

    const bodyEL = this.body.toEL(prefix);
    let result = `FOR(${loopCount}).DO(${bodyEL})`;

    // 中断条件
    if (this.properties?.breakCondition && (this as any)._breakConditionNode) {
      const breakEL = (this as any)._breakConditionNode.toEL();
      result += `.BREAK(${breakEL})`;
    }

    result += this.propertiesToEL(['loopCount', 'breakCondition']);

    return result;
  }

  clone(): ForOperator {
    const cloned = new ForOperator();
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
 * WHILE 操作符 - 条件循环
 * 对应 LiteFlow EL: WHILE(condition).DO(body).BREAK(breakCondition)
 */
export class WhileOperator extends ELNode {
  /** 循环体 */
  get body(): ELNode | undefined {
    return this.children[0];
  }

  set body(node: ELNode | undefined) {
    if (node) {
      node.parent = this;
      this.children[0] = node;
    }
  }

  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.WHILE);
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    cells.push({
      id: this.id,
      shape: 'while-node',
      data: {
        type: this.type,
        label: '条件循环 (WHILE)',
        properties: this.properties,
        hasCondition: !!this.condition,
        hasBody: !!this.body,
      },
    });

    if (this.condition) {
      cells.push(...this.condition.toCells());
      cells.push({
        id: `${this.id}_condition`,
        shape: 'edge',
        source: { cell: this.id, port: 'condition' },
        target: { cell: this.condition.id, port: 'in' },
        labels: [{ attrs: { label: { text: '?' } } }],
      });
    }

    if (this.body) {
      cells.push(...this.body.toCells());
      cells.push({
        id: `${this.id}_body`,
        shape: 'edge',
        source: { cell: this.id, port: 'out' },
        target: { cell: this.body.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'DO' } } }],
      });
    }

    return cells;
  }

  toEL(prefix?: string): string {
    if (!this.condition) {
      throw new Error('WHILE 节点必须有条件');
    }
    if (!this.body) {
      throw new Error('WHILE 节点必须有循环体');
    }

    const conditionEL = this.condition.toEL();
    const bodyEL = this.body.toEL(prefix);
    let result = `WHILE(${conditionEL}).DO(${bodyEL})`;

    // 中断条件
    if (this.properties?.breakCondition && (this as any)._breakConditionNode) {
      const breakEL = (this as any)._breakConditionNode.toEL();
      result += `.BREAK(${breakEL})`;
    }

    result += this.propertiesToEL(['breakCondition']);

    return result;
  }

  clone(): WhileOperator {
    const cloned = new WhileOperator();
    cloned.properties = this.properties ? { ...this.properties } : undefined;

    if (this.condition) {
      cloned.condition = this.condition.clone();
      cloned.condition.parent = cloned;
    }

    cloned.children = this.children.map((child) => {
      const childClone = child.clone();
      childClone.parent = cloned;
      return childClone;
    });

    return cloned;
  }
}

/**
 * ITERATOR 操作符 - 迭代循环
 * 对应 LiteFlow EL: ITERATOR(iterator).DO(body).BREAK(breakCondition)
 */
export class IteratorOperator extends ELNode {
  /** 循环体 */
  get body(): ELNode | undefined {
    return this.children[0];
  }

  set body(node: ELNode | undefined) {
    if (node) {
      node.parent = this;
      this.children[0] = node;
    }
  }

  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.ITERATOR);
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    cells.push({
      id: this.id,
      shape: 'iterator-node',
      data: {
        type: this.type,
        label: '迭代循环 (ITERATOR)',
        properties: this.properties,
        hasCondition: !!this.condition,
        hasBody: !!this.body,
      },
    });

    if (this.condition) {
      cells.push(...this.condition.toCells());
      cells.push({
        id: `${this.id}_iterator`,
        shape: 'edge',
        source: { cell: this.id, port: 'condition' },
        target: { cell: this.condition.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'ITER' } } }],
      });
    }

    if (this.body) {
      cells.push(...this.body.toCells());
      cells.push({
        id: `${this.id}_body`,
        shape: 'edge',
        source: { cell: this.id, port: 'out' },
        target: { cell: this.body.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'DO' } } }],
      });
    }

    return cells;
  }

  toEL(prefix?: string): string {
    if (!this.condition) {
      throw new Error('ITERATOR 节点必须有迭代器');
    }
    if (!this.body) {
      throw new Error('ITERATOR 节点必须有循环体');
    }

    const iteratorEL = this.condition.toEL();
    const bodyEL = this.body.toEL(prefix);
    let result = `ITERATOR(${iteratorEL}).DO(${bodyEL})`;

    // 中断条件
    if (this.properties?.breakCondition && (this as any)._breakConditionNode) {
      const breakEL = (this as any)._breakConditionNode.toEL();
      result += `.BREAK(${breakEL})`;
    }

    return result;
  }

  clone(): IteratorOperator {
    const cloned = new IteratorOperator();
    cloned.properties = this.properties ? { ...this.properties } : undefined;

    if (this.condition) {
      cloned.condition = this.condition.clone();
      cloned.condition.parent = cloned;
    }

    cloned.children = this.children.map((child) => {
      const childClone = child.clone();
      childClone.parent = cloned;
      return childClone;
    });

    return cloned;
  }
}

// 工厂方法
export function createForOperator(
  parent?: ELNode,
  loopCount?: number,
  body?: ELNode
): ForOperator {
  const operator = new ForOperator(parent);
  if (loopCount !== undefined) {
    operator.properties = { ...operator.properties, loopCount };
  }
  if (body) {
    operator.body = body;
  }
  return operator;
}

export function createWhileOperator(
  parent?: ELNode,
  condition?: ELNode,
  body?: ELNode
): WhileOperator {
  const operator = new WhileOperator(parent);
  if (condition) {
    operator.condition = condition;
    condition.parent = operator;
  }
  if (body) {
    operator.body = body;
  }
  return operator;
}

export function createIteratorOperator(
  parent?: ELNode,
  iterator?: ELNode,
  body?: ELNode
): IteratorOperator {
  const operator = new IteratorOperator(parent);
  if (iterator) {
    operator.condition = iterator;
    iterator.parent = operator;
  }
  if (body) {
    operator.body = body;
  }
  return operator;
}

export { ForOperator, WhileOperator, IteratorOperator };
