package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrOperationAuditEventMapper extends BaseMapperX<MesProEdhrOperationAuditEventDO> {

    default PageResult<MesProEdhrOperationAuditEventDO> selectPage(MesProEdhrOperationAuditPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getWorkTaskId, reqVO.getWorkTaskId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getRouteId, reqVO.getRouteId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getRouteProcessId, reqVO.getRouteProcessId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getReportId, reqVO.getReportId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getRecordCategory, reqVO.getRecordCategory())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getObjectType, reqVO.getObjectType())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getObjectId, reqVO.getObjectId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getOperationType, reqVO.getOperationType())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getActorUserId, reqVO.getActorUserId())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getPermissionDecision, reqVO.getPermissionDecision())
                .eqIfPresent(MesProEdhrOperationAuditEventDO::getResultStatus, reqVO.getResultStatus())
                .betweenIfPresent(MesProEdhrOperationAuditEventDO::getOccurredAt, reqVO.getOccurredAt())
                .orderByDesc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId));
    }

    default List<MesProEdhrOperationAuditEventDO> selectListByObject(String objectType, String objectId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eq(MesProEdhrOperationAuditEventDO::getObjectType, objectType)
                .eq(MesProEdhrOperationAuditEventDO::getObjectId, objectId)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId));
    }

    default List<MesProEdhrOperationAuditEventDO> selectSuccessfulListByBatchExecutionIdAndOperation(
            Long batchExecutionId, String operationType) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrOperationAuditEventDO>()
                .eq(MesProEdhrOperationAuditEventDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrOperationAuditEventDO::getOperationType, operationType)
                .eq(MesProEdhrOperationAuditEventDO::getResultStatus, "SUCCESS")
                .orderByDesc(MesProEdhrOperationAuditEventDO::getOccurredAt)
                .orderByDesc(MesProEdhrOperationAuditEventDO::getId));
    }
}
