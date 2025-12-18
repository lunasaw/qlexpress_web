import React from 'react';
import { Node } from '@antv/x6';
import styles from './index.module.less';

interface NodeViewProps {
  icon: string;
  node: Node;
  children?: React.ReactNode;
}

const NodeView: React.FC<NodeViewProps> = ({ icon, children }) => {
  return (
    <div className={styles.liteflowShapeWrapper}>
      <img className={styles.liteflowShapeSvg} src={icon} alt="" />
      {children}
    </div>
  );
};

export default NodeView;
