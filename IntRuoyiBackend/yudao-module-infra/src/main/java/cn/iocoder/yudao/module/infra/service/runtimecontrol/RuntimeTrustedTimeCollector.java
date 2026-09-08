package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionCheckRespVO;

import java.util.List;

public interface RuntimeTrustedTimeCollector {

    List<RuntimeControlInspectionCheckRespVO> collect();
}
