import React from 'react';
import { Node } from '@antv/x6';
import { Tooltip, Modal } from 'antd';
import { DeleteOutlined, EditOutlined, MinusSquareOutlined, PlusSquareOutlined } from '@ant-design/icons';
import { debounce } from 'lodash-es';
import styles from './index.module.less';

interface NodeToolBarProps {
  node: Node;
}

const NodeToolBar: React.FC<NodeToolBarProps> = ({ node }) => {
  const data = node.getData() || {};
  const { model, toolbar = { append: true, delete: true, prepend: true, replace: true, collapse: false } } = data;

  const showContextPad = debounce((info: unknown) => {
    node.model?.graph?.trigger('graph:showContextPad', info);
  }, 100);

  const onPrepend = (event: React.MouseEvent) => {
    showContextPad({
      x: event.clientX,
      y: event.clientY,
      node,
      scene: 'prepend',
      title: '前面插入节点',
      edge: null,
    });
  };

  const onAppend = (event: React.MouseEvent) => {
    showContextPad({
      x: event.clientX,
      y: event.clientY,
      node,
      scene: 'append',
      title: '后面插入节点',
      edge: null,
    });
  };

  const onReplace = (event: React.MouseEvent) => {
    if (model) {
      node.model?.graph?.select(model.getNodes?.() || []);
      node.model?.graph?.trigger('model:select', model);
    }
    showContextPad({
      x: event.clientX,
      y: event.clientY,
      node,
      scene: 'replace',
      title: '替换当前节点',
      edge: null,
    });
  };

  const onDelete = debounce(() => {
    if (model) {
      node.model?.graph?.select(model.selectNodes?.() || []);
      node.model?.graph?.trigger('model:select', model);
    }
    Modal.confirm({
      title: '确认要删除选中的节点？',
      content: '点击确认按钮进行删除，点击取消按钮返回',
      onOk() {
        if (model?.remove?.()) {
          node.model?.graph?.cleanSelection();
          node.model?.graph?.trigger('model:change');
        }
      },
    });
  }, 100);

  const onCollapse = debounce(() => {
    model?.toggleCollapse?.();
    node.model?.graph?.trigger('model:change');
  }, 100);

  const collapsed = model?.isCollapsed?.();

  return (
    <div className={styles.liteflowNodeToolBar}>
      {toolbar.prepend && (
        <div className={styles.liteflowAddNodePrepend} onClick={onPrepend}>
          <Tooltip title="前面插入节点">
            <div className={styles.liteflowAddNodePrependIcon} />
          </Tooltip>
        </div>
      )}
      {toolbar.append && (
        <div className={styles.liteflowAddNodeAppend} onClick={onAppend}>
          <Tooltip title="后面插入节点">
            <div className={styles.liteflowAddNodeAppendIcon} />
          </Tooltip>
        </div>
      )}
      {(toolbar.replace || toolbar.delete) && (
        <div className={styles.liteflowTopToolBar}>
          {toolbar.replace && (
            <div className={styles.liteflowToolBarBtn} onClick={onReplace}>
              <Tooltip title="替换当前节点">
                <EditOutlined />
              </Tooltip>
            </div>
          )}
          {toolbar.delete && (
            <div className={`${styles.liteflowToolBarBtn} ${styles.liteflowDeleteNode}`} onClick={onDelete}>
              <Tooltip title="删���节点">
                <DeleteOutlined />
              </Tooltip>
            </div>
          )}
        </div>
      )}
      {toolbar.collapse && (
        <div className={`${styles.liteflowBottomToolBar} ${styles.show}`}>
          <div className={`${styles.liteflowToolBarBtn} ${styles.liteflowCollapseNode}`} onClick={onCollapse}>
            <Tooltip title={collapsed ? '展开节点' : '折叠节点'}>
              {collapsed ? <PlusSquareOutlined /> : <MinusSquareOutlined />}
            </Tooltip>
          </div>
        </div>
      )}
    </div>
  );
};

export default NodeToolBar;
