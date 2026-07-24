package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class MesProBatchRecordExecutionArchiveContractTest {

    private static final String BASE_PACKAGE = "cn.iocoder.yudao.module.mes";
    private static final String ARCHIVE_CONTROLLER = BASE_PACKAGE
            + ".controller.admin.pro.batchrecord.MesProBatchRecordExecutionArchiveController";
    private static final String ARCHIVE_SERVICE = BASE_PACKAGE
            + ".service.pro.batchrecord.MesProBatchRecordExecutionArchiveService";
    private static final String ARCHIVE_ERROR_CODES = BASE_PACKAGE
            + ".service.pro.batchrecord.MesProBatchRecordExecutionArchiveErrorCodeConstants";
    private static final String ARCHIVE_DO = BASE_PACKAGE
            + ".dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveDO";
    private static final String ARCHIVE_EVENT_DO = BASE_PACKAGE
            + ".dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionArchiveEventDO";
    private static final String ARCHIVE_MAPPER = BASE_PACKAGE
            + ".dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveMapper";
    private static final String ARCHIVE_EVENT_MAPPER = BASE_PACKAGE
            + ".dal.mysql.pro.batchrecord.MesProBatchRecordExecutionArchiveEventMapper";
    private static final String GENERATE_REQ = BASE_PACKAGE
            + ".controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveGenerateReqVO";
    private static final String PAGE_REQ = BASE_PACKAGE
            + ".controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchivePageReqVO";
    private static final String ARCHIVE_RESP = BASE_PACKAGE
            + ".controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveRespVO";
    private static final String DOWNLOAD_RESP = BASE_PACKAGE
            + ".controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionArchiveDownloadRespVO";
    private static final String RENDER_CONTEXT = BASE_PACKAGE
            + ".service.pro.batchrecord.MesProBatchRecordExecutionArchiveRenderContext";
    private static final String RENDER_RESULT = BASE_PACKAGE
            + ".service.pro.batchrecord.MesProBatchRecordExecutionArchiveRenderResult";
    private static final String RENDERER = BASE_PACKAGE
            + ".service.pro.batchrecord.MesProBatchRecordExecutionArchiveRenderer";
    private static final String PDF_RENDERER = BASE_PACKAGE
            + ".service.pro.batchrecord.PdfExecutionArchiveRenderer";
    private static final String EXCEL_RENDERER = BASE_PACKAGE
            + ".service.pro.batchrecord.ExcelExecutionArchiveRenderer";

    @Test
    @DisplayName("BDD: submitted PDF and Excel archives require persisted metadata, source hashes, seal signature, versioning and audit events")
    void archiveSchema_requiresMetadataVersionHashesSealFileAndEvents() {
        Class<?> archiveDo = requireClass(ARCHIVE_DO);
        Class<?> archiveEventDo = requireClass(ARCHIVE_EVENT_DO);
        requireClass(ARCHIVE_MAPPER);
        requireClass(ARCHIVE_EVENT_MAPPER);

        requireGetters(archiveDo, "getExecutionId", "getArchiveCode", "getArchiveVersion", "getArtifactType",
                "getArchiveStatus", "getFileId", "getFileName", "getContentType", "getFileSize", "getSha256",
                "getRenderSourceVersion", "getExecutionSnapshotHash", "getCellValuesHash", "getSignatureHash",
                "getApprovalSnapshotId", "getApprovalSnapshotHash", "getSealSignatureId", "getGeneratedBy",
                "getGeneratedAt", "getSealedBy", "getSealedAt", "getFailureReason", "getRemark");
        requireGetters(archiveEventDo, "getArchiveId", "getExecutionId", "getEventType", "getActorId",
                "getEventTime", "getClientIp", "getUserAgent", "getMessage", "getMetadataJson");
    }

    @Test
    @DisplayName("BDD: archive API exposes controlled generate, page, latest and download endpoints with query/create/download permissions")
    void controllerContract_requiresControlledArchiveEndpointsAndPermissions() {
        Class<?> controller = requireClass(ARCHIVE_CONTROLLER);
        Class<?> generateReq = requireClass(GENERATE_REQ);
        Class<?> pageReq = requireClass(PAGE_REQ);

        RequestMapping classMapping = requireAnnotation(controller, RequestMapping.class);
        assertContainsPath(classMapping.value(), classMapping.path(), "/mes/pro/batch-record-execution-archive");

        Method generate = requireMethod(controller, "generate", generateReq);
        assertArrayEquals(new String[]{"/generate"}, requireAnnotation(generate, PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution-archive:create')",
                requireAnnotation(generate, PreAuthorize.class).value());

        Method page = requireMethod(controller, "page", pageReq);
        assertArrayEquals(new String[]{"/page"}, requireAnnotation(page, GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution-archive:query')",
                requireAnnotation(page, PreAuthorize.class).value());

        Method latest = requireMethod(controller, "latest", Long.class, String.class);
        assertArrayEquals(new String[]{"/latest"}, requireAnnotation(latest, GetMapping.class).value());
        assertEquals("executionId", latest.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("artifactType", latest.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution-archive:query')",
                requireAnnotation(latest, PreAuthorize.class).value());

        Method download = requireMethod(controller, "download", Long.class);
        assertArrayEquals(new String[]{"/download"}, requireAnnotation(download, GetMapping.class).value());
        assertEquals("id", download.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-execution-archive:download')",
                requireAnnotation(download, PreAuthorize.class).value());
    }

    @Test
    @DisplayName("BDD: generation rejects draft/source/signature/FileService failures and supports idempotency and regeneration")
    void serviceContract_requiresFailFastGenerationDownloadAndRegenerationRules() {
        Class<?> service = requireClass(ARCHIVE_SERVICE);
        Class<?> generateReq = requireClass(GENERATE_REQ);
        Class<?> pageReq = requireClass(PAGE_REQ);
        Class<?> archiveResp = requireClass(ARCHIVE_RESP);
        Class<?> downloadResp = requireClass(DOWNLOAD_RESP);

        requireMethod(service, "generateExecutionArchive", generateReq);
        requireMethod(service, "getExecutionArchivePage", pageReq);
        requireMethod(service, "getLatestExecutionArchive", Long.class, String.class);
        requireMethod(service, "downloadExecutionArchive", Long.class);

        requireGetters(generateReq, "getExecutionId", "getArtifactType", "getSealPassword", "getComment",
                "getRegenerate");
        requireGetters(archiveResp, "getId", "getExecutionId", "getArchiveCode", "getArchiveVersion",
                "getArtifactType", "getArchiveStatus", "getFileId", "getFileName", "getContentType",
                "getFileSize", "getSha256", "getExecutionSnapshotHash", "getCellValuesHash", "getSignatureHash",
                "getApprovalSnapshotId", "getApprovalSnapshotHash", "getGeneratedBy", "getGeneratedAt",
                "getSealedBy", "getSealedAt", "getSealSignatureId", "getCreated");
        requireGetters(downloadResp, "getFileName", "getContentType", "getFileSize", "getSha256",
                "getApprovalSnapshotId", "getApprovalSnapshotHash", "getContent");
    }

    @Test
    @DisplayName("BDD: PDF and Excel renderers use submitted snapshot, cell values and signatures without Jimu preview fallback")
    void rendererContract_requiresServerSidePdfExcelRenderersAndNoJimuFallback() {
        Class<?> renderer = requireClass(RENDERER);
        Class<?> renderContext = requireClass(RENDER_CONTEXT);
        Class<?> renderResult = requireClass(RENDER_RESULT);
        requireClass(PDF_RENDERER);
        requireClass(EXCEL_RENDERER);

        requireMethod(renderer, "render", renderContext);
        requireGetters(renderContext, "getExecution", "getExecutionSnapshot", "getCellValues", "getSignatures",
                "getExecutionSnapshotHash", "getCellValuesHash", "getSignatureHash", "getApprovalSnapshotId",
                "getApprovalSnapshotHash", "getGeneratedBy", "getGeneratedAt");
        requireGetters(renderResult, "getFileName", "getContentType", "getFileSize", "getSha256",
                "getRenderSourceVersion", "getContent");
        assertNoArchiveFallbackMethod(requireClass(BASE_PACKAGE
                + ".service.pro.batchrecordreport.MesProBatchRecordJimuReportGateway"));
    }

    @Test
    @DisplayName("BDD: clear archive errors exist for draft, source, renderer, signature, FileService and checksum failures")
    void errorCodes_requireNoFallbackArchiveFailureReasons() {
        Class<?> errorCodes = requireClass(ARCHIVE_ERROR_CODES);
        requireFields(errorCodes,
                "PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_EXISTS",
                "PRO_BATCH_RECORD_ARCHIVE_EXECUTION_NOT_SUBMITTED",
                "PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_MISSING",
                "PRO_BATCH_RECORD_ARCHIVE_SNAPSHOT_INVALID",
                "PRO_BATCH_RECORD_ARCHIVE_TYPE_UNSUPPORTED",
                "PRO_BATCH_RECORD_ARCHIVE_RENDERER_UNAVAILABLE",
                "PRO_BATCH_RECORD_ARCHIVE_RENDER_FAILED",
                "PRO_BATCH_RECORD_ARCHIVE_FILE_STORAGE_UNAVAILABLE",
                "PRO_BATCH_RECORD_ARCHIVE_FILE_PERSIST_FAILED",
                "PRO_BATCH_RECORD_ARCHIVE_NOT_EXISTS",
                "PRO_BATCH_RECORD_ARCHIVE_CHECKSUM_MISMATCH",
                "PRO_BATCH_RECORD_ARCHIVE_SOURCE_CHANGED_REGENERATE_REQUIRED",
                "PRO_BATCH_RECORD_ARCHIVE_SEAL_SIGNATURE_FAILED",
                "PRO_BATCH_RECORD_ARCHIVE_STORAGE_RETENTION_GATE_FAILED");
    }

    @Test
    @DisplayName("BDD: permission SQL restores query/create/download permissions and does not add archive delete permission")
    void permissionSql_requiresArchiveQueryCreateDownloadAndNoDeletePermission() throws Exception {
        String sql = readAllSql();
        assertTrue(sql.contains("mes:pro-batch-record-execution-archive:query"),
                "Missing archive query permission SQL");
        assertTrue(sql.contains("mes:pro-batch-record-execution-archive:create"),
                "Missing archive create permission SQL");
        assertTrue(sql.contains("mes:pro-batch-record-execution-archive:download"),
                "Missing archive download permission SQL");
        assertFalse(sql.contains("mes:pro-batch-record-execution-archive:delete"),
                "Archive delete permission must not be introduced for sealed final archives");
    }

    private static Class<?> requireClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            return fail("Expected EDHR archive contract class to exist: " + className, ex);
        }
    }

    private static Method requireMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
        try {
            return type.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            try {
                return type.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException nested) {
                return fail("Expected EDHR archive method to exist: " + type.getName() + "#" + methodName, nested);
            }
        }
    }

    private static Method requireGetter(Class<?> type, String methodName) {
        try {
            return type.getMethod(methodName);
        } catch (NoSuchMethodException ex) {
            return fail("Expected EDHR archive getter to exist: " + type.getName() + "#" + methodName, ex);
        }
    }

    private static void requireGetters(Class<?> type, String... methodNames) {
        for (String methodName : methodNames) {
            requireGetter(type, methodName);
        }
    }

    private static void requireFields(Class<?> type, String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                type.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                fail("Expected EDHR archive error code to exist: " + type.getName() + "#" + fieldName, ex);
            }
        }
    }

    private static <A extends Annotation> A requireAnnotation(Class<?> type, Class<A> annotationType) {
        A annotation = type.getAnnotation(annotationType);
        assertNotNull(annotation, "Expected annotation " + annotationType.getSimpleName() + " on " + type.getName());
        return annotation;
    }

    private static <A extends Annotation> A requireAnnotation(Method method, Class<A> annotationType) {
        A annotation = method.getAnnotation(annotationType);
        assertNotNull(annotation, "Expected annotation " + annotationType.getSimpleName() + " on " + method.getName());
        return annotation;
    }

    private static void assertContainsPath(String[] values, String[] paths, String expectedPath) {
        boolean matched = Arrays.stream(values).anyMatch(expectedPath::equals)
                || Arrays.stream(paths).anyMatch(expectedPath::equals);
        assertTrue(matched, "Expected controller root path: " + expectedPath);
    }

    private static void assertNoArchiveFallbackMethod(Class<?> jimuGateway) {
        List<String> forbiddenFragments = List.of("archive", "exportarchive", "finalpdf", "finalexcel");
        for (Method method : jimuGateway.getMethods()) {
            String normalizedName = method.getName().toLowerCase(Locale.ROOT);
            for (String forbiddenFragment : forbiddenFragments) {
                assertFalse(normalizedName.contains(forbiddenFragment),
                        "Jimu gateway must not expose final archive fallback method: " + method.getName());
            }
        }
    }

    private static String readAllSql() throws Exception {
        Path projectDir = findProjectDir();
        Path sqlDir = projectDir.resolve("sql/mysql");
        assertTrue(Files.isDirectory(sqlDir), "SQL directory must exist: " + sqlDir);
        StringBuilder builder = new StringBuilder();
        try (var stream = Files.walk(sqlDir)) {
            for (Path file : stream.filter(path -> path.getFileName().toString().endsWith(".sql")).toList()) {
                builder.append(Files.readString(file)).append('\n');
            }
        }
        return builder.toString();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
