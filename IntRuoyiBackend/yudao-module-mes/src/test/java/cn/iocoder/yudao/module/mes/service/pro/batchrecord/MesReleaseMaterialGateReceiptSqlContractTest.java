package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesReleaseMaterialGateReceiptSqlContractTest {

    @Test
    void migrationFreezesFourMaterialReceiptIntegrityAndVersionColumns() throws Exception {
        Path backendRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize().getParent();
        Path migration = backendRoot.resolve("sql").resolve("mysql")
                .resolve("20260826_mes_edhr_material_gate_receipt.sql");
        String sql = Files.readString(migration, StandardCharsets.UTF_8);
        for (String required : new String[]{"receipt_id", "tenant_id", "batch_execution_id", "gate_status",
                "material_type_keys_json", "manifest_hash", "source_snapshot_hash", "material_version_set_hash",
                "receipt_hash", "issued_by", "audit_event_id", "version"}) {
            assertTrue(sql.contains(required), "missing material receipt migration field: " + required);
        }
        assertTrue(sql.contains("uk_mes_edhr_material_gate_receipt_id"));
        assertTrue(sql.contains("uk_mes_edhr_material_gate_receipt_version"));
        assertTrue(sql.contains("release-migration:"));
    }
}
