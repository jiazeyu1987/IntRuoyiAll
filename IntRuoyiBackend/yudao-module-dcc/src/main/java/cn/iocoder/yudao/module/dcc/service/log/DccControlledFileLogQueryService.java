package cn.iocoder.yudao.module.dcc.service.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogRespVO;

public interface DccControlledFileLogQueryService {

    PageResult<DccControlledFileLogRespVO> getLogPage(DccControlledFileLogPageReqVO reqVO);

}
