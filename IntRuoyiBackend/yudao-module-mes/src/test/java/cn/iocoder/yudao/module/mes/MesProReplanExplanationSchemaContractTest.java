package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProReplanExplanationSchemaContractTest {

    @Test
    void replanExplanationSnapshot_shouldExistInMigrationInitialSchemaAndTestSchema() throws Exception {
        String migration = Files.readString(
                Path.of("..", "sql", "mysql", "20260710_mes_pro_replan_explanation_snapshot.sql"),
                StandardCharsets.UTF_8);
        String initialSchema = Files.readString(
                Path.of("..", "sql", "mysql", "ruoyi-vue-pro.sql"),
                StandardCharsets.UTF_8);
        String testSchema = Files.readString(
                Path.of("src", "test", "resources", "sql", "create_tables.sql"),
                StandardCharsets.UTF_8);

        for (String sql : List.of(migration, initialSchema, testSchema)) {
            assertTrue(sql.contains("mes_pro_replan_explanation_snapshot"));
            assertTrue(sql.contains("request_id"));
            assertTrue(sql.contains("trigger_source"));
            assertTrue(sql.contains("capacity_mode"));
            assertTrue(sql.contains("request_start_time"));
            assertTrue(sql.contains("applied_at"));
            assertTrue(sql.contains("snapshot_json"));
            assertTrue(sql.contains("tenant_id"));
        }
        for (String sql : List.of(migration, initialSchema)) {
            assertTrue(sql.contains("uk_mes_pro_replan_explanation_request"));
            assertTrue(sql.contains("idx_mes_pro_replan_explanation_latest"));
        }
    }
}
