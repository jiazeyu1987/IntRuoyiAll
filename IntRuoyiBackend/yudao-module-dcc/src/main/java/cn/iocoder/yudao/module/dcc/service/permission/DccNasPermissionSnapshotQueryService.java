package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.common.pojo.PageResult;

import java.time.LocalDateTime;
import java.util.List;

public interface DccNasPermissionSnapshotQueryService {

    SummaryResult getSummary(Long taskId);

    PageResult<ItemResult> getItems(Long taskId, Integer pageNo, Integer pageSize, String status);

    record SummaryResult(Long taskId,
                         String snapshotStatus,
                         List<String> selectedNasPaths,
                         long directorySnapshotCount,
                         long aceCount,
                         long unsupportedAceCount,
                         long unmappedPrincipalCount,
                         long blockerCount,
                         LocalDateTime capturedAt,
                         String lastFailureMessage,
                         boolean restoreSupported) {
    }

    record ItemResult(Long taskItemId,
                      String nasPath,
                      Long dccDirectoryId,
                      String snapshotStatus,
                      long aceCount,
                      List<BlockerResult> blockers) {
    }

    record BlockerResult(String code,
                         String message,
                         String principal,
                         Integer aceIndex) {
    }
}
