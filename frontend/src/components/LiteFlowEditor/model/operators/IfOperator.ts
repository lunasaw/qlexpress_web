import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';

/**
 * IF 操作符 - 条件分支
 * 对应 LiteFlow EL: IF(condition, trueBranch, falseBranch)
 *
 * children[0]: trueBranch (必须)
 * children[1]: falseBranch (可选)
 * condition: 条件节点 (必须是 BOOLEAN 类型组件)
 */
export class IfOperator extends ELNode {
  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.IF);
  }

  /**
   * 获取 true 分支
   */
  get trueBranch(): ELNode | undefined {
    return this.children[0];
  }

  /**
   * 设置 true 分支
   */
  set trueBranch(node: ELNode | undefined) {
    if (node) {
      node.parent = this;
      this.children[0] = node;
    }
  }

  /**
   * 获取 false 分支
   */
  get falseBranch(): ELNode | undefined {
    return this.children[1];
  }

  /**
   * 设置 false 分支
   */
  set falseBranch(node: ELNode | undefined) {
    if (node) {
      node.parent = this;
      this.children[1] = node;
    }
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    // 生成 IF 节点
    cells.push({
      id: this.id,
      shape: 'if-node',
      data: {
        type: this.type,
        label: '条件 (IF)',
        properties: this.properties,
        hasCondition: !!this.condition,
        hasTrueBranch: !!this.trueBranch,
        hasFalseBranch: !!this.falseBranch,
      },
    });

    // 生成条件节点
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

    // 生成 true 分支
    if (this.trueBranch) {
      cells.push(...this.trueBranch.toCells());
      cells.push({
        id: `${this.id}_true`,
        shape: 'edge',
        source: { cell: this.id, port: 'true' },
        target: { cell: this.trueBranch.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'Y' } } }],
        data: { branch: 'true' },
      });
    }

    // 生成 false 分支
    if (this.falseBranch) {
      cells.push(...this.falseBranch.toCells());
      cells.push({
        id: `${this.id}_false`,
        shape: 'edge',
        source: { cell: this.id, port: 'false' },
        target: { cell: this.falseBranch.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'N' } } }],
        data: { branch: 'false' },
      });
    }

    return cells;
  }

  toEL(prefix?: string): string {
    if (!this.condition) {
      throw new Error('IF 节点必须有条件');
    }
    if (!this.trueBranch) {
      throw new Error('IF 节点必须有 true 分支');
    }

    const conditionEL = this.condition.toEL();
    const trueBranchEL = this.trueBranch.toEL(prefix);
    const falseBranchEL = this.falseBranch ? this.falseBranch.toEL(prefix) : null;
    const modifiers = this.propertiesToEL();

    if (falseBranchEL) {
      return `IF(${conditionEL}, ${trueBranchEL}, ${falseBranchEL})${modifiers}`;
    }

    return `IF(${conditionEL}, ${trueBranchEL})${modifiers}`;
  }

  clone(): IfOperator {
    const cloned = new IfOperator();
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
 * 创建 IF 操作符的工厂方法
 */
export function createIfOperator(
  parent?: ELNode,
  condition?: ELNode,
  trueBranch?: ELNode,
  falseBranch?: ELNode
): IfOperator {
  const operator = new IfOperator(parent);

  if (condition) {
    operator.condition = condition;
    condition.parent = operator;
  }

  if (trueBranch) {
    operator.trueBranch = trueBranch;
  }

  if (falseBranch) {
    operator.falseBranch = falseBranch;
  }

  return operator;
}

export default IfOperator;
