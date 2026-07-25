package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class RuntimeControlBackupPlanOperationGateway implements BackupPlanOperationGateway {

    @Resource
    private RuntimeControlService runtimeControlService;

    @Override
    public RuntimeControlOperationRespVO backupNow(Long loginUserId) {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("backup-now");
        reqVO.setReason("系统管理备份计划页面手动备份");
        reqVO.setTargetEnvironment("prod");
        reqVO.setProdConfirmText("PROD");
        return runtimeControlService.executeAction(reqVO, String.valueOf(loginUserId));
    }
}
