package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionSignaturePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Mapper
public interface MesProBatchRecordExecutionSignatureMapper extends BaseMapperX<MesProBatchRecordExecutionSignatureDO> {

    default List<MesProBatchRecordExecutionSignatureDO> selectListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eq(MesProBatchRecordExecutionSignatureDO::getExecutionId, executionId)
                .orderByDesc(MesProBatchRecordExecutionSignatureDO::getSignedAt));
    }

    default List<MesProBatchRecordExecutionSignatureDO> selectTimelineListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eq(MesProBatchRecordExecutionSignatureDO::getExecutionId, executionId)
                .orderByAsc(MesProBatchRecordExecutionSignatureDO::getSignedAt)
                .orderByAsc(MesProBatchRecordExecutionSignatureDO::getId));
    }

    default List<MesProBatchRecordExecutionSignatureDO> selectResponsibilityListByIds(
            Collection<Long> signatureIds) {
        if (signatureIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .in(MesProBatchRecordExecutionSignatureDO::getId, signatureIds)
                .orderByAsc(MesProBatchRecordExecutionSignatureDO::getId));
    }

    default PageResult<MesProBatchRecordExecutionSignatureDO> selectPage(MesProBatchRecordExecutionSignaturePageReqVO reqVO) {
        return selectPage(reqVO, (Collection<Long>) null);
    }

    default List<Long> selectExecutionIdsByActorName(String actorName) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                        .likeIfPresent(MesProBatchRecordExecutionSignatureDO::getActorName, actorName))
                .stream()
                .map(MesProBatchRecordExecutionSignatureDO::getExecutionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    default PageResult<MesProBatchRecordExecutionSignatureDO> selectPage(MesProBatchRecordExecutionSignaturePageReqVO reqVO,
                                                                         Collection<Long> executionIds) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProBatchRecordExecutionSignatureDO>()
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getExecutionId, reqVO.getExecutionId())
                .inIfPresent(MesProBatchRecordExecutionSignatureDO::getExecutionId, executionIds)
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getActionType, reqVO.getActionType())
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getActorId, reqVO.getActorId())
                .likeIfPresent(MesProBatchRecordExecutionSignatureDO::getActorName, reqVO.getActorName())
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getProcessInstanceId, reqVO.getProcessInstanceId())
                .eqIfPresent(MesProBatchRecordExecutionSignatureDO::getBpmTaskId, reqVO.getBpmTaskId())
                .betweenIfPresent(MesProBatchRecordExecutionSignatureDO::getSignedAt,
                        reqVO.getSignedAtStart() == null ? reqVO.getBeginSignedAt() : reqVO.getSignedAtStart(),
                        reqVO.getSignedAtEnd() == null ? reqVO.getEndSignedAt() : reqVO.getSignedAtEnd())
                .orderByDesc(MesProBatchRecordExecutionSignatureDO::getSignedAt)
                .orderByDesc(MesProBatchRecordExecutionSignatureDO::getId));
    }
}
