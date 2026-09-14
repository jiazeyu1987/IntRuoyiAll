package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionErrorCodeConstants.PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskErrorCodeConstants.PRO_EDHR_WORK_TASK_STATUS_INVALID;

@Service
public class MesProEdhrPreReleaseEditabilityService {

    public static final String PRE_RELEASE_EDITABLE_REASON = "\u5173\u95ed\u524d\u53ef\u4fee\u6539\uff0c\u91cd\u65b0\u63d0\u4ea4\u5c06\u66f4\u65b0\u63d0\u4ea4\u7b7e\u540d\u8bc1\u636e";

    private static final int EXECUTION_STATUS_FILL_COMPLETED = MesProEdhrApprovalStatusMapping.EXECUTION_STATUS_FILL_COMPLETED;
    private static final Set<Integer> TERMINAL_BATCH_STATUSES = Set.of(
            MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED,
            MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED,
            MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED,
            MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_VOIDED);
    private static final Set<String> HISTORICAL_FILL_STATUSES = Set.of(
            MesProEdhrWorkTaskStatus.TODO,
            MesProEdhrWorkTaskStatus.DOING,
            MesProEdhrWorkTaskStatus.OVERDUE,
            MesProEdhrWorkTaskStatus.DONE);

    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Resource
    private MesProEdhrGoldenFingerPermissionService goldenFingerPermissionService;

