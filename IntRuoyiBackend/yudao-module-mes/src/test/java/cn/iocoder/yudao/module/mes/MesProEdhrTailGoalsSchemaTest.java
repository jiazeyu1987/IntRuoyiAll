package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
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

class MesProEdhrTailGoalsSchemaTest {

    private static final List<String> RUNTIME_SCHEMA_FILES = List.of(
            "sql/mysql/20260615_mes_edhr_tail_four_goals.sql",
            "sql/mysql/20260707_mes_route_use_config_enabled.sql",
            "sql/mysql/20260709_mes_route_flow_config_unification.sql");
    private static final String TEST_SCHEMA_FILE =
            "yudao-module-mes/src/test/resources/sql/create_tables.sql";

    @Test
    void routeBindingAndExecutionSnapshotsDeclareInternalRecordMetadata() {
        assertHasFields(MesProRouteFlowProcessBatchRecordDO.class,
                "formSlotType",
                "recordCategory",
                "validationProfile",
                "permissionScopeId",
                "recordCategorySnapshotHash",
                "requiredPolicy",
                "requiredConditionJson",
                "ownerRoleKey",
                "archiveVisibility",
                "slotConfigSnapshotHash");
        assertHasFields(MesProEdhrBatchExecutionTaskDO.class,
                "formSlotType",
                "recordCategory",
                "validationProfile",
                "permissionScopeId",
                "routeBindingId",
                "routeBindingSnapshotHash",
                "requiredPolicy",
                "requiredConditionJson",
                "ownerRoleKey",
                "archiveVisibility",
                "slotConfigSnapshotHash");
        assertHasFields(MesProBatchRecordExecutionDO.class,
                "formSlotType",
                "recordCategory",
                "validationProfile",
                "permissionScopeId",
                "routeBindingId",
                "routeBindingSnapshotHash",
                "archiveVisibility",
                "slotConfigSnapshotHash");
    }

    @Test
    void signatureDeclaresSelectedTimeAuditMetadata() {
        assertHasFields(MesProBatchRecordExecutionSignatureDO.class,
                "selectedSignedAt",
                "signatureDisplayAt",
                "signatureTimeMode",
                "selectedTimeZone",
                "selectedTimeReason",
                "selectedTimePolicyVersion",
                "selectedTimeAuditHash");
        assertHasFields(MesProEdhrBatchExecutionSignatureDO.class,
                "selectedSignedAt",
                "signatureDisplayAt",
                "signatureTimeMode",
                "selectedTimeZone",
                "selectedTimeReason",
                "selectedTimePolicyVersion",
                "selectedTimeAuditHash");
    }

