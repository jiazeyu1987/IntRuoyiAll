package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanHistoryPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanScheduleSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;

public interface BackupPlanService {

    BackupPlanStatusRespVO getStatus();

    BackupPlanStatusRespVO saveSchedule(BackupPlanScheduleSaveReqVO reqVO);

    BackupPlanStatusRespVO enable();

    BackupPlanStatusRespVO disable();

    RuntimeControlOperationRespVO backupNow(Long loginUserId);

    PageResult<RuntimeControlBackupPointRespVO> getHistoryPage(BackupPlanHistoryPageReqVO pageReqVO);
}
