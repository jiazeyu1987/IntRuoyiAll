package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchWorkbenchRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MesProEdhrBatchStageResolver {

    public EdhrBatchWorkbenchRespVO resolve(MesProEdhrBatchExecutionDO batch,
                                            List<MesProEdhrBatchExecutionTaskDO> tasks,
                                            MesProEdhrReleaseTransactionDO releaseTransaction,
                                            EdhrBatchWorkbenchRespVO.WorkbenchTaskSummary taskSummary) {
        String mainStage = resolveMainStage(batch, releaseTransaction);
        return new EdhrBatchWorkbenchRespVO()
                .setMainStage(mainStage)
                .setMainStageLabel(resolveMainStageLabel(mainStage))
                .setStageOwnerRole(resolveOwnerRole(mainStage))
                .setStageBlockers(resolveStageBlockers(batch, releaseTransaction))
                .setTaskSummary(taskSummary);
    }

    public String resolveMainStageCode(MesProEdhrBatchExecutionDO batch,
                                       MesProEdhrReleaseTransactionDO releaseTransaction) {
        return resolveMainStage(batch, releaseTransaction);
    }

    public String resolveMainStageDisplayLabel(String mainStage) {
        return resolveMainStageLabel(mainStage);
    }

    public String resolveStageOwnerRole(String mainStage) {
        return resolveOwnerRole(mainStage);
    }

    public List<String> resolveStageBlockers(MesProEdhrBatchExecutionDO batch,
                                             MesProEdhrReleaseTransactionDO releaseTransaction) {
        List<String> blockers = new ArrayList<>();
        if (batch.getBlockedCount() != null && batch.getBlockedCount() > 0) {
            blockers.add("存在阻塞工序，请先处理工序门禁或返工。");
        }
        if (batch.getStatus() != null
                && batch.getStatus() == MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REWORK_REQUIRED) {
            blockers.add("存在返工工序，需重新提交并审批通过后才能继续。");
        }
        if (releaseTransaction != null && releaseTransaction.getBlockingCheckCount() != null
                && releaseTransaction.getBlockingCheckCount() > 0) {
            blockers.add("放行预检仍存在阻塞项，不能进入正式放行。");
        }
        return blockers;
    }

    private String resolveMainStage(MesProEdhrBatchExecutionDO batch, MesProEdhrReleaseTransactionDO releaseTransaction) {
        if (releaseTransaction != null) {
            String releaseStatus = releaseTransaction.getReleaseStatus();
            if (MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL.equals(releaseStatus)) {
                return "IN_RELEASE";
            }
            if (MesProEdhrReleaseServiceImpl.STATUS_RELEASED.equals(releaseStatus)) {
                return "RELEASED";
            }
            if (MesProEdhrReleaseServiceImpl.STATUS_REJECTED.equals(releaseStatus)
                    || MesProEdhrReleaseServiceImpl.STATUS_WITHDRAWN.equals(releaseStatus)) {
                return "RELEASE_REJECTED";
            }
            if ((MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_PASSED.equals(releaseStatus)
                    || MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_FAILED.equals(releaseStatus))
                    && batch.getStatus() != null
                    && batch.getStatus() == MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED) {
                return "CLOSED";
            }
        }
        if (batch.getStatus() == null) {
            return "CREATED";
        }
        return switch (batch.getStatus()) {
            case MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CREATED -> "CREATED";
            case MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_IN_PROGRESS -> "IN_FILLING";
            case MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_READY_TO_CLOSE -> "READY_TO_CLOSE";
            case MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REWORK_REQUIRED -> "REWORK_REQUIRED";
            case MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_CLOSED -> "CLOSED";
            case MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED -> "ARCHIVED";
            case MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_REJECTED -> "RELEASE_REJECTED";
            default -> "CREATED";
        };
    }

    private String resolveMainStageLabel(String mainStage) {
        return switch (mainStage) {
            case "CREATED" -> "待开始";
            case "IN_FILLING" -> "填写中";
            case "IN_REVIEW" -> "审批中";
            case "REWORK_REQUIRED" -> "待返工";
            case "READY_TO_CLOSE" -> "待关闭";
            case "CLOSED" -> "待放行";
            case "IN_RELEASE" -> "放行审批中";
            case "RELEASE_REJECTED" -> "放行退回";
            case "RELEASED" -> "已放行";
            case "ARCHIVED" -> "已归档";
            default -> "待开始";
        };
    }

    private String resolveOwnerRole(String mainStage) {
        return switch (mainStage) {
            case "CREATED", "IN_FILLING", "REWORK_REQUIRED" -> "执行人";
            case "IN_REVIEW" -> "审批人";
            case "READY_TO_CLOSE", "CLOSED" -> "批次负责人";
            case "IN_RELEASE", "RELEASE_REJECTED", "RELEASED" -> "QA/放行员";
            case "ARCHIVED" -> "归档员/审计员";
            default -> "执行人";
        };
    }
}
