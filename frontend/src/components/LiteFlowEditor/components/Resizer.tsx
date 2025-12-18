import React, { useCallback, useEffect, useRef, useState } from 'react';
import './Resizer.css';

export type ResizerDirection = 'horizontal' | 'vertical';

interface ResizerProps {
  /** 拖拽方向: horizontal (左右调整宽度) | vertical (上下调整高度) */
  direction: ResizerDirection;
  /** 当前尺寸 */
  size: number;
  /** 最小尺寸 */
  minSize?: number;
  /** 最大尺寸 */
  maxSize?: number;
  /** 尺寸变化回调 */
  onResize: (size: number) => void;
  /** 是否反向 (从右侧或底部计算) */
  reverse?: boolean;
  /** 自定义类名 */
  className?: string;
}

/**
 * 可拖拽调整尺寸的分隔条
 */
const Resizer: React.FC<ResizerProps> = ({
  direction,
  size,
  minSize = 100,
  maxSize = 800,
  onResize,
  reverse = false,
  className = '',
}) => {
  const [isDragging, setIsDragging] = useState(false);
  const startPosRef = useRef(0);
  const startSizeRef = useRef(0);

  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      setIsDragging(true);
      startPosRef.current = direction === 'horizontal' ? e.clientX : e.clientY;
      startSizeRef.current = size;
    },
    [direction, size]
  );

  const handleMouseMove = useCallback(
    (e: MouseEvent) => {
      if (!isDragging) return;

      const currentPos = direction === 'horizontal' ? e.clientX : e.clientY;
      let delta = currentPos - startPosRef.current;

      // 如果是反向，delta 取反
      if (reverse) {
        delta = -delta;
      }

      const newSize = Math.min(maxSize, Math.max(minSize, startSizeRef.current + delta));
      onResize(newSize);
    },
    [isDragging, direction, reverse, minSize, maxSize, onResize]
  );

  const handleMouseUp = useCallback(() => {
    setIsDragging(false);
  }, []);

  useEffect(() => {
    if (isDragging) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = direction === 'horizontal' ? 'col-resize' : 'row-resize';
      document.body.style.userSelect = 'none';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isDragging, handleMouseMove, handleMouseUp, direction]);

  return (
    <div
      className={`resizer resizer-${direction} ${isDragging ? 'dragging' : ''} ${className}`}
      onMouseDown={handleMouseDown}
    >
      <div className="resizer-handle" />
    </div>
  );
};

export default Resizer;