    @Test
    void operationAuditAndObjectPermissionDataObjectsAndMappersExist() {
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionScopeDO");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionRuleDO");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionScopeMapper");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionRuleMapper");
    }

    @Test
    void runtimeAndTestSchemasExposeTailGoalTablesAndColumns() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = readRuntimeSchema(projectDir);
        String testSchema = Files.readString(projectDir.resolve(TEST_SCHEMA_FILE), StandardCharsets.UTF_8);

        assertSchemaIsNonDestructive(runtimeSchema, "runtime");
        assertSchemaIsNonDestructive(testSchema, "test");

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_route_flow_config", "enabled");
        assertSchemaContainsColumns(testSchema, "mes_pro_route_flow_config", "enabled");

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_route_flow_process_batch_record",
                "form_slot_type", "record_category", "validation_profile", "permission_scope_id",
                "record_category_snapshot_hash", "required_policy", "required_condition_json",
                "owner_role_key", "archive_visibility", "slot_config_snapshot_hash");
        assertSchemaContainsColumns(testSchema, "mes_pro_route_flow_process_batch_record",
                "form_slot_type", "record_category", "validation_profile", "permission_scope_id",
                "record_category_snapshot_hash", "required_policy", "required_condition_json",
                "owner_role_key", "archive_visibility", "slot_config_snapshot_hash");

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_edhr_batch_execution_task",
                "form_slot_type", "record_category", "validation_profile", "permission_scope_id", "route_binding_id",
                "route_binding_snapshot_hash", "required_policy", "required_condition_json",
                "owner_role_key", "archive_visibility", "slot_config_snapshot_hash");
        assertSchemaContainsColumns(testSchema, "mes_pro_edhr_batch_execution_task",
                "form_slot_type", "record_category", "validation_profile", "permission_scope_id", "route_binding_id",
                "route_binding_snapshot_hash", "required_policy", "required_condition_json",
                "owner_role_key", "archive_visibility", "slot_config_snapshot_hash");

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_batch_record_execution",
                "form_slot_type", "record_category", "validation_profile", "permission_scope_id", "route_binding_id",
                "route_binding_snapshot_hash", "archive_visibility", "slot_config_snapshot_hash");
        assertSchemaContainsColumns(testSchema, "mes_pro_batch_record_execution",
                "form_slot_type", "record_category", "validation_profile", "permission_scope_id", "route_binding_id",
                "route_binding_snapshot_hash", "archive_visibility", "slot_config_snapshot_hash");

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_batch_record_execution_signature",
                "selected_signed_at", "signature_display_at", "signature_time_mode", "selected_time_zone",
                "selected_time_reason", "selected_time_policy_version", "selected_time_audit_hash",
                "actor_username_snapshot", "actor_nickname_snapshot", "actor_dept_id_snapshot",
                "actor_dept_name_snapshot", "actor_post_names_snapshot", "actor_role_names_snapshot",
                "signature_purpose", "authorization_basis", "authentication_method",
                "record_version_snapshot", "record_hash_snapshot", "client_ip_snapshot",
                "user_agent_snapshot", "snapshot_status");
        assertSchemaContainsColumns(testSchema, "mes_pro_batch_record_execution_signature",
                "selected_signed_at", "signature_display_at", "signature_time_mode", "selected_time_zone",
                "selected_time_reason", "selected_time_policy_version", "selected_time_audit_hash",
                "actor_username_snapshot", "actor_nickname_snapshot", "actor_dept_id_snapshot",
                "actor_dept_name_snapshot", "actor_post_names_snapshot", "actor_role_names_snapshot",
                "signature_purpose", "authorization_basis", "authentication_method",
                "record_version_snapshot", "record_hash_snapshot", "client_ip_snapshot",
                "user_agent_snapshot", "snapshot_status");
        assertSchemaContainsColumns(runtimeSchema, "mes_pro_edhr_batch_execution_signature",
                "selected_signed_at", "signature_display_at", "signature_time_mode", "selected_time_zone",
                "selected_time_reason", "selected_time_policy_version", "selected_time_audit_hash");
        assertSchemaContainsColumns(testSchema, "mes_pro_edhr_batch_execution_signature",
                "selected_signed_at", "signature_display_at", "signature_time_mode", "selected_time_zone",
                "selected_time_reason", "selected_time_policy_version", "selected_time_audit_hash");

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_edhr_operation_audit_event",
                "request_id", "object_type", "object_id", "record_category", "operation_type",
                "actor_user_id", "permission_decision", "result_status", "occurred_at", "audit_hash");
        assertSchemaContainsColumns(testSchema, "mes_pro_edhr_operation_audit_event",
                "request_id", "object_type", "object_id", "record_category", "operation_type",
                "actor_user_id", "permission_decision", "result_status", "occurred_at", "audit_hash");

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_edhr_permission_scope",
                "scope_name", "object_type", "object_id", "parent_scope_id", "status", "version");
        assertSchemaContainsColumns(testSchema, "mes_pro_edhr_permission_scope",
                "scope_name", "object_type", "object_id", "parent_scope_id", "status", "version");

        assertSchemaContainsColumns(runtimeSchema, "mes_pro_edhr_permission_rule",
                "scope_id", "subject_type", "subject_id", "ability", "decision", "priority", "status", "version");
        assertSchemaContainsColumns(testSchema, "mes_pro_edhr_permission_rule",
                "scope_id", "subject_type", "subject_id", "ability", "decision", "priority", "status", "version");
    }

    @Test
    void runtimeMigrationDeclaresAuditAndPermissionMenuContracts() throws Exception {
        String runtimeSchema = readRuntimeSchema(findProjectDir());

        assertTrue(schemaContainsToken(runtimeSchema, "mes:pro-edhr-operation-audit:query"));
        assertTrue(schemaContainsToken(runtimeSchema, "mes:pro-edhr-permission-scope:query"));
        assertTrue(schemaContainsToken(runtimeSchema, "mes:pro-edhr-permission-scope:save"));
        assertTrue(schemaContainsToken(runtimeSchema, "mes:pro-edhr-permission-scope:evaluate"));
        assertTrue(schemaContainsToken(runtimeSchema, "system_role_menu"));
        assertTrue(schemaContainsToken(runtimeSchema, "tenant_admin"));
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

    private static void assertTypeExists(String className) {
        assertDoesNotThrow(() -> Class.forName(className), () -> "Missing type " + className);
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

    private static void assertSchemaIsNonDestructive(String schema, String schemaName) {
        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE)\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(schema).find(),
                "MES " + schemaName + " schema must not contain destructive table operations");
        assertFalse(Pattern.compile("\\bDELETE\\s+FROM\\s+`?mes_", Pattern.CASE_INSENSITIVE)
                        .matcher(schema).find(),
                "MES " + schemaName + " schema must not delete MES data");
    }

    private static void assertSchemaContainsColumns(String schema, String tableName, String... columns) {
        assertTrue(schemaContainsTable(schema, tableName), "Missing table reference in schema: " + tableName);
        for (String column : columns) {
            assertTrue(schemaContainsToken(schema, column),
                    "Missing column " + tableName + "." + column);
        }
    }

    private static boolean schemaContainsTable(String schema, String tableName) {
        return schemaContainsToken(schema, tableName);
    }

    private static boolean schemaContainsToken(String schema, String token) {
        return Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE).matcher(schema).find();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
