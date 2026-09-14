package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesIndependentBatchPrerequisiteReceiptSqlContractTest {

    @Test
    void migrationFreezesReceiptIntegrityAndIdempotencyColumns() throws Exception {
        Path backendRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize().getParent();
        Path migration = backendRoot.resolve("sql").resolve("mysql")
                .resolve("20260823_mes_independent_batch_prerequisite_receipt.sql");
        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        for (String required : new String[]{"receipt_id", "tenant_id", "entry_type", "source_relation_id",
                "source_snapshot_hash", "canonical_payload", "payload_hash", "signature", "issued_at",
                "expires_at", "revoked_at", "audit_event_id", "idempotency_key"}) {
            assertTrue(sql.contains(required), "missing migration field: " + required);
        }
        assertTrue(sql.contains("uk_independent_receipt_tenant_receipt"));
        assertTrue(sql.contains("uk_independent_receipt_tenant_idempotency"));
        assertTrue(sql.contains("release-migration:"));
    }
}
