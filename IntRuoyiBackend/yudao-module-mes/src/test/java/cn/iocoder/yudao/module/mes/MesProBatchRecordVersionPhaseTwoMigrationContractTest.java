package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordVersionMigrationItemDO;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProBatchRecordVersionPhaseTwoMigrationContractTest {

    private static final String GOVERNANCE_CONTROLLER =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProBatchRecordVersionGovernanceController";
    private static final String GOVERNANCE_SERVICE =
            "cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordVersionGovernanceService";
    private static final String MIGRATION_DIFF_RESP =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationDiffRespVO";
    private static final String MIGRATION_CONFIRM_REQ =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationConfirmReqVO";
    private static final String MIGRATION_CONFIRM_RESP =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationConfirmRespVO";
    private static final String DRAFT_REUPLOAD_RESP =
            "cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionDraftReuploadRespVO";

    private static final String RUNTIME_SCHEMA_FILE =
            "sql/mysql/20260708_mes_batch_record_version_phase_one.sql";
    private static final String TEST_SCHEMA_FILE =
            "yudao-module-mes/src/test/resources/sql/create_tables.sql";

    @Test
    void phaseTwoControllerExposesMigrationExperienceEndpoints() {
        Class<?> controllerType = loadClass(GOVERNANCE_CONTROLLER);
        RequestMapping requestMapping = controllerType.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping, "version governance controller must keep one governance base path");
        assertTrue(String.join(",", requestMapping.value()).contains("/mes/pro/batch-record-version/governance"),
                "phase two endpoints must reuse the existing version governance base path");

        assertEndpoint(controllerType, "getMigrationDiff", GetMapping.class, "/migration-diff");
        assertEndpoint(controllerType, "confirmMigrationItems", PostMapping.class, "/migration-confirm");
        assertEndpoint(controllerType, "reuploadDraft", PostMapping.class, "/draft-reupload");
    }

    @Test
    void phaseTwoServiceDeclaresMigrationExperienceOperations() {
        Class<?> serviceType = loadClass(GOVERNANCE_SERVICE);
        assertMethod(serviceType, "getMigrationDiff", Long.class);
        assertMethod(serviceType, "confirmMigrationItems", Long.class, loadClass(MIGRATION_CONFIRM_REQ));
        assertMethod(serviceType, "reuploadDraft", Long.class, MultipartFile.class, List.class, String.class);
    }

    @Test
    void phaseTwoVoContractCoversDiffConfirmAndDraftReupload() {
        assertHasFields(loadClass(MIGRATION_DIFF_RESP),
                "versionId", "items", "blockerCount", "confirmRequiredCount", "confirmedCount", "approvalReady");
        assertHasFields(loadClass(MIGRATION_DIFF_RESP + "$Item"),
                "itemId", "diffGroup", "diffType", "riskLevel", "sourceLogicalKey", "targetLogicalKey",
                "matchConfidence", "matchEvidenceJson", "ruleType", "businessOwnerType", "confirmed",
                "confirmedBy", "confirmedAt", "confirmComment", "message");
        assertHasFields(loadClass(MIGRATION_CONFIRM_REQ), "itemIds", "comment", "idempotencyKey");
        assertHasFields(loadClass(MIGRATION_CONFIRM_RESP),
                "versionId", "confirmedItemIds", "confirmedBy", "confirmedAt", "confirmComment", "idempotencyKey");
        assertHasFields(loadClass(DRAFT_REUPLOAD_RESP), "voidedVersionId", "newVersionId", "versionNo", "status");
    }

    @Test
    void draftReuploadRequiresRealMultipartWordImportInsteadOfMetadataOnlyReplacement() throws Exception {
        Class<?> controllerType = loadClass(GOVERNANCE_CONTROLLER);
        Method controllerMethod = assertMethod(controllerType, "reuploadDraft", Long.class, MultipartFile.class,
                List.class, String.class);
        assertRequestParam(controllerMethod.getParameters()[0], "versionId");
        assertRequestParam(controllerMethod.getParameters()[1], "file");
        assertRequestParam(controllerMethod.getParameters()[2], "productNames");

        Class<?> serviceType = loadClass(GOVERNANCE_SERVICE);
        assertMethod(serviceType, "reuploadDraft", Long.class, MultipartFile.class, List.class, String.class);

        Path projectDir = findProjectDir();
        Path controllerPath = projectDir.resolve("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/batchrecord/MesProBatchRecordVersionGovernanceController.java");
        Path servicePath = projectDir.resolve("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordVersionGovernanceServiceImpl.java");

        String controllerSource = Files.readString(controllerPath, StandardCharsets.UTF_8);
        String serviceSource = Files.readString(servicePath, StandardCharsets.UTF_8);

        assertTrue(controllerSource.contains("@RequestParam(\"file\") MultipartFile file"),
                "draft reupload endpoint must receive the actual Word file as multipart data");
        assertTrue(controllerSource.contains("@RequestParam(\"productNames\") List<String> productNames"),
                "draft reupload endpoint must receive product names for real route recognition");
        assertFalse(controllerSource.contains("@RequestBody MesProBatchRecordVersionDraftReuploadReqVO"),
                "draft reupload must not accept metadata-only JSON replacement requests");
        assertTrue(serviceSource.contains("batchRecordReportService.recognizeUploadedRoute"),
                "draft reupload must reuse the real Word import route recognition service");
        assertTrue(serviceSource.contains("file, definition.getRouteKey(), definition.getBatchRecordName(), \"UPGRADE\","),
                "draft reupload must use the explicit upgrade import action");
        assertTrue(serviceSource.contains("dccProjectResolver.requireEnabledByRoute(routeId)"),
                "draft reupload must resolve DCC identity from the version's formal route binding");
        assertTrue(serviceSource.contains("selectedProject.dccProjectCodeId(), getLoginUserId()"),
                "draft reupload must pass the formally bound DCC project ID into real import");
        assertFalse(serviceSource.contains("buildReuploadVersionNo("),
                "draft reupload must not synthesize a new version from sourceFileName/sourceFileSha256 metadata");
        assertFalse(serviceSource.contains("setSourceFileSha256(reqVO.getSourceFileSha256())"),
                "draft reupload must not write caller-provided SHA values without parsing the Word file");
    }

    @Test
    void confirmRequiredMigrationControlsPrecheckStatusUntilAuthorized() throws Exception {
        Path projectDir = findProjectDir();
        Path reportServicePath = projectDir.resolve("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java");
        Path governanceServicePath = projectDir.resolve("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordVersionGovernanceServiceImpl.java");

        String reportServiceSource = Files.readString(reportServicePath, StandardCharsets.UTF_8);
        String governanceServiceSource = Files.readString(governanceServicePath, StandardCharsets.UTF_8);

        assertTrue(reportServiceSource.contains("migrationItemMapper.countBlockingItems(targetVersion.getId()) > 0"),
                "real upgrade import must check blocking migration items before returning the version status");
        assertTrue(reportServiceSource.contains("targetVersion.setStatus(\"PRECHECK_FAILED\")"),
                "CONFIRM_REQUIRED migration items must keep the uploaded version in PRECHECK_FAILED");
        assertTrue(governanceServiceSource.contains("promotePrecheckFailedVersionWhenMigrationReady(versionId)"),
                "migration confirmation must re-evaluate precheck readiness");
        assertTrue(governanceServiceSource.contains("STATUS_PRECHECK_PASSED"),
                "confirmed migration items must promote the version to PRECHECK_PASSED when no blockers remain");
    }

    @Test
    void migrationItemContractStoresStructuredDiffAndConfirmAudit() {
        assertHasFields(MesProBatchRecordVersionMigrationItemDO.class,
                "diffGroup", "diffType", "sourceLogicalKey", "targetLogicalKey", "matchConfidence",
                "matchEvidenceJson", "riskLevel", "ruleType", "businessOwnerType", "confirmed", "confirmedBy",
                "confirmedAt", "confirmComment", "confirmIdempotencyKey");
    }

    @Test
    void confirmedConfirmRequiredItemsDoNotBlockApprovalOrInspection() throws Exception {
        Path projectDir = findProjectDir();
        Path mapperPath = projectDir.resolve("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecordreport/MesProBatchRecordVersionMigrationItemMapper.java");
        Path governanceServicePath = projectDir.resolve("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordVersionGovernanceServiceImpl.java");

        String mapperSource = Files.readString(mapperPath, StandardCharsets.UTF_8);
        String governanceSource = Files.readString(governanceServicePath, StandardCharsets.UTF_8);

        assertTrue(mapperSource.contains("countUnconfirmedConfirmRequiredItems"),
                "mapper must expose explicit unconfirmed CONFIRM_REQUIRED counting for phase two approval gates");
        assertTrue(mapperSource.contains("!Boolean.TRUE.equals(item.getConfirmed())"),
                "confirmed CONFIRM_REQUIRED items must not remain blocking");
        assertFalse(mapperSource.contains(".in(MesProBatchRecordVersionMigrationItemDO::getRiskLevel, List.of(\"BLOCKER\", \"CONFIRM_REQUIRED\"))"),
                "blocking count must not treat every CONFIRM_REQUIRED item as unresolved");
        assertTrue(governanceSource.contains(".setApprovalReady(blockingCount == 0)"),
                "approval readiness must use the unresolved blocking count");
        assertTrue(governanceSource.contains("return migrationItemMapper.countBlockingItems(versionId);"),
                "inspection and metrics must use unresolved blocking count after manual confirmation");
    }

    @Test
    void schemasRetainPhaseTwoMigrationExperienceColumns() throws Exception {
        Path projectDir = findProjectDir();
        String runtimeSchema = Files.readString(projectDir.resolve(RUNTIME_SCHEMA_FILE), StandardCharsets.UTF_8);
        String testSchema = Files.readString(projectDir.resolve(TEST_SCHEMA_FILE), StandardCharsets.UTF_8);

        for (String schema : new String[] { runtimeSchema, testSchema }) {
            assertSchemaContainsColumns(schema, "mes_pro_batch_record_version_migration_item",
                    "diff_group", "diff_type", "source_logical_key", "target_logical_key", "match_confidence",
                    "match_evidence_json", "risk_level", "rule_type", "business_owner_type", "confirmed",
                    "confirmed_by", "confirmed_at", "confirm_comment", "confirm_idempotency_key");
        }
        assertFalse(runtimeSchema.contains("DROP TABLE"), "phase two migration must be non-destructive");
        assertFalse(runtimeSchema.contains("TRUNCATE TABLE"), "phase two migration must not truncate data");
        assertFalse(Pattern.compile("DELETE\\s+FROM\\s+`?mes_", Pattern.CASE_INSENSITIVE).matcher(runtimeSchema).find(),
                "phase two migration must not delete existing MES data");
    }

    private static void assertEndpoint(Class<?> controllerType, String methodName, Class<?> annotationType, String path) {
        Method method = assertMethodByName(controllerType, methodName);
        Object annotation = method.getAnnotation(annotationType.asSubclass(java.lang.annotation.Annotation.class));
        assertNotNull(annotation, "Missing " + annotationType.getSimpleName() + " on " + methodName);
        String[] values = readAnnotationValue(annotation);
        assertTrue(String.join(",", values).contains(path), methodName + " must expose " + path);
    }

    private static Method assertMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return type.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("Missing method " + type.getName() + "#" + methodName, ex);
        }
    }

    private static Method assertMethodByName(Class<?> type, String methodName) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Missing method " + type.getName() + "#" + methodName);
    }

    private static void assertHasFields(Class<?> type, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Field field = findField(type, fieldName);
            assertNotNull(field, "Missing field " + type.getName() + "#" + fieldName);
        }
    }

    private static void assertRequestParam(Parameter parameter, String expectedName) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        assertNotNull(requestParam, "Missing @RequestParam for " + expectedName);
        assertTrue(requestParam.value().equals(expectedName) || requestParam.name().equals(expectedName),
                "Request parameter must be named " + expectedName);
    }

    private static Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void assertSchemaContainsColumns(String schema, String tableName, String... columns) {
        assertTrue(schema.contains(tableName), "Schema must contain table " + tableName);
        for (String column : columns) {
            assertTrue(schema.contains(column), "Schema for " + tableName + " must contain column " + column);
        }
    }

    private static String[] readAnnotationValue(Object annotation) {
        try {
            return (String[]) annotation.getClass().getMethod("value").invoke(annotation);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Unable to read mapping annotation value", ex);
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new AssertionError("Missing class " + className, ex);
        }
    }

    private static Path findProjectDir() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve(RUNTIME_SCHEMA_FILE))) {
                return current;
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate project directory containing " + RUNTIME_SCHEMA_FILE);
    }
}
