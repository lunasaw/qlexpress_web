import type { Cell } from '@antv/x6';
import { ELNode } from '../ELNode';
import { ConditionTypeEnum } from '../../../../types/enums';

/**
 * CATCH 操作符 - 异常捕获
 * 对应 LiteFlow EL: CATCH(tryBody).DO(catchBody)
 *
 * children[0]: tryBody (必须)
 * children[1]: catchBody (可选)
 */
export class CatchOperator extends ELNode {
  constructor(parent?: ELNode) {
    super(parent, ConditionTypeEnum.CATCH);
  }

  /** 获取 try 体 */
  get tryBody(): ELNode | undefined {
    return this.children[0];
  }

  set tryBody(node: ELNode | undefined) {
    if (node) {
      node.parent = this;
      this.children[0] = node;
    }
  }

  /** 获取 catch 体 */
  get catchBody(): ELNode | undefined {
    return this.children[1];
  }

  set catchBody(node: ELNode | undefined) {
    if (node) {
      node.parent = this;
      this.children[1] = node;
    }
  }

  toCells(): Cell.Metadata[] {
    const cells: Cell.Metadata[] = [];

    cells.push({
      id: this.id,
      shape: 'catch-node',
      data: {
        type: this.type,
        label: '异常捕获 (CATCH)',
        properties: this.properties,
        hasTryBody: !!this.tryBody,
        hasCatchBody: !!this.catchBody,
      },
    });

    if (this.tryBody) {
      cells.push(...this.tryBody.toCells());
      cells.push({
        id: `${this.id}_try`,
        shape: 'flow-edge',
        source: { cell: this.id, port: 'try' },
        target: { cell: this.tryBody.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'TRY' } } }],
      });
    }

    if (this.catchBody) {
      cells.push(...this.catchBody.toCells());
      cells.push({
        id: `${this.id}_catch`,
        shape: 'flow-edge',
        source: { cell: this.id, port: 'catch' },
        target: { cell: this.catchBody.id, port: 'in' },
        labels: [{ attrs: { label: { text: 'CATCH' } } }],
        data: { exception: true },
      });
    }

    return cells;
  }

  toEL(prefix?: string): string {
    if (!this.tryBody) {
      throw new Error('CATCH 节点必须有 try 体');
    }

    const tryEL = this.tryBody.toEL(prefix);
    let result = `CATCH(${tryEL})`;

    if (this.catchBody) {
      const catchEL = this.catchBody.toEL(prefix);
      result += `.DO(${catchEL})`;
    }

    result += this.propertiesToEL();

    return result;
  }

  clone(): CatchOperator {
    const cloned = new CatchOperator();
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
 * 创建 CATCH 操作符的工厂方法
 */
export function createCatchOperator(
  parent?: ELNode,
  tryBody?: ELNode,
  catchBody?: ELNode
): CatchOperator {
  const operator = new CatchOperator(parent);

  if (tryBody) {
    operator.tryBody = tryBody;
  }

  if (catchBody) {
    operator.catchBody = catchBody;
  }

  return operator;
}

export default CatchOperator;
