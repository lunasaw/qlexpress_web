import type { Cell } from '@antv/x6';
import type { ConditionTypeEnum, NodeTypeEnum } from '../../../types/enums';
import type { CmpProperties, CmpProperty } from '../../../types/flow';
import type { QLComponent } from '../../../types/component';

/**
 * ELNode - 前端编排节点模型基类
 *
 * 设计模式: 树形结构 + 双向链接
 * 核心职责:
 * 1. 管理节点树形结构 (parent/children)
 * 2. 转换为 X6 图形数据 (toCells)
 * 3. 转换为 EL 表达式 (toEL)
 * 4. 转换为后端 CmpProperty JSON (toJSON)
 */
export abstract class ELNode {
  // ===== 节点标识 =====
  id: string;
  type: ConditionTypeEnum | NodeTypeEnum | string;

  // ===== 树形结构 =====
  parent?: ELNode;
  children: ELNode[] = [];

  // ===== 条件节点 (IF/SWITCH/FOR/WHILE 使用) =====
  condition?: ELNode;

  // ===== 属性配置 =====
  properties?: CmpProperties;

  // ===== 组件引用 (叶子节点) =====
  componentRef?: string;
  componentData?: QLComponent;

  constructor(parent?: ELNode, type?: ConditionTypeEnum | NodeTypeEnum | string) {
    this.id = this.generateId();
    this.type = type!;
    this.parent = parent;
  }

  /**
   * 生成唯一 ID
   */
  protected generateId(): string {
    return `node_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  // ===== 子节点操作 =====

  /**
   * 添加子节点
   * @param child 子节点
   * @param index 插入位置，默认追加到末尾
   */
  appendChild(child: ELNode, index?: number): boolean {
    child.parent = this;
    if (index !== undefined && index >= 0 && index <= this.children.length) {
      this.children.splice(index, 0, child);
    } else {
      this.children.push(child);
    }
    return true;
  }

  /**
   * 在开头添加子节点
   */
  prependChild(child: ELNode): boolean {
    child.parent = this;
    this.children.unshift(child);
    return true;
  }

  /**
   * 移除子节点
   */
  removeChild(child: ELNode): boolean {
    const index = this.children.indexOf(child);
    if (index > -1) {
      this.children.splice(index, 1);
      child.parent = undefined;
      return true;
    }
    return false;
  }

  /**
   * 替换当前节点
   */
  replace(newNode: ELNode): boolean {
    if (!this.parent) return false;
    const index = this.parent.children.indexOf(this);
    if (index > -1) {
      this.parent.children[index] = newNode;
      newNode.parent = this.parent;
      this.parent = undefined;
      return true;
    }
    return false;
  }

  /**
   * 获取子节点索引
   */
  getChildIndex(child: ELNode): number {
    return this.children.indexOf(child);
  }

  /**
   * 根据索引获取子节点
   */
  getChildAt(index: number): ELNode | undefined {
    return this.children[index];
  }

  /**
   * 是否是叶子节点
   */
  get isLeaf(): boolean {
    return this.children.length === 0;
  }

  /**
   * 获取节点深度
   */
  get depth(): number {
    let d = 0;
    let node: ELNode | undefined = this;
    while (node.parent) {
      d++;
      node = node.parent;
    }
    return d;
  }

  /**
   * 获取根节点
   */
  get root(): ELNode {
    let node: ELNode = this;
    while (node.parent) {
      node = node.parent;
    }
    return node;
  }

  // ===== 抽象方法 =====

  /**
   * 转换为 X6 图形数据
   */
  abstract toCells(): Cell.Metadata[];

  /**
   * 转换为 EL 表达式
   * @param prefix 缩进前缀 (用于格式化输出)
   */
  abstract toEL(prefix?: string): string;

  // ===== 图形连接点 =====

  /**
   * 获取节点的入口点 ID
   * 用于外部节点连接到此节点
   * 默认返回节点自身 ID，编排节点需要重写
   */
  getEntryId(): string {
    return this.id;
  }

  /**
   * 获取节点的出口点 ID
   * 用��此节点连接到外部节点
   * 默认返回节点自身 ID，编排节点需要重写
   */
  getExitId(): string {
    return this.id;
  }

  // ===== JSON 转换 (对接后端 CmpProperty) =====

  /**
   * 转换为 CmpProperty JSON
   */
  toJSON(): CmpProperty {
    const json: CmpProperty = {
      type: this.type,
    };

    if (this.componentRef) {
      json.componentRef = this.componentRef;
      json.id = this.componentRef;
    } else if (this.id) {
      json.id = this.id;
    }

    if (this.properties && Object.keys(this.properties).length > 0) {
      json.properties = { ...this.properties };
    }

    if (this.condition) {
      json.condition = this.condition.toJSON();
    }

    if (this.children.length > 0) {
      json.children = this.children.map((child) => child.toJSON());
    }

    return json;
  }

  // ===== 属性修饰符生成 =====

  /**
   * 生成 EL 属性修饰符
   */
  protected propertiesToEL(exclude: string[] = []): string {
    if (!this.properties) return '';

    const parts: string[] = [];
    const props = this.properties;

    if (!exclude.includes('id') && props.id) {
      parts.push(`.id("${props.id}")`);
    }
    if (!exclude.includes('tag') && props.tag) {
      parts.push(`.tag("${props.tag}")`);
    }
    if (!exclude.includes('data') && props.data) {
      parts.push(`.data("${props.data}")`);
    }
    if (!exclude.includes('maxWaitSeconds') && props.maxWaitSeconds) {
      parts.push(`.maxWaitSeconds(${props.maxWaitSeconds})`);
    }
    if (!exclude.includes('ignoreError') && props.ignoreError) {
      parts.push(`.ignoreError(true)`);
    }
    if (!exclude.includes('must') && props.must) {
      parts.push(`.must()`);
    }
    if (!exclude.includes('any') && props.any) {
      parts.push(`.any(true)`);
    }

    return parts.join('');
  }

  // ===== 遍历方法 =====

  /**
   * 深度优先遍历
   */
  traverse(callback: (node: ELNode) => void | boolean): void {
    const result = callback(this);
    if (result === false) return;

    if (this.condition) {
      this.condition.traverse(callback);
    }

    for (const child of this.children) {
      child.traverse(callback);
    }
  }

  /**
   * 查找节点
   */
  find(predicate: (node: ELNode) => boolean): ELNode | undefined {
    if (predicate(this)) return this;

    if (this.condition) {
      const found = this.condition.find(predicate);
      if (found) return found;
    }

    for (const child of this.children) {
      const found = child.find(predicate);
      if (found) return found;
    }

    return undefined;
  }

  /**
   * 查找所有匹配的节点
   */
  findAll(predicate: (node: ELNode) => boolean): ELNode[] {
    const results: ELNode[] = [];

    this.traverse((node) => {
      if (predicate(node)) {
        results.push(node);
      }
    });

    return results;
  }

  /**
   * 根据 ID 查找节点
   */
  findById(id: string): ELNode | undefined {
    return this.find((node) => node.id === id);
  }

  /**
   * 克隆节点 (浅拷贝)
   */
  abstract clone(): ELNode;
}

export default ELNode;
