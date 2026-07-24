package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlInspectionRunRespVO;

public interface RuntimeOpsInspectionService {

    RuntimeControlInspectionRunRespVO runInspection();

    RuntimeControlInspectionRunRespVO getInspectionRun(Long id);
}
