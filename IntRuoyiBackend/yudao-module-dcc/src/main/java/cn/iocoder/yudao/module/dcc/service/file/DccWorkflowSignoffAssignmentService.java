package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccSignoffAssignmentReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTaskAssigneeSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED;

@Service
public class DccWorkflowSignoffAssignmentService {
    @Resource(name = "dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccControlledFileTaskAssigneeSnapshotMapper snapshotMapper;
    @Resource private BpmTaskService bpmTaskService;
    @Resource private cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService definitions;
    @Resource private TaskService taskService;
    @Resource private RuntimeService runtimeService;
    @Resource private AdminUserApi adminUserApi;
    @Resource private DccControlledFileRouteReadinessService readinessService;
    @Resource private DccSignatureVerificationService signatureService;
    @Resource private DccControlledFileSignatureBindingService signatureBindingService;
    @Resource private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService remediationService;

    public DccSignoffAssignmentContext assignmentContext(Long actorId,Long fileId,String taskId) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var file=fileId==null?null:fileMapper.selectById(fileId);
        if(file==null || !Objects.equals(tenant,file.getTenantId()) || taskId==null || taskId.isBlank()) denied();
        var task=bpmTaskService.getTask(taskId);
        requireTaskTenant(task);
        if(!"MATRIX_REVIEW".equals(task.getTaskDefinitionKey())) denied();
        String processDefinitionKey = requireCurrentProcessDefinitionKey(task);
        boolean nativeProcess=Objects.equals(task.getProcessInstanceId(),file.getProcessInstanceId());
        if(nativeProcess?!"PENDING_MATRIX_REVIEW".equals(file.getStatus()):!isObsoleteProcessForFile(task,fileId)) denied();
        var row=obligation(fileId,task);
        boolean assigned=row.getAssignmentSignatureId()!=null;
        if(!Objects.equals(actorId,row.getLeaderUserId())
                && !(assigned && Objects.equals(actorId,row.getAssigneeUserId()))) denied();
        if(row.getDepartmentId()==null || row.getObligationId()==null || row.getObligationId().isBlank()) denied();
        if(assigned && (row.getAssignedTime()==null || !Objects.equals(row.getBpmTaskId(),taskId))) denied();
        boolean canAssign=!assigned && Objects.equals(actorId,row.getLeaderUserId());
        var options=new java.util.LinkedHashMap<Long,DccSignoffAssignmentContext.AssigneeOption>();
        if(canAssign) {
            bpmTaskService.validateTask(actorId,taskId);
            var users=Objects.requireNonNull(adminUserApi.getUserListByDeptIds(java.util.List.of(row.getDepartmentId())),"department user directory");
            var candidates=new java.util.ArrayList<>(users);
            var leader=adminUserApi.getUser(actorId);
            if(leader==null || !Objects.equals(actorId,leader.getId())) denied();
            candidates.add(leader);
            for(var user:candidates) {
                if(user==null || user.getId()==null) throw new IllegalStateException("会签候选账号事实不完整");
                if(!Integer.valueOf(0).equals(user.getStatus()) || user.getPostIds()==null || user.getPostIds().isEmpty()) continue;
                if(!Objects.equals(user.getId(),actorId) && !Objects.equals(user.getDeptId(),row.getDepartmentId())) continue;
                options.put(user.getId(),new DccSignoffAssignmentContext.AssigneeOption(user.getId(),user.getNickname()));
            }
        }
        return new DccSignoffAssignmentContext(fileId,task.getProcessInstanceId(),taskId,row.getObligationId(),processDefinitionKey,
                row.getDepartmentId(),row.getDepartmentName(),row.getAssigneeUserId(),assigned,canAssign,java.util.List.copyOf(options.values()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void assign(Long actorId, Long fileId, DccSignoffAssignmentReqVO request) {
        if (request == null || request.getTaskId() == null || request.getAssigneeUserId() == null
                || request.getPassword() == null || request.getPassword().isBlank()
                || request.getReason() == null || request.getReason().isBlank()) denied();
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        var identity = fileMapper.selectById(fileId);
        if (identity == null || !Objects.equals(tenantId, identity.getTenantId())) denied();
        var master = masterMapper.selectByIdForUpdate(identity.getMasterId());
        var file = fileMapper.selectByIdAndTenantForUpdate(tenantId,fileId);
        if (master == null || file == null || !Objects.equals(master.getTenantId(),tenantId)) denied();
        var arrangements=selectedArrangements(request);
        String payloadHash=cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.List.of(
                        actorId,fileId,request.getTaskId(),request.getAssigneeUserId(),request.getReason().trim(),arrangements)));
        var saved=snapshotMapper.selectOne(new LambdaQueryWrapper<DccControlledFileTaskAssigneeSnapshotDO>()
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getTenantId,tenantId)
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getControlledFileId,fileId)
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getBpmTaskId,request.getTaskId()));
        if(saved!=null && saved.getAssignmentSignatureId()!=null) {
            if(!Objects.equals(actorId,saved.getLeaderUserId())
                    || !Objects.equals(saved.getAssignmentPayloadHash(),payloadHash)) denied();
            return;
        }
        Task task = bpmTaskService.getTask(request.getTaskId());
        if (task == null || !"MATRIX_REVIEW".equals(task.getTaskDefinitionKey())) denied();
        requireTaskTenant(task);
        String processDefinitionKey = requireCurrentProcessDefinitionKey(task);
        boolean revisionAssignment = DccControlledFileProcessDefinitionKeys.REVISION.equals(processDefinitionKey);
        if (!revisionAssignment && !arrangements.isEmpty()) denied();
        boolean nativeProcess = Objects.equals(task.getProcessInstanceId(), file.getProcessInstanceId());
        if (nativeProcess ? !"PENDING_MATRIX_REVIEW".equals(file.getStatus())
                : !isObsoleteProcessForFile(task, fileId)) denied();
        var row = obligation(fileId,task);
        if (!Objects.equals(actorId,row.getLeaderUserId())) denied();
        if (row.getAssignmentSignatureId() != null) {
            if (!Objects.equals(row.getAssigneeUserId(),request.getAssigneeUserId())
                    || !Objects.equals(row.getAssignmentPayloadHash(),payloadHash)) denied();
            return;
        }
        bpmTaskService.validateTask(actorId,task.getId());
        var candidate = adminUserApi.getUser(request.getAssigneeUserId());
        if (candidate == null || !Objects.equals(request.getAssigneeUserId(),candidate.getId())
                || !Integer.valueOf(0).equals(candidate.getStatus())
                || !(Objects.equals(candidate.getId(),actorId)
                    || Objects.equals(candidate.getDeptId(),row.getDepartmentId()))) denied();
        if(candidate.getPostIds()==null || candidate.getPostIds().isEmpty())
            throw exception(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_APPROVER_POST_REQUIRED);
        readinessService.requireReadyParticipants("MATRIX_REVIEW", java.util.List.of(candidate.getId()));
        String signedReason=request.getReason().trim()+"；会签指派事实：部门="+row.getDepartmentId()
                +"，会签人="+candidate.getId()+"，指派载荷="+payloadHash;
        var signature = signatureService.verifyPasswordAndCreateWorkflowSignature(actorId,fileId,task.getId(),
                task.getProcessInstanceId(),"MATRIX_REVIEW","ASSIGN",request.getPassword(),signedReason);
        if (signature == null || signature.getSignatureId() == null || !"VALID".equals(signature.getEvidenceStatus())) denied();
        if (!nativeProcess) bindObsoleteSignatureCopy(file,task,actorId);
        if (snapshotMapper.updateById(DccControlledFileTaskAssigneeSnapshotDO.builder().id(row.getId())
                .processInstanceId(task.getProcessInstanceId()).assigneeUserId(candidate.getId())
                .assigneeName(candidate.getNickname()).bpmTaskId(task.getId()).nodeInstanceId(task.getExecutionId())
                .assignmentSignatureId(signature.getSignatureId()).assignmentPayloadHash(payloadHash)
                .assignedTime(LocalDateTime.now()).build()) != 1)
            throw new IllegalStateException("会签指派证据保存失败");
        if (revisionAssignment) {
            remediationService.saveArrangements(actorId,fileId,task.getProcessInstanceId(),arrangements,request.getReason().trim());
        }
        taskService.setAssignee(task.getId(),String.valueOf(candidate.getId()));
        taskService.setVariableLocal(task.getId(),"dccAssignmentSignatureId",signature.getSignatureId());
    }

    private java.util.List<cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement>
            selectedArrangements(DccSignoffAssignmentReqVO request) {
        if(request.getRelationArrangements()==null) return java.util.List.of();
        var unique=new java.util.HashSet<Long>();
        for(var row:request.getRelationArrangements()) {
            if(row==null || row.relatedMasterId()==null || row.relatedMasterId()<=0 || row.assigneeUserId()==null
                    || row.assigneeUserId()<=0 || row.dueAt()==null || row.dueAt().getNano()!=0
                    || !unique.add(row.relatedMasterId())) denied();
        }
        return request.getRelationArrangements().stream()
                .sorted(java.util.Comparator.comparing(cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement::relatedMasterId))
                .toList();
    }

    public boolean isObsoleteProcessForFile(Task task, Long fileId) {
        if (task.getProcessDefinitionId() == null
                || !task.getProcessDefinitionId().startsWith("dcc-controlled-file-obsolete:")) return false;
        requireTaskTenant(task);
        var variables = runtimeService.getVariables(task.getProcessInstanceId());
        return variables != null && "DCC".equals(variables.get("systemCode"))
                && "CONTROLLED_FILE".equals(variables.get("objectType"))
                && "OBSOLETE".equals(variables.get("actionCode"))
                && String.valueOf(fileId).equals(String.valueOf(variables.get("objectId")));
    }

    public DccControlledFileTaskAssigneeSnapshotDO obligation(Long fileId, Task task) {
        requireTaskTenant(task);
        Object obligation = task.getTaskLocalVariables() == null ? null : task.getTaskLocalVariables()
                .get(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID);
        if (!(obligation instanceof String id) || id.isBlank()) {
            denied();
            return null;
        }
        var row = snapshotMapper.selectOne(new LambdaQueryWrapper<DccControlledFileTaskAssigneeSnapshotDO>()
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getTenantId,TenantContextHolder.getRequiredTenantId())
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getControlledFileId,fileId)
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getObligationId,id));
        if (row == null || !"MATRIX_REVIEW".equals(row.getStageCode())
                || row.getProcessInstanceId() != null && !row.getProcessInstanceId().equals(task.getProcessInstanceId())) denied();
        return row;
    }

    public void requireAssigned(Long userId, Long fileId, Task task) {
        var row = obligation(fileId,task);
        if (row.getAssignmentSignatureId() == null || row.getAssignedTime() == null
                || !Objects.equals(row.getAssigneeUserId(),userId) || !Objects.equals(row.getBpmTaskId(),task.getId())) denied();
    }

    public void requireObsoleteReviewReady(Long userId, Long fileId, String taskId) {
        var file = fileMapper.selectById(fileId);
        Task task = bpmTaskService.validateTask(userId, taskId);
        requireObsoleteReview(userId, fileId, file, taskId, task);
    }

    private void requireObsoleteReview(Long userId, Long fileId,
            cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file,
            String taskId, Task task) {
        if (file == null || !Objects.equals(fileId, file.getId())
                || !Objects.equals(TenantContextHolder.getRequiredTenantId(), file.getTenantId())
                || file.getPublishedFileId() == null || task == null || !Objects.equals(taskId, task.getId())
                || !isObsoleteProcessForFile(task, fileId)
                || !("MATRIX_REVIEW".equals(task.getTaskDefinitionKey())
                    || "MATRIX_APPROVAL".equals(task.getTaskDefinitionKey()))) denied();
        readinessService.requireReadyParticipants(task.getTaskDefinitionKey(), java.util.List.of(userId));
        if ("MATRIX_REVIEW".equals(task.getTaskDefinitionKey())) requireAssigned(userId, fileId, task);
    }

    @Transactional(rollbackFor = Exception.class)
    public DccUnifiedSignatureResult reviewObsolete(Long userId, Long fileId, String taskId,
            String password, String reason, boolean approve) {
        var identity=fileMapper.selectById(fileId);
        if(identity==null) denied();
        var master=masterMapper.selectByIdForUpdate(identity.getMasterId());
        if(master==null || !Objects.equals(master.getTenantId(),TenantContextHolder.getRequiredTenantId())) denied();
        var file = fileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),fileId);
        Task task = bpmTaskService.validateTask(userId,taskId);
        requireObsoleteReview(userId, fileId, file, taskId, task);
        var signature = signatureService.verifyPasswordAndCreateWorkflowSignature(userId,fileId,taskId,
                task.getProcessInstanceId(),task.getTaskDefinitionKey(),approve ? "APPROVE" : "REJECT",password,reason);
        bindObsoleteSignatureCopy(file,task,userId);
        authorizeSignedAction(taskId,userId,approve ? "APPROVE" : "REJECT",signature);
        if (approve) {
            @SuppressWarnings("unchecked")
            var next = (java.util.Map<String,java.util.List<Long>>) runtimeService.getVariable(task.getProcessInstanceId(),
                    BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES);
            bpmTaskService.approveTask(userId,new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO()
                    .setId(taskId).setReason(reason).setNextAssignees(next));
        } else {
            bpmTaskService.rejectTask(userId,new cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO()
                    .setId(taskId).setReason(reason));
        }
        return signature;
    }

    public void authorizeSignedAction(String taskId, Long userId, String action, DccUnifiedSignatureResult signature) {
        if (signature == null || signature.getSignatureId() == null || !"VALID".equals(signature.getEvidenceStatus())) denied();
        taskService.setVariablesLocal(taskId,java.util.Map.of("dccVerifiedSignatureId",signature.getSignatureId(),
                "dccVerifiedSignatureActorId",userId,"dccVerifiedSignatureAction",action));
    }

    private void bindObsoleteSignatureCopy(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO file,
            Task task, Long actorId) {
        if(file.getPublishedFileId()==null) denied();
        signatureBindingService.bindPublishedCopy(file,file.getPublishedFileId(),actorId,
                "dcc-obsolete-signature:"+file.getId()+":"+task.getProcessInstanceId()+":"+task.getId());
    }

    private void denied() { throw exception(CONTROLLED_FILE_TASK_ACTION_NOT_ALLOWED); }

    /** The current task round, not the file's original upload process, defines this assignment's operation. */
    private String requireCurrentProcessDefinitionKey(Task task) {
        requireTaskTenant(task);
        if (task.getProcessDefinitionId() == null || task.getProcessDefinitionId().isBlank()) denied();
        var definition = definitions.getProcessDefinition(task.getProcessDefinitionId());
        if (definition == null || !Objects.equals(definition.getId(), task.getProcessDefinitionId())
                || !Objects.equals(definition.getTenantId(), task.getTenantId())
                || !java.util.Set.of(DccControlledFileProcessDefinitionKeys.UPLOAD,
                        DccControlledFileProcessDefinitionKeys.REVISION, DccControlledFileProcessDefinitionKeys.OBSOLETE)
                        .contains(definition.getKey())) denied();
        return definition.getKey();
    }

    private void requireTaskTenant(Task task) {
        if(task==null || !String.valueOf(TenantContextHolder.getRequiredTenantId()).equals(task.getTenantId())) denied();
    }
}
