package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileProjectCodeRecognitionRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileProjectCodeRecognitionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
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

class DccControlledFileProjectCodeRecognitionControllerTest extends BaseMockitoUnitTest {

    private static final String PROJECT_CODE_RECOGNITION_PATH =
            "/dcc/controlled-files/{id:\\d+}/recognize-project-code";
    private static final String OLD_PRODUCT_NAME_RECOGNITION_PATH =
            "/dcc/controlled-files/{id:\\d+}/recognize-product-name";

    @Mock
    private DccControlledFileProjectCodeRecognitionService projectCodeRecognitionService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void recognizeProjectCode_mapsPostEndpointRequiresDocControlAndRemovesOldEndpoint() throws Exception {
        Method method = findMappedMethod(PostMapping.class, PROJECT_CODE_RECOGNITION_PATH);
        assertCommonResultType(method, DccControlledFileProjectCodeRecognitionRespVO.class);
        assertNoMappedMethod(PostMapping.class, OLD_PRODUCT_NAME_RECOGNITION_PATH);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on project-code recognition endpoint");
        assertTrue(preAuthorize.value().contains("@ss.hasRole('doc_control')"));
        assertFalse(preAuthorize.value().contains("super_admin"),
                "Project-code recognition must not allow super_admin unless the user also has doc_control");

        DccControlledFileProjectCodeRecognitionRespVO respVO = new DccControlledFileProjectCodeRecognitionRespVO();
        respVO.setControlledFileId(900L);
        respVO.setDccProjectCodeId(700L);
        respVO.setProjectName("万级净化车间沉降菌测试报告");
        respVO.setProjectCode("CODE-A");
        respVO.setMatchType("PROJECT_NAME");
        respVO.setMatchText("万级净化车间沉降菌测试报告");
        when(projectCodeRecognitionService.recognizeProjectCode(99L, 900L)).thenReturn(respVO);

        CommonResult<?> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            result = (CommonResult<?>) method.invoke(controller, 900L);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(respVO, result.getData());
        verify(projectCodeRecognitionService).recognizeProjectCode(99L, 900L);
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return mappedMethods(mappingAnnotationType)
                .filter(method -> hasFullMappingPath(method.getAnnotation(mappingAnnotationType), expectedFullPath))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing endpoint mapping: " + expectedFullPath));
    }

    private void assertNoMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        assertTrue(mappedMethods(mappingAnnotationType)
                        .noneMatch(method -> hasFullMappingPath(method.getAnnotation(mappingAnnotationType), expectedFullPath)),
                "Old endpoint mapping must be removed: " + expectedFullPath);
    }

    private Stream<Method> mappedMethods(Class<? extends Annotation> mappingAnnotationType) {
        return Arrays.stream(DccControlledFileController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(mappingAnnotationType));
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
