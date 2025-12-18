import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';
import { LITEFLOW_EDGE, NODE_TYPE_INTERMEDIATE_END } from '../../constant';

/**
 * SWITCH 操作符 - 多路分支
 * 对应 LiteFlow EL: SWITCH(condition).to(a, b, c).id('A', 'B', 'C').DEFAULT(d)
 *
 * condition: 路由条件节点 (必须是 SWITCH 类型组件)
 * children: 各分支节点
 * 每个子节点的 properties.branch 对应分支标识
 */
export class SwitchOperator extends ELNode {
  /** 默认分支 */
  defaultBranch?: ELNode;

  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.SWITCH);
  }

  /**
   * 获取分支映射 (branchId -> ELNode)
   */
  get branchMap(): Map<string, ELNode> {
    const map = new Map<string, ELNode>();
    this.children.forEach((child) => {
      const branchId = child.properties?.branch || child.componentRef || child.id;
      map.set(branchId, child);
    });
    return map;
  }

  /**
   * 添加分支
   */
  addBranch(branchId: string, node: ELNode): void {
    node.properties = { ...node.properties, branch: branchId };
    this.appendChild(node);
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];
    const endId = `${this.id}_end`;

    // 生成条件节点
    if (this.condition) {
      cells.push(...this.condition.toCells());
    }

    // 生成各分支节点
    this.children.forEach((child, index) => {
      cells.push(...child.toCells());

      // 条件节点 -> 分支 (使用入口点)
      if (this.condition) {
        const branchId = child.properties?.branch || `branch_${index}`;
        const targetId = child.getEntryId();
        cells.push({
          id: `${this.id}_branch_${index}`,
          shape: LITEFLOW_EDGE,
          source: this.condition.getExitId(),
          target: targetId,
          labels: [{ attrs: { label: { text: branchId } } }],
          data: { branch: branchId },
        });
      }
    });

    // 生成默认分支
    if (this.defaultBranch) {
      cells.push(...this.defaultBranch.toCells());
      if (this.condition) {
        const targetId = this.defaultBranch.getEntryId();
        cells.push({
          id: `${this.id}_default`,
          shape: LITEFLOW_EDGE,
          source: this.condition.getExitId(),
          target: targetId,
          labels: [{ attrs: { label: { text: 'DEFAULT' } } }],
          data: { branch: 'default' },
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

    // 各分支 -> 结束节点 (使用出口点)
    this.children.forEach((child) => {
      const sourceId = child.getExitId();
      cells.push({
        id: `${sourceId}_to_${endId}`,
        shape: LITEFLOW_EDGE,
        source: sourceId,
        target: endId,
      });
    });

    // 默认分支 -> 结束节点
    if (this.defaultBranch) {
      const sourceId = this.defaultBranch.getExitId();
      cells.push({
        id: `${this.id}_default_to_end`,
        shape: LITEFLOW_EDGE,
        source: sourceId,
        target: endId,
      });
    }

    return cells;
  }

  /**
   * 获取 SWITCH 的入口点 - 条件节点的入口
   */
  override getEntryId(): string {
    if (this.condition) {
      return this.condition.getEntryId();
    }
    return this.id;
  }

  /**
   * 获取 SWITCH 的出口点 - 结束汇聚节点
   */
  override getExitId(): string {
    return `${this.id}_end`;
  }

  toEL(prefix?: string): string {
    if (!this.condition) {
      throw new Error('SWITCH 节点必须有条件');
    }
    if (this.children.length === 0) {
      throw new Error('SWITCH 节点必须有分支');
    }

    const conditionEL = this.condition.toEL();

    // 生成分支
    const branches: string[] = [];
    const branchIds: string[] = [];

    this.children.forEach((child) => {
      branches.push(child.toEL(prefix));
      const branchId = child.properties?.branch || child.componentRef || child.id;
      branchIds.push(`'${branchId}'`);
    });

    let result = `SWITCH(${conditionEL}).to(${branches.join(', ')})`;

    // 添加分支 ID
    if (branchIds.length > 0) {
      result += `.id(${branchIds.join(', ')})`;
    }

    // 添加默认分支
    if (this.defaultBranch) {
      result += `.DEFAULT(${this.defaultBranch.toEL(prefix)})`;
    }

    // 添加通用修饰符 (排除 branch)
    result += this.propertiesToEL(['branch']);

    return result;
  }

  clone(): SwitchOperator {
    const cloned = new SwitchOperator();
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

    if (this.defaultBranch) {
      cloned.defaultBranch = this.defaultBranch.clone();
      cloned.defaultBranch.parent = cloned;
    }

    return cloned;
  }
}

/**
 * 创建 SWITCH 操作符的工厂方法
 */
export function createSwitchOperator(
  parent?: ELNode,
  condition?: ELNode,
  branches?: Array<{ branchId: string; node: ELNode }>,
  defaultBranch?: ELNode
): SwitchOperator {
  const operator = new SwitchOperator(parent);

  if (condition) {
    operator.condition = condition;
    condition.parent = operator;
  }

  if (branches) {
    branches.forEach(({ branchId, node }) => {
      operator.addBranch(branchId, node);
    });
  }

  if (defaultBranch) {
    operator.defaultBranch = defaultBranch;
    defaultBranch.parent = operator;
  }

  return operator;
}

export default SwitchOperator;
