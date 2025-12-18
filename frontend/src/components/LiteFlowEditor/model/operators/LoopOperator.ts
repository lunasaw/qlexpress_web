import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';
import { LITEFLOW_EDGE, NODE_TYPE_INTERMEDIATE_END } from '../../constant';

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
    const loopStartId = `${this.id}_loop_start`;
    const loopEndId = `${this.id}_loop_end`;

    // 循环开始节点
    cells.push({
      id: loopStartId,
      shape: 'for-node',
      data: {
        model: this,
        type: this.type,
        label: `循环 (FOR ${this.properties?.loopCount || '?'})`,
        properties: this.properties,
        hasBody: !!this.body,
        toolbar: {
          prepend: true,
          append: false,
          delete: true,
          replace: true,
        },
      },
    });

    if (this.body) {
      cells.push(...this.body.toCells());
      // 循环开始 -> 循环体
      const bodyEntryId = this.body.getEntryId();
      cells.push({
        id: `${this.id}_to_body`,
        shape: LITEFLOW_EDGE,
        source: loopStartId,
        target: bodyEntryId,
        labels: [{ attrs: { label: { text: 'DO' } } }],
      });
    }

    // 循环结束节点
    cells.push({
      id: loopEndId,
      shape: NODE_TYPE_INTERMEDIATE_END,
      data: {
        model: this,
        toolbar: {
          prepend: false,
          append: true,
          delete: false,
          replace: false,
        },
      },
      attrs: {
        label: { text: '' },
      },
    });

    // 循环体 -> 循环结束
    if (this.body) {
      const bodyExitId = this.body.getExitId();
      cells.push({
        id: `${this.id}_body_to_end`,
        shape: LITEFLOW_EDGE,
        source: bodyExitId,
        target: loopEndId,
      });
    }

    return cells;
  }

  /**
   * 获取 FOR 的入口点
   */
  override getEntryId(): string {
    return `${this.id}_loop_start`;
  }

  /**
   * 获取 FOR 的出口点
   */
  override getExitId(): string {
    return `${this.id}_loop_end`;
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
    const loopEndId = `${this.id}_loop_end`;

    // 条件节点
    if (this.condition) {
      cells.push(...this.condition.toCells());
    }

    // 循环体
    if (this.body) {
      cells.push(...this.body.toCells());
      // 条件 -> 循环体
      if (this.condition) {
        const bodyEntryId = this.body.getEntryId();
        cells.push({
          id: `${this.id}_to_body`,
          shape: LITEFLOW_EDGE,
          source: this.condition.getExitId(),
          target: bodyEntryId,
          labels: [{ attrs: { label: { text: 'Y' } } }],
        });
      }
    }

    // 循环结束节点
    cells.push({
      id: loopEndId,
      shape: NODE_TYPE_INTERMEDIATE_END,
      data: {
        model: this,
        toolbar: {
          prepend: false,
          append: true,
          delete: false,
          replace: false,
        },
      },
      attrs: {
        label: { text: '' },
      },
    });

    // 循环体 -> 条件 (回环)
    // 条件 -> 结束 (退出循环)
    if (this.condition) {
      cells.push({
        id: `${this.id}_to_end`,
        shape: LITEFLOW_EDGE,
        source: this.condition.getExitId(),
        target: loopEndId,
        labels: [{ attrs: { label: { text: 'N' } } }],
      });
    }

    if (this.body) {
      const bodyExitId = this.body.getExitId();
      cells.push({
        id: `${this.id}_body_to_end`,
        shape: LITEFLOW_EDGE,
        source: bodyExitId,
        target: loopEndId,
      });
    }

    return cells;
  }

  /**
   * 获取 WHILE 的入口点 - 条件节点的入口
   */
  override getEntryId(): string {
    if (this.condition) {
      return this.condition.getEntryId();
    }
    return this.id;
  }

  /**
   * 获取 WHILE 的出口点
   */
  override getExitId(): string {
    return `${this.id}_loop_end`;
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
    const loopEndId = `${this.id}_loop_end`;

    // 迭代器节点
    if (this.condition) {
      cells.push(...this.condition.toCells());
    }

    // 循环体
    if (this.body) {
      cells.push(...this.body.toCells());
      // 迭代器 -> 循环体
      if (this.condition) {
        const bodyEntryId = this.body.getEntryId();
        cells.push({
          id: `${this.id}_to_body`,
          shape: LITEFLOW_EDGE,
          source: this.condition.getExitId(),
          target: bodyEntryId,
          labels: [{ attrs: { label: { text: 'DO' } } }],
        });
      }
    }

    // 循环结束节点
    cells.push({
      id: loopEndId,
      shape: NODE_TYPE_INTERMEDIATE_END,
      data: {
        model: this,
        toolbar: {
          prepend: false,
          append: true,
          delete: false,
          replace: false,
        },
      },
      attrs: {
        label: { text: '' },
      },
    });

    // 循环体 -> 结束
    if (this.body) {
      const bodyExitId = this.body.getExitId();
      cells.push({
        id: `${this.id}_body_to_end`,
        shape: LITEFLOW_EDGE,
        source: bodyExitId,
        target: loopEndId,
      });
    }

    return cells;
  }

  /**
   * 获取 ITERATOR 的入口点 - 迭代器节点的入口
   */
  override getEntryId(): string {
    if (this.condition) {
      return this.condition.getEntryId();
    }
    return this.id;
  }

  /**
   * 获取 ITERATOR 的出口点
   */
  override getExitId(): string {
    return `${this.id}_loop_end`;
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

