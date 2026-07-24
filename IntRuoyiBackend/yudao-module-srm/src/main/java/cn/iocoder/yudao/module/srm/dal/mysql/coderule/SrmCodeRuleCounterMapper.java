package cn.iocoder.yudao.module.srm.dal.mysql.coderule;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.coderule.SrmCodeRuleCounterDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SRM 编码规则计数器 Mapper。
 */
@Mapper
public interface SrmCodeRuleCounterMapper extends BaseMapperX<SrmCodeRuleCounterDO> {

    default SrmCodeRuleCounterDO selectByRuleIdAndPeriodKey(Long ruleId, String periodKey) {
        return selectOne(SrmCodeRuleCounterDO::getRuleId, ruleId,
                SrmCodeRuleCounterDO::getPeriodKey, periodKey);
    }

    default SrmCodeRuleCounterDO selectByRuleIdAndPeriodKeyForUpdate(Long ruleId, String periodKey) {
        return selectOne(new LambdaQueryWrapperX<SrmCodeRuleCounterDO>()
                .eq(SrmCodeRuleCounterDO::getRuleId, ruleId)
                .eq(SrmCodeRuleCounterDO::getPeriodKey, periodKey)
                .last("FOR UPDATE"));
    }

}
