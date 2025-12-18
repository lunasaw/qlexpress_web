import { ConditionTypeEnum } from '../../constant';
import icon from '../../assets/catch-icon.svg';

const config: LiteFlowNode = {
  label: '捕获(Catch)',
  type: ConditionTypeEnum.CATCH,
  icon,
};

export default config;
