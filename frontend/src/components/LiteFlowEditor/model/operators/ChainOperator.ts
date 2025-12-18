import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';
import {
  NODE_TYPE_START,
  NODE_TYPE_END,
  LITEFLOW_EDGE,
} from '../../constant';
import type { CmpProperty } from '../../../../types/flow';

/**
 * CHAIN 操作符 - 流程链
 * 作为根节点或子流程引用使用
 */
export class ChainOperator extends ELNode {
  /** 链名称 */
  chainName?: string;

  constructor(parent?: ELNode, chainName?: string) {
    super(parent, ConditionTypeEnum.CHAIN);
    this.chainName = chainName;
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];
    const startId = `${this.id}_start`;
    const endId = `${this.id}_end`;

    // 开始节点
    cells.push({
      id: startId,
      shape: NODE_TYPE_START,
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

    // 子节点
    this.children.forEach((child) => {
      cells.push(...child.toCells());
    });

    // 结束节点
    cells.push({
      id: endId,
      shape: NODE_TYPE_END,
      data: {
        model: this,
        toolbar: {
          prepend: true,
          append: false,
          delete: false,
          replace: false,
        },
      },
      attrs: {
        label: { text: '' },
      },
    });

    // 连线：START -> 第一个子节点 (使用入口点)
    if (this.children.length > 0) {
      const firstChildEntryId = this.children[0].getEntryId();
      cells.push({
        id: `${this.id}_start_edge`,
        shape: LITEFLOW_EDGE,
        source: startId,
        target: firstChildEntryId,
      });

      // 连线：最后一个子节点 -> END (使用出口点)
      const lastChild = this.children[this.children.length - 1];
      const lastChildExitId = lastChild.getExitId();
      cells.push({
        id: `${this.id}_end_edge`,
        shape: LITEFLOW_EDGE,
        source: lastChildExitId,
        target: endId,
      });
    } else {
      // 无子节点时直接连接 START -> END
      cells.push({
        id: `${this.id}_direct_edge`,
        shape: LITEFLOW_EDGE,
        source: startId,
        target: endId,
      });
    }

    return cells;
  }

  /**
   * 获取 CHAIN 的入口点 - START 节点
   */
  override getEntryId(): string {
    return `${this.id}_start`;
  }

  /**
   * 获取 CHAIN 的出口点 - END 节点
   */
  override getExitId(): string {
    return `${this.id}_end`;
  }

  toEL(prefix?: string): string {
    if (this.children.length === 0) {
      return '';
    }

    // 如果只有一个子节点，直接返回子节点的 EL
    if (this.children.length === 1) {
      return this.children[0].toEL(prefix);
    }

    // 多个子节点时，用 THEN 包装
    const childrenEL = this.children.map((child) => child.toEL(prefix));
    return `THEN(${childrenEL.join(', ')})`;
  }

  /**
   * 重写 toJSON，CHAIN 作为根容器节点，不需要 id 字段
   * chainName 用于生成 EL 表达式时的链名称，不放入 JSON
   */
  toJSON(): CmpProperty {
    const json: CmpProperty = {
      type: this.type,
    };

    // CHAIN 不设置 id 字段，因为后端会把 id 当作组件引用
    // chainName 只用于 EL 表达式生成

    if (this.properties && Object.keys(this.properties).length > 0) {
      json.properties = { ...this.properties };
    }

    if (this.children.length > 0) {
      json.children = this.children.map((child) => child.toJSON());
    }

    return json;
  }

  clone(): ChainOperator {
    const cloned = new ChainOperator(undefined, this.chainName);
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
 * 创建 CHAIN 操作符的工厂方法
 */
export function createChainOperator(
  parent?: ELNode,
  chainName?: string,
  children?: ELNode[]
): ChainOperator {
  const operator = new ChainOperator(parent, chainName);
  if (children) {
    children.forEach((child) => operator.appendChild(child));
  }
  return operator;
}

export default ChainOperator;
