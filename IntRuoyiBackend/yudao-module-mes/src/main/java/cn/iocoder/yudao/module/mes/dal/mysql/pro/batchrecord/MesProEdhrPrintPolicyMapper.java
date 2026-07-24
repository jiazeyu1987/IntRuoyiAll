package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPrintPolicyPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintPolicyDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrPrintPolicyMapper extends BaseMapperX<MesProEdhrPrintPolicyDO> {

    default PageResult<MesProEdhrPrintPolicyDO> selectPage(MesProEdhrPrintPolicyPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrPrintPolicyDO>()
                .likeIfPresent(MesProEdhrPrintPolicyDO::getPolicyCode, reqVO.getPolicyCode())
                .likeIfPresent(MesProEdhrPrintPolicyDO::getPolicyName, reqVO.getPolicyName())
                .eqIfPresent(MesProEdhrPrintPolicyDO::getBusinessType, reqVO.getBusinessType())
                .eqIfPresent(MesProEdhrPrintPolicyDO::getTemplateType, reqVO.getTemplateType())
                .eqIfPresent(MesProEdhrPrintPolicyDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProEdhrPrintPolicyDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MesProEdhrPrintPolicyDO::getId));
    }

    default MesProEdhrPrintPolicyDO selectByPolicyCode(String policyCode) {
        return selectOne(MesProEdhrPrintPolicyDO::getPolicyCode, policyCode);
    }

    default MesProEdhrPrintPolicyDO selectActivePolicy(String businessType, String templateType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrPrintPolicyDO>()
                .eq(MesProEdhrPrintPolicyDO::getBusinessType, businessType)
                .eq(MesProEdhrPrintPolicyDO::getTemplateType, templateType)
                .eq(MesProEdhrPrintPolicyDO::getStatus, "ACTIVE"));
    }
}
