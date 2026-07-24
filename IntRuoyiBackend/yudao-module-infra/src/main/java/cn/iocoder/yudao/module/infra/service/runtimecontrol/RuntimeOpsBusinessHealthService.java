package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBusinessHealthRespVO;

public interface RuntimeOpsBusinessHealthService {

    RuntimeControlBusinessHealthRespVO getBusinessHealth();
}
