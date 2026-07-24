package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccNasPermissionRestoreController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionRestoreApplyReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionRestoreApplyRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionRestorePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionRestoreStatusRespVO;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasPermissionRestoreControllerTest extends BaseMockitoUnitTest {

    private static final Long TASK_ID = 6201L;
    private static final Long OPERATOR_USER_ID = 99L;
    private static final String PREVIEW_PATH =
            "/dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore/preview";
    private static final String APPLY_PATH =
            "/dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore";
    private static final Long RESTORE_ID = 7301L;
    private static final String STATUS_PATH =
            "/dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-restore/{restoreId}";

    @Mock
    private DccNasPermissionRestoreService restoreService;

    @InjectMocks
    private DccNasPermissionRestoreController controller;

    @Test
    void previewPermissionRestore_delegatesToServiceAndReturnsCommonResultFields() throws Exception {
        when(restoreService.preview(TASK_ID)).thenReturn(new DccNasPermissionRestoreService.PreviewResult(
                TASK_ID,
                true,
                "plan-hash-001",
                "REPLACE_DIRECTORY_RULES",
                2L,
                3L,
                true,
                null,
                List.of(new DccNasPermissionRestoreService.RestoreBlocker(
                        "DCC_NAS_PRINCIPAL_UNMAPPED",
                        "NAS principal is not mapped",
                        7101L,
                        "\\\\nas\\dcc\\quality",
                        "S-1-5-21-1001")),
                List.of(new DccNasPermissionRestoreService.RestoreRulePreview(
                        8101L,
                        "\\\\nas\\dcc\\quality",
                        "USER",
                        9101L,
                        true,
                        true,
                        true))));

        CommonResult<?> result = invokeMappedEndpoint(
                GetMapping.class, PREVIEW_PATH, DccNasPermissionRestorePreviewRespVO.class, TASK_ID);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(DccNasPermissionRestorePreviewRespVO.class.isInstance(result.getData()));
        Object data = result.getData();
        assertEquals(TASK_ID, readProperty(data, "taskId"));
        assertEquals(Boolean.TRUE, readProperty(data, "canRestore"));
        assertEquals("plan-hash-001", readProperty(data, "planHash"));
        assertEquals("REPLACE_DIRECTORY_RULES", readProperty(data, "restoreMode"));
        assertLongProperty(data, "directoryCount", 2L);
        assertLongProperty(data, "ruleCount", 3L);
        assertEquals(Boolean.TRUE, readProperty(data, "runtimeEnforcementReady"));
        assertEquals(null, readProperty(data, "runtimeEnforcementBlocker"));

        List<?> blockers = readListProperty(data, "blockers");
        assertEquals(1, blockers.size());
        assertEquals("DCC_NAS_PRINCIPAL_UNMAPPED", readProperty(blockers.get(0), "code"));
        assertEquals("NAS principal is not mapped", readProperty(blockers.get(0), "message"));
        assertEquals(7101L, readProperty(blockers.get(0), "directorySnapshotId"));
        assertEquals("\\\\nas\\dcc\\quality", readProperty(blockers.get(0), "nasPath"));
        assertEquals("S-1-5-21-1001", readProperty(blockers.get(0), "trusteeSid"));

        List<?> sampleRules = readListProperty(data, "sampleRules");
        assertEquals(1, sampleRules.size());
        assertEquals(8101L, readProperty(sampleRules.get(0), "directoryId"));
        assertEquals("\\\\nas\\dcc\\quality", readProperty(sampleRules.get(0), "nasPath"));
        assertEquals("USER", readProperty(sampleRules.get(0), "subjectType"));
        assertEquals(9101L, readProperty(sampleRules.get(0), "subjectId"));
        assertEquals(Boolean.TRUE, readProperty(sampleRules.get(0), "canQuery"));
        assertEquals(Boolean.TRUE, readProperty(sampleRules.get(0), "canPreview"));
        assertEquals(Boolean.TRUE, readProperty(sampleRules.get(0), "canDownload"));
        verify(restoreService).preview(TASK_ID);
    }

    @Test
    void applyPermissionRestore_buildsCommandWithLoginUserAndReturnsCommonResultFields() throws Exception {
        DccNasPermissionRestoreApplyReqVO reqVO = new DccNasPermissionRestoreApplyReqVO();
        reqVO.setIdempotencyKey("restore-key-001");
        reqVO.setPlanHash("plan-hash-001");
        reqVO.setRestoreMode("REPLACE_DIRECTORY_RULES");
        reqVO.setChangeReason("Restore NAS ACL snapshot after migration verification");

        when(restoreService.apply(any(DccNasPermissionRestoreService.ApplyRestoreCommand.class)))
                .thenReturn(new DccNasPermissionRestoreService.ApplyResult(
                        7301L,
                        TASK_ID,
                        "WAITING",
                        2L,
                        3L,
                        0L,
                        0L));

        CommonResult<?> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(OPERATOR_USER_ID);

            result = invokeMappedEndpoint(
                    PostMapping.class, APPLY_PATH, DccNasPermissionRestoreApplyRespVO.class, TASK_ID, reqVO);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(DccNasPermissionRestoreApplyRespVO.class.isInstance(result.getData()));
        Object data = result.getData();
        assertEquals(7301L, readProperty(data, "restoreId"));
        assertEquals(TASK_ID, readProperty(data, "taskId"));
        assertEquals("WAITING", readProperty(data, "status"));
        assertLongProperty(data, "directoryCount", 2L);
        assertLongProperty(data, "ruleCount", 3L);
        assertLongProperty(data, "completedDirectoryCount", 0L);
        assertLongProperty(data, "failedDirectoryCount", 0L);

        ArgumentCaptor<DccNasPermissionRestoreService.ApplyRestoreCommand> commandCaptor =
                ArgumentCaptor.forClass(DccNasPermissionRestoreService.ApplyRestoreCommand.class);
        verify(restoreService).apply(commandCaptor.capture());
        DccNasPermissionRestoreService.ApplyRestoreCommand command = commandCaptor.getValue();
        assertEquals(TASK_ID, command.taskId());
        assertEquals("restore-key-001", command.idempotencyKey());
        assertEquals("plan-hash-001", command.planHash());
        assertEquals("REPLACE_DIRECTORY_RULES", command.restoreMode());
        assertEquals("Restore NAS ACL snapshot after migration verification", command.changeReason());
        assertEquals(OPERATOR_USER_ID, command.operatorUserId());
    }

    @Test
    void applyReqVO_requiresNotBlankValidationForIdempotencyPlanHashAndRestoreMode() throws Exception {
        assertFieldHasNotBlank("idempotencyKey");
        assertFieldHasNotBlank("planHash");
        assertFieldHasNotBlank("restoreMode");
    }

    @Test
    void getPermissionRestoreStatus_mapsEndpointAndReturnsPollableStatusFields() throws Exception {
        LocalDateTime startedAt = LocalDateTime.of(2026, 5, 26, 22, 0);
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 26, 22, 10);
        when(restoreService.getStatus(TASK_ID, RESTORE_ID))
                .thenReturn(new DccNasPermissionRestoreService.RestoreStatusResult(
                        RESTORE_ID,
                        TASK_ID,
                        "FAILED",
                        2L,
                        3L,
                        1L,
                        1L,
                        "Directory rule hash changed after preview",
                        startedAt,
                        completedAt));

        CommonResult<?> result = invokeMappedEndpoint(
                GetMapping.class, STATUS_PATH, DccNasPermissionRestoreStatusRespVO.class, TASK_ID, RESTORE_ID);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(DccNasPermissionRestoreStatusRespVO.class.isInstance(result.getData()));
        Object data = result.getData();
        assertEquals(RESTORE_ID, readProperty(data, "restoreId"));
        assertEquals(TASK_ID, readProperty(data, "taskId"));
        assertEquals("FAILED", readProperty(data, "status"));
        assertLongProperty(data, "directoryCount", 2L);
        assertLongProperty(data, "ruleCount", 3L);
        assertLongProperty(data, "completedDirectoryCount", 1L);
        assertLongProperty(data, "failedDirectoryCount", 1L);
        assertEquals("Directory rule hash changed after preview", readProperty(data, "lastFailureMessage"));
        assertEquals(startedAt, readProperty(data, "startedAt"));
        assertEquals(completedAt, readProperty(data, "completedAt"));
        verify(restoreService).getStatus(TASK_ID, RESTORE_ID);
    }

    private CommonResult<?> invokeMappedEndpoint(Class<? extends Annotation> mappingAnnotationType,
                                                String expectedFullPath,
                                                Class<?> expectedDataType,
                                                Object... args) throws Exception {
        Method method = findMappedMethod(mappingAnnotationType, expectedFullPath);
        assertCommonResultType(method, expectedDataType);
        Object result = method.invoke(controller, args);
        assertTrue(result instanceof CommonResult);
        return (CommonResult<?>) result;
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccNasPermissionRestoreController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(mappingAnnotationType))
                .filter(method -> hasFullMappingPath(method.getAnnotation(mappingAnnotationType), expectedFullPath))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing endpoint mapping: " + expectedFullPath));
    }

    private boolean hasFullMappingPath(Annotation methodMapping, String expectedFullPath) {
        return classPrefixes().flatMap(prefix -> annotationPaths(methodMapping)
                        .map(methodPath -> normalizePath(prefix + "/" + methodPath)))
                .anyMatch(expectedFullPath::equals);
    }

    private Stream<String> classPrefixes() {
        RequestMapping requestMapping = DccNasPermissionRestoreController.class.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return Stream.of("");
        }
        return Stream.concat(Arrays.stream(requestMapping.value()), Arrays.stream(requestMapping.path()))
                .distinct();
    }

    private Stream<String> annotationPaths(Annotation annotation) {
        try {
            String[] value = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
            String[] path = (String[]) annotation.annotationType().getMethod("path").invoke(annotation);
            return Stream.concat(Arrays.stream(value), Arrays.stream(path)).distinct();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Cannot inspect endpoint mapping annotation", ex);
        }
    }

    private void assertCommonResultType(Method method, Class<?> expectedDataType) {
        assertEquals(CommonResult.class, method.getReturnType());
        Type genericReturnType = method.getGenericReturnType();
        assertTrue(genericReturnType instanceof ParameterizedType);
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        assertEquals(CommonResult.class, parameterizedType.getRawType());
        assertEquals(expectedDataType, parameterizedType.getActualTypeArguments()[0]);
    }

    private void assertFieldHasNotBlank(String fieldName) throws NoSuchFieldException {
        assertNotNull(DccNasPermissionRestoreApplyReqVO.class.getDeclaredField(fieldName)
                .getAnnotation(NotBlank.class));
    }

    private List<?> readListProperty(Object target, String propertyName) throws Exception {
        Object value = readProperty(target, propertyName);
        assertTrue(value instanceof List);
        return (List<?>) value;
    }

    private void assertLongProperty(Object target, String propertyName, long expected) throws Exception {
        Object value = readProperty(target, propertyName);
        assertTrue(value instanceof Number);
        assertEquals(expected, ((Number) value).longValue());
    }

    private Object readProperty(Object target, String propertyName) throws Exception {
        String suffix = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        for (String getterName : List.of("get" + suffix, "is" + suffix)) {
            try {
                return target.getClass().getMethod(getterName).invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Try the next JavaBeans getter style.
            }
        }
        throw new AssertionError("Missing getter for property: " + propertyName);
    }

    private String normalizePath(String path) {
        String normalized = path.replace('\\', '/').replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
