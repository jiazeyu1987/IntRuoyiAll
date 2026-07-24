package cn.iocoder.yudao.module.srm.dal.mysql.coderule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.coderule.vo.SrmCodeRulePageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.coderule.SrmCodeRuleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * SRM 编码规则 Mapper。
 */
@Mapper
public interface SrmCodeRuleMapper extends BaseMapperX<SrmCodeRuleDO> {

    default PageResult<SrmCodeRuleDO> selectPage(SrmCodeRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmCodeRuleDO>()
                .likeIfPresent(SrmCodeRuleDO::getRuleCode, reqVO.getRuleCode())
                .eqIfPresent(SrmCodeRuleDO::getTargetForm, reqVO.getTargetForm())
                .eqIfPresent(SrmCodeRuleDO::getEnabled, reqVO.getEnabled())
                .betweenIfPresent(SrmCodeRuleDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SrmCodeRuleDO::getId));
    }

    default SrmCodeRuleDO selectByRuleCode(String ruleCode) {
        return selectOne(SrmCodeRuleDO::getRuleCode, ruleCode);
    }

    default SrmCodeRuleDO selectByTargetForm(String targetForm) {
        return selectOne(SrmCodeRuleDO::getTargetForm, targetForm);
    }

    default SrmCodeRuleDO selectByTargetFormForUpdate(String targetForm) {
        return selectOne(new LambdaQueryWrapperX<SrmCodeRuleDO>()
                .eq(SrmCodeRuleDO::getTargetForm, targetForm)
                .last("FOR UPDATE"));
    }

}
