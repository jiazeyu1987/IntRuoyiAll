package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrUnifiedChangeRequestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordVersionPhaseThreeGovernanceContractTest {

    private static final String GOVERNANCE_CONTROLLER =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProBatchRecordVersionGovernanceController";
    private static final String GOVERNANCE_SERVICE =
            "cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordVersionGovernanceService";
    private static final String GOVERNANCE_SUMMARY_RESP =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceSummaryRespVO";
    private static final String GOVERNANCE_IMPACT_RESP =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceImpactRespVO";
    private static final String GOVERNANCE_INSPECTION_RESP =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceInspectionRespVO";
    private static final String GOVERNANCE_METRICS_RESP =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceMetricsRespVO";
    private static final String GOVERNANCE_ROLLBACK_REQ =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceRollbackReqVO";

    private static final List<String> RUNTIME_SCHEMA_FILES = List.of(
            "sql/mysql/20260708_mes_batch_record_version_phase_one.sql",
            "sql/mysql/20260709_mes_route_flow_config_unification.sql");
    private static final String TEST_SCHEMA_FILE =
            "yudao-module-mes/src/test/resources/sql/create_tables.sql";

    @Test
    void governanceControllerExposesMinimalPhaseThreeEndpoints() {
        Class<?> controllerType = loadClass(GOVERNANCE_CONTROLLER);
        RequestMapping requestMapping = controllerType.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "governance controller must declare a request mapping");
        assertTrue(String.join(",", requestMapping.value()).contains("/mes/pro/batch-record-version/governance"),
                "governance controller must be mounted under the batch record version governance API");

        assertEndpoint(controllerType, "getSummary", GetMapping.class, "/summary");
        assertEndpoint(controllerType, "getImpact", GetMapping.class, "/impact");
        assertEndpoint(controllerType, "getInspection", GetMapping.class, "/inspection");
        assertEndpoint(controllerType, "getMetrics", GetMapping.class, "/metrics");
        assertEndpoint(controllerType, "requestRollback", PostMapping.class, "/rollback/request");
    }

    @Test
    void governanceServiceDeclaresVersionBoundedOperations() {
        Class<?> serviceType = loadClass(GOVERNANCE_SERVICE);
        assertMethod(serviceType, "getSummary", Long.class);
        assertMethod(serviceType, "getImpact", Long.class);
        assertMethod(serviceType, "getInspection", Long.class);
        assertMethod(serviceType, "getMetrics", Long.class);
        assertMethod(serviceType, "requestRollback", loadClass(GOVERNANCE_ROLLBACK_REQ));
    }

    @Test
    void phaseThreeVoContractCoversGovernanceFeatureSet() {
        assertHasFields(loadClass(GOVERNANCE_SUMMARY_RESP),
                "definitionId", "currentVersionId", "currentVersionNo", "versionCount",
                "activeExecutionCount", "historicalExecutionCount", "slotBindingCount",
                "rollbackPendingCount", "blockingInspectionCount");
        assertHasFields(loadClass(GOVERNANCE_IMPACT_RESP),
                "versionId", "executionCount", "taskCount", "routeBindingCount",
                "permissionRuleCount", "slotConfigSnapshotHashes", "ownerRoleKeys", "riskLevel");
        assertHasFields(loadClass(GOVERNANCE_INSPECTION_RESP),
                "versionId", "inspectionCode", "inspectionStatus", "issueCount",
                "issueSummary", "nextAction");
        assertHasFields(loadClass(GOVERNANCE_METRICS_RESP),
                "versionId", "pendingApprovalCount", "approvedVersionCount", "rollbackRequestCount",
                "confirmRequiredItemCount", "blockerItemCount", "latestInspectionStatus");
        assertHasFields(loadClass(GOVERNANCE_ROLLBACK_REQ),
                "definitionId", "targetVersionId", "reason", "impactSummaryJson", "signoffEvidenceHash",
                "idempotencyKey");
    }

    @Test
    void slotVersioningContractUsesExistingBusinessBindings() {
        assertHasFields(MesProRouteFlowProcessBatchRecordDO.class,
                "batchRecordDefinitionId", "batchRecordVersionId", "formSlotType",
                "ownerRoleKey", "slotConfigSnapshotHash");
        assertHasFields(MesProBatchRecordExecutionDO.class,
                "batchRecordDefinitionId", "batchRecordVersionId", "formSlotType",
                "permissionScopeId", "slotConfigSnapshotHash");
        assertHasFields(MesProEdhrBatchExecutionTaskDO.class,
                "batchRecordDefinitionId", "batchRecordVersionId", "formSlotType",
                "ownerRoleKey", "slotConfigSnapshotHash");
        assertHasFields(MesProEdhrProcessFormPermissionRuleDO.class,
                "batchRecordDefinitionId", "batchRecordVersionId", "ruleType",
                "signatureRole", "dueMinutes");
    }

    @Test
    void rollbackIsControlledByUnifiedChangeAndNeverDirectOverwrite() {
        assertHasFields(MesProEdhrUnifiedChangeRequestDO.class,
                "controlledObjectType", "controlledObjectId", "currentVersion", "targetVersion",
                "changeType", "changeStatus", "impactSummaryJson", "approvalSignoffEvidenceHash",
                "idempotencyKey", "evidenceHash");
        assertFalse(controllerSource().contains("updateCurrentVersionIfMatch("),
                "rollback request API must not directly switch definition.current_version_id");
        assertTrue(controllerSource().contains("requestRollback"),
                "controller must expose rollback as a request operation");
    }

    @Test
    void schemasRetainPhaseThreeGovernanceColumns() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = readRuntimeSchema(projectDir);
        String testSchema = Files.readString(projectDir.resolve(TEST_SCHEMA_FILE), StandardCharsets.UTF_8);

        for (String schema : new String[] { runtimeSchema, testSchema }) {
            assertSchemaContainsColumns(schema, "mes_pro_route_flow_process_batch_record",
                    "batch_record_version_id", "form_slot_type", "owner_role_key", "slot_config_snapshot_hash");
            assertSchemaContainsColumns(schema, "mes_pro_batch_record_execution",
                    "batch_record_version_id", "form_slot_type", "permission_scope_id", "slot_config_snapshot_hash");
            assertSchemaContainsColumns(schema, "mes_pro_edhr_batch_execution_task",
                    "batch_record_version_id", "form_slot_type", "owner_role_key", "slot_config_snapshot_hash");
            assertSchemaContainsColumns(schema, "mes_pro_edhr_process_form_permission_rule",
                    "batch_record_version_id", "rule_type", "signature_role", "due_minutes");
            assertSchemaContainsColumns(schema, "mes_pro_edhr_unified_change_request",
                    "controlled_object_type", "controlled_object_id", "controlled_object_code", "current_version",
                    "target_version", "change_type", "change_status", "impact_summary_json",
                    "approval_signoff_evidence_hash", "idempotency_key", "evidence_hash");
            assertSchemaContainsColumns(schema, "mes_pro_edhr_unified_change_impact",
                    "change_request_id", "impact_type", "impact_object_type", "impact_object_id",
                    "risk_level", "requires_training", "requires_revalidation", "requires_release_recheck",
                    "evidence_hash");
            assertSchemaContainsColumns(schema, "mes_pro_edhr_unified_change_event",
                    "change_request_id", "event_type", "from_status", "to_status", "actor_user_id",
                    "signoff_evidence_hash", "event_snapshot_json", "occurred_at", "idempotency_key");
        }
    }

    @Test
    void processFormPermissionRuleUniqueKeyIsVersionScoped() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = normalizeSchema(readRuntimeSchema(projectDir));
        String testSchema = normalizeSchema(Files.readString(projectDir.resolve(TEST_SCHEMA_FILE),
                StandardCharsets.UTF_8));

        assertVersionScopedProcessFormPermissionRuleUnique(runtimeSchema, "runtime schema");
        assertVersionScopedProcessFormPermissionRuleUnique(testSchema, "test schema");
    }

    private static void assertEndpoint(Class<?> controllerType, String methodName, Class<?> annotationType, String path) {
        Method method = assertMethodByName(controllerType, methodName);
        Object annotation = method.getAnnotation(annotationType.asSubclass(java.lang.annotation.Annotation.class));
        assertNotNull(annotation, "Missing " + annotationType.getSimpleName() + " on " + methodName);
        String annotationText = annotation.toString();
        assertTrue(annotationText.contains(path), methodName + " must map to " + path);
    }

    private static Class<?> loadClass(String className) {
        return assertDoesNotThrow(() -> Class.forName(className), () -> "Missing class " + className);
    }

    private static Method assertMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        return assertDoesNotThrow(() -> type.getDeclaredMethod(methodName, parameterTypes),
                () -> "Missing method " + type.getSimpleName() + "." + methodName);
    }

    private static Method assertMethodByName(Class<?> type, String methodName) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Missing method " + type.getSimpleName() + "." + methodName);
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

    private static void assertVersionScopedProcessFormPermissionRuleUnique(String schema, String sourceName) {
        assertTrue(Pattern.compile("uk_mes_pro_edhr_process_form_rule[^\\n;]*batch_record_version_id",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(schema).find(),
                sourceName + " unique key uk_mes_pro_edhr_process_form_rule must include batch_record_version_id");
    }

    private static String normalizeSchema(String schema) {
        return schema == null ? "" : schema.replace("`", "")
                .replace("\"", "").replace("\r", "\n");
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

    private static String controllerSource() {
        Path source = findProjectDir().resolve(
                "yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/"
                        + "MesProBatchRecordVersionGovernanceController.java");
        return assertDoesNotThrow(() -> Files.readString(source, StandardCharsets.UTF_8));
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
