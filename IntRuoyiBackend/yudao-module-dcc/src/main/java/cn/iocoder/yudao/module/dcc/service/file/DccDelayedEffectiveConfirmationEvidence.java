package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/** Sole confirmation evidence consumer. The existing bound resolver owns deployed graph/round proof. */
@Service
public class DccDelayedEffectiveConfirmationEvidence {
    public static final String STAGE = "DELAYED_EFFECTIVE_CONFIRM";
    public static final String MEANING = "DELAYED_EFFECTIVE_CONFIRM_APPROVE";
    public static final String PAYLOAD = "v5-delayed-effective";
    public static final String STATUS = "PENDING_DELAYED_EFFECTIVE_CONFIRM";
    public static final String CONTRACT = "PUBLIC_MATRIX_NATIVE_V2";
    @Resource private DccControlledFileSignatureMapper signatureMapper;
    @Resource private DccElectronicSignatureManagementService signatureManagement;
    @Resource private DccBoundWorkflowContractResolver boundContracts;
    @Resource private BpmTaskService tasks;

    public record VerifiedConfirmation(LocalDateTime selectedEffectiveTime, LocalDateTime signedAt,
            Long signatureId, String processInstanceId, String taskId, Integer attributeRound, Long actorId, String evidenceHash) { }

    public VerifiedConfirmation requireConfirmed(DccControlledFileDO file) {
        return requireConfirmed(file, boundContracts.resolve(file));
    }
    public VerifiedConfirmation requireConfirmed(DccControlledFileDO file, DccBoundWorkflowContractResolver.BoundExecution bound) {
        requireIdentity(file, bound);
        var signature = requireProjection(file, bound, file.getDelayedEffectiveSignatureId());
        if (!Objects.equals(signature.getSelectedEffectiveTime(), file.getDelayedEffectiveTime())
                || !Objects.equals(signature.getSignedAt(), file.getDelayedEffectiveConfirmedAt())
                || !Objects.equals(signature.getProcessInstanceId(), file.getDelayedEffectiveProcessInstanceId()))
            throw invalid("本轮延迟确认文件投影与不可变签名不一致");
        // During synchronous End the historic end fact may already exist; otherwise accept only the
        // exact engine-approved task local status plus the official DCC signed-action authorization.
        var historic = tasks.getHistoricTask(signature.getTaskId());
        if (historic != null && historic.getEndTime() != null) {
            if (!sameTask(signature, bound, historic.getId(), historic.getTenantId(), historic.getProcessInstanceId(),
                    historic.getProcessDefinitionId(), historic.getTaskDefinitionKey(), historic.getAssignee())
                    || !approved(historic.getTaskLocalVariables())) throw invalid("延迟确认历史任务缺少同轮真实完成事实");
        } else {
            var current = tasks.getTask(signature.getTaskId());
            if (current == null || !sameTask(signature, bound, current.getId(), current.getTenantId(), current.getProcessInstanceId(),
                    current.getProcessDefinitionId(), current.getTaskDefinitionKey(), current.getAssignee())
                    || current.isSuspended() || !approved(current.getTaskLocalVariables())
                    || !signedAuthorization(current.getTaskLocalVariables(), signature))
                throw invalid("延迟确认缺少真实结束命令或已完成历史任务");
        }
        return result(signature);
    }

    /** Confirmation command only, before task completion; never sufficient evidence to control a file. */
    public VerifiedConfirmation requireSignedForActiveTask(DccControlledFileDO file,
            DccBoundWorkflowContractResolver.BoundExecution bound, Long signatureId) {
        requireIdentity(file, bound);
        var signature = requireProjection(file, bound, signatureId);
        var current = tasks.getTask(signature.getTaskId());
        if (current == null || current.isSuspended() || !sameTask(signature, bound, current.getId(), current.getTenantId(),
                current.getProcessInstanceId(), current.getProcessDefinitionId(), current.getTaskDefinitionKey(), current.getAssignee()))
            throw invalid("延迟确认签名不属于本轮活动原编制人任务");
        return result(signature);
    }

