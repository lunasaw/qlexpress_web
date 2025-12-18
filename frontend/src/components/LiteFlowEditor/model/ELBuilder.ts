import { ELNode } from './ELNode';
import {
  NodeOperator,
  ThenOperator,
  WhenOperator,
  IfOperator,
  SwitchOperator,
  ForOperator,
  WhileOperator,
  IteratorOperator,
  CatchOperator,
  AndOperator,
  OrOperator,
  NotOperator,
  ChainOperator,
} from './operators';
import { ConditionTypeEnum } from '../../../types/enums';
import type { CmpProperty } from '../../../types/flow';
import type { QLComponent } from '../../../types/component';

/**
 * 组件数据映射表
 * key: componentId, value: QLComponent
 */
type ComponentMap = Map<string, QLComponent>;

/**
 * ELBuilder - 从 CmpProperty JSON 构建 ELNode 树
 *
 * 主要职责:
 * 1. 解析后端返回的 CmpProperty JSON
 * 2. 构建对应的 ELNode 树结构
 * 3. 关联组件数据 (componentData)
 */
export class ELBuilder {
  private componentMap: ComponentMap;

  constructor(components?: QLComponent[]) {
    this.componentMap = new Map();
    if (components) {
      components.forEach((comp) => {
        this.componentMap.set(comp.componentId, comp);
      });
    }
  }

  /**
   * 设置组件数据
   */
  setComponents(components: QLComponent[]): void {
    this.componentMap.clear();
    components.forEach((comp) => {
      this.componentMap.set(comp.componentId, comp);
    });
  }

  /**
   * 从 CmpProperty 构建 ELNode 树
   */
  build(json: CmpProperty, parent?: ELNode): ELNode {
    const type = json.type?.toUpperCase();

    switch (type) {
      case ConditionTypeEnum.CHAIN:
        return this.buildChain(json, parent);

      case ConditionTypeEnum.THEN:
      case ConditionTypeEnum.SER:
        return this.buildThen(json, parent);

      case ConditionTypeEnum.WHEN:
      case ConditionTypeEnum.PAR:
        return this.buildWhen(json, parent);

      case ConditionTypeEnum.IF:
        return this.buildIf(json, parent);

      case ConditionTypeEnum.SWITCH:
        return this.buildSwitch(json, parent);

      case ConditionTypeEnum.FOR:
        return this.buildFor(json, parent);

      case ConditionTypeEnum.WHILE:
        return this.buildWhile(json, parent);

      case ConditionTypeEnum.ITERATOR:
        return this.buildIterator(json, parent);

      case ConditionTypeEnum.CATCH:
        return this.buildCatch(json, parent);

      case ConditionTypeEnum.AND:
        return this.buildAnd(json, parent);

      case ConditionTypeEnum.OR:
        return this.buildOr(json, parent);

      case ConditionTypeEnum.NOT:
        return this.buildNot(json, parent);

      default:
        // 默认作为叶子节点处理
        return this.buildNode(json, parent);
    }
  }

  /**
   * 构建 CHAIN 节点
   */
  private buildChain(json: CmpProperty, parent?: ELNode): ChainOperator {
    const node = new ChainOperator(parent, json.id);
    node.properties = json.properties;

    if (json.children) {
      json.children.forEach((childJson) => {
        const child = this.build(childJson, node);
        node.appendChild(child);
      });
    }

    return node;
  }

  /**
   * 构建 THEN 节点
   */
  private buildThen(json: CmpProperty, parent?: ELNode): ThenOperator {
    const node = new ThenOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    if (json.children) {
      json.children.forEach((childJson) => {
        const child = this.build(childJson, node);
        node.appendChild(child);
      });
    }

    return node;
  }

  /**
   * 构建 WHEN 节点
   */
  private buildWhen(json: CmpProperty, parent?: ELNode): WhenOperator {
    const node = new WhenOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    if (json.children) {
      json.children.forEach((childJson) => {
        const child = this.build(childJson, node);
        node.appendChild(child);
      });
    }

    return node;
  }

  /**
   * 构建 IF 节点
   */
  private buildIf(json: CmpProperty, parent?: ELNode): IfOperator {
    const node = new IfOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    // 构建条件
    if (json.condition) {
      node.condition = this.build(json.condition, node);
    }

    // 构建分支
    if (json.children) {
      if (json.children[0]) {
        node.trueBranch = this.build(json.children[0], node);
      }
      if (json.children[1]) {
        node.falseBranch = this.build(json.children[1], node);
      }
    }

    return node;
  }

