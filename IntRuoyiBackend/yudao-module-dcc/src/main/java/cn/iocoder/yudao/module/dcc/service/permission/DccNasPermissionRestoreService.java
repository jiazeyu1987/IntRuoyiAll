package cn.iocoder.yudao.module.dcc.service.permission;

import java.util.List;
import java.time.LocalDateTime;

public interface DccNasPermissionRestoreService {

    PreviewResult preview(Long taskId);

    ApplyResult apply(ApplyRestoreCommand command);

    RestoreStatusResult getStatus(Long taskId, Long restoreId);

    record PreviewResult(Long taskId,
                         boolean canRestore,
                         String planHash,
                         String restoreMode,
                         long directoryCount,
                         long ruleCount,
                         boolean runtimeEnforcementReady,
                         String runtimeEnforcementBlocker,
                         List<RestoreBlocker> blockers,
                         List<RestoreRulePreview> sampleRules) {
    }

    record RestoreRulePreview(Long directoryId,
                              String nasPath,
                              String subjectType,
                              Long subjectId,
                              boolean canQuery,
                              boolean canPreview,
                              boolean canDownload) {
    }

    record RestoreBlocker(String code,
                          String message,
                          Long directorySnapshotId,
                          String nasPath,
                          String trusteeSid) {
    }

    record ApplyRestoreCommand(Long taskId,
                               String idempotencyKey,
                               String planHash,
                               String restoreMode,
                               String changeReason,
                               Long operatorUserId) {
    }

    record ApplyResult(Long restoreId,
                       Long taskId,
                       String status,
                       long directoryCount,
                       long ruleCount,
                       long completedDirectoryCount,
                       long failedDirectoryCount) {
    }

    record RestoreStatusResult(Long restoreId,
                               Long taskId,
                               String status,
                               long directoryCount,
                               long ruleCount,
                               long completedDirectoryCount,
                               long failedDirectoryCount,
                               String lastFailureMessage,
                               LocalDateTime startedAt,
                               LocalDateTime completedAt) {
    }
}