    private void requireIdentity(DccControlledFileDO file, DccBoundWorkflowContractResolver.BoundExecution bound) {
        if (file == null || file.getId() == null || file.getRequesterId() == null || file.getRequesterId() <= 0
                || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId()) || Boolean.TRUE.equals(file.getDeleted())
                || !"DELAYED".equals(file.getEffectiveMode()) || file.getEffectiveDate() != null || bound == null
                || !Objects.equals(bound.tenantId(), file.getTenantId()) || !Objects.equals(bound.controlledFileId(), file.getId())
                || !Objects.equals(bound.processInstanceId(), file.getProcessInstanceId()) || !Objects.equals(bound.processKey(), file.getProcessDefinitionKey())
                || !CONTRACT.equals(bound.executionContract())
                || bound.flowRule() != DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V6_DELAYED_EFFECTIVE_CONFIRM
                || bound.attributeRound() == null || bound.attributeRound() <= 0 || bound.processDefinitionId() == null
                || bound.definitionDigest() == null || !bound.definitionDigest().matches("[a-f0-9]{64}"))
            throw invalid("延迟确认需要本租户精确新图、文件版本和申请轮次");
    }
    private DccControlledFileSignatureDO requireProjection(DccControlledFileDO file,
            DccBoundWorkflowContractResolver.BoundExecution bound, Long signatureId) {
        var s = signatureId == null ? null : signatureMapper.selectById(signatureId);
        if (s == null || Boolean.TRUE.equals(s.getDeleted()) || !Objects.equals(s.getId(), signatureId)
                || !Objects.equals(s.getControlledFileId(), file.getId()) || !Objects.equals(s.getRevisionId(), file.getId())
                || !Objects.equals(s.getVersionNo(), file.getVersionNo()) || !Objects.equals(s.getActorId(), file.getRequesterId())
                || !Objects.equals(s.getProcessInstanceId(), bound.processInstanceId()) || !Objects.equals(s.getAttributeRound(), bound.attributeRound())
                || !"APPROVE".equals(s.getActionType()) || !MEANING.equals(s.getMeaningCode()) || !PAYLOAD.equals(s.getEvidencePayloadVersion())
                || !Boolean.TRUE.equals(s.getPasswordVerified()) || !"VALID".equals(s.getEvidenceStatus())
                || s.getEvidenceHash() == null || !s.getEvidenceHash().matches("[a-f0-9]{64}") || s.getTaskId() == null || s.getTaskId().isBlank()
                || s.getComment() == null || s.getComment().isBlank() || s.getSignedAt() == null || s.getSelectedEffectiveTime() == null
                || s.getSelectedEffectiveTime().getNano() != 0 || s.getSelectedEffectiveTime().isBefore(s.getSignedAt()))
            throw invalid("延迟确认缺少原编制人同版本同轮真实有效签名或所选时间早于签名");
        var checked = signatureManagement.verifySignatureEvidence(signatureId);
        if (checked == null || !Objects.equals(checked.getSignatureId(), signatureId) || !"VALID".equals(checked.getVerificationStatus()))
            throw invalid("延迟确认签名HMAC或正式签名证据验证失败");
        return s;
    }
    private boolean sameTask(DccControlledFileSignatureDO signature, DccBoundWorkflowContractResolver.BoundExecution bound,
            String id, String tenant, String process, String definition, String stage, String actor) {
        return Objects.equals(signature.getTaskId(), id) && String.valueOf(bound.tenantId()).equals(tenant)
                && Objects.equals(bound.processInstanceId(), process) && Objects.equals(bound.processDefinitionId(), definition)
                && STAGE.equals(stage) && String.valueOf(signature.getActorId()).equals(actor);
    }
    private boolean approved(Map<String, Object> local) {
        return local != null && Objects.equals(BpmTaskStatusEnum.APPROVE.getStatus(), local.get(BpmnVariableConstants.TASK_VARIABLE_STATUS));
    }
    private boolean signedAuthorization(Map<String, Object> local, DccControlledFileSignatureDO s) {
        return local != null && local.get("dccVerifiedSignatureId") instanceof Number id && id.longValue() == s.getId()
                && String.valueOf(s.getActorId()).equals(String.valueOf(local.get("dccVerifiedSignatureActorId")))
                && "APPROVE".equals(local.get("dccVerifiedSignatureAction"));
    }
    private VerifiedConfirmation result(DccControlledFileSignatureDO s) {
        return new VerifiedConfirmation(s.getSelectedEffectiveTime(), s.getSignedAt(), s.getId(), s.getProcessInstanceId(),
                s.getTaskId(), s.getAttributeRound(), s.getActorId(), s.getEvidenceHash());
    }
    private IllegalStateException invalid(String message) { return new IllegalStateException(message); }
}
