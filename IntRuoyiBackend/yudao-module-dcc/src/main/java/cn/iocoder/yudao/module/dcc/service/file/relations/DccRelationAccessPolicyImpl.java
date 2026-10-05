package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTaskAssigneeSnapshotMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProcessDefinitionKeys;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import java.util.Objects;

/** Formal bridge; no dependency on relation-enriched detail reads, avoiding circular authorization. */
@Service
public class DccRelationAccessPolicyImpl implements DccRelationAccessPolicy {
    @Lazy @Resource private DccControlledFileQueryService query;
    @Resource private DccControlledFileTaskAssigneeSnapshotMapper obligations;
    @Resource private TaskService tasks;
    @Resource private AdminUserApi users;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccProjectAccessService projects;
    @Resource(name="dccControlledFileMapper") private DccControlledFileMapper files;
    @Resource private BpmTaskService bpmTasks;
    @Resource private BpmProcessDefinitionService definitions;
    public void assertNameVisible(Long actor,Long id) { query.assertRelationNameVisible(actor,id); }
    public void assertContentReadable(Long actor,Long id) { query.assertRelationContentReadable(actor,id); }
    public void assertCanEditRelations(Long actor,Long id) { query.assertRelationEditable(actor,id); }
    private void requireParticipant(Long actor,Long fileId,String round) {
        assertNameVisible(actor,fileId);
        if(actor==null || round==null || round.isBlank() || obligations.selectListByControlledFileId(fileId).stream()
                .noneMatch(row->Objects.equals(row.getTenantId(),TenantContextHolder.getRequiredTenantId())
                        && Objects.equals(row.getProcessInstanceId(),round)
                        && "MATRIX_REVIEW".equals(row.getStageCode())
                        && (Objects.equals(row.getLeaderUserId(),actor) || Objects.equals(row.getAssigneeUserId(),actor))))
            throw new DccRelationFailure("DCC_RELATION_ARRANGEMENT_FORBIDDEN");
    }
    public void assertCanArrange(Long actor,Long fileId,String round) {
        requireParticipant(actor,fileId,round);
        var active=tasks.createTaskQuery().processInstanceId(round).taskDefinitionKey("MATRIX_REVIEW")
                .taskAssignee(String.valueOf(actor)).taskTenantId(String.valueOf(TenantContextHolder.getRequiredTenantId())).list();
        if(active.isEmpty()) throw new DccRelationFailure("DCC_RELATION_SIGNOFF_TASK_MISSING");
    }
    public void assertCanReadArrangements(Long actor,Long fileId,String round) {
        assertNameVisible(actor,fileId);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(actor==null || actor<=0 || round==null || round.isBlank())
            throw new DccRelationFailure("DCC_RELATION_ARRANGEMENT_FORBIDDEN");
        var rows=obligations.selectListByControlledFileId(fileId);
        // Already-bound participant facts also authorize historical reads after the task has ended.
        if(rows.stream().anyMatch(row->Objects.equals(row.getTenantId(),tenant)
                && Objects.equals(row.getProcessInstanceId(),round) && "MATRIX_REVIEW".equals(row.getStageCode())
                && (Objects.equals(row.getLeaderUserId(),actor) || Objects.equals(row.getAssigneeUserId(),actor)))) return;
        var file=files.selectById(fileId);
        if(file==null || !Objects.equals(file.getId(),fileId) || !Objects.equals(file.getTenantId(),tenant)
                || !Objects.equals(file.getProcessInstanceId(),round) || !"PENDING_MATRIX_REVIEW".equals(file.getStatus()))
            throw new DccRelationFailure("DCC_RELATION_ARRANGEMENT_FORBIDDEN");
        var account=users.getUser(actor);
        if(account==null || !Objects.equals(account.getId(),actor) || !Objects.equals(account.getTenantId(),tenant)
                || !Integer.valueOf(0).equals(account.getStatus()))
            throw new DccRelationFailure("DCC_RELATION_ARRANGEMENT_FORBIDDEN");
        // A NULL round is an unsigned initial obligation, never a free-standing participant fact.
        var currentTasks=tasks.createTaskQuery().processInstanceId(round).taskDefinitionKey("MATRIX_REVIEW")
                .taskAssignee(String.valueOf(actor)).taskTenantId(String.valueOf(tenant))
                .active().includeTaskLocalVariables().list();
        for(var task:currentTasks) {
            if(task.getId()==null || task.getProcessDefinitionId()==null
                    || !Objects.equals(task.getTenantId(),String.valueOf(tenant))
                    || !Objects.equals(task.getProcessInstanceId(),round) || !"MATRIX_REVIEW".equals(task.getTaskDefinitionKey())) continue;
            var definition=definitions.getProcessDefinition(task.getProcessDefinitionId());
            if(definition==null || !Objects.equals(definition.getId(),task.getProcessDefinitionId())
                    || !Objects.equals(definition.getTenantId(),task.getTenantId())
                    || !DccControlledFileProcessDefinitionKeys.REVISION.equals(definition.getKey())) continue;
            Object obligation=task.getTaskLocalVariables()==null ? null
                    : task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID);
            if(!(obligation instanceof String obligationId) || obligationId.isBlank()) continue;
            boolean exactInitial=rows.stream().anyMatch(row->Objects.equals(row.getTenantId(),tenant)
                    && Objects.equals(row.getControlledFileId(),fileId) && "MATRIX_REVIEW".equals(row.getStageCode())
                    && row.getProcessInstanceId()==null && row.getAssignmentSignatureId()==null && row.getAssignedTime()==null
                    && row.getDepartmentId()!=null && row.getDepartmentId()>0 && Objects.equals(row.getLeaderUserId(),actor)
                    && Objects.equals(row.getObligationId(),obligationId)
                    && (row.getBpmTaskId()==null || Objects.equals(row.getBpmTaskId(),task.getId()))
                    && (row.getNodeInstanceId()==null || Objects.equals(row.getNodeInstanceId(),task.getExecutionId())));
            if(!exactInitial) continue;
            var validated=bpmTasks.validateTask(actor,task.getId());
            if(validated!=null && Objects.equals(validated.getId(),task.getId())
                    && Objects.equals(validated.getTenantId(),task.getTenantId())
                    && Objects.equals(validated.getProcessInstanceId(),round)
                    && Objects.equals(validated.getProcessDefinitionId(),task.getProcessDefinitionId())
                    && "MATRIX_REVIEW".equals(validated.getTaskDefinitionKey())
                    && String.valueOf(actor).equals(validated.getAssignee()) && validated.getTaskLocalVariables()!=null
                    && Objects.equals(obligationId,validated.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID))) return;
        }
        throw new DccRelationFailure("DCC_RELATION_ARRANGEMENT_FORBIDDEN");
    }
    public void assertAssigneeAvailable(Long actor,Long masterId) {
        var account=actor==null?null:users.getUser(actor);
        var master=masterId==null?null:masters.selectById(masterId);
        if(account==null || !Objects.equals(account.getId(),actor) || !Integer.valueOf(0).equals(account.getStatus())
                || master==null || !Objects.equals(master.getTenantId(),TenantContextHolder.getRequiredTenantId()))
            throw new DccRelationFailure("DCC_RELATION_ASSIGNEE_UNAVAILABLE");
        projects.assertProjectEditorOrOwner(actor,master.getDccProjectCodeId());
    }
}
