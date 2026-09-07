package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccLogicalIdentityConcurrencyTest {

    @Test
    void schemaHasTenantProjectLeafAndNormalizedFileNumberUniqueKey() throws Exception {
        Path migration = Path.of("..", "sql", "mysql", "20260906_dcc_new_file_lifecycle_p1.sql");
        String sql = Files.readString(migration, StandardCharsets.UTF_8).toLowerCase();
        assertTrue(sql.contains("dcc_project_code_id"));
        assertTrue(sql.contains("file_type_taxonomy_leaf_id"));
        assertTrue(sql.contains("normalized_file_number"));
        assertTrue(sql.contains("unique key"));
        assertTrue(sql.contains("tenant_id") && sql.contains("dcc_project_code_id")
                && sql.contains("file_type_taxonomy_leaf_id") && sql.contains("normalized_file_number"));
    }
}
