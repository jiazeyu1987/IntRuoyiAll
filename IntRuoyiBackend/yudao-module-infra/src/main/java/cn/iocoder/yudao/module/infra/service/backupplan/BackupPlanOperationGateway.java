package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;

public interface BackupPlanOperationGateway {

    RuntimeControlOperationRespVO backupNow(Long loginUserId);
}
