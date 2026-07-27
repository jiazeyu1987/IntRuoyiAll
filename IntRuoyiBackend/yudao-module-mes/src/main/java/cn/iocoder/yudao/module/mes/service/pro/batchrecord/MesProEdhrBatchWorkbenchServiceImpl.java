package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordDomainTraceSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS;

@Service
public class MesProEdhrBatchWorkbenchServiceImpl implements MesProEdhrBatchWorkbenchService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Resource
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Resource
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Resource
    private MesProEdhrOperationAuditEventMapper operationAuditEventMapper;
    @Resource
    private MesProBatchRecordExecutionFieldAuditBatchMapper fieldAuditBatchMapper;
    @Resource
    private MesProBatchRecordDomainTraceSnapshotMapper domainTraceSnapshotMapper;
    @Resource
    private MesProEdhrBatchStageResolver batchStageResolver;
    @Resource
    private MesProEdhrBatchExecutionVisibilityService batchExecutionVisibilityService;

    @Override
    public EdhrBatchWorkbenchRespVO getWorkbench(Long batchExecutionId) {
        MesProEdhrBatchExecutionDO batch = batchExecutionMapper.selectById(batchExecutionId);
        if (batch == null) {
            throw exception(PRO_EDHR_BATCH_EXECUTION_NOT_EXISTS);
        }
        List<MesProEdhrBatchExecutionTaskDO> tasks = batchTaskMapper.selectListByBatchExecutionId(batchExecutionId);
        batchExecutionVisibilityService.requireVisibleBatch(batch, tasks, currentUserId());
        MesProEdhrReleaseTransactionDO releaseTransaction = releaseTransactionMapper.selectByBatchExecutionId(batchExecutionId);
        EdhrBatchWorkbenchRespVO.WorkbenchTaskSummary taskSummary = buildTaskSummary(tasks);
        EdhrBatchWorkbenchRespVO resolved = batchStageResolver.resolve(batch, tasks, releaseTransaction, taskSummary);
        return resolved
                .setBatchExecutionId(batch.getId())
                .setBatchExecutionCode(batch.getBatchExecutionCode())
                .setWorkOrderCode(batch.getWorkOrderCode())
                .setBatchCode(batch.getBatchCode())
                .setProductName(batch.getProductName())
                .setProductCode(batch.getProductCode())
                .setRouteName(batch.getRouteName())
                .setRouteCode(batch.getRouteCode())
                .setBatchStatus(batch.getStatus())
                .setRequiredProgress(resolveRequiredProgress(batch))
                .setBlockedCount(batch.getBlockedCount())
                .setReleaseSummary(buildReleaseSummary(releaseTransaction))
                .setAuditSummary(buildAuditSummary(batchExecutionId));
    }

    private EdhrBatchWorkbenchRespVO.WorkbenchTaskSummary buildTaskSummary(List<MesProEdhrBatchExecutionTaskDO> tasks) {
        int approved = 0;
        int submitted = 0;
        int rework = 0;
        int blocked = 0;
        for (MesProEdhrBatchExecutionTaskDO task : tasks) {
            if (task.getStatus() == null) {
                continue;
            }
            if (task.getStatus() == MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_APPROVED) {
                approved++;
            }
            if (task.getStatus() == MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_SUBMITTED) {
                submitted++;
            }
            if (task.getStatus() == MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_REWORK_REQUIRED
                    || task.getStatus() == MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_REJECTED) {
                rework++;
            }
            if (task.getStatus() == MesProEdhrBatchExecutionServiceImpl.TASK_STATUS_BLOCKED) {
                blocked++;
            }
        }
        return new EdhrBatchWorkbenchRespVO.WorkbenchTaskSummary()
                .setTotalCount(tasks.size())
                .setApprovedCount(approved)
                .setSubmittedCount(submitted)
                .setReworkCount(rework)
                .setBlockedCount(blocked);
    }

    private EdhrBatchWorkbenchRespVO.WorkbenchReleaseSummary buildReleaseSummary(MesProEdhrReleaseTransactionDO releaseTransaction) {
        if (releaseTransaction == null) {
            return new EdhrBatchWorkbenchRespVO.WorkbenchReleaseSummary()
                    .setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_REQUIRED)
                    .setReleaseStatusLabel("待预检")
                    .setPrecheckSummary("尚未发起放行预检");
        }
        return new EdhrBatchWorkbenchRespVO.WorkbenchReleaseSummary()
                .setReleaseTransactionId(releaseTransaction.getId())
                .setReleaseStatus(releaseTransaction.getReleaseStatus())
                .setReleaseStatusLabel(resolveReleaseStatusLabel(releaseTransaction.getReleaseStatus()))
                .setBlockingCheckCount(releaseTransaction.getBlockingCheckCount())
                .setFailedCheckCount(releaseTransaction.getFailedCheckCount())
                .setPrecheckSummary(buildPrecheckSummary(releaseTransaction))
                .setLastPrecheckAt(releaseTransaction.getLastPrecheckAt() == null
                        ? null : releaseTransaction.getLastPrecheckAt().format(DATE_TIME_FORMATTER));
    }

    private EdhrBatchWorkbenchRespVO.WorkbenchAuditSummary buildAuditSummary(Long batchExecutionId) {
        MesProEdhrOperationAuditPageReqVO reqVO = new MesProEdhrOperationAuditPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(1);
        reqVO.setBatchExecutionId(batchExecutionId);
        reqVO.setObjectType("BATCH_EXECUTION");
        reqVO.setObjectId(String.valueOf(batchExecutionId));
        List<MesProEdhrOperationAuditEventDO> operationEvents = operationAuditEventMapper
                .selectPage(reqVO)
                .getList();
        MesProEdhrOperationAuditEventDO latestOperation = operationEvents.isEmpty() ? null : operationEvents.get(0);
        List<MesProBatchRecordExecutionFieldAuditBatchDO> fieldAuditBatches =
                fieldAuditBatchMapper.selectListByExecutionId(batchExecutionId);
        MesProBatchRecordExecutionFieldAuditBatchDO latestFieldAudit = fieldAuditBatches.isEmpty() ? null
                : fieldAuditBatches.get(fieldAuditBatches.size() - 1);
        List<Long> executionIds = batchTaskMapper.selectListByBatchExecutionId(batchExecutionId).stream()
                .map(MesProEdhrBatchExecutionTaskDO::getExecutionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        List<MesProBatchRecordDomainTraceSnapshotDO> domainTraceSnapshots =
                executionIds.isEmpty() ? List.of() : domainTraceSnapshotMapper.selectListByExecutionIds(executionIds);
        MesProBatchRecordDomainTraceSnapshotDO latestDomainTrace = domainTraceSnapshots.stream()
                .filter(snapshot -> snapshot.getVerifiedAt() != null)
                .max(java.util.Comparator.comparing(MesProBatchRecordDomainTraceSnapshotDO::getVerifiedAt)
                        .thenComparing(MesProBatchRecordDomainTraceSnapshotDO::getId))
                .orElse(null);
        return new EdhrBatchWorkbenchRespVO.WorkbenchAuditSummary()
                .setLatestOperationAuditId(latestOperation == null ? null : latestOperation.getId())
                .setLatestOperationAt(latestOperation == null || latestOperation.getOccurredAt() == null ? null
                        : latestOperation.getOccurredAt().format(DATE_TIME_FORMATTER))
                .setFieldAuditBatchCount((long) fieldAuditBatches.size())
                .setLatestFieldAuditAt(latestFieldAudit == null || latestFieldAudit.getChangedAt() == null ? null
                        : latestFieldAudit.getChangedAt().format(DATE_TIME_FORMATTER))
                .setLatestDomainTraceAt(latestDomainTrace == null || latestDomainTrace.getVerifiedAt() == null ? null
                        : latestDomainTrace.getVerifiedAt().format(DATE_TIME_FORMATTER));
    }

    private int resolveRequiredProgress(MesProEdhrBatchExecutionDO batch) {
        int total = batch.getTaskTotal() == null ? 0 : batch.getTaskTotal();
        int approved = batch.getTaskApprovedCount() == null ? 0 : batch.getTaskApprovedCount();
        if (total <= 0) {
            return 0;
        }
        return Math.min(100, (int) Math.round(approved * 100D / total));
    }

    private String buildPrecheckSummary(MesProEdhrReleaseTransactionDO releaseTransaction) {
        Integer blockingCount = releaseTransaction.getBlockingCheckCount();
        Integer failedCount = releaseTransaction.getFailedCheckCount();
        if ((blockingCount == null || blockingCount == 0) && (failedCount == null || failedCount == 0)) {
            return "放行预检通过";
        }
        return "阻塞 " + (blockingCount == null ? 0 : blockingCount) + " / 失败 " + (failedCount == null ? 0 : failedCount);
    }

    private String resolveReleaseStatusLabel(String releaseStatus) {
        if (MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_REQUIRED.equals(releaseStatus)) return "待预检";
        if (MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_FAILED.equals(releaseStatus)) return "预检失败";
        if (MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED.equals(releaseStatus)) return "预检通过";
        if (MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL.equals(releaseStatus)) return "待审批";
        if (MesProEdhrReleaseServiceImpl.STATUS_RELEASED.equals(releaseStatus)) return "已放行";
        if (MesProEdhrReleaseServiceImpl.STATUS_REJECTED.equals(releaseStatus)) return "已驳回";
        if (MesProEdhrReleaseServiceImpl.STATUS_WITHDRAWN.equals(releaseStatus)) return "已撤回";
        return releaseStatus == null ? "待预检" : releaseStatus;
    }

    private Long currentUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? 0L : loginUserId;
    }
}
