package cn.iocoder.yudao.module.mes;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrInitBatchController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitIssuePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitManifestUploadReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrInitBatchServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrInitBatchContractTest {

    private static final String RUNTIME_SCHEMA_FILE =
            "sql/mysql/20260618_mes_edhr_init_batch_precheck.sql";

    @Test
    void dataObjectsAndMappersDeclareInitializationContract() {
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitBatchDO");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitManifestDO");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitIssueDO");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrInitBatchMapper");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrInitManifestMapper");
        assertTypeExists("cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrInitIssueMapper");

        assertHasFields("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitBatchDO",
                "targetEnvironment", "targetTenantId", "dataVersion", "ownerUserId",
                "approvalOwnerUserId", "blockingIssueCount");
        assertHasFields("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitManifestDO",
                "initBatchId", "packageType", "manifestHash", "checksumJson", "manifestJson");
        assertHasFields("cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitIssueDO",
                "issueCode", "issueLevel", "sourceFileName", "sourceRowNo", "sourceFieldName",
                "responsibleName", "impactScopeJson");
    }

    @Test
    void controllerMappingsExposeFirstSliceOnly() throws Exception {
        RequestMapping mapping = MesProEdhrInitBatchController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-init-batch"}, mapping.value());

        assertGetMapping("getPage", new Class<?>[]{MesProEdhrInitBatchPageReqVO.class},
                "/page", "mes:pro-edhr-init-batch:query");
        assertGetMapping("get", new Class<?>[]{Long.class},
                "/get", "mes:pro-edhr-init-batch:query");
        assertPostMapping("create", new Class<?>[]{MesProEdhrInitBatchCreateReqVO.class},
                "/create", "mes:pro-edhr-init-batch:create");
        assertPostMapping("upload", new Class<?>[]{MesProEdhrInitManifestUploadReqVO.class},
                "/upload", "mes:pro-edhr-init-batch:create");
        assertPostMapping("precheck", new Class<?>[]{Long.class},
                "/precheck", "mes:pro-edhr-init-batch:precheck");
        assertGetMapping("getIssuePage", new Class<?>[]{MesProEdhrInitIssuePageReqVO.class},
                "/issue/page", "mes:pro-edhr-init-batch:query");
    }

    @Test
    void serviceConstantsDeclareFailFastPrecheckStates() {
        assertEquals("DRAFT", MesProEdhrInitBatchServiceImpl.STATUS_DRAFT);
        assertEquals("PRECHECK_FAILED", MesProEdhrInitBatchServiceImpl.STATUS_PRECHECK_FAILED);
        assertEquals("PRECHECK_PASSED", MesProEdhrInitBatchServiceImpl.STATUS_PRECHECK_PASSED);
        assertEquals("MISSING_MANIFEST", MesProEdhrInitBatchServiceImpl.ISSUE_CODE_MISSING_MANIFEST);
        assertEquals("BLOCKER", MesProEdhrInitBatchServiceImpl.ISSUE_LEVEL_BLOCKER);
        assertEquals("OPEN", MesProEdhrInitBatchServiceImpl.ISSUE_STATUS_OPEN);
        assertEquals("SUPERSEDED", MesProEdhrInitBatchServiceImpl.ISSUE_STATUS_SUPERSEDED);
    }

    @Test
    void runtimeMigrationDeclaresMenuAndNonDestructiveSchema() throws Exception {
        String schema = Files.readString(findProjectDir().resolve(RUNTIME_SCHEMA_FILE), StandardCharsets.UTF_8);
        for (String table : new String[]{
                "mes_pro_edhr_init_batch",
                "mes_pro_edhr_init_manifest",
                "mes_pro_edhr_init_issue"}) {
            assertTrue(schemaContainsToken(schema, table), "Missing table " + table);
        }
        for (String permission : new String[]{
                "mes:pro-edhr-init-batch:query",
                "mes:pro-edhr-init-batch:create",
                "mes:pro-edhr-init-batch:precheck",
                "mes:pro-edhr-init-batch:import",
                "mes:pro-edhr-init-batch:signoff"}) {
            assertTrue(schemaContainsToken(schema, permission), "Missing permission " + permission);
        }

        assertFalse(Pattern.compile("\\b(DROP\\s+TABLE|TRUNCATE\\s+TABLE|DELETE\\s+FROM)\\b",
                        Pattern.CASE_INSENSITIVE).matcher(schema).find(),
                "Initialization migration must not contain destructive table/data operations");
        assertFalse(Pattern.compile("\\b(INSERT\\s+IGNORE|ON\\s+DUPLICATE\\s+KEY\\s+UPDATE)\\b",
                        Pattern.CASE_INSENSITIVE).matcher(schema).find(),
                "Initialization migration must not silently ignore or overwrite rows");
    }

    private static void assertGetMapping(String methodName,
                                         Class<?>[] parameterTypes,
                                         String path,
                                         String permission) throws Exception {
        Method method = MesProEdhrInitBatchController.class.getDeclaredMethod(methodName, parameterTypes);
        assertArrayEquals(new String[]{path}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private static void assertPostMapping(String methodName,
                                          Class<?>[] parameterTypes,
                                          String path,
                                          String permission) throws Exception {
        Method method = MesProEdhrInitBatchController.class.getDeclaredMethod(methodName, parameterTypes);
        assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private static void assertHasFields(String className, String... fieldNames) {
        Class<?> type = assertTypeExists(className);
        for (String fieldName : fieldNames) {
            assertDoesNotThrow(() -> declaredField(type, fieldName),
                    () -> "Missing field " + type.getSimpleName() + "." + fieldName);
        }
    }

    private static Field declaredField(Class<?> type, String fieldName) throws NoSuchFieldException {
        return type.getDeclaredField(fieldName);
    }

    private static Class<?> assertTypeExists(String className) {
        return assertDoesNotThrow(() -> Class.forName(className), () -> "Missing type " + className);
    }

    private static boolean schemaContainsToken(String schema, String token) {
        return Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE).matcher(schema).find();
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
