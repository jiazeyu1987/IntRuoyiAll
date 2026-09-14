package cn.iocoder.yudao.module.mes.productionrelease.core;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesReleaseDecisionPersistenceContractTest {

    @Test
    void releaseDecisionSchemaAndMapperMustPersistTheFinalizationEvidence() throws Exception {
        Path root = locateWorktreeRoot();
        String migration = Files.readString(root.resolve("sql/mysql/20260822_mes_edhr_release_final_state_trace.sql"),
                StandardCharsets.UTF_8);
        String decisionDo = Files.readString(root.resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrReleaseDecisionDO.java"),
                StandardCharsets.UTF_8);
        String decisionMapper = Files.readString(root.resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrReleaseDecisionMapper.java"),
                StandardCharsets.UTF_8);

        assertTrue(migration.contains("mes_pro_edhr_release_decision"));
        assertTrue(migration.contains("release_transaction_id"));
        assertTrue(migration.contains("payload_hash"));
        assertTrue(migration.contains("material_gate_snapshot_hash"));
        assertTrue(migration.contains("uk_mes_edhr_release_decision_transaction"));
        assertTrue(migration.contains("duplicate terminal release decisions require historical reconciliation"));
        assertTrue(migration.contains("ensure_mes_edhr_release_decision_transaction_index"));
        assertTrue(decisionDo.contains("releaseTransactionId"));
        assertTrue(decisionDo.contains("decisionStatus"));
        assertTrue(decisionDo.contains("payloadHash"));
        assertTrue(decisionDo.contains("sourceSnapshotHash"));
        assertTrue(decisionMapper.contains("selectByTransactionIdAndStatusAndIdempotencyKey"));
        assertTrue(decisionMapper.contains("selectReleasedByTransactionIdForUpdate"));
        assertTrue(decisionMapper.contains("selectByTransactionIdForUpdate"));
    }

    private static Path locateWorktreeRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("sql/mysql"))
                    && Files.isDirectory(current.resolve("yudao-module-mes/src/main"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("MES backend worktree root was not found");
    }
}
