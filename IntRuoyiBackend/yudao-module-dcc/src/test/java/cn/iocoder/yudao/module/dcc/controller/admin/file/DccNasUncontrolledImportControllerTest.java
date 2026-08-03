package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccNasUncontrolledImportControllerTest extends BaseMockitoUnitTest {

    private static final String CONTENT_PATH =
            "/dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/content";

    @Mock
    private DccControlledFileNasTransferService nasTransferService;
    @InjectMocks
    private DccNasUncontrolledImportController controller;

    @Test
    void nasUncontrolledImport_mapsContentAsBinaryWithSnapshotQueryParamsAndWritePermission() {
        Method content = findMappedMethod(GetMapping.class, CONTENT_PATH);

        assertEquals(ResponseEntity.class, content.getReturnType());
        assertNotEquals(CommonResult.class, content.getReturnType());
        assertPermissionContains(content,
                "dcc:controlled-file:submit",
                "dcc:controlled-file:directory:manage",
                "dcc:controlled-file:category:manage");
        assertRequestParam(content, "sourceSignature", String.class);
        assertRequestParam(content, "localRelativePath", String.class);
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccNasUncontrolledImportController.class.getDeclaredMethods())
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
        RequestMapping requestMapping = DccNasUncontrolledImportController.class.getAnnotation(RequestMapping.class);
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

    private void assertPermissionContains(Method method, String... expectedPermissions) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on NAS uncontrolled import endpoint");
        for (String expectedPermission : expectedPermissions) {
            assertTrue(preAuthorize.value().contains(expectedPermission),
                    "Missing permission " + expectedPermission + " on " + method.getName());
        }
    }

    private void assertRequestParam(Method method, String expectedName, Class<?> expectedType) {
        boolean found = Arrays.stream(method.getParameters())
                .filter(parameter -> expectedType.equals(parameter.getType()))
                .anyMatch(parameter -> hasRequestParamName(parameter, expectedName));
        assertTrue(found, "Missing @RequestParam " + expectedName);
    }

    private boolean hasRequestParamName(Parameter parameter, String expectedName) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        return requestParam != null
                && (expectedName.equals(requestParam.value()) || expectedName.equals(requestParam.name()));
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
