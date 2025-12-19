import React from 'react';

import { CopyOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';

import { Graph, Node } from '@antv/x6';
import { shortcuts } from '../../../../common/shortcuts';
import { getSelectedNodes } from '../../../../utils/flowChartUtils';

const nodeMenuConfig = [
  {
    key: 'replace',
    title: '替换节点',
    icon: <EditOutlined />,
    handler: (flowGraph: Graph) => {
      const selectedNodes = getSelectedNodes(flowGraph);
      if (selectedNodes.length === 1) {
        const node = selectedNodes[0] as Node;
        const { model } = node.getData() || {};
        // 获取节点在画布上的位置
        const pos = node.getBBox();
        // 计算屏幕坐标
        const point = flowGraph.localToClient({ x: pos.x + pos.width / 2, y: pos.y + pos.height / 2 });
        flowGraph.trigger('model:select', model);
        // 使用 setTimeout 延迟显示 ContextPad，避免被 useClickAway 立即关闭
        setTimeout(() => {
          flowGraph.trigger('graph:showContextPad', {
            x: point.x,
            y: point.y,
            node,
            scene: 'replace',
            title: '替换当前节点',
            edge: null,
          });
        }, 0);
      }
    },
  },
  {
    key: 'delete',
    title: '删除',
    icon: <DeleteOutlined />,
    handler: shortcuts.delete.handler,
  },
];

export default nodeMenuConfig;
