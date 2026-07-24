package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionRestoreService;
import lombok.Data;

@Data
public class DccNasPermissionRestoreApplyRespVO {

    private Long restoreId;
    private Long taskId;
    private String status;
    private Long directoryCount;
    private Long ruleCount;
    private Long completedDirectoryCount;
    private Long failedDirectoryCount;

    public static DccNasPermissionRestoreApplyRespVO of(DccNasPermissionRestoreService.ApplyResult result) {
        DccNasPermissionRestoreApplyRespVO respVO = new DccNasPermissionRestoreApplyRespVO();
        respVO.setRestoreId(result.restoreId());
        respVO.setTaskId(result.taskId());
        respVO.setStatus(result.status());
        respVO.setDirectoryCount(result.directoryCount());
        respVO.setRuleCount(result.ruleCount());
        respVO.setCompletedDirectoryCount(result.completedDirectoryCount());
        respVO.setFailedDirectoryCount(result.failedDirectoryCount());
        return respVO;
    }
}
