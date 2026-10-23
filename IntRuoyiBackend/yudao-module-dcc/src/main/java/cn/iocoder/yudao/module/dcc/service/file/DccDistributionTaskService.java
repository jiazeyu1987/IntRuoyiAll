package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionRecipientReceiptRespVO;
import java.util.List;

public interface DccDistributionTaskService {
    cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO retryAutomaticDistribution(Long userId,cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionRetryReqVO request);
    cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO getAutomaticDistributionResult(Long userId,Long controlledFileId);

    PageResult<DccDistributionTaskRespVO> getMyDistributionTaskPage(Long userId, DccDistributionTaskPageReqVO reqVO);

    List<DccDistributionRecipientReceiptRespVO> getDistributionRecipientReceipts(Long userId, Long controlledFileId);
}
