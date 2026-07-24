package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionApprovalEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordVersionPhaseOneSchemaTest {

    private static final String TEST_SCHEMA_FILE =
            "yudao-module-mes/src/test/resources/sql/create_tables.sql";
    private static final List<String> RUNTIME_SCHEMA_FILES = List.of(
            "sql/mysql/20260708_mes_batch_record_version_phase_one.sql",
            "sql/mysql/20260709_mes_route_flow_config_unification.sql");

    @Test
    void dataObjectsDeclarePhaseOneVersionContracts() {
        assertHasFields(MesProBatchRecordDefinitionDO.class,
                "batchRecordName", "routeKey", "currentVersionId");
        assertHasFields(MesProBatchRecordVersionDO.class,
                "definitionId", "versionNo", "status", "sourceVersionId", "sourceFileSha256",
                "routeId", "sourceRouteId", "approvalInstanceId", "submittedBy", "approvedBy");
        assertHasFields(MesProBatchRecordVersionMigrationItemDO.class,
                "definitionId", "versionId", "sourceVersionId", "itemType", "sourceLogicalKey",
                "targetLogicalKey", "matchConfidence", "matchEvidenceJson", "riskLevel");
        assertHasFields(MesProBatchRecordVersionApprovalEventDO.class,
                "definitionId", "versionId", "approvalInstanceId", "approvalEventId",
                "approvalResult", "processedResult");
        assertHasFields(MesProBatchRecordReportDO.class,
                "batchRecordName", "productName", "batchRecordDefinitionId", "batchRecordVersionId");
        assertHasFields(MesProRouteFlowProcessBatchRecordDO.class,
                "batchRecordDefinitionId", "batchRecordVersionId");
        assertHasFields(MesProBatchRecordExecutionDO.class,
                "batchRecordDefinitionId", "batchRecordVersionId", "routeId");
        assertHasFields(MesProEdhrBatchExecutionTaskDO.class,
                "batchRecordDefinitionId", "batchRecordVersionId");
        assertHasFields(MesProEdhrProcessFormPermissionRuleDO.class,
                "batchRecordDefinitionId", "batchRecordVersionId");
    }

    @Test
    void runtimeAndTestSchemasDeclarePhaseOneContracts() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = readRuntimeSchema(projectDir);
        String testSchema = Files.readString(projectDir.resolve(TEST_SCHEMA_FILE), StandardCharsets.UTF_8);

        assertSchemaIsNonDestructive(runtimeSchema);

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_batch_record_definition",
                "batch_record_name", "route_key", "current_version_id");
        assertSchemaContainsColumns(testSchema, "mes_pro_batch_record_definition",
                "batch_record_name", "route_key", "current_version_id");
        assertSchemaContainsColumns(runtimeSchema, "mes_pro_batch_record_version",
                "definition_id", "version_no", "status", "source_version_id", "source_file_sha256",
                "route_id", "source_route_id", "approval_instance_id");
        assertSchemaContainsColumns(testSchema, "mes_pro_batch_record_version",
                "definition_id", "version_no", "status", "source_version_id", "source_file_sha256",
                "route_id", "source_route_id", "approval_instance_id");
        assertSchemaContainsColumns(runtimeSchema, "mes_pro_batch_record_version_migration_item",
                "source_logical_key", "target_logical_key", "match_confidence", "match_evidence_json", "risk_level");
        assertSchemaContainsColumns(testSchema, "mes_pro_batch_record_version_migration_item",
                "source_logical_key", "target_logical_key", "match_confidence", "match_evidence_json", "risk_level");
        assertSchemaContainsColumns(runtimeSchema, "mes_pro_batch_record_version_approval_event",
                "approval_instance_id", "approval_event_id", "approval_result", "processed_result");
        assertSchemaContainsColumns(testSchema, "mes_pro_batch_record_version_approval_event",
                "approval_instance_id", "approval_event_id", "approval_result", "processed_result");
        assertSchemaContainsColumns(runtimeSchema, "mes_pro_batch_record_report",
                "batch_record_name", "product_name", "batch_record_definition_id", "batch_record_version_id");
        assertSchemaContainsColumns(testSchema, "mes_pro_batch_record_report",
                "batch_record_name", "product_name", "batch_record_definition_id", "batch_record_version_id");

        for (String tableName : new String[] {
                "mes_pro_route_flow_process_batch_record",
                "mes_pro_batch_record_execution",
                "mes_pro_edhr_batch_execution_task",
                "mes_pro_edhr_process_form_permission_rule"
        }) {
            assertSchemaContainsColumns(runtimeSchema, tableName,
                    "batch_record_definition_id", "batch_record_version_id");
            assertSchemaContainsColumns(testSchema, tableName,
                    "batch_record_definition_id", "batch_record_version_id");
        }
        assertSchemaContainsColumns(runtimeSchema, "mes_pro_batch_record_execution", "route_id");
        assertSchemaContainsColumns(testSchema, "mes_pro_batch_record_execution", "route_id");

        assertTrue(schemaContainsToken(runtimeSchema, "uk_mes_batch_record_definition_name_route"));
        assertTrue(schemaContainsToken(runtimeSchema, "uk_mes_batch_record_version_no"));
        assertTrue(schemaContainsToken(runtimeSchema, "uk_mes_batch_record_version_hash_pending"));
        assertTrue(schemaContainsToken(runtimeSchema, "uk_mes_batch_record_approval_event"));
        assertTrue(schemaContainsToken(runtimeSchema, "mes:pro-batch-record-template:version-approve"),
                "Phase one schema must seed version approval permission menu");
        assertTrue(schemaContainsToken(runtimeSchema, "edhr_rehearsal_approver_t1"),
                "Phase one schema must bind version approval permission to the test tenant approval role");
    }

    private static void assertHasFields(Class<?> type, String... fieldNames) {
        for (String fieldName : fieldNames) {
            assertDoesNotThrow(() -> declaredField(type, fieldName),
                    () -> "Missing field " + type.getSimpleName() + "." + fieldName);
        }
    }

    private static Field declaredField(Class<?> type, String fieldName) throws NoSuchFieldException {
        return type.getDeclaredField(fieldName);
    }

    private static void assertSchemaContainsColumns(String schema, String tableName, String... columns) {
        assertTrue(schemaContainsToken(schema, tableName), "Missing table reference in schema: " + tableName);
        for (String column : columns) {
            assertTrue(schemaContainsToken(schema, column),
                    "Missing column " + tableName + "." + column);
        }
    }

    private static void assertSchemaIsNonDestructive(String schema) {
        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE)\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(schema).find(),
                "Phase one schema must not contain destructive table operations");
        assertFalse(Pattern.compile("\\bDELETE\\s+FROM\\s+`?mes_", Pattern.CASE_INSENSITIVE)
                        .matcher(schema).find(),
                "Phase one schema must not delete MES data");
    }

    private static boolean schemaContainsToken(String schema, String token) {
        return Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE).matcher(schema).find();
    }

    private static String readRuntimeSchema(Path projectDir) throws Exception {
        StringBuilder builder = new StringBuilder();
        for (String schemaFile : RUNTIME_SCHEMA_FILES) {
            Path path = projectDir.resolve(schemaFile);
            assertTrue(Files.exists(path), "Runtime schema file must exist: " + schemaFile);
            builder.append(Files.readString(path, StandardCharsets.UTF_8)).append('\n');
        }
        return builder.toString();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
