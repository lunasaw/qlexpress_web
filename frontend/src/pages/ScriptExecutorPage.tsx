import React from 'react';
import { ScriptExecutor } from '../components/ScriptExecutor';
import './pages.css';

/**
 * 脚本执行器页面
 */
const ScriptExecutorPage: React.FC = () => {
  const defaultScript = `// QLExpress 脚本示例
// 定义变量
a = 10;
b = 20;

// 计算结果
result = a + b;

// 条件判断
if (result > 25) {
    message = "大于25";
} else {
    message = "小于等于25";
}

// 返回结果
return result;
`;

  return (
    <div className="script-executor-page">
      <ScriptExecutor
        script={defaultScript}
        showEditor={true}
        title="QLExpress 脚本执行器"
      />
    </div>
  );
};

export default ScriptExecutorPage;
