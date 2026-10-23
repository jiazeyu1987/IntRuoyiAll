package cn.iocoder.yudao.module.dcc.service.file;

public interface DccSignatureVerificationService {
    DccUnifiedSignatureResult verifyPasswordAndCreateDelayedConfirmationSignature(Long actorId, Long controlledFileId,
            String taskId, String processInstanceId, Integer attributeRound, java.time.LocalDateTime selectedEffectiveTime,
            String password, String comment);

    DccUnifiedSignatureResult verifyPasswordAndCreateSignature(Long actorId, Long controlledFileId, String taskId,
                                                               String stageCode, String actionType, String password,
                                                               String comment);

    DccUnifiedSignatureResult verifyPasswordAndCreateWorkflowSignature(Long actorId, Long controlledFileId,
            String taskId, String processInstanceId, String stageCode, String actionType, String password, String comment);

}
