package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMessageJobStatusEnum;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MESSAGE_JOB_REPLAY_NOT_ALLOWED;

@Service
@Slf4j
public class DccControlledFileMessageDeliveryService {

    @Resource
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource
    private NotifyMessageSendApi notifyMessageSendApi;
    @Resource
    private cn.iocoder.yudao.module.system.service.notify.NotifyMessageService notifyMessageService;
    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    private DccDistributionDispatchAttemptService dispatchAttemptService;

    public void dispatchMessageJob(DccControlledFileMessageJobDO messageJob, Map<String, Object> templateParams) {
        if (messageJob == null || messageJob.getId() == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Runnable action = () -> TenantUtils.execute(tenantId, () -> sendMessageJob(messageJob, templateParams));
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (RuntimeException ex) {
                        log.error("[afterCommit][DCC message delivery failed, messageJobId={}, businessType={}, failureType={}]",
                                messageJob.getId(), messageJob.getBusinessType(), ex.getClass().getSimpleName());
                    }
                }
            });
            return;
        }
        throw new IllegalStateException("DCC notification dispatch requires the business transaction; use replay for committed jobs");
    }

    public void replayMessageJob(DccControlledFileMessageJobDO messageJob) {
        if (messageJob == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        sendMessageJob(messageJob, null);
    }

    private void sendMessageJob(DccControlledFileMessageJobDO messageJob, Map<String, Object> templateParams) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        DccDistributionDispatchAttemptDOHolder attemptIdentity = new DccDistributionDispatchAttemptDOHolder();
        try {
            MessageAttempt attempt = transaction.execute(ignored -> {
                // Establish batch -> job lock order before starting a persistent new-distribution attempt.
                var identity=messageJobMapper.selectById(messageJob.getId());
                cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccDistributionDispatchAttemptDO persistedAttempt=null;
                if(identity!=null && identity.getDistributionBatchId()!=null && !"SENT".equals(identity.getStatus()))
                    persistedAttempt=dispatchAttemptService.startAttempt(identity.getDistributionBatchId(),identity.getId());
                DccControlledFileMessageJobDO current = requireLockedJob(tenantId, messageJob.getId());
                if (DccControlledFileMessageJobStatusEnum.SENT.getCode().equals(current.getStatus())) {
                    return null;
                }
                if (!DccControlledFileMessageJobStatusEnum.PENDING.getCode().equals(current.getStatus())
                        && !DccControlledFileMessageJobStatusEnum.FAILED.getCode().equals(current.getStatus())) {
                    throw exception(CONTROLLED_FILE_MESSAGE_JOB_REPLAY_NOT_ALLOWED);
                }
                if (current.getBusinessType() == null || current.getBusinessId() == null
                        || current.getRecipientUserId() == null || StrUtil.isBlank(current.getTemplateCode()))
                    throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
                Map<String, Object> params = "DISTRIBUTION".equals(current.getBusinessType()) || templateParams == null
                        ? resolveTemplateParams(current) : Map.copyOf(templateParams);
                if (messageJobMapper.updateById(DccControlledFileMessageJobDO.builder().id(current.getId())
                        .status("PENDING").errorMessage("").build()) != 1)
                    throw new IllegalStateException("DCC notification attempt was not persisted");
                if(persistedAttempt!=null){attemptIdentity.id=persistedAttempt.getId();attemptIdentity.batchId=persistedAttempt.getBatchId();}
                return new MessageAttempt(current, params);
            });
            if (attempt == null) return;
            // The stable job identity commits before the platform call. The platform owns its idempotent transaction.
            var attemptJob = attempt.job();
            NotifySendSingleToUserIdempotentReqDTO reqDTO = new NotifySendSingleToUserIdempotentReqDTO();
            reqDTO.setUserId(attemptJob.getRecipientUserId());
            reqDTO.setTemplateCode(attemptJob.getTemplateCode());
            reqDTO.setBusinessKey("DCC_MESSAGE_JOB:" + tenantId + ":" + attemptJob.getId());
            reqDTO.setTemplateParams(attempt.params());
            TransactionTemplate outside = new TransactionTemplate(transactionManager);
            outside.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
            PlatformSendEvidence evidence = outside.execute(ignored -> {
                Long messageId = notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(reqDTO);
                if (messageId == null || messageId <= 0)
                    throw new IllegalStateException("DCC notification platform message id is missing");
                LocalDateTime sentAt;
                if ("DISTRIBUTION".equals(attemptJob.getBusinessType())) {
                    var platformMessage = notifyMessageService.getNotifyMessage(messageId);
                    if (platformMessage == null || !Objects.equals(platformMessage.getTenantId(), tenantId)
                            || !Objects.equals(platformMessage.getUserId(), attemptJob.getRecipientUserId())
                            || !Objects.equals(platformMessage.getUserType(), 2)
                            || !Objects.equals(platformMessage.getBusinessKey(), reqDTO.getBusinessKey())
                            || !Objects.equals(platformMessage.getTemplateCode(), attemptJob.getTemplateCode())
                            || platformMessage.getCreateTime() == null)
                        throw new IllegalStateException("DCC notification platform send evidence is missing or mismatched");
                    sentAt = platformMessage.getCreateTime();
                } else {
                    sentAt = LocalDateTime.now();
                }
                return new PlatformSendEvidence(messageId, sentAt);
            });
            Long messageId = evidence.messageId();
            LocalDateTime sentAt = evidence.sentAt();
            transaction.executeWithoutResult(ignored -> {
                if(attemptIdentity.batchId!=null)dispatchAttemptService.lockBatch(attemptIdentity.batchId);
                DccControlledFileMessageJobDO current = requireLockedJob(tenantId, attemptJob.getId());
                if ("SENT".equals(current.getStatus())) {
                    if(attemptIdentity.id!=null)dispatchAttemptService.finishAttempt(attemptIdentity.batchId,attemptIdentity.id,true,messageId,null);
                    return;
                }
                if (!Objects.equals(current.getBusinessType(), attemptJob.getBusinessType())
                        || !Objects.equals(current.getBusinessId(), attemptJob.getBusinessId())
                        || !Objects.equals(current.getRecipientUserId(), attemptJob.getRecipientUserId())
                        || !Objects.equals(current.getTemplateCode(), attemptJob.getTemplateCode()))
                    throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
                if (messageJobMapper.updateById(DccControlledFileMessageJobDO.builder()
                        .id(current.getId())
                        .status(DccControlledFileMessageJobStatusEnum.SENT.getCode())
                        .errorMessage("")
                        .sentAt(sentAt)
                        .build()) != 1) {
                    throw new IllegalStateException("DCC notification result was not persisted");
                }
                if(attemptIdentity.id!=null)dispatchAttemptService.finishAttempt(attemptIdentity.batchId,attemptIdentity.id,true,messageId,null);
            });
        } catch (RuntimeException ex) {
            try {
                transaction.executeWithoutResult(ignored -> {
                    if(attemptIdentity.batchId!=null)dispatchAttemptService.lockBatch(attemptIdentity.batchId);
                    DccControlledFileMessageJobDO current = requireLockedJob(tenantId, messageJob.getId());
                    if (DccControlledFileMessageJobStatusEnum.SENT.getCode().equals(current.getStatus())) {
                        if(attemptIdentity.id!=null)dispatchAttemptService.finishAttempt(attemptIdentity.batchId,attemptIdentity.id,false,null,ex);
                        return;
                    }
                    if (messageJobMapper.updateById(DccControlledFileMessageJobDO.builder()
                            .id(current.getId())
                            .status(DccControlledFileMessageJobStatusEnum.FAILED.getCode())
                            .errorMessage(DccDistributionAuditSupport.failure(ex))
                            .build()) != 1) {
                        throw new IllegalStateException("DCC notification failure was not persisted");
                    }
                    if(attemptIdentity.id!=null)dispatchAttemptService.finishAttempt(attemptIdentity.batchId,attemptIdentity.id,false,null,ex);
                });
            } catch (RuntimeException persistenceFailure) {
                if (persistenceFailure != ex) {
                    ex.addSuppressed(persistenceFailure);
                }
            }
            throw ex;
        }
    }

    private DccControlledFileMessageJobDO requireLockedJob(Long tenantId, Long id) {
        DccControlledFileMessageJobDO job = messageJobMapper.selectByIdAndTenantForUpdate(tenantId, id);
        if (job == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        return job;
    }

    private record MessageAttempt(DccControlledFileMessageJobDO job, Map<String, Object> params) {}
    private record PlatformSendEvidence(Long messageId, LocalDateTime sentAt) {}
    private static final class DccDistributionDispatchAttemptDOHolder { Long id;Long batchId; }

    private Map<String, Object> resolveTemplateParams(DccControlledFileMessageJobDO messageJob) {
        return switch (messageJob.getBusinessType()) {
            case DccControlledFileFinalizationServiceImpl.MESSAGE_BUSINESS_TYPE_DISTRIBUTION ->
                    buildDistributionNotifyParams(messageJob);
            case DccControlledFileFinalizationServiceImpl.MESSAGE_BUSINESS_TYPE_TRAINING ->
                    buildBaseNotifyParams(resolveFileForTrainingJob(messageJob.getBusinessId()), false);
            case DccControlledFileObsoleteServiceImpl.MESSAGE_BUSINESS_TYPE_OBSOLETE ->
                    buildBaseNotifyParams(resolveFileForObsoleteJob(messageJob.getBusinessId()), true);
            default -> throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        };
    }

    private Map<String, Object> buildDistributionNotifyParams(DccControlledFileMessageJobDO job) {
        DccControlledFileDistributionDO distribution = distributionMapper.selectById(job.getBusinessId());
        if (distribution == null || distribution.getControlledFileId() == null
                || !"PUBLIC_FOLDER".equals(distribution.getDistributionMedium())
                || !Set.of("PENDING", "SENT", "READ", "ACKNOWLEDGED").contains(distribution.getStatus())) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        DccControlledFileDO file = controlledFileMapper.selectById(distribution.getControlledFileId());
        if (file == null || !Objects.equals(file.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || file.getControlledTime() == null || file.getActivatedTime() == null
                || file.getDistributedTime() == null || !Set.of("ACTIVE", "OBSOLETE", "SUPERSEDED").contains(file.getStatus())
                || StrUtil.isBlank(file.getVersionNo())) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        var recipients = distributionRecipientMapper.selectListByDistributionId(distribution.getId());
        if (recipients == null) throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        var matches = recipients.stream().filter(row -> row != null
                && Objects.equals(row.getMessageJobId(), job.getId())
                && Objects.equals(row.getUserId(), job.getRecipientUserId())).toList();
        if (matches.size() != 1 || matches.get(0).getId() == null)
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        Map<String, Object> params = buildBaseNotifyParams(file, false);
        if (DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE.equals(file.getEffectiveMode())
                || DccWorkflowDatePolicy.DELAYED.equals(file.getEffectiveMode())) {
            if (file.getEffectiveTime() == null) {
                throw new IllegalStateException("下发通知缺少实际生效时间");
            }
            params.put("effectiveDate", file.getEffectiveTime().toLocalDate().toString());
        }
        params.put("controlledFileId", file.getId().toString());
        params.put("distributionId", distribution.getId().toString());
        params.put("recipientId", matches.get(0).getId().toString());
        if (StrUtil.isNotBlank(file.getProcessInstanceId()))
            params.put("approvalProcessInstanceId", file.getProcessInstanceId());
        return params;
    }

    private DccControlledFileDO resolveFileForTrainingJob(Long trainingId) {
        DccControlledFileTrainingDO training = trainingMapper.selectById(trainingId);
        if (training == null || training.getControlledFileId() == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        DccControlledFileDO file = controlledFileMapper.selectById(training.getControlledFileId());
        if (file == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        return file;
    }

    private DccControlledFileDO resolveFileForObsoleteJob(Long controlledFileId) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        return file;
    }

    private Map<String, Object> buildBaseNotifyParams(DccControlledFileDO file, boolean includeReason) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("title", StrUtil.blankToDefault(file.getTitle(), file.getFileName()));
        params.put("version", StrUtil.blankToDefault(file.getVersionNo(), "-"));
        if (file.getEffectiveDate() != null) {
            params.put("effectiveDate", file.getEffectiveDate().toString());
        }
        if (includeReason) {
            params.put("reason", StrUtil.blankToDefault(file.getObsoleteReason(), "-"));
        }
        return params;
    }

    private String resolveFailureReason(RuntimeException ex) {
        String reason = StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName());
        return reason.replaceAll("(?i)(password|passwd|pwd|token|secret|authorization|api[_-]?key)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]")
                .replaceAll("(?i)Bearer\\s+[^\\s,;]+", "Bearer [REDACTED]");
    }
}
