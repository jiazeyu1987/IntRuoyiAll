package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMetadataUpdateReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileMetadataUpdateService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
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

class DccControlledFileMetadataUpdateControllerTest extends BaseMockitoUnitTest {

    private static final String METADATA_PATH = "/dcc/controlled-files/{id:\\d+}/metadata";

    @Mock
    private DccControlledFileMetadataUpdateService metadataUpdateService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void updateMetadata_mapsPutEndpointAndRequiresDocControlRoleOnly() throws Exception {
        Method method = findMappedMethod(PutMapping.class, METADATA_PATH);
        assertCommonResultType(method, Boolean.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on metadata update endpoint");
        assertTrue(preAuthorize.value().contains("@ss.hasRole('doc_control')"));
        assertFalse(preAuthorize.value().contains("super_admin"),
                "Metadata update must not allow super_admin unless the user also has doc_control");

        DccControlledFileMetadataUpdateReqVO reqVO = new DccControlledFileMetadataUpdateReqVO();
        reqVO.setProductName("离心泵");
        reqVO.setDccProjectCodeId(3000L);
        reqVO.setNeedTraining(Boolean.TRUE);
        reqVO.setFileTypeLevel1("体系文件");
        reqVO.setFileTypeLevel2("技术文件");
        reqVO.setFileTypeLevel3("设计开发");
        reqVO.setFileTypeLevel4("验证资料");
        reqVO.setFileTypeLevel5("归档件");
        reqVO.setFileName("SOP-001");
        reqVO.setProductCode("PRD20260604001");
        reqVO.setFileNumber("DOC-001");
        reqVO.setCategoryId(10L);
        reqVO.setDirectoryId(20L);

        CommonResult<?> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            result = (CommonResult<?>) method.invoke(controller, 900L, reqVO);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(Boolean.TRUE, result.getData());
        verify(metadataUpdateService).updateMetadata(99L, 900L, reqVO);
    }

    @Test
    void metadataUpdateReq_allowsBlankFileNumberAtBeanValidationBoundary() throws Exception {
        Field fileNameField = DccControlledFileMetadataUpdateReqVO.class.getDeclaredField("fileName");
        Field fileNumberField = DccControlledFileMetadataUpdateReqVO.class.getDeclaredField("fileNumber");
        Field directoryIdField = DccControlledFileMetadataUpdateReqVO.class.getDeclaredField("directoryId");

        assertTrue(hasAnnotation(fileNameField, "jakarta.validation.constraints.NotBlank"),
                "fileName must stay required");
        assertFalse(hasAnnotation(fileNumberField, "jakarta.validation.constraints.NotBlank"),
                "fileNumber is optional because not every controlled file has a number");
        assertFalse(hasAnnotation(directoryIdField, "jakarta.validation.constraints.NotNull"),
                "directoryId is optional when the target category has no configured directory binding");
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

    private boolean hasAnnotation(Field field, String annotationClassName) {
        return Arrays.stream(field.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().getName().equals(annotationClassName));
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
