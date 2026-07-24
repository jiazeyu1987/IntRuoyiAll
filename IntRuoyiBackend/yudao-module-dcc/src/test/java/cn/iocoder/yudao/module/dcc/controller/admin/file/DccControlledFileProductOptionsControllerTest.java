package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileProductOptionRespVO;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileProductOptionsControllerTest extends BaseMockitoUnitTest {

    private static final String PRODUCT_OPTIONS_PATH = "/dcc/controlled-files/product-options";

    @Mock
    private MdmProductApi productApi;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void productOptions_mapsGetEndpointAndUsesDccPermissionsInsteadOfMdmQuery() throws Exception {
        Method method = findMappedMethod(GetMapping.class, PRODUCT_OPTIONS_PATH);
        assertCommonResultListType(method);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on DCC product options endpoint");
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:submit"),
                "DCC upload users must be able to read product options without mdm product page access");
        assertTrue(preAuthorize.value().contains("doc_control"),
                "DCC metadata editors must be able to read product options");
        assertFalse(preAuthorize.value().contains("mdm:product:query"),
                "DCC product options must not require broad MDM product management query permission");

        MdmProductRespDTO product = MdmProductRespDTO.builder()
                .id(5000L)
                .productCode("PM-001")
                .dccProductCode("ABC12345678901")
                .nameCn("测试产品")
                .modelSpecification("M1")
                .category("DCC")
                .status("ENABLE")
                .build();
        when(productApi.listSimpleProducts("ENABLE", true, "ABC12345678901")).thenReturn(List.of(product));

        CommonResult<?> result = (CommonResult<?>) method.invoke(controller, "ENABLE", true, "ABC12345678901");

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertInstanceOf(List.class, result.getData());
        List<?> data = (List<?>) result.getData();
        assertEquals(1, data.size());
        DccControlledFileProductOptionRespVO option = (DccControlledFileProductOptionRespVO) data.get(0);
        assertEquals(5000L, option.getId());
        assertEquals("PM-001", option.getProductCode());
        assertEquals("ABC12345678901", option.getDccProductCode());
        assertEquals("测试产品", option.getNameCn());
        verify(productApi).listSimpleProducts("ENABLE", true, "ABC12345678901");
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

    private void assertCommonResultListType(Method method) {
        assertEquals(CommonResult.class, method.getReturnType());
        Type genericReturnType = method.getGenericReturnType();
        assertTrue(genericReturnType instanceof ParameterizedType);
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        assertEquals(CommonResult.class, parameterizedType.getRawType());
        assertTrue(parameterizedType.getActualTypeArguments()[0] instanceof ParameterizedType);
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
