package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrWorkTaskPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MesProEdhrWorkTaskMapper extends BaseMapperX<MesProEdhrWorkTaskDO> {

    List<String> APPROVAL_CENTER_TASK_TYPES = List.of("REVIEW", "APPROVE", "RELEASE_APPROVE");
    List<String> PRODUCTION_RELEASE_REPORT_NODE_TYPES = List.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");
    String TERMINAL_BATCH_STATUS_SQL = "30, 40, 50, 60";
    default PageResult<MesProEdhrWorkTaskDO> selectMyPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                          Long assigneeUserId,
                                                          String status) {
        return selectPage(reqVO, excludeTerminalBatchWrapper(baseMyWrapper(reqVO, assigneeUserId, true))
                .eq(MesProEdhrWorkTaskDO::getStatus, status)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default PageResult<MesProEdhrWorkTaskDO> selectDonePage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                            Long assigneeUserId) {
        return selectPage(reqVO, baseMyWrapper(reqVO, assigneeUserId, false)
                .eq(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.DONE)
                .orderByDesc(MesProEdhrWorkTaskDO::getCompletedAt)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default PageResult<MesProEdhrWorkTaskDO> selectApprovalCenterTodoPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                          Long assigneeUserId,
                                                                          String status) {
        return selectPage(reqVO, excludeTerminalBatchWrapper(baseApprovalCenterWrapper(reqVO, assigneeUserId))
                .eq(MesProEdhrWorkTaskDO::getStatus, status)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default PageResult<MesProEdhrWorkTaskDO> selectApprovalCenterDonePage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                          Long assigneeUserId) {
        return selectPage(reqVO, baseApprovalCenterWrapper(reqVO, assigneeUserId)
                .eq(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.DONE)
                .orderByDesc(MesProEdhrWorkTaskDO::getCompletedAt)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default Long countMy(Long assigneeUserId, String taskType, String status) {
        boolean includeProcessFormCandidates = MesProEdhrWorkTaskStatus.TODO.equals(status)
                || MesProEdhrWorkTaskStatus.OVERDUE.equals(status);
        LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper = applyMyTaskVisibility(
                new LambdaQueryWrapperX<>(), assigneeUserId, includeProcessFormCandidates)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .eqIfPresent(MesProEdhrWorkTaskDO::getStatus, status);
        if (MesProEdhrWorkTaskStatus.TODO.equals(status) || MesProEdhrWorkTaskStatus.OVERDUE.equals(status)) {
            excludeTerminalBatchWrapper(wrapper);
        }
        return selectCount(wrapper);
    }

    default PageResult<MesProEdhrWorkTaskDO> selectCandidateTodoPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                     Long candidateUserId,
                                                                     String status) {
        LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper = excludeTerminalBatchWrapper(
                new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getStatus, status)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(MesProEdhrWorkTaskDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .likeIfPresent(MesProEdhrWorkTaskDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getProcessName, reqVO.getProcessName()));
        applyProductionReleaseNodeTypeFilter(wrapper, reqVO.getNodeTypes());
        if (candidateUserId != null) {
            String candidateToken = "," + candidateUserId + ",";
            wrapper.apply("CONCAT(',', candidate_user_snapshot, ',') LIKE {0}", "%" + candidateToken + "%");
        }
        wrapper.orderByDesc(MesProEdhrWorkTaskDO::getId);
        return selectPage(reqVO, wrapper);
    }

    default Long countApprovalCenterTodoDuplicateTasks(MesProEdhrWorkTaskPageReqVO reqVO,
                                                       Long assigneeUserId,
                                                       Long candidateUserId,
                                                       String status) {
        LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper = excludeTerminalBatchWrapper(
                baseApprovalCenterWrapper(reqVO, assigneeUserId))
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getStatus, status);
        if (candidateUserId != null) {
            String candidateToken = "," + candidateUserId + ",";
            wrapper.apply("CONCAT(',', candidate_user_snapshot, ',') LIKE {0}", "%" + candidateToken + "%");
        }
        return selectCount(wrapper);
    }

    default List<MesProEdhrWorkTaskDO> selectTimelineListByExecutionId(Long executionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .orderByAsc(MesProEdhrWorkTaskDO::getCreateTime)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectTimelineListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrWorkTaskDO::getCreateTime)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectDueActiveTasks(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .in(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING)
                .isNotNull(MesProEdhrWorkTaskDO::getDueTime)
                .le(MesProEdhrWorkTaskDO::getDueTime, now)
                .orderByAsc(MesProEdhrWorkTaskDO::getDueTime)
                .orderByAsc(MesProEdhrWorkTaskDO::getId)
                .last("LIMIT " + limit));
    }

    default int updateToOverdueIfActive(Long id, LocalDateTime now, String reason) {
        return update(new MesProEdhrWorkTaskDO()
                        .setStatus(MesProEdhrWorkTaskStatus.OVERDUE)
                        .setOverdueAt(now)
                        .setOverdueReason(reason)
                        .setReason(reason)
                        .setRemark(reason),
                new LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, id)
                        .in(MesProEdhrWorkTaskDO::getStatus,
                                MesProEdhrWorkTaskStatus.TODO, MesProEdhrWorkTaskStatus.DOING));
    }

    default MesProEdhrWorkTaskDO selectActiveByBatchTaskAndType(Long batchTaskId, String taskType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBatchTaskId, batchTaskId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectActiveListByExecutionAndType(Long executionId, String taskType) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default MesProEdhrWorkTaskDO selectActiveByBusinessScopeAndType(String businessScopeType, Long businessScopeId,
                                                                    String taskType) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, businessScopeType)
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, businessScopeId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default MesProEdhrWorkTaskDO selectByPqcReleaseApplicationScopeId(Long applicationId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getPqcReleaseApplicationScopeId, applicationId));
    }

    default MesProEdhrWorkTaskDO selectReleaseReportByBatchTaskId(Long batchTaskId) {
        if (batchTaskId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBatchTaskId, batchTaskId)
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "RELEASE_REPORT_NODE")
                .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, batchTaskId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "FILL")
                .orderByDesc(MesProEdhrWorkTaskDO::getId)
                .last("LIMIT 1"));
    }

    @Select("SELECT * FROM mes_pro_edhr_work_task WHERE id = #{id} FOR UPDATE")
    MesProEdhrWorkTaskDO selectByIdForUpdate(@Param("id") Long id);

    default int completeReleaseReportTask(Long id, LocalDateTime completedAt) {
        return update(new MesProEdhrWorkTaskDO()
                        .setStatus(MesProEdhrWorkTaskStatus.DONE)
                        .setCompletedAt(completedAt)
                        .setReason("REPORT_COMPLETED")
                        .setRemark("production release report completed"),
                new LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, id)
                        .eq(MesProEdhrWorkTaskDO::getTaskType, "FILL")
                        .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "RELEASE_REPORT_NODE")
                        .in(MesProEdhrWorkTaskDO::getStatus,
                                MesProEdhrWorkTaskStatus.TODO,
                                MesProEdhrWorkTaskStatus.DOING,
                                MesProEdhrWorkTaskStatus.OVERDUE));
    }

    default int completeManagerReleaseTask(Long id, LocalDateTime completedAt, String opinion) {
        return update(new MesProEdhrWorkTaskDO()
                        .setStatus(MesProEdhrWorkTaskStatus.DONE)
                        .setCompletedAt(completedAt)
                        .setReason("APPROVE")
                        .setRemark(opinion),
                new LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, id)
                        .eq(MesProEdhrWorkTaskDO::getTaskType, "RELEASE_APPROVE")
                        .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "RELEASE_TRANSACTION")
                        .in(MesProEdhrWorkTaskDO::getStatus,
                                MesProEdhrWorkTaskStatus.TODO,
                                MesProEdhrWorkTaskStatus.DOING,
                                MesProEdhrWorkTaskStatus.OVERDUE));
    }

    default int completePqcDecisionTask(Long id, LocalDateTime completedAt, String decision) {
        return update(new MesProEdhrWorkTaskDO()
                        .setStatus(MesProEdhrWorkTaskStatus.DONE)
                        .setCompletedAt(completedAt)
                        .setReason(decision),
                new LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, id)
                        .eq(MesProEdhrWorkTaskDO::getTaskType, "PQC_PRODUCTION_RELEASE")
                        .in(MesProEdhrWorkTaskDO::getStatus,
                                MesProEdhrWorkTaskStatus.TODO,
                                MesProEdhrWorkTaskStatus.DOING,
                                MesProEdhrWorkTaskStatus.OVERDUE));
    }

    default List<MesProEdhrWorkTaskDO> selectActiveListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getBatchExecutionId, batchExecutionId)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectActiveFillOrReworkList() {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .in(MesProEdhrWorkTaskDO::getTaskType, "FILL", "REWORK")
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default List<MesProEdhrWorkTaskDO> selectActiveListByExecutionAndBpmTaskId(Long executionId, String bpmTaskId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getBpmTaskId, bpmTaskId)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    default MesProEdhrWorkTaskDO selectActiveReviewByExecutionAndBpmTaskId(Long executionId, String bpmTaskId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getBpmTaskId, bpmTaskId)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId))
                .stream()
                .findFirst()
                .orElse(null);
    }

    default List<MesProEdhrWorkTaskDO> selectActiveCandidatePeers(Long executionId, String signatureCellKey,
                                                                  Long excludingWorkTaskId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getTaskType, "REVIEW")
                .eq(MesProEdhrWorkTaskDO::getSignatureCellKey, signatureCellKey)
                .ne(MesProEdhrWorkTaskDO::getId, excludingWorkTaskId)
                .in(MesProEdhrWorkTaskDO::getStatus,
                        MesProEdhrWorkTaskStatus.TODO,
                        MesProEdhrWorkTaskStatus.DOING,
                        MesProEdhrWorkTaskStatus.OVERDUE)
                .orderByAsc(MesProEdhrWorkTaskDO::getId));
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> baseMyWrapper(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                    Long assigneeUserId,
                                                                    boolean includeProcessFormCandidates) {
        return applyMyTaskVisibility(new LambdaQueryWrapperX<>(), assigneeUserId, includeProcessFormCandidates)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(MesProEdhrWorkTaskDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .likeIfPresent(MesProEdhrWorkTaskDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getProcessName, reqVO.getProcessName());
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> applyMyTaskVisibility(
            LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper,
            Long userId,
            boolean includeProcessFormCandidates) {
        if (!includeProcessFormCandidates) {
            wrapper.eq(MesProEdhrWorkTaskDO::getAssigneeUserId, userId);
            return wrapper;
        }
        String candidateToken = "," + userId + ",";
        wrapper.and(query -> query
                .eq(MesProEdhrWorkTaskDO::getAssigneeUserId, userId)
                .or()
                .apply("CONCAT(',', candidate_user_snapshot, ',') LIKE {0}",
                        "%" + candidateToken + "%"));
        return wrapper;
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> baseApprovalCenterWrapper(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                                Long assigneeUserId) {
        return new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eqIfPresent(MesProEdhrWorkTaskDO::getAssigneeUserId, assigneeUserId)
                .in(MesProEdhrWorkTaskDO::getTaskType, APPROVAL_CENTER_TASK_TYPES)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(MesProEdhrWorkTaskDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .likeIfPresent(MesProEdhrWorkTaskDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getProcessName, reqVO.getProcessName());
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> excludeTerminalBatchWrapper(
            LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper) {
        wrapper.and(query -> query
                .isNull(MesProEdhrWorkTaskDO::getBatchExecutionId)
                .or()
                .notInSql(MesProEdhrWorkTaskDO::getBatchExecutionId,
                        "SELECT id FROM mes_pro_edhr_batch_execution WHERE deleted = 0 AND status IN ("
                                + TERMINAL_BATCH_STATUS_SQL + ")"));
        return wrapper;
    }

    private void applyProductionReleaseNodeTypeFilter(
            LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper, List<String> nodeTypes) {
        if (nodeTypes == null || nodeTypes.isEmpty()) {
            return;
        }
        List<String> requested = nodeTypes.stream().distinct().toList();
        if (requested.stream().anyMatch(nodeType -> !PRODUCTION_RELEASE_REPORT_NODE_TYPES.contains(nodeType))) {
            wrapper.apply("1 = 0");
            return;
        }
        String quotedNodeTypes = requested.stream()
                .map(nodeType -> "'" + nodeType + "'")
                .collect(java.util.stream.Collectors.joining(","));
        wrapper.inSql(MesProEdhrWorkTaskDO::getBatchTaskId,
                "SELECT id FROM mes_pro_edhr_batch_execution_task WHERE node_type IN (" + quotedNodeTypes + ")");
    }
}
