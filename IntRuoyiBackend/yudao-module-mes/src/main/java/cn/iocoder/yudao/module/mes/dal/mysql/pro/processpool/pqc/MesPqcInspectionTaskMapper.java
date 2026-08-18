package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Mapper
public interface MesPqcInspectionTaskMapper extends BaseMapperX<MesPqcInspectionTaskDO> {

    default MesPqcInspectionTaskDO selectByIdForUpdate(Long id) {
        if (id == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .eq(MesPqcInspectionTaskDO::getId, id)
                .last("FOR UPDATE"));
    }

    default MesPqcInspectionTaskDO selectPendingByActiveOrderProcess(Long activeOrderId, Long routeProcessId,
                                                                     Long processId) {
        return selectOne(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .eq(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderId)
                .eq(MesPqcInspectionTaskDO::getRouteProcessId, routeProcessId)
                .eq(MesPqcInspectionTaskDO::getProcessId, processId)
                .eq(MesPqcInspectionTaskDO::getTaskStatus, "PENDING")
                .orderByAsc(MesPqcInspectionTaskDO::getBusinessDate)
                .orderByAsc(MesPqcInspectionTaskDO::getInspectionType)
                .orderByAsc(MesPqcInspectionTaskDO::getRoundNo)
                .last("LIMIT 1"));
    }

    default List<MesPqcInspectionTaskDO> selectListByActiveOrderId(Long activeOrderId) {
        return selectList(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .eq(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesPqcInspectionTaskDO::getBusinessDate)
                .orderByAsc(MesPqcInspectionTaskDO::getInspectionType)
                .orderByAsc(MesPqcInspectionTaskDO::getRoundNo)
                .orderByAsc(MesPqcInspectionTaskDO::getId));
    }

