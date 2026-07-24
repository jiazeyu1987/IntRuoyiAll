package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataImportPreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMetadataImportExportService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileMetadataImportExportControllerTest extends BaseMockitoUnitTest {

    private static final String EXPORT_PATH = "/dcc/controlled-files/metadata/export-excel";
    private static final String RECOGNITION_RECORD_EXPORT_PATH = "/dcc/controlled-files/recognition-records/export-excel";
    private static final String TEMPLATE_PATH = "/dcc/controlled-files/metadata/import-template";
    private static final String PREVIEW_PATH = "/dcc/controlled-files/metadata/import-preview";
    private static final String CONFIRM_PATH = "/dcc/controlled-files/metadata/import-confirm";

    @Mock
    private DccControlledFileMetadataImportExportService metadataImportExportService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void metadataImportExportEndpoints_mapToDocControlRole() throws Exception {
        Method exportMethod = findMappedMethod(GetMapping.class, EXPORT_PATH);
        Method recognitionRecordExportMethod = findMappedMethod(GetMapping.class, RECOGNITION_RECORD_EXPORT_PATH);
        Method templateMethod = findMappedMethod(GetMapping.class, TEMPLATE_PATH);
        Method previewMethod = findMappedMethod(PostMapping.class, PREVIEW_PATH);
        Method confirmMethod = findMappedMethod(PostMapping.class, CONFIRM_PATH);

        assertCommonResultType(previewMethod, DccControlledFileMetadataImportPreviewRespVO.class);
        assertCommonResultType(confirmMethod, DccControlledFileMetadataImportPreviewRespVO.class);
        assertPreAuthorizeDocControlOnly(exportMethod);
        assertPreAuthorizeDocControlOnly(recognitionRecordExportMethod);
        assertPreAuthorizeDocControlOnly(templateMethod);
        assertPreAuthorizeDocControlOnly(previewMethod);
        assertPreAuthorizeDocControlOnly(confirmMethod);
    }

    @Test
    void previewAndConfirmDelegateToImportExportServiceWithLoginUser() throws Exception {
        Method previewMethod = findMappedMethod(PostMapping.class, PREVIEW_PATH);
        Method confirmMethod = findMappedMethod(PostMapping.class, CONFIRM_PATH);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "metadata.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});
        DccControlledFileMetadataImportPreviewRespVO respVO = DccControlledFileMetadataImportPreviewRespVO.builder()
                .totalCount(1)
                .updateCount(1)
                .unchangedCount(0)
                .failureCount(0)
                .build();

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            when(metadataImportExportService.previewImport(99L, file)).thenReturn(respVO);
            when(metadataImportExportService.confirmImport(99L, file)).thenReturn(respVO);

            CommonResult<?> previewResult = (CommonResult<?>) previewMethod.invoke(controller, file);
            CommonResult<?> confirmResult = (CommonResult<?>) confirmMethod.invoke(controller, file);

            assertTrue(Boolean.TRUE.equals(previewResult.isSuccess()));
            assertEquals(respVO, previewResult.getData());
            assertTrue(Boolean.TRUE.equals(confirmResult.isSuccess()));
            assertEquals(respVO, confirmResult.getData());
        }

        verify(metadataImportExportService).previewImport(99L, file);
        verify(metadataImportExportService).confirmImport(99L, file);
    }

    @Test
    void exportEndpointAcceptsBrowserFilters() throws Exception {
        Method exportMethod = findMappedMethod(GetMapping.class, EXPORT_PATH);
        assertEquals(1, exportMethod.getParameterCount());
        assertEquals(DccControlledFilePageReqVO.class, exportMethod.getParameterTypes()[0]);
    }

    @Test
    void recognitionRecordExportEndpointAcceptsBrowserFilters() throws Exception {
        Method exportMethod = findMappedMethod(GetMapping.class, RECOGNITION_RECORD_EXPORT_PATH);
        assertEquals(1, exportMethod.getParameterCount());
        assertEquals(DccControlledFilePageReqVO.class, exportMethod.getParameterTypes()[0]);
    }

    private void assertPreAuthorizeDocControlOnly(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on " + method.getName());
        assertTrue(preAuthorize.value().contains("@ss.hasRole('doc_control')"));
        assertFalse(preAuthorize.value().contains("super_admin"));
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccControlledFileController.class.getDeclaredMethods())
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
        RequestMapping requestMapping = DccControlledFileController.class.getAnnotation(RequestMapping.class);
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

    private String normalizePath(String path) {
        String normalized = path.replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
