package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Sole formal delayed command. All write ports join the same Spring transaction; no generic approve bypass. */
@Service
public class DccDelayedEffectiveConfirmationService {
    @Resource private DccControlledFileMapper fileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccBoundWorkflowContractResolver boundContracts;
    @Resource private DccDelayedEffectiveConfirmationEvidence confirmationEvidence;
    @Resource private DccSignatureVerificationService signatureService;
    @Resource private DccElectronicSignatureAuthorizationService electronicAuthorization;
    @Resource private DccElectronicSignatureManagementService signatureManagement;
    @Resource private DccControlledFileSignatureMapper signatures;
    @Resource private DccControlledFileRouteSnapshotMapper routes;
    @Resource private DccControlledFileTaskAssigneeSnapshotMapper assignments;
    @Resource private DccControlledFileAttachmentMapper attachments;
    @Resource private DccControlledFileLifecycleService lifecycle;
    @Resource private DccWorkflowDatePolicy datePolicy;
    @Resource private DccWorkflowFileStateAudit audit;
    @Resource private AdminUserApi accounts;
    @Resource private BpmTaskService tasks;
    @Resource private TaskService engineTasks;
    @Resource private HistoryService history;

    @Transactional(rollbackFor=Exception.class)
    public DccControlledFileDelayedEffectiveConfirmRespVO read(Long actor,Long id,String taskId,String process,Integer attributeRound){
        var locked=lock(actor,id);var file=locked.file();var bound=boundContracts.resolve(file);
        requireTask(actor,file,bound,taskId,process,attributeRound);requirePrerequisites(file,bound);
        return context(file,bound,taskId).canConfirm(true).build();
    }