    public MesProEdhrPreReleaseEditability requireSubmittedOrdinaryEditable(MesProBatchRecordExecutionDO execution,
                                                                            Long workTaskId) {
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        if (!Objects.equals(execution.getStatus(), EXECUTION_STATUS_FILL_COMPLETED)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        MesProEdhrBatchExecutionTaskDO batchTask = requireRouteFormBatchTask(execution.getId());
        MesProEdhrBatchExecutionDO batch = requireMutableBatch(batchTask.getBatchExecutionId());
        MesProEdhrWorkTaskDO workTask = validateHistoricalFillTask(workTaskId, execution.getId(), batchTask);
        return new MesProEdhrPreReleaseEditability(true, PRE_RELEASE_EDITABLE_REASON, batch, batchTask, workTask);
    }

    public MesProEdhrPreReleaseEditability requireSubmittedOrdinaryGoldenFingerEditable(
            MesProBatchRecordExecutionDO execution, Long workTaskId) {
        if (execution == null) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_NOT_EXISTS);
        }
        if (!Objects.equals(execution.getStatus(), EXECUTION_STATUS_FILL_COMPLETED)) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_STATUS_INVALID);
        }
        MesProEdhrBatchExecutionTaskDO batchTask = requireRouteFormBatchTask(execution.getId());
        MesProEdhrBatchExecutionDO batch = requireMutableBatch(batchTask.getBatchExecutionId());
        MesProEdhrWorkTaskDO workTask = validateHistoricalFillTaskForGoldenFinger(workTaskId, execution.getId(), batchTask);
        return new MesProEdhrPreReleaseEditability(true, PRE_RELEASE_EDITABLE_REASON, batch, batchTask, workTask);
    }

    public MesProEdhrPreReleaseEditability resolveSubmittedOrdinaryEditableForCurrentUser(
            MesProBatchRecordExecutionDO execution) {
        if (execution == null || !Objects.equals(execution.getStatus(), EXECUTION_STATUS_FILL_COMPLETED)) {
            return MesProEdhrPreReleaseEditability.locked(null);
        }
        MesProEdhrBatchExecutionTaskDO batchTask = selectRouteFormBatchTask(execution.getId());
        if (batchTask == null) {
            return MesProEdhrPreReleaseEditability.locked(null);
        }
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchTask.getBatchExecutionId());
        if (batch == null || TERMINAL_BATCH_STATUSES.contains(batch.getStatus())) {
            return MesProEdhrPreReleaseEditability.locked(null);
        }
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        boolean goldenFingerMode = hasGoldenFingerActionBypass(currentUserId);
        MesProEdhrWorkTaskDO workTask = goldenFingerMode
                ? selectHistoricalFillTaskForGoldenFinger(execution.getId(), batchTask.getId())
                : selectHistoricalFillTaskForCurrentUser(execution.getId(), batchTask.getId());
        if (workTask == null) {
            return MesProEdhrPreReleaseEditability.locked(null, batch, batchTask, null);
        }
        return new MesProEdhrPreReleaseEditability(true, PRE_RELEASE_EDITABLE_REASON, batch, batchTask, workTask);
    }

    private MesProEdhrBatchExecutionTaskDO requireRouteFormBatchTask(Long executionId) {
        MesProEdhrBatchExecutionTaskDO batchTask = selectRouteFormBatchTask(executionId);
        if (batchTask == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_TASK_NOT_EXISTS);
        }
        return batchTask;
    }

    private MesProEdhrBatchExecutionTaskDO selectRouteFormBatchTask(Long executionId) {
        MesProEdhrBatchExecutionTaskDO batchTask = batchTaskMapper.selectByExecutionId(executionId);
        if (batchTask == null
                || !Objects.equals(MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM,
                StrUtil.blankToDefault(batchTask.getNodeType(), MesProEdhrBatchExecutionServiceImpl.NODE_TYPE_ROUTE_FORM))) {
            return null;
        }
        return batchTask;
    }

    private MesProEdhrBatchExecutionDO requireMutableBatch(Long batchExecutionId) {
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
        if (batch == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        if (TERMINAL_BATCH_STATUSES.contains(batch.getStatus())) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_STATUS_INVALID);
        }
        return batch;
    }

    private MesProEdhrWorkTaskDO validateHistoricalFillTask(Long workTaskId, Long executionId,
                                                            MesProEdhrBatchExecutionTaskDO batchTask) {
        if (workTaskId == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!Objects.equals(workTask.getExecutionId(), executionId)
                || !Objects.equals(workTask.getBatchExecutionId(), batchTask.getBatchExecutionId())
                || !Objects.equals(workTask.getBatchTaskId(), batchTask.getId())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "\u975e\u5f53\u524d\u666e\u901a\u8868\u5355\u586b\u5199\u4efb\u52a1\uff0c\u7981\u6b62\u5199\u5165");
        }
        boolean fillTaskType = Objects.equals(workTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                || Objects.equals(workTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_REWORK);
        if (!fillTaskType || !HISTORICAL_FILL_STATUSES.contains(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        if (!isAssignedOrCandidate(workTask, requireLoginUserId())) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        return workTask;
    }

    private MesProEdhrWorkTaskDO validateHistoricalFillTaskForGoldenFinger(Long workTaskId, Long executionId,
                                                                           MesProEdhrBatchExecutionTaskDO batchTask) {
        if (workTaskId == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        MesProEdhrWorkTaskDO workTask = workTaskMapper.selectById(workTaskId);
        if (workTask == null) {
            throw exception(PRO_EDHR_WORK_TASK_NOT_EXISTS);
        }
        if (!Objects.equals(workTask.getExecutionId(), executionId)
                || !Objects.equals(workTask.getBatchExecutionId(), batchTask.getBatchExecutionId())
                || !Objects.equals(workTask.getBatchTaskId(), batchTask.getId())) {
            throw exception(PRO_BATCH_RECORD_EXECUTION_WRITE_TASK_INVALID, "\u975e\u5f53\u524d\u666e\u901a\u8868\u5355\u586b\u5199\u4efb\u52a1\uff0c\u7981\u6b62\u5199\u5165");
        }
        boolean fillTaskType = Objects.equals(workTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_FILL)
                || Objects.equals(workTask.getTaskType(), MesProEdhrWorkTaskService.TASK_TYPE_REWORK);
        if (!fillTaskType || !HISTORICAL_FILL_STATUSES.contains(workTask.getStatus())) {
            throw exception(PRO_EDHR_WORK_TASK_STATUS_INVALID);
        }
        return workTask;
    }

    private MesProEdhrWorkTaskDO selectHistoricalFillTaskForCurrentUser(Long executionId, Long batchTaskId) {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (currentUserId == null || currentUserId <= 0) {
            return null;
        }
        List<MesProEdhrWorkTaskDO> workTasks = workTaskMapper.selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getBatchTaskId, batchTaskId)
                .in(MesProEdhrWorkTaskDO::getTaskType,
                        MesProEdhrWorkTaskService.TASK_TYPE_FILL,
                        MesProEdhrWorkTaskService.TASK_TYPE_REWORK)
                .in(MesProEdhrWorkTaskDO::getStatus, HISTORICAL_FILL_STATUSES)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
        return workTasks.stream()
                .filter(workTask -> isAssignedOrCandidate(workTask, currentUserId))
                .findFirst()
                .orElse(null);
    }

    private MesProEdhrWorkTaskDO selectHistoricalFillTaskForGoldenFinger(Long executionId, Long batchTaskId) {
        List<MesProEdhrWorkTaskDO> workTasks = workTaskMapper.selectList(new LambdaQueryWrapperX<MesProEdhrWorkTaskDO>()
                .eq(MesProEdhrWorkTaskDO::getExecutionId, executionId)
                .eq(MesProEdhrWorkTaskDO::getBatchTaskId, batchTaskId)
                .in(MesProEdhrWorkTaskDO::getTaskType,
                        MesProEdhrWorkTaskService.TASK_TYPE_FILL,
                        MesProEdhrWorkTaskService.TASK_TYPE_REWORK)
                .in(MesProEdhrWorkTaskDO::getStatus, HISTORICAL_FILL_STATUSES)
                .orderByDesc(MesProEdhrWorkTaskDO::getId));
        return workTasks.stream().findFirst().orElse(null);
    }

    private boolean hasGoldenFingerActionBypass(Long userId) {
        return goldenFingerPermissionService != null
                && goldenFingerPermissionService.hasGoldenFingerPermission(userId);
    }

    private Long requireLoginUserId() {
        Long currentUserId = SecurityFrameworkUtils.getLoginUserId();
        if (currentUserId == null || currentUserId <= 0) {
            throw exception(PRO_EDHR_WORK_TASK_ASSIGNEE_MISMATCH);
        }
        return currentUserId;
    }

    private boolean isAssignedOrCandidate(MesProEdhrWorkTaskDO workTask, Long userId) {
        return MesProEdhrWorkTaskAuthorization.isAssignedOrCandidate(workTask, userId);
    }

    public record MesProEdhrPreReleaseEditability(boolean editable,
                                                  String reason,
                                                  MesProEdhrBatchExecutionDO batch,
                                                  MesProEdhrBatchExecutionTaskDO batchTask,
                                                  MesProEdhrWorkTaskDO workTask) {

        private static MesProEdhrPreReleaseEditability locked(String reason) {
            return locked(reason, null, null, null);
        }

        private static MesProEdhrPreReleaseEditability locked(String reason,
                                                              MesProEdhrBatchExecutionDO batch,
                                                              MesProEdhrBatchExecutionTaskDO batchTask,
                                                              MesProEdhrWorkTaskDO workTask) {
            return new MesProEdhrPreReleaseEditability(false, reason, batch, batchTask, workTask);
        }
    }
}
