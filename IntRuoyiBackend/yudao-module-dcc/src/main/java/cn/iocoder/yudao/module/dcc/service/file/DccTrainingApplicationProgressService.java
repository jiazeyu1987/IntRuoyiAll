package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.FlowableUtils;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileApplicationEvidenceMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;

/** Read-only DCC metadata for an eligible training recipient. Does not grant BPM actions or file bytes. */
@Service
public class DccTrainingApplicationProgressService {
    @Resource private DccControlledFileMapper files;
    @Resource private DccOfflineTrainingRecordService training;
    @Resource private DccControlledFileApplicationEvidenceMapper mappings;
    @Resource private DccApplicationHistoryGuard history;
    @Resource private BpmTaskService tasks;
    @Resource private DccBoundWorkflowContractResolver boundContracts;

    @Transactional(readOnly = true)
    public Progress read(Long actor, Long fileId, String round) {
        var file = files.selectById(fileId);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (actor == null || file == null || !Objects.equals(file.getId(),fileId)
                || !Objects.equals(file.getTenantId(),tenant) || Boolean.TRUE.equals(file.getDeleted())
                || round == null || round.isBlank() || !round.equals(round.strip())
                || !Objects.equals(file.getProcessInstanceId(),round) || !training.canReadProgress(actor,file))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        var matched = Objects.requireNonNull(mappings.selectApplicationRounds(tenant,file.getDccProjectCodeId(),fileId))
                .stream().filter(r -> Objects.equals(r.getControlledFileId(),fileId) && round.equals(r.getBpmRound())).toList();
        if (matched.size() != 1 || !Set.of("UPLOAD","REVISION").contains(matched.get(0).getApplicationType())
                || matched.get(0).getAttributeRound() == null || matched.get(0).getAttributeRound() < 1)
            throw new IllegalStateException("培训文件与正式申请轮次不一致");
        var mapping = matched.get(0);
        history.require(file,mapping.getApplicationType(),round);
        var flowContract = boundContracts.resolveRoundFlowContract(file,mapping);
        requireFlowContract(file,mapping,flowContract);
        var projected = Objects.requireNonNull(tasks.getTaskListByProcessInstanceId(round,true)).stream().map(task -> {
            if (task == null || task.getId() == null || task.getId().isBlank()
                    || !round.equals(task.getProcessInstanceId()) || !tenant.toString().equals(task.getTenantId())
                    || task.getTaskDefinitionKey() == null || task.getTaskDefinitionKey().isBlank()
                    || task.getTaskLocalVariables() == null
                    || !(task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS) instanceof Number status))
                throw new IllegalStateException("培训审批任务身份或状态未记录");
            return new ProgressTask(task.getId(),round,task.getTaskDefinitionKey(),status.intValue(),
                    task.getAssignee(),FlowableUtils.getTaskReason(task),
                    task.getCreateTime()==null?null:cn.iocoder.yudao.framework.common.util.date.DateUtils.of(task.getCreateTime()),
                    task.getEndTime()==null?null:cn.iocoder.yudao.framework.common.util.date.DateUtils.of(task.getEndTime()));
        }).toList();
        if (projected.isEmpty()) throw new IllegalStateException("培训申请缺少正式审批任务历史");
        return new Progress(fileId.toString(),mapping.getApplicationType(),round,mapping.getAttributeRound(),projected,flowContract);
    }

    private void requireFlowContract(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file,
            DccApplicationRoundSummary mapping,DccBoundWorkflowContractResolver.RoundFlowContract contract) {
        if(contract==null || !Objects.equals(contract.tenantId(),TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(contract.tenantId(),file.getTenantId()) || !Objects.equals(contract.controlledFileId(),file.getId())
                || !Objects.equals(contract.applicationType(),mapping.getApplicationType())
                || !Objects.equals(contract.attributeRound(),mapping.getAttributeRound()) || !Objects.equals(contract.bpmRound(),mapping.getBpmRound())
                || !Objects.equals(contract.processDefinitionKey(),file.getProcessDefinitionKey()))
            throw new IllegalStateException("DCC_TRAINING_FLOW_TUPLE_MISMATCH");
        if(contract.status()==null || contract.processDefinitionId()==null || contract.processDefinitionId().isBlank()
                || contract.deploymentId()==null || contract.deploymentId().isBlank() || contract.definitionDigest()==null
                || !contract.definitionDigest().matches("[a-f0-9]{64}"))
            throw new IllegalStateException("DCC_TRAINING_FLOW_READ_FACTS_MISSING");
        if(contract.status()==DccBoundWorkflowContractResolver.RoundContractStatus.READY) {
            if(contract.needTraining()==null || contract.flowRule()==null || contract.executionContract()==null
                    || contract.executionContract().isBlank() || contract.unavailableReason()!=null)
                throw new IllegalStateException("DCC_TRAINING_FLOW_READY_FACTS_MISSING");
        } else if(contract.flowRule()!=null || contract.executionContract()!=null || contract.unavailableReason()==null
                || contract.unavailableReason().isBlank()) {
            throw new IllegalStateException("DCC_TRAINING_FLOW_UNAVAILABLE_FACTS_INVALID");
        }
    }

    public record Progress(String controlledFileId,String applicationType,String bpmRound,Integer attributeRound,
                           List<ProgressTask> tasks,DccBoundWorkflowContractResolver.RoundFlowContract flowContract) {}
    public record ProgressTask(String id,String processInstanceId,String taskDefinitionKey,Integer status,
                               String assignee,String reason,LocalDateTime createTime,LocalDateTime endTime) {}
}
