package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditFilePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditFileRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditRecognizeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasControlAuditTaskRespVO;

public interface DccNasControlAuditService {

    DccNasControlAuditTaskRespVO startTask(Long userId);

    DccNasControlAuditTaskRespVO getTask(Long taskId);

    PageResult<DccNasControlAuditFileRespVO> getTaskFilePage(Long taskId, DccNasControlAuditFilePageReqVO reqVO);

    DccNasControlAuditRecognizeRespVO recognizeTaskFiles(Long taskId);

    DccNasControlAuditReportFile downloadReport(Long taskId);

    void recoverInterruptedTasksOnStartup();

    void processWaitingTasks();
}
