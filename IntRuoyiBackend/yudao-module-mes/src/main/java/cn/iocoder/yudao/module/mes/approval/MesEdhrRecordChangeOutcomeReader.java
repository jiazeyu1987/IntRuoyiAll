package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordChangeEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrRecordChangeEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrRecordChangeApprovalEvidence;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MesEdhrRecordChangeOutcomeReader {
    @Resource private MesProEdhrRecordChangeEventMapper events;
    @Resource private MesEdhrRecordChangeApprovalEvidence evidence;

    public MesEdhrApprovalOutcome read(MesProEdhrWorkTaskDO task, Long tenant) {
        require("RECORD_CHANGE_APPROVE".equals(task.getTaskType())
                && "RECORD_CHANGE".equals(task.getBusinessScopeType()) && task.getBusinessScopeId() != null,
                "任务类型或范围不符");
        var event = events.selectOne(new LambdaQueryWrapperX<MesProEdhrRecordChangeEventDO>()
                .eq(MesProEdhrRecordChangeEventDO::getId, task.getBusinessScopeId())
                .eq(MesProEdhrRecordChangeEventDO::getDeleted, false).apply("tenant_id = {0}", tenant));
        if (event == null || event.getApprovalSignatureId() == null) {
            return new MesEdhrApprovalOutcome("COMPLETED", null, event == null ? null : event.getChangeStatus(),
                    task.getStatus(), "UNKNOWN", null, null, task.getReason(), null, null, null, null, null, null);
        }
        require(Objects.equals(event.getBatchExecutionId(), task.getBatchExecutionId())
                && Objects.equals(event.getExecutionId(), task.getExecutionId())
                && Objects.equals(event.getApprovedBy(), task.getAssigneeUserId())
                && Objects.equals(event.getRequestedBy(), task.getSourceUserId())
                && Objects.equals(event.getApprovedAt(), task.getCompletedAt()), "任务与正式批准事实不一致");
        boolean negative = java.util.Set.of("REJECTED", "RETURNED", "RESUBMITTED").contains(event.getChangeStatus());
        if (negative) evidence.requireRejected(event);
        else evidence.requireApproved(event);
        return new MesEdhrApprovalOutcome("COMPLETED", negative ? ApprovalTaskReviewResult.REJECT : ApprovalTaskReviewResult.APPROVE,
                event.getChangeStatus(), task.getStatus(), negative ? "REJECT" : "APPROVE", event.getChangeType(),
                negative ? event.getDecisionReason() : null, event.getReasonText(), null, event.getApprovedBy(), event.getApprovedAt(),
                "EXISTING_RECORD_CHANGE_EVENT", event.getId().toString(), event.getApprovedBy());
    }

    private void require(boolean condition, String reason) {
        if (!condition) throw new IllegalStateException("EDHR_RECORD_CHANGE_OUTCOME_INVALID: " + reason);
    }
}
