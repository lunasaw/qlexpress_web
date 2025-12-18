import React from 'react';
import { Node } from '@antv/x6';
import { ConditionTypeEnum } from '../../../../types/enums';
import { getIconByType } from '../../cells';
import styles from './index.module.less';

// 简化的节点类型，用于判断是否显示徽章
const HIDE_BADGE_TYPES = [
  'COMMON',
  'NodeComponent',
  'BOOLEAN',
  'NodeBooleanComponent',
  'VIRTUAL',
  'NodeVirtualComponent',
  ConditionTypeEnum.CHAIN,
];

interface NodeBadgeProps {
  node: Node;
}

const NodeBadge: React.FC<NodeBadgeProps> = ({ node }) => {
  const data = node.getData() || {};
  const { model } = data;

  if (!model) {
    return null;
  }

  const currentModel = model.proxy || model;
  const nodeShape = node.shape;

  // 如果节点类型与形状相同，或者是不需要显示徽章的类型，则不显示
  if (
    currentModel.type === nodeShape ||
    HIDE_BADGE_TYPES.includes(currentModel.type) ||
    nodeShape === 'LITEFLOW_INTERMEDIATE_END'
  ) {
    return null;
  }

  const badgeIcon = getIconByType(currentModel.type);
  if (!badgeIcon) {
    return null;
  }

  return (
    <div className={styles.liteflowShapeBadgeWrapper}>
      <img className={styles.liteflowShapeBadgeSvg} src={badgeIcon} alt="" />
    </div>
  );
};

export default NodeBadge;
