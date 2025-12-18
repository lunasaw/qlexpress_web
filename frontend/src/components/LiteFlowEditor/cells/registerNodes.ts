import { Graph } from '@antv/x6';

/**
 * 通用端口配置
 */
const commonPorts = {
  groups: {
    in: {
      position: 'top',
      attrs: {
        circle: {
          r: 4,
          magnet: true,
          stroke: '#c0c0c0',
          strokeWidth: 1,
          fill: '#fff',
        },
      },
    },
    out: {
      position: 'bottom',
      attrs: {
        circle: {
          r: 4,
          magnet: true,
          stroke: '#c0c0c0',
          strokeWidth: 1,
          fill: '#fff',
        },
      },
    },
  },
  items: [
    { id: 'in', group: 'in' },
    { id: 'out', group: 'out' },
  ],
};

/**
 * 条件节点端口配置
 */
const conditionPorts = {
  groups: {
    in: {
      position: 'top',
      attrs: {
        circle: {
          r: 4,
          magnet: true,
          stroke: '#c0c0c0',
          strokeWidth: 1,
          fill: '#fff',
        },
      },
    },
    condition: {
      position: 'left',
      attrs: {
        circle: {
          r: 4,
          magnet: true,
          stroke: '#faad14',
          strokeWidth: 1,
          fill: '#fff',
        },
      },
    },
    true: {
      position: { name: 'bottom', args: { dx: -20 } },
      attrs: {
        circle: {
          r: 4,
          magnet: true,
          stroke: '#52c41a',
          strokeWidth: 1,
          fill: '#fff',
        },
      },
    },
    false: {
      position: { name: 'bottom', args: { dx: 20 } },
      attrs: {
        circle: {
          r: 4,
          magnet: true,
          stroke: '#ff4d4f',
          strokeWidth: 1,
          fill: '#fff',
        },
      },
    },
  },
  items: [
    { id: 'in', group: 'in' },
    { id: 'condition', group: 'condition' },
    { id: 'true', group: 'true' },
    { id: 'false', group: 'false' },
  ],
};

/**
 * 注册所有自定义节点
 */
