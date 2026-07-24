package cn.iocoder.yudao.module.dcc.service.file;

public interface DccTrainingAssignmentAckService {

    void acknowledgeTraining(Long userId, Long controlledFileId);
}
