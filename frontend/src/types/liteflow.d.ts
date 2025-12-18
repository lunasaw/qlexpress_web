/**
 * LiteFlow 节点类型声明
 */

declare module '*.css';
declare module '*.less';
declare module '*.png';
declare module '*.svg' {
  const url: string;
  export default url;
}

declare interface LiteFlowNode {
  type: string;
  label: string;
  icon: string;
  shape?: string;
  node?: {
    primer?: string;
    [key: string]: unknown;
  };
  disabled?: boolean;
}

declare interface IMenuInfo {
  x: number;
  y: number;
  scene: string;
  visible: boolean;
}

declare type IContextPadScene = 'append' | 'prepend' | 'replace';