export function registerNodes() {
  // ===== 开始/结束节点 =====
  Graph.registerNode(
    'start-end-node',
    {
      inherit: 'circle',
      width: 60,
      height: 60,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#52c41a',
          fill: '#f6ffed',
        },
        label: {
          fontSize: 14,
          fill: '#333',
        },
      },
      ports: {
        groups: {
          out: {
            position: 'bottom',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#52c41a',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
          in: {
            position: 'top',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#ff4d4f',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
        },
        items: [
          { id: 'out', group: 'out' },
          { id: 'in', group: 'in' },
        ],
      },
    },
    true
  );

  // ===== 通用业务组件节点 =====
  Graph.registerNode(
    'common-node',
    {
      inherit: 'rect',
      width: 180,
      height: 60,
      attrs: {
        body: {
          strokeWidth: 1,
          stroke: '#1890ff',
          fill: '#e6f7ff',
          rx: 4,
          ry: 4,
        },
        label: {
          fontSize: 14,
          fill: '#333',
        },
      },
      ports: commonPorts,
    },
    true
  );

  // ===== THEN 容器节点 =====
  Graph.registerNode(
    'then-node',
    {
      inherit: 'rect',
      width: 240,
      height: 80,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#1890ff',
          strokeDasharray: '5 5',
          fill: '#e6f7ff',
          rx: 8,
          ry: 8,
        },
        label: {
          fontSize: 14,
          fontWeight: 'bold',
          fill: '#1890ff',
        },
      },
      ports: commonPorts,
    },
    true
  );

  // ===== WHEN 容器节点 =====
  Graph.registerNode(
    'when-node',
    {
      inherit: 'rect',
      width: 240,
      height: 80,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#52c41a',
          strokeDasharray: '5 5',
          fill: '#f6ffed',
          rx: 8,
          ry: 8,
        },
        label: {
          fontSize: 14,
          fontWeight: 'bold',
          fill: '#52c41a',
        },
      },
      ports: commonPorts,
    },
    true
  );

  // ===== IF 条件节点 =====
  Graph.registerNode(
    'if-node',
    {
      inherit: 'polygon',
      width: 120,
      height: 80,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#faad14',
          fill: '#fffbe6',
          refPoints: '0,0.5 0.5,0 1,0.5 0.5,1',
        },
        label: {
          fontSize: 14,
          fontWeight: 'bold',
          fill: '#faad14',
        },
      },
      ports: conditionPorts,
    },
    true
  );

  // ===== SWITCH 选择节点 =====
  Graph.registerNode(
    'switch-node',
    {
      inherit: 'polygon',
      width: 120,
      height: 80,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#722ed1',
          fill: '#f9f0ff',
          refPoints: '0,0.5 0.5,0 1,0.5 0.5,1',
        },
        label: {
          fontSize: 14,
          fontWeight: 'bold',
          fill: '#722ed1',
        },
      },
      ports: {
        groups: {
          in: {
            position: 'top',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#c0c0c0',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
          condition: {
            position: 'left',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#722ed1',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
          out: {
            position: 'bottom',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#722ed1',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
          default: {
            position: 'right',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#999',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
        },
        items: [
          { id: 'in', group: 'in' },
          { id: 'condition', group: 'condition' },
          { id: 'out', group: 'out' },
          { id: 'default', group: 'default' },
        ],
      },
    },
    true
  );

  // ===== FOR 循环节点 =====
  Graph.registerNode(
    'for-node',
    {
      inherit: 'polygon',
      width: 140,
      height: 80,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#13c2c2',
          fill: '#e6fffb',
          refPoints: '0.25,0 0.75,0 1,0.5 0.75,1 0.25,1 0,0.5',
        },
        label: {
          fontSize: 14,
          fontWeight: 'bold',
          fill: '#13c2c2',
        },
      },
      ports: commonPorts,
    },
    true
  );

  // ===== WHILE 循环节点 =====
  Graph.registerNode(
    'while-node',
    {
      inherit: 'polygon',
      width: 140,
      height: 80,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#13c2c2',
          fill: '#e6fffb',
          refPoints: '0.25,0 0.75,0 1,0.5 0.75,1 0.25,1 0,0.5',
        },
        label: {
          fontSize: 14,
          fontWeight: 'bold',
          fill: '#13c2c2',
        },
      },
      ports: {
        groups: {
          in: {
            position: 'top',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#c0c0c0',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
          condition: {
            position: 'left',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#13c2c2',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
          out: {
            position: 'bottom',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#c0c0c0',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
        },
        items: [
          { id: 'in', group: 'in' },
          { id: 'condition', group: 'condition' },
          { id: 'out', group: 'out' },
        ],
      },
    },
    true
  );

  // ===== ITERATOR 迭代节点 =====
  Graph.registerNode(
    'iterator-node',
    {
      inherit: 'while-node',
    },
    true
  );

  // ===== CATCH 异常捕获节点 =====
  Graph.registerNode(
    'catch-node',
    {
      inherit: 'rect',
      width: 160,
      height: 80,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#ff4d4f',
          fill: '#fff2f0',
          rx: 8,
          ry: 8,
        },
        label: {
          fontSize: 14,
          fontWeight: 'bold',
          fill: '#ff4d4f',
        },
      },
      ports: {
        groups: {
          in: {
            position: 'top',
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#c0c0c0',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
          try: {
            position: { name: 'bottom', args: { dx: -30 } },
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#1890ff',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
          catch: {
            position: { name: 'bottom', args: { dx: 30 } },
            attrs: {
              circle: {
                r: 4,
                magnet: true,
                stroke: '#ff4d4f',
                strokeWidth: 1,
                fill: '#fff',
              },
            },
          },
        },
        items: [
          { id: 'in', group: 'in' },
          { id: 'try', group: 'try' },
          { id: 'catch', group: 'catch' },
        ],
      },
    },
    true
  );

  // ===== 逻辑运算节点 =====
  Graph.registerNode(
    'logic-node',
    {
      inherit: 'rect',
      width: 100,
      height: 60,
      attrs: {
        body: {
          strokeWidth: 2,
          stroke: '#eb2f96',
          fill: '#fff0f6',
          rx: 30,
          ry: 30,
        },
        label: {
          fontSize: 14,
          fontWeight: 'bold',
          fill: '#eb2f96',
        },
      },
      ports: commonPorts,
    },
    true
  );

  // ===== 注册边 =====
  Graph.registerEdge(
    'flow-edge',
    {
      inherit: 'edge',
      attrs: {
        line: {
          stroke: '#c0c0c0',
          strokeWidth: 2,
          targetMarker: {
            name: 'block',
            width: 12,
            height: 8,
          },
        },
      },
      router: {
        name: 'manhattan',
        args: {
          padding: 20,
        },
      },
      connector: {
        name: 'rounded',
        args: {
          radius: 8,
        },
      },
    },
    true
  );

  console.log('LiteFlow 节点注册完成');
}

export default registerNodes;
