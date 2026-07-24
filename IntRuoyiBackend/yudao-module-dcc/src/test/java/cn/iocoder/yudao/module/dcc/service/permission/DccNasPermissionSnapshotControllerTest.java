package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccNasPermissionSnapshotController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionSnapshotItemRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPermissionSnapshotSummaryRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasPermissionSnapshotControllerTest extends BaseMockitoUnitTest {

    private static final Long TASK_ID = 6201L;
    private static final String SUMMARY_PATH =
            "/dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-snapshot";
    private static final String ITEMS_PATH =
            "/dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-snapshot/items";

    @Mock
    private DccNasPermissionSnapshotQueryService snapshotQueryService;

    @InjectMocks
    private DccNasPermissionSnapshotController controller;

    @Test
    void getPermissionSnapshotSummary_mapsEndpointPermissionAndDelegatesToQueryService() throws Exception {
        LocalDateTime capturedAt = LocalDateTime.of(2026, 5, 26, 21, 30);
        when(snapshotQueryService.getSummary(TASK_ID)).thenReturn(new DccNasPermissionSnapshotQueryService.SummaryResult(
                TASK_ID,
                "CAPTURED",
                List.of("Quality/SOP"),
                34892L,
                128004L,
                0L,
                0L,
                0L,
                capturedAt,
                null,
                true));

        Method method = findMappedMethod(GetMapping.class, SUMMARY_PATH);
        assertCommonResultType(method, DccNasPermissionSnapshotSummaryRespVO.class);
        assertHasManagePermission(method);

        CommonResult<?> result = invoke(method, TASK_ID);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(DccNasPermissionSnapshotSummaryRespVO.class.isInstance(result.getData()));
        Object data = result.getData();
        assertEquals(TASK_ID, readProperty(data, "taskId"));
        assertEquals("CAPTURED", readProperty(data, "snapshotStatus"));
        assertEquals(List.of("Quality/SOP"), readProperty(data, "selectedNasPaths"));
        assertLongProperty(data, "directorySnapshotCount", 34892L);
        assertLongProperty(data, "aceCount", 128004L);
        assertLongProperty(data, "unsupportedAceCount", 0L);
        assertLongProperty(data, "unmappedPrincipalCount", 0L);
        assertLongProperty(data, "blockerCount", 0L);
        assertEquals(capturedAt, readProperty(data, "capturedAt"));
        assertEquals(null, readProperty(data, "lastFailureMessage"));
        assertEquals(Boolean.TRUE, readProperty(data, "restoreSupported"));
        verify(snapshotQueryService).getSummary(TASK_ID);
    }

    @Test
    void getPermissionSnapshotItems_mapsEndpointPermissionAndReturnsPagedBlockers() throws Exception {
        when(snapshotQueryService.getItems(TASK_ID, 1, 100, "BLOCKED"))
                .thenReturn(new PageResult<>(List.of(new DccNasPermissionSnapshotQueryService.ItemResult(
                        90001L,
                        "Quality/SOP/研发",
                        60001L,
                        "BLOCKED",
                        7L,
                        List.of(new DccNasPermissionSnapshotQueryService.BlockerResult(
                                "DCC_NAS_ACL_DENY_UNSUPPORTED",
                                "NAS ACL contains explicit DENY ACE",
                                "CORP\\temp-user",
                                3)))), 1L));

        Method method = findMappedMethod(GetMapping.class, ITEMS_PATH);
        assertCommonResultOfPageResult(method);
        assertHasManagePermission(method);

        CommonResult<?> result = invoke(method, TASK_ID, 1, 100, "BLOCKED");

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(result.getData() instanceof PageResult);
        PageResult<?> page = (PageResult<?>) result.getData();
        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertTrue(DccNasPermissionSnapshotItemRespVO.class.isInstance(page.getList().get(0)));
        Object item = page.getList().get(0);
        assertEquals(90001L, readProperty(item, "taskItemId"));
        assertEquals("Quality/SOP/研发", readProperty(item, "nasPath"));
        assertEquals(60001L, readProperty(item, "dccDirectoryId"));
        assertEquals("BLOCKED", readProperty(item, "snapshotStatus"));
        assertLongProperty(item, "aceCount", 7L);

        List<?> blockers = readListProperty(item, "blockers");
        assertEquals(1, blockers.size());
        assertEquals("DCC_NAS_ACL_DENY_UNSUPPORTED", readProperty(blockers.get(0), "code"));
        assertEquals("NAS ACL contains explicit DENY ACE", readProperty(blockers.get(0), "message"));
        assertEquals("CORP\\temp-user", readProperty(blockers.get(0), "principal"));
        assertEquals(3, readProperty(blockers.get(0), "aceIndex"));
        verify(snapshotQueryService).getItems(TASK_ID, 1, 100, "BLOCKED");
    }

    private CommonResult<?> invoke(Method method, Object... args) throws Exception {
        Object result = method.invoke(controller, args);
        assertTrue(result instanceof CommonResult);
        return (CommonResult<?>) result;
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccNasPermissionSnapshotController.class.getDeclaredMethods())
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
        RequestMapping requestMapping = DccNasPermissionSnapshotController.class.getAnnotation(RequestMapping.class);
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

    private void assertCommonResultOfPageResult(Method method) {
        assertEquals(CommonResult.class, method.getReturnType());
        Type genericReturnType = method.getGenericReturnType();
        assertTrue(genericReturnType instanceof ParameterizedType);
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        assertEquals(CommonResult.class, parameterizedType.getRawType());
        assertTrue(parameterizedType.getActualTypeArguments()[0].getTypeName().contains(PageResult.class.getName()));
    }

    private void assertHasManagePermission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on backend API surface");
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:access-rule:manage"));
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
