package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchExecutionActiveContextSchemaTest {

    private static final String BASE_SCHEMA_FILE = "sql/mysql/20260608_edhr_batch_execution_schema.sql";
    private static final String MIGRATION_FILE = "sql/mysql/20260714_mes_edhr_batch_execution_active_context.sql";
    private static final String TEST_SCHEMA_FILE = "yudao-module-mes/src/test/resources/sql/create_tables.sql";
    private static final String DO_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/MesProEdhrBatchExecutionDO.java";
    private static final String MAPPER_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrBatchExecutionMapper.java";
    private static final String SERVICE_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java";
    private static final String CHANGE_SERVICE_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrRecordChangeServiceImpl.java";

    @Test
    void activeContextSchemaAllowsVoidedBatchToReleaseWorkOrderBatchRouteKey() throws Exception {
        Path projectDir = findProjectDir();
        String baseSchema = read(projectDir, BASE_SCHEMA_FILE);
        String migration = read(projectDir, MIGRATION_FILE);
        String testSchema = read(projectDir, TEST_SCHEMA_FILE);
        String batchDo = read(projectDir, DO_FILE);
        String mapper = read(projectDir, MAPPER_FILE);
        String service = read(projectDir, SERVICE_FILE);
        String changeService = read(projectDir, CHANGE_SERVICE_FILE);

        assertActiveContextSchema(baseSchema);
        assertActiveContextSchema(testSchema);
        assertTrue(migration.contains("ADD COLUMN `active_context_key`"), "migration must add active context key");
        assertTrue(migration.contains("`status` <> 60"), "migration must not backfill voided batches as active");
        assertTrue(migration.contains("DROP INDEX `uk_mes_pro_edhr_batch_execution_context`"),
                "migration must replace the old context index");
        assertTrue(migration.contains("ADD UNIQUE INDEX `uk_mes_pro_edhr_batch_execution_active_context`"),
                "migration must add the active context unique index");

        assertTrue(batchDo.contains("private String activeContextKey;"),
                "DO must expose activeContextKey");
        assertTrue(mapper.contains("clearActiveContextKey"),
                "mapper must provide an explicit null update for activeContextKey");
        assertTrue(mapper.contains("notIn(MesProEdhrBatchExecutionDO::getStatus, BATCH_STATUS_VOIDED)"),
                "open-or-create queries must ignore voided batches");
        assertTrue(service.contains("buildActiveContextKey(workOrder.getId(), batchCode, route.getId())"),
                "new batch creation must set activeContextKey");
        assertTrue(changeService.contains("batchExecutionMapper.clearActiveContextKey(batch.getId())"),
                "void approval callback must release activeContextKey");
    }

    private static void assertActiveContextSchema(String schema) {
        assertTrue(schema.contains("active_context_key"), "schema must contain active_context_key");
        assertTrue(schema.contains("uk_mes_pro_edhr_batch_execution_active_context"),
                "schema must use active context unique key");
        assertFalse(schema.contains("uk_mes_pro_edhr_batch_execution_context\" UNIQUE (\"tenant_id\", \"work_order_id\""),
                "test schema must not keep old full context unique key");
        assertFalse(schema.contains("UNIQUE KEY `uk_mes_pro_edhr_batch_execution_context`"),
                "runtime schema must not keep old full context unique key");
    }

    private static String read(Path projectDir, String relativePath) throws Exception {
        return Files.readString(projectDir.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(BASE_SCHEMA_FILE))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate project directory containing " + BASE_SCHEMA_FILE);
    }
}
