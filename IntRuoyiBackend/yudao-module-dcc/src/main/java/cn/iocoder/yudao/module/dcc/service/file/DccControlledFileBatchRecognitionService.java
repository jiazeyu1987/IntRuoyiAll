package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionTaskRespVO;

public interface DccControlledFileBatchRecognitionService {

    DccControlledFileBatchRecognitionTaskRespVO createTask(Long userId,
                                                           DccControlledFileBatchRecognitionCreateReqVO reqVO);

    DccControlledFileBatchRecognitionTaskRespVO getTask(Long userId, Long taskId);

    DccControlledFileBatchRecognitionTaskRespVO getLatestTask(Long userId, String recognitionType);

    DccControlledFileBatchRecognitionTaskRespVO stopTask(Long userId, Long taskId);

    void processWaitingTasks();

    void recoverInterruptedTasksOnStartup();
}
