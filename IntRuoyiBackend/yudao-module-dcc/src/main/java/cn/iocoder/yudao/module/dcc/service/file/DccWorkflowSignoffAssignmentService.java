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
    @Resource private DccControlledFileSignatureMapper signatureMapper;
    @Resource private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageSnapshotService relationStageService;
    @Resource private DccApplicationRoundService rounds;
    @Resource private cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper formInstances;
    @Resource private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationRemediationService remediationService;
    @Resource private DccObsoleteFormRuntimeStore obsoleteRuntimeStore;
    @Resource private DccBoundWorkflowContractResolver obsoleteBoundContracts;

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
            if(leader==null || !Objects.equals(actorId,leader.getId()) || !Objects.equals(tenant,leader.getTenantId())) denied();
            candidates.add(leader);
            for(var user:candidates) {
                if(user==null || user.getId()==null) throw new IllegalStateException("会签候选账号事实不完整");
                if(!Objects.equals(tenant,user.getTenantId())) throw new IllegalStateException("会签候选目录包含其他租户账号");
                if(!Integer.valueOf(0).equals(user.getStatus()) || user.getPostIds()==null || user.getPostIds().isEmpty()) continue;
                if(!Objects.equals(user.getDeptId(),row.getDepartmentId())) continue;
                if(user.getNickname()==null || user.getNickname().isBlank()) throw new IllegalStateException("会签候选账号名称缺失");
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
        var supplemental=selectedSupplementalRelations(request);
        var saved=snapshotMapper.selectOne(new LambdaQueryWrapper<DccControlledFileTaskAssigneeSnapshotDO>()
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getTenantId,tenantId)
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getControlledFileId,fileId)
                .eq(DccControlledFileTaskAssigneeSnapshotDO::getBpmTaskId,request.getTaskId()));
        if(saved!=null && saved.getAssignmentSignatureId()!=null) {
            if(!Objects.equals(actorId,saved.getLeaderUserId())) denied();
            var prepared=prepareRelationStage(actorId,fileId,file.getProcessInstanceId(),saved.getProcessInstanceId(),request.getTaskId(),
                    saved.getObligationId(),saved.getNodeInstanceId(),saved.getDepartmentId(),supplemental,arrangements,request.getReason());
            String payloadHash=assignmentCommandHash(tenantId,actorId,fileId,request,arrangements,supplemental,saved,
                    prepared==null?null:prepared.payloadHash());
            if(!Objects.equals(actorId,saved.getLeaderUserId())
                    || !Objects.equals(saved.getAssignmentPayloadHash(),payloadHash)) denied();
            if(prepared!=null) {
                var original=signatureMapper.selectActionSignature(fileId,request.getTaskId(),actorId,"ASSIGN");
                if(original==null || !Objects.equals(original.getId(),saved.getAssignmentSignatureId())
                        || !Objects.equals(original.getProcessInstanceId(),saved.getProcessInstanceId())
                        || !Objects.equals(original.getSignedAt(),saved.getAssignedTime()) || !"VALID".equals(original.getEvidenceStatus())) denied();
                relationStageService.freeze(prepared,new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.SignatureIdentity(
                        original.getId(),original.getSignedAt(),original.getEvidenceHash()));
            }
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
            denied(); // Exact persisted task replay must pass the stage preparation and signature checks above.
        }
        if(!nativeProcess && !supplemental.isEmpty())
            throw new IllegalStateException("非空补充关系的正式冻结与签名事务端口尚未接入，不能保存假指派结果");
        bpmTaskService.validateTask(actorId,task.getId());
        var candidate = adminUserApi.getUser(request.getAssigneeUserId());
        if (candidate == null || !Objects.equals(request.getAssigneeUserId(),candidate.getId())
                || !Objects.equals(tenantId,candidate.getTenantId())
                || !Integer.valueOf(0).equals(candidate.getStatus())
                || row.getDepartmentId()==null || !Objects.equals(row.getDepartmentId(),candidate.getDeptId())) denied();
        if(candidate.getPostIds()==null || candidate.getPostIds().isEmpty())
            throw exception(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_APPROVER_POST_REQUIRED);
        readinessService.requireReadyParticipants("MATRIX_REVIEW", java.util.List.of(candidate.getId()));
        var prepared=prepareRelationStage(actorId,fileId,file.getProcessInstanceId(),task.getProcessInstanceId(),task.getId(),
                row.getObligationId(),task.getExecutionId(),row.getDepartmentId(),supplemental,arrangements,request.getReason());
        String payloadHash=assignmentCommandHash(tenantId,actorId,fileId,request,arrangements,supplemental,row,
                prepared==null?null:prepared.payloadHash());
        String signedReason=request.getReason().trim()+"；会签指派事实：部门="+row.getDepartmentId()
                +"，会签人="+candidate.getId()+"，指派载荷="+payloadHash
                +"，会签人实际部门="+candidate.getDeptId();
        if(prepared!=null) signedReason+="；"+prepared.signedReasonMarker();
        var signature = signatureService.verifyPasswordAndCreateWorkflowSignature(actorId,fileId,task.getId(),
                task.getProcessInstanceId(),"MATRIX_REVIEW","ASSIGN",request.getPassword(),signedReason);
        if (signature == null || signature.getSignatureId() == null || signature.getSignedAt() == null
                || !"VALID".equals(signature.getEvidenceStatus())) denied();
        if (!nativeProcess) bindObsoleteSignatureCopy(file,task,actorId);
        if(prepared!=null) relationStageService.freeze(prepared,
                new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.SignatureIdentity(
                        signature.getSignatureId(),signature.getSignedAt(),signature.getEvidenceHash()));
        if (snapshotMapper.updateById(DccControlledFileTaskAssigneeSnapshotDO.builder().id(row.getId())
                .processInstanceId(task.getProcessInstanceId()).assigneeUserId(candidate.getId())
                .assigneeName(candidate.getNickname()).bpmTaskId(task.getId()).nodeInstanceId(task.getExecutionId())
                .assignmentSignatureId(signature.getSignatureId()).assignmentPayloadHash(payloadHash)
                .assignedTime(signature.getSignedAt()).build()) != 1)
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
        if(isObsoleteProcessForFile(task,fileId)){
            var evidence=requireObsoleteRuntime(task,fileId);if(evidence!=null){var member=requireRuntimeMember(evidence,task);
                var frozen=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(member.frozenUserIdsJson(),Long.class);
                if(!Objects.equals(row.getTenantId(),evidence.head().tenantId())||!Objects.equals(row.getProcessInstanceId(),task.getProcessInstanceId())||!Objects.equals(row.getObligationId(),member.obligationId())
                        ||!Objects.equals(row.getDepartmentId(),member.departmentId())||frozen.size()!=1||!Objects.equals(row.getLeaderUserId(),frozen.get(0)))denied();}
        }
        return row;
    }

    public void requireAssigned(Long userId, Long fileId, Task task) {
        var row = obligation(fileId,task);
        if (row.getAssignmentSignatureId() == null || row.getAssignedTime() == null
                || !Objects.equals(row.getAssigneeUserId(),userId) || !Objects.equals(row.getBpmTaskId(),task.getId())) denied();
        if(isObsoleteProcessForFile(task,fileId)&&!Objects.equals(row.getNodeInstanceId(),task.getExecutionId()))denied();
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
                || !("SUPERVISOR_REVIEW".equals(task.getTaskDefinitionKey())||"MATRIX_REVIEW".equals(task.getTaskDefinitionKey())
                    || "MATRIX_APPROVAL".equals(task.getTaskDefinitionKey()))) denied();
        var evidence=requireObsoleteRuntime(task,fileId);
        if(evidence!=null){var member=requireRuntimeMember(evidence,task);
            if(!Objects.equals(String.valueOf(userId),task.getAssignee())||task.getExecutionId()==null||task.getExecutionId().isBlank())denied();
            if(!"MATRIX_REVIEW".equals(task.getTaskDefinitionKey())&&!cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(member.frozenUserIdsJson(),Long.class).contains(userId))denied();
        }else if("SUPERVISOR_REVIEW".equals(task.getTaskDefinitionKey()))denied();
        readinessService.requireReadyParticipants(task.getTaskDefinitionKey(), java.util.List.of(userId));
        if ("MATRIX_REVIEW".equals(task.getTaskDefinitionKey())) requireAssigned(userId, fileId, task);
    }
    private DccObsoleteFormRuntimeStore.RuntimeEvidence requireObsoleteRuntime(Task task,Long fileId){
        var found=formInstances.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO>()
                .eq("tenant_id",TenantContextHolder.getRequiredTenantId()).eq("bpm_process_instance_id",task.getProcessInstanceId()).eq("deleted",false));
        if(found==null||found.size()!=1)throw new IllegalStateException("作废真实BPM必须唯一对应Form");var form=found.get(0);var file=fileMapper.selectById(fileId);
        if(file==null||!Objects.equals(form.getTenantId(),file.getTenantId())||!Objects.equals(form.getObjectId(),fileId.toString())||!Objects.equals(form.getObjectVersion(),file.getVersionNo())||!Objects.equals(form.getBpmProcessInstanceId(),task.getProcessInstanceId())
                ||!"DCC".equals(form.getSystemCode())||!"CONTROLLED_FILE".equals(form.getObjectType())||!"OBSOLETE".equals(form.getActionCode()))denied();
        var data=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(form.getFormDataJson(),java.util.Map.class);
        if(data==null||!(data.get("dccExecutionPreparation") instanceof java.util.Map<?,?> proof))throw new IllegalStateException("作废任务缺少真实保存proof");
        if("DCC_OBSOLETE_FORM_PREPARATION_V2".equals(proof.get("formatVersion")))return obsoleteRuntimeStore.requireByBpm(file.getTenantId(),fileId,task.getProcessInstanceId());
        if(!"DCC_OBSOLETE_FORM_PREPARATION_V1".equals(proof.get("formatVersion")))throw new IllegalStateException("作废任务proof版本不支持");
        var round=new DccApplicationRoundSummary();round.setControlledFileId(fileId);round.setApplicationType("OBSOLETE");round.setBpmRound(task.getProcessInstanceId());round.setAttributeRound(rounds.require("OBSOLETE",fileId,task.getProcessInstanceId()));
        var historical=obsoleteBoundContracts.resolveRoundFlowContract(file,round);if(historical.status()!=DccBoundWorkflowContractResolver.RoundContractStatus.READY||!Objects.equals(historical.formInstanceId(),form.getId())||!Objects.equals(historical.processDefinitionId(),task.getProcessDefinitionId()))denied();return null;
    }
    private cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccObsoleteFormRuntimeObligationDO requireRuntimeMember(DccObsoleteFormRuntimeStore.RuntimeEvidence evidence,Task task){
        if(!Objects.equals(task.getProcessDefinitionId(),evidence.head().preparedProcessDefinitionId())||!Objects.equals(task.getProcessInstanceId(),evidence.head().bpmProcessInstanceId())||!Objects.equals(task.getTenantId(),evidence.head().tenantId().toString()))denied();
        String obligationId=null;if("MATRIX_REVIEW".equals(task.getTaskDefinitionKey())){
            var value=task.getTaskLocalVariables()==null?null:task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_DCC_OBLIGATION_ID);if(!(value instanceof String id)||id.isBlank())denied();else obligationId=id;
        }
        final String selected=obligationId;var matches=evidence.obligations().stream().filter(r->Objects.equals(r.stageCode(),task.getTaskDefinitionKey())&&Objects.equals(r.nodeInstanceId(),task.getTaskDefinitionKey())&&(selected==null||Objects.equals(selected,r.obligationId()))).toList();
        if(matches.size()!=1)denied();return matches.get(0);
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

    private java.util.List<Long> selectedSupplementalRelations(DccSignoffAssignmentReqVO request) {
        var selected=request.getSupplementalRelatedControlledFileIds();
        if(selected==null) return java.util.List.of();
        if(selected.stream().anyMatch(id->id==null || id<=0) || selected.stream().distinct().count()!=selected.size()) denied();
        return selected.stream().sorted().toList();
    }

    private String assignmentCommandHash(Long tenantId,Long actorId,Long fileId,DccSignoffAssignmentReqVO request,
            java.util.List<cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement> arrangements,
            java.util.List<Long> supplemental,DccControlledFileTaskAssigneeSnapshotDO obligation,String relationPayloadHash) {
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                java.util.Arrays.asList("IC07_ASSIGN_REQUEST_V1",tenantId,actorId,fileId,request.getTaskId(),
                        obligation.getObligationId(),obligation.getDepartmentId(),obligation.getLeaderUserId(),
                        request.getAssigneeUserId(),request.getReason().trim(),arrangements,supplemental,relationPayloadHash)));
    }

    private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.Prepared prepareRelationStage(
            Long actorId,Long fileId,String fileRound,String taskRound,String taskId,String obligationId,String nodeInstanceId,
            Long departmentId,java.util.List<Long> supplements,
            java.util.List<cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.Arrangement> arrangements,String reason) {
        if(taskRound==null || taskRound.isBlank() || taskId==null || taskId.isBlank() || obligationId==null || obligationId.isBlank()
                || nodeInstanceId==null || nodeInstanceId.isBlank() || departmentId==null || departmentId<=0) denied();
        var file=fileMapper.selectById(fileId);if(file==null)denied();
        String applicationType;Long formId=null;
        if(Objects.equals(fileRound,taskRound)){
            if(DccControlledFileProcessDefinitionKeys.UPLOAD.equals(file.getProcessDefinitionKey()))applicationType="UPLOAD";
            else if(DccControlledFileProcessDefinitionKeys.REVISION.equals(file.getProcessDefinitionKey()))applicationType="REVISION";
            else throw new IllegalStateException("会签关联必须具有真实native申请类型");
        }else{
            if(!supplements.isEmpty()||!arrangements.isEmpty())throw new IllegalArgumentException("独立作废不能补充关联或整改安排");
            var forms=formInstances.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<cn.iocoder.yudao.module.bpm.dal.dataobject.formcenter.FormActionInstanceDO>()
                    .eq("tenant_id",TenantContextHolder.getRequiredTenantId()).eq("bpm_process_instance_id",taskRound).eq("deleted",false).last("FOR UPDATE"));
            if(forms==null||forms.size()!=1)throw new IllegalStateException("作废轮次必须绑定唯一正式FormCenter来源");
            var form=forms.get(0);
            if(!"DCC".equals(form.getSystemCode())||!"CONTROLLED_FILE".equals(form.getObjectType())||!"OBSOLETE".equals(form.getActionCode())
                    ||!String.valueOf(fileId).equals(form.getObjectId())||!Objects.equals(file.getVersionNo(),form.getObjectVersion())
                    ||!"IN_APPROVAL".equals(form.getStatus())||form.getId()==null)denied();
            applicationType="OBSOLETE";formId=form.getId();
        }
        int attributeRound=rounds.require(applicationType,fileId,taskRound);
        var identity=new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.ValidatedTaskIdentity(
                TenantContextHolder.getRequiredTenantId(),taskRound,taskId,obligationId,"MATRIX_REVIEW",nodeInstanceId,departmentId,"ASSIGN",
                applicationType,attributeRound,formId);
        String key="dcc-signoff-stage:"+cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.Arrays.asList(fileId,identity)));
        return Objects.requireNonNull(relationStageService.prepare(actorId,fileId,identity,supplements,arrangements,key,null,reason.trim()),
                "正式指派关系准备事实缺失");
    }

    private void requireTaskTenant(Task task) {
        if(task==null || !String.valueOf(TenantContextHolder.getRequiredTenantId()).equals(task.getTenantId())) denied();
    }
}
