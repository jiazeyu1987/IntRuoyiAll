package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrUnifiedChangeRequestDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrUnifiedChangeRequestMapper extends BaseMapperX<MesProEdhrUnifiedChangeRequestDO> {

    default MesProEdhrUnifiedChangeRequestDO selectByControlledObjectTypeAndControlledObjectIdAndChangeTypeAndIdempotencyKey(
            String controlledObjectType, String controlledObjectId, String changeType, String idempotencyKey) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrUnifiedChangeRequestDO>()
                .eq(MesProEdhrUnifiedChangeRequestDO::getControlledObjectType, controlledObjectType)
                .eq(MesProEdhrUnifiedChangeRequestDO::getControlledObjectId, controlledObjectId)
                .eq(MesProEdhrUnifiedChangeRequestDO::getChangeType, changeType)
                .eq(MesProEdhrUnifiedChangeRequestDO::getIdempotencyKey, idempotencyKey));
    }

    default PageResult<MesProEdhrUnifiedChangeRequestDO> selectPage(MesProEdhrUnifiedChangePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrUnifiedChangeRequestDO>()
                .eqIfPresent(MesProEdhrUnifiedChangeRequestDO::getControlledObjectType, reqVO.getControlledObjectType())
                .eqIfPresent(MesProEdhrUnifiedChangeRequestDO::getControlledObjectId, reqVO.getControlledObjectId())
                .likeIfPresent(MesProEdhrUnifiedChangeRequestDO::getControlledObjectCode, reqVO.getControlledObjectCode())
                .eqIfPresent(MesProEdhrUnifiedChangeRequestDO::getChangeType, reqVO.getChangeType())
                .eqIfPresent(MesProEdhrUnifiedChangeRequestDO::getChangeStatus, reqVO.getChangeStatus())
                .eqIfPresent(MesProEdhrUnifiedChangeRequestDO::getRiskLevel, reqVO.getRiskLevel())
                .orderByDesc(MesProEdhrUnifiedChangeRequestDO::getRequestedAt)
                .orderByDesc(MesProEdhrUnifiedChangeRequestDO::getId));
    }

    default Long countByControlledObjectAndChangeType(String controlledObjectType, String controlledObjectId,
                                                      String changeType) {
        return selectCount(new LambdaQueryWrapperX<MesProEdhrUnifiedChangeRequestDO>()
                .eq(MesProEdhrUnifiedChangeRequestDO::getControlledObjectType, controlledObjectType)
                .eq(MesProEdhrUnifiedChangeRequestDO::getControlledObjectId, controlledObjectId)
                .eq(MesProEdhrUnifiedChangeRequestDO::getChangeType, changeType));
    }

    default Long countByControlledObjectChangeTypeAndStatus(String controlledObjectType, String controlledObjectId,
                                                            String changeType, String changeStatus) {
        return selectCount(new LambdaQueryWrapperX<MesProEdhrUnifiedChangeRequestDO>()
                .eq(MesProEdhrUnifiedChangeRequestDO::getControlledObjectType, controlledObjectType)
                .eq(MesProEdhrUnifiedChangeRequestDO::getControlledObjectId, controlledObjectId)
                .eq(MesProEdhrUnifiedChangeRequestDO::getChangeType, changeType)
                .eq(MesProEdhrUnifiedChangeRequestDO::getChangeStatus, changeStatus));
    }
}
