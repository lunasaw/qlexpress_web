import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';
import { LITEFLOW_EDGE, NODE_TYPE_INTERMEDIATE_END } from '../../constant';

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
    const forkId = `${this.id}_fork`;
    const joinId = `${this.id}_join`;

    // 生成分叉节点
    cells.push({
      id: forkId,
      shape: 'when-node',
      data: {
        model: this,
        type: this.type,
        label: '并行 (WHEN)',
        properties: this.properties,
        childCount: this.children.length,
        toolbar: {
          prepend: true,
          append: false,
          delete: true,
          replace: true,
        },
      },
    });

    // 生成子节点
    this.children.forEach((child) => {
      cells.push(...child.toCells());
    });

    // 生成汇聚节点
    cells.push({
      id: joinId,
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

    // 生成从分叉节点到各子节点的连线 (使用入口点)
    this.children.forEach((child) => {
      const targetId = child.getEntryId();
      cells.push({
        id: `${forkId}_to_${targetId}`,
        shape: LITEFLOW_EDGE,
        source: forkId,
        target: targetId,
        data: { parallel: true },
      });
    });

    // 生成从各子节点到汇聚节点的连线 (使用出口点)
    this.children.forEach((child) => {
      const sourceId = child.getExitId();
      cells.push({
        id: `${sourceId}_to_${joinId}`,
        shape: LITEFLOW_EDGE,
        source: sourceId,
        target: joinId,
        data: { parallel: true },
      });
    });

    return cells;
  }

  /**
   * 获取 WHEN 的入口点 - 分叉节点
   */
  override getEntryId(): string {
    return `${this.id}_fork`;
  }

  /**
   * 获取 WHEN 的出口点 - 汇聚节点
   */
  override getExitId(): string {
    return `${this.id}_join`;
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
