package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMessageJobReplayReqVO;

public interface DccControlledFileMessageReplayService {

    int replayMessageJobs(DccControlledFileMessageJobReplayReqVO reqVO);
}
