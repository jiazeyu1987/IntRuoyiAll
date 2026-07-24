package cn.iocoder.yudao.module.mes;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProRecordbookBatchControlledSyncMigrationContractTest {

    private static final String MIGRATION_FILE =
            "sql/mysql/20260722_mes_recordbook_batch_controlled_sync.sql";
    private static final String TEST_SCHEMA_FILE =
            "yudao-module-mes/src/test/resources/sql/create_tables.sql";
    private static final String ROUTE_BINDING_DO_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/route/"
                    + "MesProRouteFlowProcessBatchRecordDO.java";
    private static final String BATCH_TASK_DO_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
                    + "MesProEdhrBatchExecutionTaskDO.java";
    private static final String EXECUTION_DO_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
                    + "MesProBatchRecordExecutionDO.java";
    private static final String AUDIT_ITEM_DO_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/batchrecord/"
                    + "MesProBatchRecordExecutionFieldAuditItemDO.java";
    private static final String FIELD_AUDIT_SERVICE_FILE =
            "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                    + "MesProBatchRecordExecutionFieldAuditServiceImpl.java";

    @Test
    void migrationAndRuntimeContractsContainRecordbookControlledSyncFields() throws Exception {
        Path projectDir = findProjectDir();
        String migration = read(projectDir, MIGRATION_FILE);
        String testSchema = read(projectDir, TEST_SCHEMA_FILE);

        assertTrue(migration.contains("-- release-migration: allowedEnvironments=test,backup,prod;"));
        assertTrue(migration.contains("dependsOn=20260526_edhr_field_audit_schema,20260720_mes_batch_shared_form_binding"));
        assertTrue(migration.contains("SIGNAL SQLSTATE '45000'"));
        assertTrue(migration.contains("ADD COLUMN `recordbook_enabled` bit(1) NOT NULL DEFAULT b'1'"));
        assertTrue(migration.contains("ADD COLUMN `recordbook_value_json` longtext DEFAULT NULL"));
        assertTrue(migration.contains("ADD COLUMN `recordbook_value_display` varchar(1000) DEFAULT NULL"));
        assertTrue(migration.contains("ADD COLUMN `batch_record_value_json` longtext DEFAULT NULL"));
        assertTrue(migration.contains("ADD COLUMN `batch_record_value_display` varchar(1000) DEFAULT NULL"));
        assertTrue(migration.contains("WHERE `record_category` = 'INTERNAL_RECORD'"));
        assertFalse(migration.contains("DROP TABLE"));
        assertFalse(migration.contains("TRUNCATE TABLE"));
        assertFalse(migration.contains("DELETE FROM"));

        assertTrue(testSchema.contains("\"recordbook_enabled\" bit NOT NULL DEFAULT TRUE"));
        assertTrue(testSchema.contains("\"recordbook_value_json\" clob DEFAULT NULL"));
        assertTrue(testSchema.contains("\"recordbook_value_display\" varchar(1000) DEFAULT NULL"));
        assertTrue(testSchema.contains("\"batch_record_value_json\" clob DEFAULT NULL"));
        assertTrue(testSchema.contains("\"batch_record_value_display\" varchar(1000) DEFAULT NULL"));

        assertTrue(read(projectDir, ROUTE_BINDING_DO_FILE).contains("private Boolean recordbookEnabled;"));
        assertTrue(read(projectDir, BATCH_TASK_DO_FILE).contains("private Boolean recordbookEnabled;"));
        assertTrue(read(projectDir, EXECUTION_DO_FILE).contains("private Boolean recordbookEnabled;"));
        String auditItemDo = read(projectDir, AUDIT_ITEM_DO_FILE);
        assertTrue(auditItemDo.contains("private String recordbookValueJson;"));
        assertTrue(auditItemDo.contains("private String recordbookValueDisplay;"));
        assertTrue(auditItemDo.contains("private String batchRecordValueJson;"));
        assertTrue(auditItemDo.contains("private String batchRecordValueDisplay;"));
        String fieldAuditService = read(projectDir, FIELD_AUDIT_SERVICE_FILE);
        assertFalse(fieldAuditService.contains("return TextNode.valueOf(value);"));
    }

    private static String read(Path projectDir, String relativePath) throws Exception {
        return Files.readString(projectDir.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(MIGRATION_FILE))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate project directory containing " + MIGRATION_FILE);
    }
}
