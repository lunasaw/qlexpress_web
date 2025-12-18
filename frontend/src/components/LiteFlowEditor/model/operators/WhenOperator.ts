import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';

/**
 * WHEN 操作符 - 并行执行
 * 对应 LiteFlow EL: WHEN(a, b, c).maxWaitSeconds(10).ignoreError(true)
 */
export class WhenOperator extends ELNode {
  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.WHEN);
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    // 生成并行容器节点
    cells.push({
      id: this.id,
      shape: 'when-node',
      data: {
        type: this.type,
        label: '并行 (WHEN)',
        properties: this.properties,
        childCount: this.children.length,
      },
    });

    // 生成子节点
    this.children.forEach((child) => {
      cells.push(...child.toCells());
    });

    // 生成从容器到各子节点的连线 (分叉)
    this.children.forEach((child) => {
      cells.push({
        id: `${this.id}_to_${child.id}`,
        shape: 'flow-edge',
        source: { cell: this.id, port: 'out' },
        target: { cell: child.id, port: 'in' },
        data: { parallel: true },
      });
    });

    return cells;
  }

  toEL(prefix?: string): string {
    if (this.children.length === 0) {
      return '';
    }

    const childrenEL = this.children.map((child) => child.toEL(prefix)).filter(Boolean);

    // WHEN 特有的修饰符
    let modifiers = '';
    if (this.properties) {
      const props = this.properties;
      if (props.maxWaitSeconds) {
        modifiers += `.maxWaitSeconds(${props.maxWaitSeconds})`;
      }
      if (props.ignoreError) {
        modifiers += `.ignoreError(true)`;
      }
      if (props.must) {
        modifiers += `.must()`;
      }
      if (props.any) {
        modifiers += `.any(true)`;
      }
    }

    // 通用修饰符 (排除已处理的)
    modifiers += this.propertiesToEL(['maxWaitSeconds', 'ignoreError', 'must', 'any']);

    if (prefix !== undefined) {
      const indent = prefix + '  ';
      return `${prefix}WHEN(\n${childrenEL.map((el) => `${indent}${el}`).join(',\n')}\n${prefix})${modifiers}`;
    }

    return `WHEN(${childrenEL.join(', ')})${modifiers}`;
  }

  clone(): WhenOperator {
    const cloned = new WhenOperator();
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
 * 创建 WHEN 操作符的工厂方法
 */
export function createWhenOperator(parent?: ELNode, children?: ELNode[]): WhenOperator {
  const operator = new WhenOperator(parent);
  if (children) {
    children.forEach((child) => operator.appendChild(child));
  }
  return operator;
}

export default WhenOperator;
