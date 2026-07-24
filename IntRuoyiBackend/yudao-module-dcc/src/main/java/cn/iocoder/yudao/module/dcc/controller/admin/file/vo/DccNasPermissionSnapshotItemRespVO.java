package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.permission.DccNasPermissionSnapshotQueryService;
import lombok.Data;

import java.util.List;

@Data
public class DccNasPermissionSnapshotItemRespVO {

    private Long taskItemId;
    private String nasPath;
    private Long dccDirectoryId;
    private String snapshotStatus;
    private Long aceCount;
    private List<Blocker> blockers;

    public static DccNasPermissionSnapshotItemRespVO of(
            DccNasPermissionSnapshotQueryService.ItemResult result) {
        DccNasPermissionSnapshotItemRespVO respVO = new DccNasPermissionSnapshotItemRespVO();
        respVO.setTaskItemId(result.taskItemId());
        respVO.setNasPath(result.nasPath());
        respVO.setDccDirectoryId(result.dccDirectoryId());
        respVO.setSnapshotStatus(result.snapshotStatus());
        respVO.setAceCount(result.aceCount());
        respVO.setBlockers(result.blockers().stream().map(Blocker::of).toList());
        return respVO;
    }

    @Data
    public static class Blocker {

        private String code;
        private String message;
        private String principal;
        private Integer aceIndex;

        private static Blocker of(DccNasPermissionSnapshotQueryService.BlockerResult result) {
            Blocker respVO = new Blocker();
            respVO.setCode(result.code());
            respVO.setMessage(result.message());
            respVO.setPrincipal(result.principal());
            respVO.setAceIndex(result.aceIndex());
            return respVO;
        }
    }
}
