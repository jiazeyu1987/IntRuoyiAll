package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskRespVO;

public interface DccDistributionTaskService {

    PageResult<DccDistributionTaskRespVO> getMyDistributionTaskPage(Long userId, DccDistributionTaskPageReqVO reqVO);
}
