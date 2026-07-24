package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;

import java.util.List;

public interface RuntimeBackupDrillService {

    List<RuntimeControlBackupPointRespVO> listBackupPoints();

    RuntimeControlBackupPointRespVO getBackupPoint(String backupId);
}
