package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionRestoreService;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DccNasPermissionRestoreStatusRespVO {

    private Long restoreId;
    private Long taskId;
    private String status;
    private Long directoryCount;
    private Long ruleCount;
    private Long completedDirectoryCount;
    private Long failedDirectoryCount;
    private String lastFailureMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static DccNasPermissionRestoreStatusRespVO of(
            DccNasPermissionRestoreService.RestoreStatusResult result) {
        DccNasPermissionRestoreStatusRespVO respVO = new DccNasPermissionRestoreStatusRespVO();
        respVO.setRestoreId(result.restoreId());
        respVO.setTaskId(result.taskId());
        respVO.setStatus(result.status());
        respVO.setDirectoryCount(result.directoryCount());
        respVO.setRuleCount(result.ruleCount());
        respVO.setCompletedDirectoryCount(result.completedDirectoryCount());
        respVO.setFailedDirectoryCount(result.failedDirectoryCount());
        respVO.setLastFailureMessage(result.lastFailureMessage());
        respVO.setStartedAt(result.startedAt());
        respVO.setCompletedAt(result.completedAt());
        return respVO;
    }
}
