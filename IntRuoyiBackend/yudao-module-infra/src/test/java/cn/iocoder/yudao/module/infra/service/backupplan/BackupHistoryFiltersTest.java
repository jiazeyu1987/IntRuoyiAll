package cn.iocoder.yudao.module.infra.service.backupplan;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class BackupHistoryFiltersTest {
    @Test void filtersDisplayedNasFieldsBeforePaginationWithoutExtraReads() {
        var first = row("backup-20261113-102030", "RECOVERABLE", "FULL");
        var second = row("backup-20261112-102030", "UNRECOVERABLE", "INCREMENTAL");
        assertEquals(List.of(first), BackupHistoryFilters.filter(List.of(first, second), "[{\"key\":\"generatedTime\",\"operator\":\"contains\",\"value\":\"2026-11-13\"},{\"key\":\"result\",\"operator\":\"eq\",\"value\":\"成功\"}]"));
        assertEquals(List.of(second), BackupHistoryFilters.filter(List.of(first, second), "[{\"key\":\"backupMode\",\"operator\":\"in\",\"value\":[\"增量备份\"]}]"));
    }
    @Test void invalidConditionsFailEvenWhenRepositoryIsEmpty() {
        assertThrows(RuntimeException.class, () -> BackupHistoryFilters.filter(List.of(), "[{\"key\":\"unknown\",\"operator\":\"eq\",\"value\":\"x\"}]"));
        assertThrows(RuntimeException.class, () -> BackupHistoryFilters.filter(List.of(), "invalid"));
    }
    private RuntimeControlBackupPointRespVO row(String id, String status, String kind) {
        var row = new RuntimeControlBackupPointRespVO(); row.setBackupId(id); row.setRecoverabilityStatus(status); row.setBackupKind(kind); return row;
    }
}
