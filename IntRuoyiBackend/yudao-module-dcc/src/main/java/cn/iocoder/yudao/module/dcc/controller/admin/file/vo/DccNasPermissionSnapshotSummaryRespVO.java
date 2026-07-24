package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionSnapshotQueryService;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DccNasPermissionSnapshotSummaryRespVO {

    private Long taskId;
    private String snapshotStatus;
    private List<String> selectedNasPaths;
    private Long directorySnapshotCount;
    private Long aceCount;
    private Long unsupportedAceCount;
    private Long unmappedPrincipalCount;
    private Long blockerCount;
    private LocalDateTime capturedAt;
    private String lastFailureMessage;
    private Boolean restoreSupported;

    public static DccNasPermissionSnapshotSummaryRespVO of(
            DccNasPermissionSnapshotQueryService.SummaryResult result) {
        DccNasPermissionSnapshotSummaryRespVO respVO = new DccNasPermissionSnapshotSummaryRespVO();
        respVO.setTaskId(result.taskId());
        respVO.setSnapshotStatus(result.snapshotStatus());
        respVO.setSelectedNasPaths(result.selectedNasPaths());
        respVO.setDirectorySnapshotCount(result.directorySnapshotCount());
        respVO.setAceCount(result.aceCount());
        respVO.setUnsupportedAceCount(result.unsupportedAceCount());
        respVO.setUnmappedPrincipalCount(result.unmappedPrincipalCount());
        respVO.setBlockerCount(result.blockerCount());
        respVO.setCapturedAt(result.capturedAt());
        respVO.setLastFailureMessage(result.lastFailureMessage());
        respVO.setRestoreSupported(result.restoreSupported());
        return respVO;
    }
}
