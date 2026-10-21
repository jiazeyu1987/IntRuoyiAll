package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Master -> version lock order is shared by finalization and scheduled activation. */
@Service
@Slf4j
public class DccControlledFileLifecycleService {
    /** Official GxP SYSTEM_ACTOR identity, not a human account or electronic signer. */
    private static final Long SYSTEM_ACTOR_ID = -1L;
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private DccControlledFileObsoleteAuditMapper obsoleteAuditMapper;
    @Resource private ApplicationEventPublisher eventPublisher;
    @Resource private DccWorkflowDatePolicy datePolicy;
    @Resource private DccControlledFileVersionPolicy versionPolicy;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private DccObsoleteRetentionService obsoleteRetentionService;
    @Resource private DccWorkflowFileStateAudit fileStateAudit;
    @Resource private DccControlledContentAdapter platformAdapter;
    @Resource private DccControlledFileSignatureMapper approvalSignatureMapper;
    @Resource private DccElectronicSignatureManagementService signatureManagementService;
    @Resource private cn.iocoder.yudao.module.bpm.service.task.BpmTaskService bpmTaskService;
    @Resource private DccLifecycleEffectiveContractResolver effectiveContractResolver;
    @Resource private DccDelayedEffectiveConfirmationEvidence confirmationEvidence;
    // Batch -> existing distribution -> Finalization -> Lifecycle is resolved only when a real event is accepted.
    @Lazy @Resource private DccDistributionBatchService distributionBatchService;

    public LocalDateTime currentTime() { return datePolicy.now(); }

    public DccWorkflowDatePolicy.ReminderDates distributionReminderDates() { return datePolicy.reminderDates(); }

