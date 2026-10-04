package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID;

/** Consumes the frozen obsolete process roster in the actual domain effect transaction. */
@Service
public class DccWorkflowObsoleteEvidenceGuard {
    @Resource private DccControlledFileTaskAssigneeSnapshotMapper taskSnapshotMapper;
    @Resource private DccControlledFileSignatureMapper signatureMapper;
    @Resource private DccElectronicSignatureManagementService signatureManagementService;
    @Resource private BpmTaskService bpmTaskService;

    public Long require(DccControlledFileDO file,String round,Map<String,Object> variables) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var obligationMap=map(variables.get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_DCC_TASK_OBLIGATION_IDS));
        var startMap=map(variables.get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES));
        var approvalMap=map(variables.get(BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES));
        var obligationIds=strings(obligationMap.get("MATRIX_REVIEW"));
        var leaders=accounts(startMap.get("MATRIX_REVIEW"));
        var approvers=accounts(approvalMap.get("MATRIX_APPROVAL"));
        Long finalApprover=accounts(Collections.singletonList(variables.get(
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_LAST_APPROVER_USER_ID))).get(0);
        if(!approvers.contains(finalApprover)) missing();
        if(obligationIds.size()!=leaders.size() || new HashSet<>(obligationIds).size()!=obligationIds.size()) missing();
        var snapshots=taskSnapshotMapper.selectListByControlledFileId(file.getId());
        var signatures=signatureMapper.selectListByControlledFileId(file.getId());
        if(snapshots==null || signatures==null) missing();
        var current=snapshots.stream().filter(Objects::nonNull)
                .filter(row->Objects.equals(file.getId(),row.getControlledFileId()) && Objects.equals(tenant,row.getTenantId())
                    && round.equals(row.getProcessInstanceId()) && "MATRIX_REVIEW".equals(row.getStageCode())).toList();
        if(current.size()!=obligationIds.size() || !new HashSet<>(obligationIds).equals(current.stream()
                .map(DccControlledFileTaskAssigneeSnapshotDO::getObligationId).collect(java.util.stream.Collectors.toSet()))) missing();
        Set<String> signedTasks=new HashSet<>();
        for(var row:current) {
            int index=obligationIds.indexOf(row.getObligationId());
            if(!Objects.equals(row.getLeaderUserId(),leaders.get(index)) || row.getAssignedTime()==null
                    || row.getAssignmentSignatureId()==null || row.getBpmTaskId()==null || !signedTasks.add(row.getBpmTaskId())) missing();
            var assignment=signatures.stream().filter(Objects::nonNull).filter(sig->Objects.equals(sig.getId(),row.getAssignmentSignatureId()))
                    .filter(sig->validIdentity(sig,file,round,row.getBpmTaskId(),row.getLeaderUserId(),"ASSIGN","MATRIX_REVIEW_ASSIGN"))
                    .findFirst().orElseThrow(()->exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING));
            var approval=signatures.stream().filter(Objects::nonNull)
                    .filter(sig->validIdentity(sig,file,round,row.getBpmTaskId(),row.getAssigneeUserId(),"APPROVE","MATRIX_REVIEW_APPROVE"))
                    .findFirst().orElseThrow(()->exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING));
            requireFinishedTask(approval,round,"MATRIX_REVIEW");
            verify(assignment);verify(approval);
        }
        var approval=signatures.stream().filter(Objects::nonNull)
                .filter(sig->finalApprover.equals(sig.getActorId())
                    && validIdentity(sig,file,round,sig.getTaskId(),sig.getActorId(),"APPROVE","MATRIX_APPROVAL_APPROVE"))
                .findFirst().orElseThrow(()->exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING));
        requireFinishedTask(approval,round,"MATRIX_APPROVAL");
        verify(approval);
        return approval.getActorId();
    }
    private boolean validIdentity(DccControlledFileSignatureDO signature,DccControlledFileDO file,String round,String task,
            Long actor,String action,String meaning) {
        return signature.getId()!=null && Objects.equals(file.getId(),signature.getControlledFileId())
                && Objects.equals(file.getId(),signature.getRevisionId()) && Objects.equals(file.getVersionNo(),signature.getVersionNo())
                && "v4-workflow".equals(signature.getEvidencePayloadVersion()) && round.equals(signature.getProcessInstanceId())
                && task!=null && !task.isBlank() && task.equals(signature.getTaskId()) && actor!=null && actor.equals(signature.getActorId())
                && action.equals(signature.getActionType()) && meaning.equals(signature.getMeaningCode())
                && Boolean.TRUE.equals(signature.getPasswordVerified()) && signature.getSignedAt()!=null
                && signature.getEvidenceHash()!=null && !signature.getEvidenceHash().isBlank() && "VALID".equals(signature.getEvidenceStatus());
    }
    private void requireFinishedTask(DccControlledFileSignatureDO signature,String round,String stage) {
        var task=bpmTaskService.getHistoricTask(signature.getTaskId());
        var local=task==null ? null:task.getTaskLocalVariables();
        if(task==null || !round.equals(task.getProcessInstanceId())
                || !String.valueOf(TenantContextHolder.getRequiredTenantId()).equals(task.getTenantId())
                || !stage.equals(task.getTaskDefinitionKey()) || !String.valueOf(signature.getActorId()).equals(task.getAssignee())
                || task.getEndTime()==null || local==null || !Integer.valueOf(2).equals(local.get(BpmnVariableConstants.TASK_VARIABLE_STATUS))) missing();
    }
    private void verify(DccControlledFileSignatureDO signature) {
        var verified=signatureManagementService.verifySignatureEvidence(signature.getId());
        if(verified==null || !Objects.equals(signature.getId(),verified.getSignatureId()) || !"VALID".equals(verified.getVerificationStatus()))
            throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_INVALID);
    }
    private Map<?,?> map(Object value) {
        if(!(value instanceof Map<?,?>)) missing();
        return (Map<?,?>)value;
    }
    private List<String> strings(Object value) {
        if(!(value instanceof List<?> list) || list.isEmpty()) { missing();return List.of(); }
        var result=new ArrayList<String>();
        for(Object id:list) { if(!(id instanceof String text) || text.isBlank()) missing();result.add((String)id); }
        return result;
    }
    private List<Long> accounts(Object value) {
        if(!(value instanceof List<?> list) || list.isEmpty()) { missing();return List.of(); }
        var result=new ArrayList<Long>();
        for(Object id:list) {
            String text=String.valueOf(id);if(!text.matches("[1-9][0-9]*")) missing();
            result.add(Long.parseLong(text));
        }
        return result;
    }
    private void missing() { throw exception(CONTROLLED_FILE_SIGNATURE_EVIDENCE_MISSING); }
}
