package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlProbeLatestRespVO;

public interface RuntimeOpsProbeService {

    RuntimeControlProbeLatestRespVO runProbes();

    RuntimeControlProbeLatestRespVO getLatestProbes();
}
