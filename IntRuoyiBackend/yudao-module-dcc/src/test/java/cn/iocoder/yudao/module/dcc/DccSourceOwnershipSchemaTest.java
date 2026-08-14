package cn.iocoder.yudao.module.dcc;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccSourceOwnershipSchemaTest {

    @Test
    void migrationDefinesExclusiveOwnershipAndRestartableMigrationEvidence() throws Exception {
        String sql = Files.readString(Path.of("..", "sql", "mysql", "20260811_dcc_source_ownership.sql"),
                StandardCharsets.UTF_8).toLowerCase();

        assertTrue(sql.contains("dcc_controlled_file_source_ownership"));
        assertTrue(sql.contains("controlled_file_id"));
        assertTrue(sql.contains("source_file_id"));
        assertTrue(sql.contains("origin_source_file_id"));
        assertTrue(sql.contains("source_sha256"));
        assertTrue(sql.contains("unique key `uk_dcc_source_owner_file` (`tenant_id`, `controlled_file_id`)"));
        assertTrue(sql.contains("unique key `uk_dcc_source_owner_source` (`tenant_id`, `source_file_id`)"));
        assertTrue(sql.contains("dcc_controlled_file_source_migration"));
        assertTrue(sql.contains("legacy_source_file_id"));
        assertTrue(sql.contains("isolated_source_file_id"));
        assertTrue(sql.contains("migration_status"));
        assertTrue(sql.contains("unique key `uk_dcc_source_migration_file` (`tenant_id`, `controlled_file_id`)"));
    }
}