  /**
   * 构建 SWITCH 节点
   */
  private buildSwitch(json: CmpProperty, parent?: ELNode): SwitchOperator {
    const node = new SwitchOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    // 构建条件
    if (json.condition) {
      node.condition = this.build(json.condition, node);
    }

    // 构建分支
    if (json.children) {
      json.children.forEach((childJson) => {
        const child = this.build(childJson, node);
        node.appendChild(child);
      });
    }

    return node;
  }

  /**
   * 构建 FOR 节点
   */
  private buildFor(json: CmpProperty, parent?: ELNode): ForOperator {
    const node = new ForOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    // 构建循环体
    if (json.children && json.children[0]) {
      node.body = this.build(json.children[0], node);
    }

    return node;
  }

  /**
   * 构建 WHILE 节点
   */
  private buildWhile(json: CmpProperty, parent?: ELNode): WhileOperator {
    const node = new WhileOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    // 构建条件
    if (json.condition) {
      node.condition = this.build(json.condition, node);
    }

    // 构建循环体
    if (json.children && json.children[0]) {
      node.body = this.build(json.children[0], node);
    }

    return node;
  }

  /**
   * 构建 ITERATOR 节点
   */
  private buildIterator(json: CmpProperty, parent?: ELNode): IteratorOperator {
    const node = new IteratorOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    // 构建迭代器
    if (json.condition) {
      node.condition = this.build(json.condition, node);
    }

    // 构建循环体
    if (json.children && json.children[0]) {
      node.body = this.build(json.children[0], node);
    }

    return node;
  }

  /**
   * 构建 CATCH 节点
   */
  private buildCatch(json: CmpProperty, parent?: ELNode): CatchOperator {
    const node = new CatchOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    // 构建 try 体和 catch 体
    if (json.children) {
      if (json.children[0]) {
        node.tryBody = this.build(json.children[0], node);
      }
      if (json.children[1]) {
        node.catchBody = this.build(json.children[1], node);
      }
    }

    return node;
  }

  /**
   * 构建 AND 节点
   */
  private buildAnd(json: CmpProperty, parent?: ELNode): AndOperator {
    const node = new AndOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    if (json.children) {
      json.children.forEach((childJson) => {
        const child = this.build(childJson, node);
        node.appendChild(child);
      });
    }

    return node;
  }

  /**
   * 构建 OR 节点
   */
  private buildOr(json: CmpProperty, parent?: ELNode): OrOperator {
    const node = new OrOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    if (json.children) {
      json.children.forEach((childJson) => {
        const child = this.build(childJson, node);
        node.appendChild(child);
      });
    }

    return node;
  }

  /**
   * 构建 NOT 节点
   */
  private buildNot(json: CmpProperty, parent?: ELNode): NotOperator {
    const node = new NotOperator(parent);
    node.id = json.id || node.id;
    node.properties = json.properties;

    if (json.children && json.children[0]) {
      node.operand = this.build(json.children[0], node);
    }

    return node;
  }

  /**
   * 构建叶子节点
   */
  private buildNode(json: CmpProperty, parent?: ELNode): NodeOperator {
    const componentRef = json.componentRef || json.id;
    const componentData = componentRef ? this.componentMap.get(componentRef) : undefined;

    const node = new NodeOperator(parent, componentRef, componentData);
    node.id = json.id || node.id;
    node.properties = json.properties;

    return node;
  }

  // ===== 静态工厂方法 =====

  /**
   * 创建空的流程链
   */
  static createChain(chainName?: string): ChainOperator {
    return new ChainOperator(undefined, chainName);
  }

  /**
   * 创建串行节点
   */
  static createThen(): ThenOperator {
    return new ThenOperator();
  }

  /**
   * 创建并行节点
   */
  static createWhen(): WhenOperator {
    return new WhenOperator();
  }

  /**
   * 创建条件节点
   */
  static createIf(): IfOperator {
    return new IfOperator();
  }

  /**
   * 创建选择节点
   */
  static createSwitch(): SwitchOperator {
    return new SwitchOperator();
  }

  /**
   * 创建次数循环节点
   */
  static createFor(loopCount?: number): ForOperator {
    const node = new ForOperator();
    if (loopCount !== undefined) {
      node.properties = { loopCount };
    }
    return node;
  }

  /**
   * 创建条件循环节点
   */
  static createWhile(): WhileOperator {
    return new WhileOperator();
  }

  /**
   * 创建迭代循环节点
   */
  static createIterator(): IteratorOperator {
    return new IteratorOperator();
  }

  /**
   * 创建异常捕获节点
   */
  static createCatch(): CatchOperator {
    return new CatchOperator();
  }

  /**
   * 创建叶子节点
   */
  static createNode(componentRef: string, componentData?: QLComponent): NodeOperator {
    return new NodeOperator(undefined, componentRef, componentData);
  }
}

export default ELBuilder;
