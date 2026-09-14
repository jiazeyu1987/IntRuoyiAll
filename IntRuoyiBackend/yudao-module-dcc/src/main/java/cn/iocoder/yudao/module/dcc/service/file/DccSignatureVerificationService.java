package cn.iocoder.yudao.module.dcc.service.file;

public interface DccSignatureVerificationService {

    DccUnifiedSignatureResult verifyPasswordAndCreateSignature(Long actorId, Long controlledFileId, String taskId,
                                                               String stageCode, String actionType, String password,
                                                               String comment);

}
