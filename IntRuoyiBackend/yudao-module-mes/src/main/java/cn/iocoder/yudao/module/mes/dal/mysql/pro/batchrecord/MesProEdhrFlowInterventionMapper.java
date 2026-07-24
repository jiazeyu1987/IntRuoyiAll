package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrFlowInterventionPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFlowInterventionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrFlowInterventionMapper extends BaseMapperX<MesProEdhrFlowInterventionDO> {

    default MesProEdhrFlowInterventionDO selectByBusinessObjectTypeAndBusinessObjectIdAndInterventionActionAndIdempotencyKey(
            String businessObjectType, String businessObjectId, String interventionAction, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrFlowInterventionDO>()
                .eq(MesProEdhrFlowInterventionDO::getBusinessObjectType, businessObjectType)
                .eq(MesProEdhrFlowInterventionDO::getBusinessObjectId, businessObjectId)
                .eq(MesProEdhrFlowInterventionDO::getInterventionAction, interventionAction)
                .eq(MesProEdhrFlowInterventionDO::getIdempotencyKey, idempotencyKey));
    }

    default PageResult<MesProEdhrFlowInterventionDO> selectPage(MesProEdhrFlowInterventionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrFlowInterventionDO>()
                .eqIfPresent(MesProEdhrFlowInterventionDO::getBusinessObjectType, reqVO.getBusinessObjectType())
                .eqIfPresent(MesProEdhrFlowInterventionDO::getBusinessObjectId, reqVO.getBusinessObjectId())
                .likeIfPresent(MesProEdhrFlowInterventionDO::getBusinessObjectCode, reqVO.getBusinessObjectCode())
                .eqIfPresent(MesProEdhrFlowInterventionDO::getFlowInstanceId, reqVO.getFlowInstanceId())
                .eqIfPresent(MesProEdhrFlowInterventionDO::getInterventionAction, reqVO.getInterventionAction())
                .eqIfPresent(MesProEdhrFlowInterventionDO::getInterventionStatus, reqVO.getInterventionStatus())
                .orderByDesc(MesProEdhrFlowInterventionDO::getRequestedAt)
                .orderByDesc(MesProEdhrFlowInterventionDO::getId));
    }
}
