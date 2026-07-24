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
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MESSAGE_JOB_CONTEXT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_MESSAGE_JOB_REPLAY_NOT_ALLOWED;

@Service
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

    public void dispatchMessageJob(DccControlledFileMessageJobDO messageJob, Map<String, Object> templateParams) {
        Runnable action = () -> sendMessageJob(messageJob, templateParams);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
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
        try {
            NotifySendSingleToUserReqDTO reqDTO = new NotifySendSingleToUserReqDTO();
            reqDTO.setUserId(messageJob.getRecipientUserId());
            reqDTO.setTemplateCode(messageJob.getTemplateCode());
            reqDTO.setTemplateParams(templateParams);
            notifyMessageSendApi.sendSingleMessageToAdmin(reqDTO);
            messageJobMapper.updateById(DccControlledFileMessageJobDO.builder()
                    .id(messageJob.getId())
                    .status(DccControlledFileMessageJobStatusEnum.SENT.getCode())
                    .errorMessage(null)
                    .sentAt(LocalDateTime.now())
                    .build());
        } catch (RuntimeException ex) {
            messageJobMapper.updateById(DccControlledFileMessageJobDO.builder()
                    .id(messageJob.getId())
                    .status(DccControlledFileMessageJobStatusEnum.FAILED.getCode())
                    .errorMessage(resolveFailureReason(ex))
                    .sentAt(null)
                    .build());
            throw ex;
        }
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