    default List<MesPqcInspectionTaskDO> selectListByActiveOrderIds(Collection<Long> activeOrderIds) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .in(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderIds)
                .orderByAsc(MesPqcInspectionTaskDO::getActiveOrderId)
                .orderByAsc(MesPqcInspectionTaskDO::getBusinessDate)
                .orderByAsc(MesPqcInspectionTaskDO::getInspectionType)
                .orderByAsc(MesPqcInspectionTaskDO::getRoundNo)
                .orderByAsc(MesPqcInspectionTaskDO::getId));
    }

    default List<MesPqcInspectionTaskDO> selectListByActiveOrderIdsAndStatuses(
            Collection<Long> activeOrderIds, Collection<String> taskStatuses) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()
                || taskStatuses == null || taskStatuses.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .in(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderIds)
                .in(MesPqcInspectionTaskDO::getTaskStatus, taskStatuses)
                .orderByAsc(MesPqcInspectionTaskDO::getBusinessDate)
                .orderByAsc(MesPqcInspectionTaskDO::getInspectionType)
                .orderByAsc(MesPqcInspectionTaskDO::getRoundNo)
                .orderByAsc(MesPqcInspectionTaskDO::getId));
    }

    default Set<Long> selectActiveOrderIdsByTaskStatus(Collection<Long> activeOrderIds, String taskStatus) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return Set.of();
        }
        return selectList(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .in(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderIds)
                .eq(MesPqcInspectionTaskDO::getTaskStatus, taskStatus))
                .stream()
                .map(MesPqcInspectionTaskDO::getActiveOrderId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    default MesPqcInspectionTaskDO selectByIdentity(Long activeOrderId, Long routeProcessId,
                                                    String inspectionType, LocalDate businessDate,
                                                    String shiftCode, Integer roundNo) {
        return selectOne(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .eq(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderId)
                .eq(MesPqcInspectionTaskDO::getRouteProcessId, routeProcessId)
                .eq(MesPqcInspectionTaskDO::getInspectionType, inspectionType)
                .eq(MesPqcInspectionTaskDO::getBusinessDate, businessDate)
                .eq(MesPqcInspectionTaskDO::getShiftCode, shiftCode)
                .eq(MesPqcInspectionTaskDO::getRoundNo, roundNo));
    }

    default MesPqcInspectionTaskDO selectByQaIdentity(Long activeOrderId, Long regulationVersionId,
                                                      Long qaProcessId, String qaItemCode,
                                                      String inspectionRuleKey,
                                                      LocalDate businessDate) {
        return selectOne(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .eq(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderId)
                .eq(MesPqcInspectionTaskDO::getRegulationVersionId, regulationVersionId)
                .eq(MesPqcInspectionTaskDO::getQaProcessId, qaProcessId)
                .eq(MesPqcInspectionTaskDO::getQaItemCode, qaItemCode)
                .eq(MesPqcInspectionTaskDO::getInspectionRuleKey, inspectionRuleKey)
                .eq(MesPqcInspectionTaskDO::getBusinessDate, businessDate));
    }

    default MesPqcInspectionTaskDO selectPendingByQaOverlayIdentity(Long activeOrderId, Long regulationVersionId,
                                                                    Long qaProcessId, String inspectionRuleKey) {
        return selectOne(new LambdaQueryWrapperX<MesPqcInspectionTaskDO>()
                .eq(MesPqcInspectionTaskDO::getActiveOrderId, activeOrderId)
                .eq(MesPqcInspectionTaskDO::getRegulationVersionId, regulationVersionId)
                .eq(MesPqcInspectionTaskDO::getQaProcessId, qaProcessId)
                .eq(MesPqcInspectionTaskDO::getInspectionRuleKey, inspectionRuleKey)
                .eq(MesPqcInspectionTaskDO::getTaskStatus, MesPqcInspectionTaskDO.TASK_STATUS_PENDING)
                .orderByAsc(MesPqcInspectionTaskDO::getBusinessDate)
                .orderByAsc(MesPqcInspectionTaskDO::getInspectionType)
                .orderByAsc(MesPqcInspectionTaskDO::getShiftCode)
                .orderByAsc(MesPqcInspectionTaskDO::getRoundNo)
                .orderByAsc(MesPqcInspectionTaskDO::getId)
                .last("LIMIT 1"));
    }

    default int updateSubmittedIfPending(Long id, Integer actualInspectionQuantity,
                                         String submittedContentHash,
                                         String pendingStatus, String submittedStatus) {
        return update(null, new LambdaUpdateWrapper<MesPqcInspectionTaskDO>()
                .set(MesPqcInspectionTaskDO::getActualInspectionQuantity, actualInspectionQuantity)
                .set(MesPqcInspectionTaskDO::getSubmittedContentHash, submittedContentHash)
                .set(MesPqcInspectionTaskDO::getTaskStatus, submittedStatus)
                .eq(MesPqcInspectionTaskDO::getId, id)
                .eq(MesPqcInspectionTaskDO::getTaskStatus, pendingStatus));
    }

    default int updateSubmittedEventId(Long id, Long submittedEventId) {
        return update(null, new LambdaUpdateWrapper<MesPqcInspectionTaskDO>()
                .set(MesPqcInspectionTaskDO::getSubmittedEventId, submittedEventId)
                .eq(MesPqcInspectionTaskDO::getId, id)
                .eq(MesPqcInspectionTaskDO::getTaskStatus, MesPqcInspectionTaskDO.TASK_STATUS_SUBMITTED)
                .isNull(MesPqcInspectionTaskDO::getSubmittedEventId));
    }

    default int updateConfirmedIfSubmitted(Long id, String submittedStatus, String confirmedStatus) {
        return update(null, new LambdaUpdateWrapper<MesPqcInspectionTaskDO>()
                .set(MesPqcInspectionTaskDO::getTaskStatus, confirmedStatus)
                .eq(MesPqcInspectionTaskDO::getId, id)
                .eq(MesPqcInspectionTaskDO::getTaskStatus, submittedStatus));
    }
}
