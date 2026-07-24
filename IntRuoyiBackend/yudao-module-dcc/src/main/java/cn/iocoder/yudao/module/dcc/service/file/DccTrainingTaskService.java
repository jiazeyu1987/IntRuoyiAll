package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingExecutionPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingExecutionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionHeartbeatReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionStartReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionStopReqVO;

public interface DccTrainingTaskService {

    PageResult<DccTrainingTaskRespVO> getMyTrainingTaskPage(Long userId, DccTrainingTaskPageReqVO reqVO);

    DccTrainingTaskRespVO getTrainingTask(Long userId, Long progressId);

    DccControlledFileBinary readTrainingPreviewFile(Long userId, Long progressId,
                                                    DccRequestAuditContext auditContext);

    DccTrainingTaskRespVO startViewSession(Long userId, Long progressId, DccTrainingViewSessionStartReqVO reqVO);

    DccTrainingTaskRespVO heartbeatViewSession(Long userId, Long progressId, DccTrainingViewSessionHeartbeatReqVO reqVO);

    DccTrainingTaskRespVO stopViewSession(Long userId, Long progressId, DccTrainingViewSessionStopReqVO reqVO);

    void acknowledgeTraining(Long userId, Long progressId);

    PageResult<DccTrainingExecutionRespVO> getTrainingExecutionPage(Long userId, DccTrainingExecutionPageReqVO reqVO);
}