    @Transactional(rollbackFor=Exception.class)
    public DccControlledFileDelayedEffectiveConfirmRespVO confirm(Long actor,Long id,DccControlledFileDelayedEffectiveConfirmReqVO request){
        if(request==null || request.getSelectedEffectiveTime()==null || request.getPassword()==null || request.getPassword().isBlank()
                || request.getReason()==null || request.getReason().isBlank() || request.getReason().trim().length()>500
                || request.getIdempotencyKey()==null || request.getIdempotencyKey().isBlank() || request.getIdempotencyKey().length()>128
                || !request.getIdempotencyKey().equals(request.getIdempotencyKey().trim())) throw invalid("请填写本次生效时间、电子签名密码、真实原因及幂等身份");
        var locked=lock(actor,id);var file=locked.file();var bound=boundContracts.resolve(file);
        requireExpected(bound,request);
        electronicAuthorization.validateElectronicSignatureEnabled(actor);
        // Committed replay is proved before mutable status/date gates; never re-sign or replace S.
        if(file.getDelayedEffectiveSignatureId()!=null){
            var verified=confirmationEvidence.requireConfirmed(file,bound);
            var saved=signatures.selectById(verified.signatureId());var task=tasks.getHistoricTask(verified.taskId());
            var local=task==null?null:task.getTaskLocalVariables();
            if(!Objects.equals(verified.taskId(),request.getTaskId()) || !Objects.equals(verified.selectedEffectiveTime(),request.getSelectedEffectiveTime())
                    || saved==null || !Objects.equals(saved.getComment(),request.getReason().trim()) || local==null
                    || !Objects.equals(local.get("dccDelayedConfirmationKey"),request.getIdempotencyKey()))
                throw invalid("延迟生效确认重放载荷或幂等身份冲突");
            return result(file,bound,verified);
        }
        requireTask(actor,file,bound,request.getTaskId(),request.getProcessInstanceId(),request.getAttributeRound());
        datePolicy.requireDelayedEffectiveTime(request.getSelectedEffectiveTime(),datePolicy.now());
        requirePrerequisites(file,bound);
        var before=audit.capture(file,locked.master());
        var signed=signatureService.verifyPasswordAndCreateDelayedConfirmationSignature(actor,id,request.getTaskId(),file.getProcessInstanceId(),
                bound.attributeRound(),request.getSelectedEffectiveTime(),request.getPassword(),request.getReason().trim());
        if(signed==null || signed.getSignatureId()==null)throw invalid("延迟生效确认未返回真实签名身份");
        var verified=confirmationEvidence.requireSignedForActiveTask(file,bound,signed.getSignatureId());
        datePolicy.requireDelayedEffectiveTime(verified.selectedEffectiveTime(),verified.signedAt());
        if(fileMapper.update(null,new LambdaUpdateWrapper<DccControlledFileDO>().eq(DccControlledFileDO::getId,id)
                .eq(DccControlledFileDO::getTenantId,TenantContextHolder.getRequiredTenantId()).eq(DccControlledFileDO::getProcessInstanceId,file.getProcessInstanceId())
                .eq(DccControlledFileDO::getStatus,DccDelayedEffectiveConfirmationEvidence.STATUS).isNull(DccControlledFileDO::getDelayedEffectiveSignatureId)
                .set(DccControlledFileDO::getDelayedEffectiveTime,verified.selectedEffectiveTime()).set(DccControlledFileDO::getDelayedEffectiveConfirmedAt,verified.signedAt())
                .set(DccControlledFileDO::getDelayedEffectiveSignatureId,verified.signatureId()).set(DccControlledFileDO::getDelayedEffectiveProcessInstanceId,verified.processInstanceId()))!=1)
            throw invalid("延迟生效确认保存冲突，未推进流程");
        file.setDelayedEffectiveTime(verified.selectedEffectiveTime());file.setDelayedEffectiveConfirmedAt(verified.signedAt());
        file.setDelayedEffectiveSignatureId(verified.signatureId());file.setDelayedEffectiveProcessInstanceId(verified.processInstanceId());
        audit.recordDelayedConfirmation(before,verified.processInstanceId(),signatures.selectById(verified.signatureId()));
        engineTasks.setVariablesLocal(request.getTaskId(),Map.of("dccVerifiedSignatureId",verified.signatureId(),"dccVerifiedSignatureActorId",actor,
                "dccVerifiedSignatureAction","APPROVE","dccDelayedConfirmationKey",request.getIdempotencyKey()));
        tasks.approveTask(actor,new BpmTaskApproveReqVO().setId(request.getTaskId()).setReason(request.getReason().trim()));
        confirmationEvidence.requireConfirmed(file,bound);
        return result(file,bound,verified);
    }
    private record Locked(DccControlledFileDO file,DccControlledFileMasterDO master){}
    private Locked lock(Long actor,Long id){
        var identity=fileMapper.selectById(id);Long tenant=TenantContextHolder.getRequiredTenantId();
        if(actor==null || identity==null || !Objects.equals(identity.getTenantId(),tenant) || !Objects.equals(identity.getRequesterId(),actor))throw invalid("只有本次原编制人可确认延迟生效");
        var master=masterMapper.selectByIdForUpdate(identity.getMasterId());var file=fileMapper.selectByIdAndTenantForUpdate(tenant,id);
        var user=accounts.getUser(actor);
        if(master==null || file==null || !Objects.equals(master.getTenantId(),tenant) || !Objects.equals(master.getId(),file.getMasterId())
                || !Objects.equals(file.getRequesterId(),actor) || Boolean.TRUE.equals(file.getDeleted()) || user==null || !Objects.equals(user.getTenantId(),tenant)
                || !Integer.valueOf(0).equals(user.getStatus()))throw invalid("原编制人账号、租户或正式版本身份无效");
        return new Locked(file,master);
    }
    private void requireExpected(DccBoundWorkflowContractResolver.BoundExecution bound,DccControlledFileDelayedEffectiveConfirmReqVO req){
        if(bound==null || !Objects.equals(bound.processInstanceId(),req.getProcessInstanceId()) || !Objects.equals(bound.attributeRound(),req.getAttributeRound())
                || !Objects.equals(bound.processDefinitionId(),req.getExpectedProcessDefinitionId()) || !Objects.equals(bound.definitionDigest(),req.getExpectedDefinitionDigest()))throw invalid("延迟确认请求的正式定义或轮次已变化");
    }
    private Task requireTask(Long actor,DccControlledFileDO file,DccBoundWorkflowContractResolver.BoundExecution bound,String taskId,String process,Integer round){
        var task=taskId==null?null:tasks.getTask(taskId);
        if(bound==null || !bound.delayedConfirmationSupported() || !"DELAYED".equals(file.getEffectiveMode()) || file.getEffectiveDate()!=null
                || !DccDelayedEffectiveConfirmationEvidence.STATUS.equals(file.getStatus()) || !Objects.equals(bound.attributeRound(),round)
                || !Objects.equals(file.getProcessInstanceId(),process) || task==null || task.isSuspended()
                || !Objects.equals(taskId,task.getId()) || !String.valueOf(file.getTenantId()).equals(task.getTenantId())
                || !Objects.equals(process,task.getProcessInstanceId()) || !Objects.equals(bound.processDefinitionId(),task.getProcessDefinitionId())
                || !DccDelayedEffectiveConfirmationEvidence.STAGE.equals(task.getTaskDefinitionKey()) || !String.valueOf(actor).equals(task.getAssignee()))
            throw invalid("延迟确认必须办理本次原编制人的精确活动任务");
        return task;
    }
    private void requirePrerequisites(DccControlledFileDO file,DccBoundWorkflowContractResolver.BoundExecution bound){
        var all=signatures.selectListByControlledFileId(file.getId());
        DccFrozenApprovalSignatures.requireComplete(file,routes.selectListByControlledFileId(file.getId()),all,assignments.selectListByControlledFileId(file.getId()),bound);
        lifecycle.requirePublicDocumentControlEvidence(file,bound);
        for(var signature:all){if(!Objects.equals(signature.getProcessInstanceId(),file.getProcessInstanceId()))continue;
            var verification=signatureManagement.verifySignatureEvidence(signature.getId());
            if(verification==null || !Objects.equals(verification.getSignatureId(),signature.getId()) || !"VALID".equals(verification.getVerificationStatus()))throw invalid("前序审批签名证据无效");
            if("APPROVE".equals(signature.getActionType())){var task=tasks.getHistoricTask(signature.getTaskId());
                if(task==null || task.getEndTime()==null || !Objects.equals(task.getProcessInstanceId(),file.getProcessInstanceId())
                        || !Objects.equals(task.getProcessDefinitionId(),bound.processDefinitionId()) || !String.valueOf(file.getTenantId()).equals(task.getTenantId())
                        || !String.valueOf(signature.getActorId()).equals(task.getAssignee()) || task.getTaskLocalVariables()==null
                        || !Objects.equals(BpmTaskStatusEnum.APPROVE.getStatus(),task.getTaskLocalVariables().get(BpmnVariableConstants.TASK_VARIABLE_STATUS)))throw invalid("前序审批缺少真实完成任务");}
        }
        if(Boolean.TRUE.equals(file.getNeedTraining())){
            var events=history.createHistoricActivityInstanceQuery().processInstanceId(file.getProcessInstanceId()).activityId("TRAINING").list();
            var attachment=attachments.selectTrainingRecordForUpdate(file.getTenantId(),file.getId(),file.getProcessInstanceId());
            if(events.size()!=1 || events.get(0).getEndTime()==null || file.getTrainingRecordFileId()==null || attachment==null
                    || !Objects.equals(attachment.getTenantId(),file.getTenantId()) || !Objects.equals(attachment.getUploadedBy(),file.getRequesterId())
                    || !Objects.equals(attachment.getStorageFileId(),file.getTrainingRecordFileId()))throw invalid("本轮必要培训及原上传人正式记录尚未完成");
        } else if(!Boolean.FALSE.equals(file.getNeedTraining()))throw invalid("本轮培训选择缺失");
    }
    private DccControlledFileDelayedEffectiveConfirmRespVO.DccControlledFileDelayedEffectiveConfirmRespVOBuilder context(DccControlledFileDO file,DccBoundWorkflowContractResolver.BoundExecution bound,String task){
        return DccControlledFileDelayedEffectiveConfirmRespVO.builder().controlledFileId(file.getId()).taskId(task).processInstanceId(file.getProcessInstanceId()).attributeRound(bound.attributeRound())
                .processDefinitionId(bound.processDefinitionId()).definitionDigest(bound.definitionDigest()).versionNo(file.getVersionNo()).effectiveMode(file.getEffectiveMode())
                .stageCode(DccDelayedEffectiveConfirmationEvidence.STAGE).timeZone("Asia/Shanghai").serverTime(datePolicy.now());
    }
    private DccControlledFileDelayedEffectiveConfirmRespVO result(DccControlledFileDO file,DccBoundWorkflowContractResolver.BoundExecution bound,DccDelayedEffectiveConfirmationEvidence.VerifiedConfirmation v){
        return context(file,bound,v.taskId()).canConfirm(false).selectedEffectiveTime(v.selectedEffectiveTime()).signedAt(v.signedAt()).signatureId(v.signatureId()).evidenceHash(v.evidenceHash()).status("CONFIRMED").build();
    }
    private IllegalArgumentException invalid(String message){return new IllegalArgumentException(message);}
}
