package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditTaskRespVO;

public interface DccNasControlAuditService {

    DccNasControlAuditTaskRespVO startTask(Long userId);

    DccNasControlAuditTaskRespVO getTask(Long taskId);

    DccNasControlAuditReportFile downloadReport(Long taskId);

    void recoverInterruptedTasksOnStartup();

    void processWaitingTasks();
}
