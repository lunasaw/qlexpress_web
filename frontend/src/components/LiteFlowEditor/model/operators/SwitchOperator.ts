import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';

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

    // 生成 SWITCH 节点
    cells.push({
      id: this.id,
      shape: 'switch-node',
      data: {
        type: this.type,
        label: '选择 (SWITCH)',
        properties: this.properties,
        hasCondition: !!this.condition,
        branchCount: this.children.length,
        hasDefault: !!this.defaultBranch,
      },
    });

    // 生成条件节点
    if (this.condition) {
      cells.push(...this.condition.toCells());
      cells.push({
        id: `${this.id}_condition`,
        shape: 'flow-edge',
        source: { cell: this.id, port: 'condition' },
        target: { cell: this.condition.id, port: 'in' },
        labels: [{ attrs: { label: { text: '?' } } }],
      });
    }

    // 生成各分支节点
    this.children.forEach((child, index) => {
      cells.push(...child.toCells());

      const branchId = child.properties?.branch || `branch_${index}`;
      cells.push({
        id: `${this.id}_branch_${index}`,
        shape: 'flow-edge',
        source: { cell: this.id, port: `out_${index}` },
        target: { cell: child.id, port: 'in' },
        labels: [{ attrs: { label: { text: branchId } } }],
        data: { branch: branchId },
      });
    });

    // 生成默认分支
    if (this.defaultBranch) {
      cells.push(...this.defaultBranch.toCells());
      cells.push({
        id: `${this.id}_default`,
        shape: 'flow-edge',
        source: { cell: this.id, port: 'default' },
        target: { cell: this.defaultBranch.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'DEFAULT' } } }],
        data: { branch: 'default' },
      });
    }

    return cells;
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