    /** W05 calls immediately after saving the real DOC_CONTROL_REVIEW signature, before BPM advances.
     * Effectiveness is a signed fact; it does not grant daily access or replace the current version. */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void recordStandardizationSignatureEffect(Long actorId,Long fileId,Long signatureId) {
        requireTransaction();
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(actorId==null || actorId<=0 || fileId==null || fileId<=0 || signatureId==null || signatureId<=0)
            throw new IllegalArgumentException("文控生效需要真实操作者、文件版本及签名身份");
        var identity=controlledFileMapper.selectById(fileId);
        if(identity==null || !Objects.equals(identity.getTenantId(),tenant))
            throw new IllegalArgumentException("文控生效文件不存在或租户不匹配");
        var master=masterMapper.selectByIdForUpdate(identity.getMasterId());
        var file=controlledFileMapper.selectByIdAndTenantForUpdate(tenant,fileId);
        if(master==null || file==null || !Objects.equals(master.getTenantId(),tenant)
                || !Objects.equals(file.getMasterId(),master.getId()) || Boolean.TRUE.equals(file.getDeleted())
                || !Set.of(DccControlledFileProcessDefinitionKeys.UPLOAD,DccControlledFileProcessDefinitionKeys.REVISION)
                    .contains(file.getProcessDefinitionKey()==null?"":file.getProcessDefinitionKey()))
            throw new IllegalArgumentException("文控生效必须对应本轮正式上传或升版");
        boolean legacyDateContract=requireEffectiveContract(file,true);
        var signature=requireStandardizationSignature(file,signatureId);
        if(!Objects.equals(signature.getActorId(),actorId))
            throw new IllegalArgumentException("文控生效操作者与正式签名不匹配");
        if(legacyDateContract || DccWorkflowDatePolicy.PRESET_DATE.equals(file.getEffectiveMode())
                || DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())) return;
        if(file.getEffectiveTime()!=null || file.getEffectiveSignatureId()!=null || file.getEffectiveProcessInstanceId()!=null) {
            if(!Objects.equals(file.getEffectiveTime(),signature.getSignedAt())
                    || !Objects.equals(file.getEffectiveSignatureId(),signatureId)
                    || !Objects.equals(file.getEffectiveProcessInstanceId(),file.getProcessInstanceId()))
                throw new IllegalArgumentException("本轮已记录不同文控签名生效事实");
            return;
        }
        if(!"PENDING_DOC_CONTROL_REVIEW".equals(file.getStatus()) || file.getControlledTime()!=null || file.getActivatedTime()!=null)
            throw new IllegalStateException("只有本轮文控审核完成签名可首次记录生效事实");
        var task=bpmTaskService.getTask(signature.getTaskId());
        if(task==null || !Objects.equals(task.getId(),signature.getTaskId())
                || !Objects.equals(task.getProcessInstanceId(),file.getProcessInstanceId())
                || !tenant.toString().equals(task.getTenantId()) || !"DOC_CONTROL_REVIEW".equals(task.getTaskDefinitionKey())
                || !actorId.toString().equals(task.getAssignee()))
            throw new IllegalStateException("文控生效签名不属于当前正式文控任务");
        var beforeEffect=fileStateAudit.capture(file,master);
        requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(fileId)
                .effectiveTime(signature.getSignedAt()).effectiveSignatureId(signatureId)
                .effectiveProcessInstanceId(file.getProcessInstanceId()).build()));
        file.setEffectiveTime(signature.getSignedAt());file.setEffectiveSignatureId(signatureId);
        file.setEffectiveProcessInstanceId(file.getProcessInstanceId());
        fileStateAudit.recordSignatureEffect(beforeEffect,file.getProcessInstanceId(),signature);
        emit("EFFECTIVE",file,master.getCurrentActiveControlledFileId(),signature.getSignedAt());
    }

    private DccControlledFileSignatureDO requireStandardizationSignature(DccControlledFileDO file,Long signatureId) {
        var signature=signatureId==null?null:approvalSignatureMapper.selectById(signatureId);
        if(signature==null || Boolean.TRUE.equals(signature.getDeleted()) || !Objects.equals(signature.getId(),signatureId)
                || !Objects.equals(signature.getControlledFileId(),file.getId()) || !Objects.equals(signature.getRevisionId(),file.getId())
                || !Objects.equals(signature.getVersionNo(),file.getVersionNo()) || file.getProcessInstanceId()==null
                || file.getProcessInstanceId().isBlank() || !file.getProcessInstanceId().equals(signature.getProcessInstanceId())
                || !"APPROVE".equals(signature.getActionType()) || !"DOC_CONTROL_REVIEW_APPROVE".equals(signature.getMeaningCode())
                || !Boolean.TRUE.equals(signature.getPasswordVerified()) || signature.getSignedAt()==null
                || signature.getTaskId()==null || signature.getTaskId().isBlank() || signature.getActorId()==null || signature.getActorId()<=0
                || !"VALID".equals(signature.getEvidenceStatus()) || signature.getEvidenceHash()==null || signature.getEvidenceHash().isBlank()
                || !"v4-workflow".equals(signature.getEvidencePayloadVersion()))
            throw new IllegalArgumentException("文控生效缺少同版本同轮次的真实审核签名");
        var verification=signatureManagementService.verifySignatureEvidence(signatureId);
        if(verification==null || !Objects.equals(verification.getSignatureId(),signatureId)
                || !"VALID".equals(verification.getVerificationStatus()))
            throw new IllegalStateException("文控生效签名证据验证失败");
        return signature;
    }

    private void requireSignedEffect(DccControlledFileDO file) {
        var signature=requireStandardizationSignature(file,file.getEffectiveSignatureId());
        if(!Objects.equals(file.getEffectiveTime(),signature.getSignedAt())
                || !Objects.equals(file.getEffectiveProcessInstanceId(),file.getProcessInstanceId()))
            throw new IllegalStateException("已签名生效事实与本轮真实签名不一致");
    }

    /** Reused by PUBLIC training and finalization; a saved effectiveness timestamp is not approval evidence. */
    DccControlledFileSignatureDO requirePublicDocumentControlEvidence(DccControlledFileDO file,
            DccBoundWorkflowContractResolver.BoundExecution bound) {
        if(bound==null || !bound.trainingAfterDocumentControl() || file==null
                || !Objects.equals(bound.tenantId(),TenantContextHolder.getRequiredTenantId())
                || !Objects.equals(bound.tenantId(),file.getTenantId()) || !Objects.equals(bound.controlledFileId(),file.getId())
                || !Objects.equals(bound.processInstanceId(),file.getProcessInstanceId()))
            throw new IllegalStateException("文控后培训需要同轮次正式PUBLIC契约");
        var docs=Objects.requireNonNull(approvalSignatureMapper.selectListByControlledFileId(file.getId()),"审批签名投影")
                .stream().filter(Objects::nonNull).filter(s->Objects.equals(file.getProcessInstanceId(),s.getProcessInstanceId())
                    && "APPROVE".equals(s.getActionType()) && "DOC_CONTROL_REVIEW_APPROVE".equals(s.getMeaningCode())
                    && !Boolean.TRUE.equals(s.getDeleted())).toList();
        if(docs.size()!=1) throw new IllegalStateException("本轮文控审核正式签名不唯一或缺失");
        var doc=requireStandardizationSignature(file,docs.get(0).getId());
        requireCompletedPublicTask(file,bound,doc,"DOC_CONTROL_REVIEW");
        var owner=file.getFileOwnerSignatureId()==null?null:approvalSignatureMapper.selectById(file.getFileOwnerSignatureId());
        if(owner==null || file.getFileOwnerUserId()==null || file.getFileOwnerUserId()<=0
                || file.getFileOwnerUsernameSnapshot()==null || file.getFileOwnerNicknameSnapshot()==null
                || !Objects.equals(file.getFileOwnerProcessInstanceId(),file.getProcessInstanceId())
                || !Objects.equals(file.getFileOwnerSelectedTime(),owner.getSignedAt())
                || !Objects.equals(file.getFileOwnerApprovalTaskId(),owner.getTaskId())
                || !Objects.equals(owner.getControlledFileId(),file.getId()) || !Objects.equals(owner.getRevisionId(),file.getId())
                || !Objects.equals(owner.getVersionNo(),file.getVersionNo()) || !Objects.equals(owner.getProcessInstanceId(),file.getProcessInstanceId())
                || !"APPROVE".equals(owner.getActionType()) || !"MATRIX_APPROVAL_APPROVE".equals(owner.getMeaningCode())
                || !Boolean.TRUE.equals(owner.getPasswordVerified()) || !"VALID".equals(owner.getEvidenceStatus())
                || owner.getSignedAt()==null || owner.getEvidenceHash()==null || owner.getEvidenceHash().isBlank()
                || !"v4-workflow".equals(owner.getEvidencePayloadVersion()) || Boolean.TRUE.equals(owner.getDeleted()))
            throw new IllegalStateException("文件负责人缺少同轮次真实批准证据");
        String suffix="；批准文件负责人事实："+cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                List.of(file.getFileOwnerUserId(),file.getFileOwnerUsernameSnapshot(),file.getFileOwnerNicknameSnapshot()));
        if(owner.getComment()==null || !owner.getComment().endsWith(suffix) || owner.getComment().length()<=suffix.length())
            throw new IllegalStateException("文件负责人投影与原签名载荷不一致");
        var verified=signatureManagementService.verifySignatureEvidence(owner.getId());
        if(verified==null || !Objects.equals(owner.getId(),verified.getSignatureId()) || !"VALID".equals(verified.getVerificationStatus()))
            throw new IllegalStateException("文件负责人批准签名验签失败");
        requireCompletedPublicTask(file,bound,owner,"MATRIX_APPROVAL");
        return doc;
    }

    private void requireCompletedPublicTask(DccControlledFileDO file,DccBoundWorkflowContractResolver.BoundExecution bound,
            DccControlledFileSignatureDO signature,String stage) {
        var task=bpmTaskService.getHistoricTask(signature.getTaskId());
        if(task==null || !Objects.equals(task.getId(),signature.getTaskId())
                || !Objects.equals(task.getProcessInstanceId(),file.getProcessInstanceId())
                || !Objects.equals(task.getProcessDefinitionId(),bound.processDefinitionId())
                || !String.valueOf(file.getTenantId()).equals(task.getTenantId()) || !stage.equals(task.getTaskDefinitionKey())
                || signature.getActorId()==null || !String.valueOf(signature.getActorId()).equals(task.getAssignee())
                || task.getEndTime()==null || task.getTaskLocalVariables()==null
                || !Integer.valueOf(cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum.APPROVE.getStatus()).equals(task.getTaskLocalVariables()
                    .get(cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.TASK_VARIABLE_STATUS)))
            throw new IllegalStateException("PUBLIC文控或负责人批准任务缺少真实完成事实");
    }

    private boolean requireEffectiveContract(DccControlledFileDO file,boolean atControl) {
        var contract=effectiveContractResolver.resolve(file);
        if(contract==null || !Objects.equals(contract.tenantId(),file.getTenantId())
                || !Objects.equals(contract.controlledFileId(),file.getId())
                || !Objects.equals(contract.processInstanceId(),file.getProcessInstanceId()))
            throw new IllegalArgumentException("生效方式缺少同版本同轮次正式流程契约");
        if(contract.rule()==DccLifecycleEffectiveContract.Rule.DELAYED_CONFIRMATION_MODE) {
            if(!DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())
                    && !DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(file.getEffectiveMode()))
                throw new IllegalArgumentException("本轮新确认流程仅允许立即或延迟生效");
            datePolicy.requireSubmittedEffectiveConfiguration(file.getEffectiveMode(),file.getEffectiveDate());
            return false;
        }
        if(contract.rule()==DccLifecycleEffectiveContract.Rule.NEW_EXPLICIT_MODE) {
            if(atControl) datePolicy.requireSubmittedEffectiveConfiguration(file.getEffectiveMode(),file.getEffectiveDate());
            else if(!DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(file.getEffectiveMode())
                    && !DccWorkflowDatePolicy.PRESET_DATE.equals(file.getEffectiveMode()))
                throw new IllegalArgumentException("本轮新生效方式未识别");
            return false;
        }
        // No inference from null: only the instance's exact registered frozen graph can select legacy semantics.
        if(contract.rule()!=DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE || file.getEffectiveMode()!=null)
            throw new IllegalArgumentException("历史未知生效方式没有正式冻结流程契约，不能执行新写入");
        if(file.getEffectiveTime()!=null || file.getEffectiveSignatureId()!=null || file.getEffectiveProcessInstanceId()!=null)
            throw new IllegalStateException("旧日期契约不可混入推断的新生效事实");
        if(atControl) datePolicy.requireReviewDate(file.getEffectiveDate());
        else if(file.getEffectiveDate()==null) throw new IllegalStateException("旧正式日期契约缺少已保存生效日期");
        return true;
    }

    /** Review boundary for W05: the already-submitted explicit selection or the exact registered legacy graph.
     * New submission validation remains the strict String/date policy, never this legacy read boundary. */
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public void requireReviewEffectiveSelection(DccControlledFileDO file) {
        requireTransaction();
        if(file==null || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId()))
            throw new IllegalArgumentException("文控审核日期契约与当前租户不匹配");
        requireEffectiveContract(file,true);
    }

    void completeControl(DccControlledFileDO file, DccControlledFileMasterDO master, Long actorId) {
        requireTransaction();
        Long tenant = TenantContextHolder.getRequiredTenantId();
        if (file == null || master == null || !Objects.equals(file.getMasterId(),master.getId())
                || !Objects.equals(file.getTenantId(),tenant) || !Objects.equals(master.getTenantId(),tenant)
                || file.getProcessInstanceId() == null || file.getProcessInstanceId().isBlank())
            throw new IllegalArgumentException("受控文件、轮次及租户身份不匹配");
        if (file.getControlledTime() != null) {
            if(DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())) {
                requireEffectiveContract(file,false);
                requireControlledConfirmation(file);
            }
            return;
        }
        requireEffectiveContract(file,true);
        var confirmation=DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())
                ? confirmationEvidence.requireConfirmed(file) : null;
        if(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(file.getEffectiveMode())) requireSignedEffect(file);
        else if(file.getEffectiveTime()!=null || file.getEffectiveSignatureId()!=null || file.getEffectiveProcessInstanceId()!=null)
            throw new IllegalStateException("按日期生效必须受控且到期后才能首次生成生效事实");
        LocalDateTime now = datePolicy.now();
        if (master.getLatestControlledFileId() != null && !master.getLatestControlledFileId().equals(file.getId())) {
            var latest = controlledFileMapper.selectByIdAndTenantForUpdate(tenant,master.getLatestControlledFileId());
            if (latest == null || latest.getControlledTime() == null
                    || !Objects.equals(latest.getMasterId(),master.getId()))
                throw new IllegalStateException("最新受控定位无正式受控事实");
            var candidate = versionPolicy.parseStored(file);
            var previous = versionPolicy.parseStored(latest);
            if(candidate==null || previous==null || candidate.compareTo(previous)<=0)
                throw new IllegalStateException("受控版本不得倒退");
        }
        if (file.getPublishedFileId() == null || file.getStampedFileId() == null)
            throw new IllegalStateException("受控文件未生成，不能完成受控");
        var beforeControl = fileStateAudit.capture(file, master);
        closeLowerWorkingIterations(file,master,tenant);
        LocalDateTime controlledAt=confirmation==null ? now.truncatedTo(java.time.temporal.ChronoUnit.MICROS) : confirmation.signedAt();
        DccControlledFileDO update = DccControlledFileDO.builder().id(file.getId())
                .controlledTime(controlledAt).publishedTime(now).status("CONTROLLED_PENDING_EFFECTIVE")
                .publishedFileId(file.getPublishedFileId()).stampedFileId(file.getStampedFileId())
                .stampedTime(file.getStampedTime()).finalizationError("").build();
        requireOne(controlledFileMapper.updateById(update));
        requireOne(masterMapper.updateById(DccControlledFileMasterDO.builder().id(master.getId())
                .latestControlledFileId(file.getId()).status("ACTIVE_CHAIN").build()));
        file.setControlledTime(controlledAt);
        file.setPublishedTime(now);
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE");
        master.setLatestControlledFileId(file.getId());
        master.setStatus("ACTIVE_CHAIN");
        platformAdapter.recordNativeControl(file,actorId,lifecycleEventKey("CONTROLLED",file));
        fileStateAudit.recordControl(beforeControl, file.getProcessInstanceId());
        emit("CONTROLLED", file, master.getCurrentActiveControlledFileId(), controlledAt);
        if (DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(file.getEffectiveMode())
                || (confirmation!=null ? !confirmation.selectedEffectiveTime().isAfter(now)
                    : !file.getEffectiveDate().isAfter(now.toLocalDate()))) activateLocked(file, master, now);
    }

    private void closeLowerWorkingIterations(DccControlledFileDO controlled,DccControlledFileMasterDO master,Long tenant) {
        var target=versionPolicy.parseStored(controlled);
        if(target==null) throw new IllegalStateException("受控候选缺少正式版本号");
        var chain=controlledFileMapper.selectListByMasterIdForUpdate(master.getId());
        if(chain==null) throw new IllegalStateException("受控版本链缺失");
        var lower=new java.util.ArrayList<DccControlledFileDO>();
        for(var row:chain) {
            if(row==null || !"WORKING".equals(row.getStatus()) || Objects.equals(row.getId(),controlled.getId())) continue;
            if(!Objects.equals(row.getTenantId(),tenant) || !Objects.equals(row.getMasterId(),master.getId()))
                throw new IllegalStateException("工作稿与受控链租户身份不匹配");
            var version=versionPolicy.parseStored(row);
            if(version==null) throw new IllegalStateException("工作稿版本号不合法："+row.getId());
            if(version.compareTo(target)<0) lower.add(row);
        }
        for(var row:lower) requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(row.getId())
                .status("SUPERSEDED").supersededByFileId(controlled.getId()).build()));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean activateDue(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        DccControlledFileDO identity = controlledFileMapper.selectById(id);
        if (identity == null || !Objects.equals(tenantId, identity.getTenantId()))
            throw new IllegalArgumentException("生效文件不存在或租户不匹配");
        DccControlledFileMasterDO master = masterMapper.selectByIdForUpdate(identity.getMasterId());
        DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, id);
        if (master == null || file == null || !Objects.equals(master.getId(), file.getMasterId())
                || !Objects.equals(master.getTenantId(), tenantId))
            throw new IllegalStateException("生效文件与逻辑文件身份不匹配");
        if("OBSOLETE".equals(file.getStatus())) {
            if(file.getControlledTime()==null || file.getObsoletedTime()==null
                    || Objects.equals(master.getCurrentActiveControlledFileId(),file.getId()))
                throw new IllegalStateException("已作废版本的正式事实或执行定位异常");
            return false;
        }
        if (file.getActivatedTime() != null) return false;
        LocalDateTime now = datePolicy.now();
        if (file.getControlledTime() == null
                || !"CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()))
            throw new IllegalStateException("文件未受控或不在待生效状态");
        boolean legacyDateContract=requireEffectiveContract(file,false);
        if(DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())) {
            var confirmation=requireControlledConfirmation(file);
            if(confirmation.selectedEffectiveTime().isAfter(now)) return false;
        } else if(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(file.getEffectiveMode())) {
            if(file.getEffectiveDate()!=null) throw new IllegalStateException("签名生效方式不得带预设日期");
            requireSignedEffect(file);
        } else if(legacyDateContract || DccWorkflowDatePolicy.PRESET_DATE.equals(file.getEffectiveMode())) {
            if(file.getEffectiveDate()==null) throw new IllegalStateException("按日期生效缺少正式预设日期");
            if(file.getEffectiveDate().isAfter(now.toLocalDate())) return false;
        } else throw new IllegalArgumentException("历史未知生效方式不能推断为预设日期执行新生效写入");
        return activateLocked(file, master, now);
    }

    private boolean activateLocked(DccControlledFileDO file, DccControlledFileMasterDO master, LocalDateTime now) {
        requireTransaction();
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var chain=Objects.requireNonNull(controlledFileMapper.selectListByMasterIdForUpdate(master.getId()),"locked controlled version chain");
        var candidate=versionPolicy.parseStored(file);
        if(candidate==null || candidate.isWorkingIteration() || file.getControlledTime()==null
                || !Objects.equals(file.getTenantId(),tenant) || !Objects.equals(file.getMasterId(),master.getId()))
            throw new IllegalStateException("待生效版本缺少正式受控身份");
        Long oldId=master.getCurrentActiveControlledFileId();
        var current=oldId==null?null:chain.stream().filter(row->Objects.equals(row.getId(),oldId)).findFirst()
                .orElseThrow(()->new IllegalStateException("当前执行定位未命中受控链"));
        if(current!=null && (!Objects.equals(current.getTenantId(),tenant) || !"ACTIVE".equals(current.getStatus())
                || current.getControlledTime()==null || current.getActivatedTime()==null))
            throw new IllegalStateException("当前执行版本身份或状态异常");
        if(current!=null && !Objects.equals(oldId,file.getId())) {
            var executing=versionPolicy.parseStored(current);
            if(executing==null || executing.isWorkingIteration())throw new IllegalStateException("当前执行版本号异常");
            if(candidate.compareTo(executing)<0) {
                // A pre-fix pending lower version is retired against the real current execution, never activated backwards.
                var retired=obsoleteLowerControlled(current,master,chain,now);
                for(var before:retired)fileStateAudit.recordAutomaticObsolete(before,current.getProcessInstanceId());
                return false;
            }
            if(candidate.compareTo(executing)==0)throw new IllegalStateException("待生效与执行版本不得重复正式版本号");
        }
        var beforeActivation=fileStateAudit.capture(file,master);
        if(DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())) {
            var confirmation=requireControlledConfirmation(file);
            if(confirmation.selectedEffectiveTime().isAfter(now))
                throw new IllegalStateException("延迟生效时间尚未到达，不能启用或下发");
            if(file.getEffectiveTime()==null) {
                if(file.getEffectiveSignatureId()!=null || file.getEffectiveProcessInstanceId()!=null)
                    throw new IllegalStateException("延迟生效存在不完整或混用的立即生效事实");
                requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId())
                        .effectiveTime(now).effectiveProcessInstanceId(file.getProcessInstanceId()).build()));
                file.setEffectiveTime(now);file.setEffectiveProcessInstanceId(file.getProcessInstanceId());
                emit("EFFECTIVE",file,oldId,now);
            } else if(file.getEffectiveSignatureId()!=null
                    || !Objects.equals(file.getEffectiveProcessInstanceId(),file.getProcessInstanceId()))
                throw new IllegalStateException("延迟实际生效事实含混用签名或错误轮次");
        } else if(DccWorkflowDatePolicy.PRESET_DATE.equals(file.getEffectiveMode())) {
            if(file.getEffectiveTime()==null) {
                if(file.getEffectiveSignatureId()!=null || file.getEffectiveProcessInstanceId()!=null)
                    throw new IllegalStateException("按日期生效存在不完整或伪造签名事实");
                requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId())
                        .effectiveTime(now).effectiveProcessInstanceId(file.getProcessInstanceId()).build()));
                file.setEffectiveTime(now);file.setEffectiveProcessInstanceId(file.getProcessInstanceId());
                emit("EFFECTIVE",file,oldId,now);
            } else if(file.getEffectiveSignatureId()!=null || !Objects.equals(file.getEffectiveProcessInstanceId(),file.getProcessInstanceId()))
                throw new IllegalStateException("按日期生效事实含伪造签名或错误轮次");
        }
        if(file.getEffectiveMode()!=null && (file.getEffectiveTime()==null || !Objects.equals(file.getEffectiveProcessInstanceId(),file.getProcessInstanceId())))
            throw new IllegalStateException("日常启用必须同时持有正式受控及本轮生效事实");
        var beforeObsolete=obsoleteLowerControlled(file,master,chain,now);
        LocalDateTime activatedAt=now.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(file.getId())
                .activatedTime(activatedAt).status("ACTIVE").build()));
        requireOne(masterMapper.updateById(DccControlledFileMasterDO.builder().id(master.getId())
                .currentActiveControlledFileId(file.getId()).status("ACTIVE_CHAIN").build()));
        file.setActivatedTime(activatedAt);file.setStatus("ACTIVE");master.setCurrentActiveControlledFileId(file.getId());master.setStatus("ACTIVE_CHAIN");
        platformAdapter.recordNativeActivation(file,SYSTEM_ACTOR_ID,lifecycleEventKey("ACTIVATED",file));
        for(var before:beforeObsolete)fileStateAudit.recordAutomaticObsolete(before,file.getProcessInstanceId());
        fileStateAudit.recordActivation(beforeActivation,file.getProcessInstanceId());
        emit("ACTIVATED",file,oldId,activatedAt);
        return true;
    }

    /** All rows are already locked beneath the same Master; dates/artifacts and original approval evidence stay untouched. */
    private List<DccWorkflowFileStateAudit.Snapshot> obsoleteLowerControlled(DccControlledFileDO successor,
            DccControlledFileMasterDO master,List<DccControlledFileDO> chain,LocalDateTime now) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var target=versionPolicy.parseStored(successor);
        if(target==null || target.isWorkingIteration() || successor.getProcessInstanceId()==null || successor.getProcessInstanceId().isBlank())
            throw new IllegalStateException("自动作废缺少真实后继受控版本/审批轮次");
        var lower=new java.util.ArrayList<DccControlledFileDO>();
        for(var row:chain) {
            if(row==null || !Objects.equals(row.getTenantId(),tenant) || !Objects.equals(row.getMasterId(),master.getId()))
                throw new IllegalStateException("受控版本链租户/逻辑身份异常");
            if(!Set.of("ACTIVE","CONTROLLED_PENDING_EFFECTIVE").contains(row.getStatus()))continue;
            if("ACTIVE".equals(row.getStatus()) && !Objects.equals(row.getId(),master.getCurrentActiveControlledFileId()))
                throw new IllegalStateException("受控版本链有执行定位之外的ACTIVE版本");
            var version=versionPolicy.parseStored(row);
            if(row.getControlledTime()==null || version==null || version.isWorkingIteration())
                throw new IllegalStateException("受控版本链缺少正式受控事实");
            if(version.compareTo(target)<0)lower.add(row);
        }
        var before=new java.util.ArrayList<DccWorkflowFileStateAudit.Snapshot>();
        for(var row:lower)before.add(fileStateAudit.capture(row,master));
        for(var row:lower) {
            requireOne(controlledFileMapper.updateById(DccControlledFileDO.builder().id(row.getId())
                    .status("OBSOLETE").obsoletedTime(now).obsoletedBy(SYSTEM_ACTOR_ID).supersededByFileId(successor.getId())
                    .obsoleteReason("新版生效自动作废").build()));
            requireOne(obsoleteAuditMapper.insert(DccControlledFileObsoleteAuditDO.builder().controlledFileId(row.getId())
                    .operatorId(SYSTEM_ACTOR_ID).obsoleteReason("新版生效自动作废").statusBefore(row.getStatus()).statusAfter("OBSOLETE").build()));
            platformAdapter.recordNativeObsoleted(row,successor,SYSTEM_ACTOR_ID,
                    "DCC:OBSOLETE:"+cn.hutool.crypto.digest.DigestUtil.sha256Hex(
                            lifecycleEventKey("ACTIVATED",successor)+":"+row.getId()));
        }
        if(!lower.isEmpty())obsoleteRetentionService.retain(tenant,master.getId(),now);
        return before;
    }

    public List<DccControlledFileDO> pendingDistribution(boolean remindersOnly) {
        return pendingDistribution(remindersOnly,null);
    }

    public List<DccControlledFileDO> pendingDistribution(boolean remindersOnly,DccWorkflowDatePolicy.ReminderDates dates) {
        if(remindersOnly) throw new IllegalStateException("文控提醒策略尚未批准");
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        var query = new LambdaQueryWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getTenantId, tenantId)
                .eq(DccControlledFileDO::getStatus, "ACTIVE")
                .isNotNull(DccControlledFileDO::getControlledTime)
                .isNotNull(DccControlledFileDO::getActivatedTime)
                .isNull(DccControlledFileDO::getDistributedTime);
        return controlledFileMapper.selectList(query.orderByAsc(DccControlledFileDO::getActivatedTime)
                .orderByAsc(DccControlledFileDO::getId));
    }

    public List<DccControlledFileDO> dueVersions() {
        LocalDateTime now=datePolicy.now();
        return controlledFileMapper.selectList(new LambdaQueryWrapper<DccControlledFileDO>()
                .eq(DccControlledFileDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .eq(DccControlledFileDO::getStatus, "CONTROLLED_PENDING_EFFECTIVE")
                .and(q->q.and(delayed->delayed.eq(DccControlledFileDO::getEffectiveMode,DccWorkflowDatePolicy.DELAYED)
                        .le(DccControlledFileDO::getDelayedEffectiveTime,now))
                    .or(dated->dated.and(mode->mode.isNull(DccControlledFileDO::getEffectiveMode)
                            .or().eq(DccControlledFileDO::getEffectiveMode,DccWorkflowDatePolicy.PRESET_DATE))
                        .le(DccControlledFileDO::getEffectiveDate,now.toLocalDate())))
                .orderByAsc(DccControlledFileDO::getEffectiveDate).orderByAsc(DccControlledFileDO::getId));
    }

    private DccDelayedEffectiveConfirmationEvidence.VerifiedConfirmation requireControlledConfirmation(DccControlledFileDO file) {
        var confirmation=confirmationEvidence.requireConfirmed(file);
        if(!Objects.equals(file.getControlledTime(),confirmation.signedAt()))
            throw new IllegalStateException("延迟受控时间与本轮真实确认签名不一致");
        return confirmation;
    }

    private void emit(String type, DccControlledFileDO file, Long oldId, LocalDateTime now) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        String eventKey = lifecycleEventKey(type,file);
        requireOne(jdbcTemplate.update("""
                INSERT INTO dcc_workflow_lifecycle_event
                  (tenant_id,event_key,event_type,master_id,controlled_file_id,previous_active_file_id,
                   version_no,approval_process_instance_id,occurred_at,delivery_status)
                VALUES (?,?,?,?,?,?,?,?,?,'PENDING')
                """, tenant, eventKey, type, file.getMasterId(), file.getId(), oldId,
                file.getVersionNo(), file.getProcessInstanceId(), now));
        var event=new DccControlledFileLifecycleEvent(
                eventKey,
                type, tenant, file.getMasterId(), file.getId(), oldId, file.getVersionNo(),
                file.getProcessInstanceId(), now);
        if("ACTIVATED".equals(type)) {
            requireTransaction();
            if(!TransactionSynchronizationManager.isSynchronizationActive())
                throw new IllegalStateException("自动下发接收需要生效事务的正式提交同步边界");
            Long batchId=distributionBatchService.acceptActivatedEvent(event);
            if(batchId==null || batchId<=0)
                throw new IllegalStateException("生效事件没有得到真实持久下发批次身份");
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void afterCommit() {
                    TenantUtils.execute(tenant,()->{
                        try {
                            var result=distributionBatchService.processAutomaticBatch(batchId);
                            if(result==null || !Objects.equals(result.batchId(),batchId))
                                throw new IllegalStateException("提交后下发准备返回的批次身份不一致");
                            if("FAILED".equals(result.status()))
                                log.warn("DCC distribution preparation blocked after committed activation: tenant={}, file={}, batch={}, code={}",
                                        tenant,file.getId(),batchId,result.errorCode());
                        } catch(RuntimeException failure) {
                            // The acceptance is durable and recoverable. An independent prepare/audit failure
                            // cannot turn a committed ACTIVE file into a failed finalization or abort later callbacks.
                            log.error("DCC distribution preparation failed after committed activation; persisted batch remains recoverable: tenant={}, file={}, batch={}, failureType={}",
                                    tenant,file.getId(),batchId,failure.getClass().getSimpleName());
                        }
                    });
                }
            });
        }
        eventPublisher.publishEvent(event);
    }

    private String lifecycleEventKey(String type,DccControlledFileDO file) {
        return "DCC:"+TenantContextHolder.getRequiredTenantId()+":"+file.getId()+":"+file.getProcessInstanceId()+":"+type;
    }

    private void requireTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("受控与生效必须在正式事务内执行");
    }

    private void requireOne(int rows) {
        if (rows != 1) throw new IllegalStateException("生命周期持久化行数异常：" + rows);
    }
}
