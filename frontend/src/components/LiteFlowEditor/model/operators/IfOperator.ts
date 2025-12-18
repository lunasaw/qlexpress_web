import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';
import { LITEFLOW_EDGE, NODE_TYPE_INTERMEDIATE_END } from '../../constant';

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
    const endId = `${this.id}_end`;

    // 生成条件节点
    if (this.condition) {
      cells.push(...this.condition.toCells());
    }

    // 生成 true 分支
    if (this.trueBranch) {
      cells.push(...this.trueBranch.toCells());
      // 条件节点 -> true 分支 (使用入口点)
      if (this.condition) {
        const targetId = this.trueBranch.getEntryId();
        cells.push({
          id: `${this.id}_to_true`,
          shape: LITEFLOW_EDGE,
          source: this.condition.getExitId(),
          target: targetId,
          labels: [{ attrs: { label: { text: 'Y' } } }],
        });
      }
    }

    // 生成 false 分支
    if (this.falseBranch) {
      cells.push(...this.falseBranch.toCells());
      // 条件节点 -> false 分支 (使用入口点)
      if (this.condition) {
        const targetId = this.falseBranch.getEntryId();
        cells.push({
          id: `${this.id}_to_false`,
          shape: LITEFLOW_EDGE,
          source: this.condition.getExitId(),
          target: targetId,
          labels: [{ attrs: { label: { text: 'N' } } }],
        });
      }
    }

    // 生成结束汇聚节点
    cells.push({
      id: endId,
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

    // true 分支 -> 结束节点 (使用出口点)
    if (this.trueBranch) {
      const sourceId = this.trueBranch.getExitId();
      cells.push({
        id: `${this.id}_true_to_end`,
        shape: LITEFLOW_EDGE,
        source: sourceId,
        target: endId,
      });
    }

    // false 分支 -> 结束节点 (使用出口点)
    if (this.falseBranch) {
      const sourceId = this.falseBranch.getExitId();
      cells.push({
        id: `${this.id}_false_to_end`,
        shape: LITEFLOW_EDGE,
        source: sourceId,
        target: endId,
      });
    }

    return cells;
  }

  /**
   * 获取 IF 的入口点 - 条件节点的入口
   */
  override getEntryId(): string {
    if (this.condition) {
      return this.condition.getEntryId();
    }
    return this.id;
  }

  /**
   * 获取 IF 的出口点 - 结束汇聚节点
   */
  override getExitId(): string {
    return `${this.id}_end`;
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
