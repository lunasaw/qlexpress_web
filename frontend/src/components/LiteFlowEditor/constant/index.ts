/**
 * LiteFlow 编辑器常量定义
 * 参考 liteflow-editor-client 设计
 */

// ===== 节点尺寸 =====
export const NODE_WIDTH = 120; // 节点宽度
export const NODE_HEIGHT = 40; // 节点高度
export const MIN_ZOOM = 0.1;
export const MAX_ZOOM = 3.0;
export const ZOOM_STEP = 0.1;

// ===== 布局相关常量 =====
export const RANK_SEP = 80; // 层级间距（垂直/水平方向主轴）
export const NODE_SEP = 50; // 节点间距（同层级）

// ===== 特殊节点类型标识 =====
export const NODE_TYPE_START = 'LITEFLOW_START';
export const NODE_TYPE_END = 'LITEFLOW_END';
export const NODE_TYPE_INTERMEDIATE_END = 'LITEFLOW_INTERMEDIATE_END';

// ===== 边和连接器标识 =====
export const LITEFLOW_EDGE = 'LITEFLOW_EDGE';
export const LITEFLOW_ANCHOR = 'LITEFLOW_ANCHOR';
export const LITEFLOW_ROUTER = 'LITEFLOW_ROUTER';

// ===== 颜色常量 =====
export const LINE_COLOR = '#c1c1c1';

// 从 enums.ts 重导出以方便使用
export { NodeTypeEnum, ConditionTypeEnum, ComponentType } from '../../../types/enums';
