package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
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
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private PlatformTransactionManager transactionManager;

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
        action.run();
    }

    public void replayMessageJob(DccControlledFileMessageJobDO messageJob) {
        if (messageJob == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        String status = messageJob.getStatus();
        if (!DccControlledFileMessageJobStatusEnum.PENDING.getCode().equals(status)
                && !DccControlledFileMessageJobStatusEnum.FAILED.getCode().equals(status)) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_REPLAY_NOT_ALLOWED);
        }
        sendMessageJob(messageJob, resolveTemplateParams(messageJob));
    }

    private void sendMessageJob(DccControlledFileMessageJobDO messageJob, Map<String, Object> templateParams) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        try {
            transaction.executeWithoutResult(ignored -> {
                DccControlledFileMessageJobDO current = requireLockedJob(tenantId, messageJob.getId());
                if (DccControlledFileMessageJobStatusEnum.SENT.getCode().equals(current.getStatus())) {
                    return;
                }
                if (!DccControlledFileMessageJobStatusEnum.PENDING.getCode().equals(current.getStatus())
                        && !DccControlledFileMessageJobStatusEnum.FAILED.getCode().equals(current.getStatus())) {
                    throw exception(CONTROLLED_FILE_MESSAGE_JOB_REPLAY_NOT_ALLOWED);
                }
                NotifySendSingleToUserIdempotentReqDTO reqDTO = new NotifySendSingleToUserIdempotentReqDTO();
                reqDTO.setUserId(current.getRecipientUserId());
                reqDTO.setTemplateCode(current.getTemplateCode());
                reqDTO.setBusinessKey("DCC_MESSAGE_JOB:" + tenantId + ":" + current.getId());
                reqDTO.setTemplateParams(templateParams);
                Long messageId = notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(reqDTO);
                if (messageId == null) {
                    throw new IllegalStateException("DCC notification platform message id is missing");
                }
                if (messageJobMapper.updateById(DccControlledFileMessageJobDO.builder()
                        .id(current.getId())
                        .status(DccControlledFileMessageJobStatusEnum.SENT.getCode())
                        .errorMessage("")
                        .sentAt(LocalDateTime.now())
                        .build()) != 1) {
                    throw new IllegalStateException("DCC notification result was not persisted");
                }
            });
        } catch (RuntimeException ex) {
            try {
                transaction.executeWithoutResult(ignored -> {
                    DccControlledFileMessageJobDO current = requireLockedJob(tenantId, messageJob.getId());
                    if (DccControlledFileMessageJobStatusEnum.SENT.getCode().equals(current.getStatus())) {
                        return;
                    }
                    if (messageJobMapper.updateById(DccControlledFileMessageJobDO.builder()
                            .id(current.getId())
                            .status(DccControlledFileMessageJobStatusEnum.FAILED.getCode())
                            .errorMessage(resolveFailureReason(ex))
                            .build()) != 1) {
                        throw new IllegalStateException("DCC notification failure was not persisted");
                    }
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

    private Map<String, Object> resolveTemplateParams(DccControlledFileMessageJobDO messageJob) {
        return switch (messageJob.getBusinessType()) {
            case DccControlledFileFinalizationServiceImpl.MESSAGE_BUSINESS_TYPE_DISTRIBUTION ->
                    buildBaseNotifyParams(resolveFileForDistributionJob(messageJob.getBusinessId()), false);
            case DccControlledFileFinalizationServiceImpl.MESSAGE_BUSINESS_TYPE_TRAINING ->
                    buildBaseNotifyParams(resolveFileForTrainingJob(messageJob.getBusinessId()), false);
            case DccControlledFileObsoleteServiceImpl.MESSAGE_BUSINESS_TYPE_OBSOLETE ->
                    buildBaseNotifyParams(resolveFileForObsoleteJob(messageJob.getBusinessId()), true);
            default -> throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        };
    }

    private DccControlledFileDO resolveFileForDistributionJob(Long distributionId) {
        DccControlledFileDistributionDO distribution = distributionMapper.selectById(distributionId);
        if (distribution == null || distribution.getControlledFileId() == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        DccControlledFileDO file = controlledFileMapper.selectById(distribution.getControlledFileId());
        if (file == null) {
            throw exception(CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID);
        }
        return file;
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
        if (ex instanceof ServiceException serviceException) {
            return StrUtil.blankToDefault(serviceException.getMessage(), "DCC message delivery failed");
        }
        return StrUtil.blankToDefault(ex.getMessage(), "DCC message delivery failed");
    }
}
