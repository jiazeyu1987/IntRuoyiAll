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
    @org.apache.ibatis.annotations.Select("SELECT * FROM mes_pro_edhr_work_task WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 FOR UPDATE")
    MesProEdhrWorkTaskDO selectByIdAndTenantForUpdate(@org.apache.ibatis.annotations.Param("id") Long id,
            @org.apache.ibatis.annotations.Param("tenantId") Long tenantId);

    List<String> APPROVAL_CENTER_TASK_TYPES = List.of("REVIEW", "APPROVE", "RELEASE_APPROVE");
    List<String> PRODUCTION_RELEASE_REPORT_NODE_TYPES = List.of(
            "INCOMING_INSPECTION_REPORT",
            "STERILIZATION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_REPORT",
            "FINISHED_PRODUCT_INSPECTION_RECORD");
    String TASK_TYPE_ARCHIVE = "ARCHIVE";
    String TERMINAL_BATCH_STATUS_SQL = "30, 40, 50, 60";
    String ARCHIVE_TODO_EXCLUDED_BATCH_STATUS_SQL = "40, 50, 60";
    default PageResult<MesProEdhrWorkTaskDO> selectMyPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                          Long assigneeUserId,
                                                          String status) {
        return selectPage(reqVO, applyOpenWorkTaskBatchVisibility(
                baseMyWrapper(reqVO, assigneeUserId, true), reqVO.getTaskType())
                .eq(MesProEdhrWorkTaskDO::getStatus, status)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default PageResult<MesProEdhrWorkTaskDO> selectDonePage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                            Long assigneeUserId) {
        return selectPage(reqVO, applyDoneTaskVisibility(baseMyFilterWrapper(reqVO), assigneeUserId)
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
        LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper = baseMyFilterWrapper(reqVO);
        wrapper.in(MesProEdhrWorkTaskDO::getTaskType, "REVIEW", "APPROVE", "RELEASE_APPROVE", "PQC_PRODUCTION_RELEASE");
        return selectPage(reqVO, applyDoneTaskVisibility(wrapper, assigneeUserId)
                .eq(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.DONE)
                .orderByDesc(MesProEdhrWorkTaskDO::getCompletedAt)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
    }

    default Long countMy(Long assigneeUserId, String taskType, String status) {
        boolean includeProcessFormCandidates = MesProEdhrWorkTaskStatus.TODO.equals(status)
                || MesProEdhrWorkTaskStatus.OVERDUE.equals(status);
        LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper = new LambdaQueryWrapperX<>();
        if (MesProEdhrWorkTaskStatus.DONE.equals(status)) {
            applyDoneTaskVisibility(wrapper, assigneeUserId);
        } else {
            applyMyTaskVisibility(wrapper, assigneeUserId, includeProcessFormCandidates);
        }
        wrapper
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, taskType)
                .eqIfPresent(MesProEdhrWorkTaskDO::getStatus, status);
        if (MesProEdhrWorkTaskStatus.TODO.equals(status) || MesProEdhrWorkTaskStatus.OVERDUE.equals(status)) {
            applyOpenWorkTaskBatchVisibility(wrapper, taskType);
        }
        return selectCount(wrapper);
    }

    default PageResult<MesProEdhrWorkTaskDO> selectCandidateTodoPage(MesProEdhrWorkTaskPageReqVO reqVO,
                                                                     Long candidateUserId,
                                                                     String status) {
        LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper = applyOpenWorkTaskBatchVisibility(
                new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getStatus, status)
                .eqIfPresent(MesProEdhrWorkTaskDO::getTaskType, reqVO.getTaskType())
                .eqIfPresent(MesProEdhrWorkTaskDO::getBatchExecutionId, reqVO.getBatchExecutionId())
                .likeIfPresent(MesProEdhrWorkTaskDO::getWorkOrderCode, reqVO.getWorkOrderCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getBatchCode, reqVO.getBatchCode())
                .likeIfPresent(MesProEdhrWorkTaskDO::getProcessName, reqVO.getProcessName()),
                reqVO.getTaskType());
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
        return applyMyTaskVisibility(baseMyFilterWrapper(reqVO), assigneeUserId, includeProcessFormCandidates);
    }

    default int completePqcQaClosureTask(Long id, Long applicationId, Long reviewId,
                                        LocalDateTime at, String closure) {
        return update(new MesProEdhrWorkTaskDO().setStatus(MesProEdhrWorkTaskStatus.DONE)
                        .setCompletedAt(at).setReason(closure)
                        .setReviewSourceType("EDHR_NONCONFORMANCE_REVIEW").setReviewSourceId(reviewId),
                new LambdaUpdateWrapper<MesProEdhrWorkTaskDO>()
                        .eq(MesProEdhrWorkTaskDO::getId, id)
                        .eq(MesProEdhrWorkTaskDO::getTaskType, "PQC_PRODUCTION_RELEASE")
                        .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "RELEASE_APPLICATION")
                        .eq(MesProEdhrWorkTaskDO::getBusinessScopeId, applicationId)
                        .in(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.TODO,
                                MesProEdhrWorkTaskStatus.DOING, MesProEdhrWorkTaskStatus.OVERDUE));
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> baseMyFilterWrapper(MesProEdhrWorkTaskPageReqVO reqVO) {
        return new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
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
        return excludeBatchStatusWrapper(wrapper, TERMINAL_BATCH_STATUS_SQL);
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> applyOpenWorkTaskBatchVisibility(
            LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper,
            String taskType) {
        String normalizedTaskType = taskType == null ? null : taskType.trim();
        if (TASK_TYPE_ARCHIVE.equals(normalizedTaskType)) {
            return excludeBatchStatusWrapper(wrapper, ARCHIVE_TODO_EXCLUDED_BATCH_STATUS_SQL);
        }
        if (normalizedTaskType != null && !normalizedTaskType.isEmpty()) {
            return excludeTerminalBatchWrapper(wrapper);
        }
        wrapper.and(query -> query
                .isNull(MesProEdhrWorkTaskDO::getBatchExecutionId)
                .or()
                .notInSql(MesProEdhrWorkTaskDO::getBatchExecutionId,
                        "SELECT id FROM mes_pro_edhr_batch_execution WHERE deleted = 0 AND status IN ("
                                + TERMINAL_BATCH_STATUS_SQL + ")")
                .or(archiveQuery -> archiveQuery
                        .eq(MesProEdhrWorkTaskDO::getTaskType, TASK_TYPE_ARCHIVE)
                        .notInSql(MesProEdhrWorkTaskDO::getBatchExecutionId,
                                "SELECT id FROM mes_pro_edhr_batch_execution WHERE deleted = 0 AND status IN ("
                                        + ARCHIVE_TODO_EXCLUDED_BATCH_STATUS_SQL + ")")));
        return wrapper;
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> applyDoneTaskVisibility(
            LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper, Long userId) {
        if (userId == null) {
            return wrapper;
        }
        // Completed PQC decisions belong to their formal decider, never an unused assigned candidate.
        wrapper.and(query -> query
                .and(owner -> owner.ne(MesProEdhrWorkTaskDO::getTaskType, "PQC_PRODUCTION_RELEASE")
                        .eq(MesProEdhrWorkTaskDO::getAssigneeUserId, userId))
                .or(pqc -> pqc.eq(MesProEdhrWorkTaskDO::getTaskType, "PQC_PRODUCTION_RELEASE")
                        .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "RELEASE_APPLICATION")
                        .eq(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.DONE)
                        .apply("EXISTS (SELECT 1 FROM mes_pro_process_pool_active_order_release_application pa"
                                + " JOIN system_users pu ON pu.id = pa.pqc_decided_by AND pu.tenant_id = pa.tenant_id"
                                + " AND pu.deleted = 0 AND pu.status = 0"
                                + " WHERE pa.id = mes_pro_edhr_work_task.business_scope_id"
                                + " AND pa.pqc_release_work_task_id = mes_pro_edhr_work_task.id"
                                + " AND pa.work_order_id = mes_pro_edhr_work_task.work_order_id"
                                + " AND pa.tenant_id = mes_pro_edhr_work_task.tenant_id AND pa.deleted = 0"
                                + " AND pa.active_order_id > 0 AND pa.batch_execution_id > 0"
                                + " AND (mes_pro_edhr_work_task.batch_execution_id IS NULL"
                                + " OR mes_pro_edhr_work_task.batch_execution_id = pa.batch_execution_id)"
                                + " AND pa.pqc_decided_by = {0} AND pa.pqc_decided_by > 0"
                                + " AND pa.pqc_decided_at = mes_pro_edhr_work_task.completed_at"
                                + " AND CAST(pa.pqc_decision AS BINARY) = CAST(mes_pro_edhr_work_task.reason AS BINARY)"
                                + " AND ((pa.pqc_decision = 'APPROVE' AND pa.application_status IN"
                                + " ('REPORT_UPLOAD_PENDING','MANAGER_RELEASE_PENDING','RELEASED','NONCONFORMANCE_REWORK','NONCONFORMANCE_VOID'))"
                                + " OR (pa.pqc_decision = 'REJECT' AND pa.application_status = 'PQC_RELEASE_REJECTED'"
                                + " AND pa.pqc_reject_reason IS NOT NULL AND TRIM(pa.pqc_reject_reason) <> ''))"
                                + " AND (pa.application_status NOT IN ('NONCONFORMANCE_REWORK','NONCONFORMANCE_VOID') OR EXISTS"
                                + " (SELECT 1 FROM mes_pro_edhr_nonconformance_review nr WHERE nr.id = pa.qa_closure_review_id"
                                + " AND nr.tenant_id = pa.tenant_id AND nr.deleted = 0 AND nr.review_status = 'closed'"
                                + " AND nr.active_order_id = pa.active_order_id AND nr.work_order_id = pa.work_order_id"
                                + " AND (nr.batch_execution_id IS NULL OR nr.batch_execution_id = pa.batch_execution_id)"
                                + " AND nr.closed_at IS NOT NULL AND nr.qa_user_id > 0"
                                + " AND ((nr.disposition = 'rework' AND pa.application_status = 'NONCONFORMANCE_REWORK')"
                                + " OR (nr.disposition = 'void' AND pa.application_status = 'NONCONFORMANCE_VOID')))))", userId))
                .or(qa -> qa.eq(MesProEdhrWorkTaskDO::getTaskType, "PQC_PRODUCTION_RELEASE")
                        .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "RELEASE_APPLICATION")
                        .eq(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.DONE)
                        .eq(MesProEdhrWorkTaskDO::getReviewSourceType, "EDHR_NONCONFORMANCE_REVIEW")
                        .apply("EXISTS (SELECT 1 FROM mes_pro_edhr_nonconformance_review nr"
                                + " JOIN mes_pro_process_pool_active_order_release_application pa ON pa.qa_closure_review_id = nr.id"
                                + " AND pa.tenant_id = nr.tenant_id AND pa.deleted = 0"
                                + " JOIN system_users qu ON qu.id = nr.qa_user_id AND qu.tenant_id = nr.tenant_id"
                                + " AND qu.deleted = 0 AND qu.status = 0"
                                + " WHERE nr.id = mes_pro_edhr_work_task.review_source_id AND nr.deleted = 0"
                                + " AND nr.tenant_id = mes_pro_edhr_work_task.tenant_id"
                                + " AND nr.review_status = 'closed' AND nr.qa_user_id = {0}"
                                + " AND pa.id = mes_pro_edhr_work_task.business_scope_id"
                                + " AND pa.pqc_release_work_task_id = mes_pro_edhr_work_task.id"
                                + " AND nr.active_order_id = pa.active_order_id AND (nr.batch_execution_id IS NULL OR nr.batch_execution_id = pa.batch_execution_id)"
                                + " AND nr.work_order_id = pa.work_order_id AND pa.work_order_id = mes_pro_edhr_work_task.work_order_id"
                                + " AND (mes_pro_edhr_work_task.batch_execution_id IS NULL OR mes_pro_edhr_work_task.batch_execution_id = pa.batch_execution_id)"
                                + " AND pa.pqc_decision IS NULL AND pa.pqc_decided_by IS NULL AND pa.pqc_decided_at IS NULL"
                                + " AND nr.closed_at = mes_pro_edhr_work_task.completed_at"
                                + " AND CAST(pa.application_status AS BINARY) = CAST(mes_pro_edhr_work_task.reason AS BINARY)"
                                + " AND ((nr.disposition = 'rework' AND pa.application_status = 'NONCONFORMANCE_REWORK')"
                                + " OR (nr.disposition = 'void' AND pa.application_status = 'NONCONFORMANCE_VOID')))", userId))
                .or(actor -> actor
                        .eq(MesProEdhrWorkTaskDO::getTaskType, "RELEASE_APPROVE")
                        .eq(MesProEdhrWorkTaskDO::getBusinessScopeType, "RELEASE_TRANSACTION")
                        .eq(MesProEdhrWorkTaskDO::getStatus, MesProEdhrWorkTaskStatus.DONE)
                        .apply("EXISTS (SELECT 1 FROM mes_pro_edhr_release_transaction rt"
                                + " WHERE rt.id = mes_pro_edhr_work_task.business_scope_id"
                                + " AND rt.batch_execution_id = mes_pro_edhr_work_task.batch_execution_id"
                                + " AND rt.work_order_id = mes_pro_edhr_work_task.work_order_id"
                                + " AND rt.tenant_id = mes_pro_edhr_work_task.tenant_id"
                                + " AND rt.deleted = 0 AND rt.release_status = 'RELEASED'"
                                + " AND rt.approved_by = {0})", userId)));
        return wrapper;
    }

    private LambdaQueryWrapperX<MesProEdhrWorkTaskDO> excludeBatchStatusWrapper(
            LambdaQueryWrapperX<MesProEdhrWorkTaskDO> wrapper,
            String batchStatusSql) {
        wrapper.and(query -> query
                .isNull(MesProEdhrWorkTaskDO::getBatchExecutionId)
                .or()
                .notInSql(MesProEdhrWorkTaskDO::getBatchExecutionId,
                        "SELECT id FROM mes_pro_edhr_batch_execution WHERE deleted = 0 AND status IN ("
                                + batchStatusSql + ")"));
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
