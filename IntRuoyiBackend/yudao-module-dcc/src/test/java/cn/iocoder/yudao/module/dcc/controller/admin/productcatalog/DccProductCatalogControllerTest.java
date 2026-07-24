package cn.iocoder.yudao.module.dcc.controller.admin.productcatalog;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRegistrationExpiryCompareRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogUpdateReqVO;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccProductCatalogControllerTest extends BaseMockitoUnitTest {

    private static final String PAGE_PATH = "/dcc/product-catalog/page";
    private static final String CREATE_PATH = "/dcc/product-catalog/create";
    private static final String UPDATE_PATH = "/dcc/product-catalog/update";
    private static final String DELETE_PATH = "/dcc/product-catalog/delete";
    private static final String REGISTRATION_EXPIRY_COMPARE_PATH =
            "/dcc/product-catalog/registration-expiry/compare";

    @InjectMocks
    private DccProductCatalogController controller;

    @Mock
    private DccProductCatalogService productCatalogService;

    @Test
    void createProductCatalogShouldRequireCreatePermissionAndDelegate() throws Exception {
        Method method = findMappedMethod(PostMapping.class, CREATE_PATH);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on product catalog create endpoint");
        assertTrue(preAuthorize.value().contains("dcc:project-code:create"));

        DccProductCatalogSaveReqVO reqVO = new DccProductCatalogSaveReqVO();
        reqVO.setDataSource("子公司产品");
        reqVO.setProduct("测试产品");
        DccProductCatalogRespVO row = new DccProductCatalogRespVO();
        row.setDataSource("子公司产品");
        row.setProduct("测试产品");
        row.setOriginalRowNo(22);
        when(productCatalogService.createProductCatalog(reqVO)).thenReturn(row);

        CommonResult<DccProductCatalogRespVO> result = controller.createProductCatalog(reqVO);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals("测试产品", result.getData().getProduct());
        assertEquals(22, result.getData().getOriginalRowNo());
        verify(productCatalogService).createProductCatalog(reqVO);
    }

    @Test
    void updateProductCatalogShouldRequireUpdatePermissionAndDelegate() throws Exception {
        Method method = findMappedMethod(PutMapping.class, UPDATE_PATH);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on product catalog update endpoint");
        assertTrue(preAuthorize.value().contains("dcc:project-code:update"));

        DccProductCatalogUpdateReqVO reqVO = new DccProductCatalogUpdateReqVO();
        reqVO.setDataSource("瑛泰产品");
        reqVO.setOriginalRowNo(8);
        reqVO.setProduct("更新后的产品");

        CommonResult<Boolean> result = controller.updateProductCatalog(reqVO);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        verify(productCatalogService).updateProductCatalog(reqVO);
    }

    @Test
    void deleteProductCatalogShouldRequireDeletePermissionAndDelegate() throws Exception {
        Method method = findMappedMethod(DeleteMapping.class, DELETE_PATH);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on product catalog delete endpoint");
        assertTrue(preAuthorize.value().contains("dcc:project-code:delete"));

        CommonResult<Boolean> result = controller.deleteProductCatalog("子公司产品", 12);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        verify(productCatalogService).deleteProductCatalog("子公司产品", 12);
    }

    @Test
    void getProductCatalogPageShouldRequireProjectCodeQueryPermissionAndDelegate() throws Exception {
        Method method = findMappedMethod(GetMapping.class, PAGE_PATH);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on product catalog page endpoint");
        assertTrue(preAuthorize.value().contains("dcc:project-code:query"));
        assertCommonResultPageType(method, DccProductCatalogRespVO.class);

        DccProductCatalogPageReqVO reqVO = new DccProductCatalogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        DccProductCatalogRespVO row = new DccProductCatalogRespVO();
        row.setDataSource("子公司产品");
        row.setProduct("导管鞘组（大腔鞘）");
        when(productCatalogService.getProductCatalogPage(reqVO))
                .thenReturn(new PageResult<>(List.of(row), 1L));

        CommonResult<PageResult<DccProductCatalogRespVO>> result = controller.getProductCatalogPage(reqVO);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(1L, result.getData().getTotal());
        assertEquals("导管鞘组（大腔鞘）", result.getData().getList().get(0).getProduct());
        verify(productCatalogService).getProductCatalogPage(reqVO);
    }

    @Test
    void compareRegistrationExpiryShouldRequireProjectCodeQueryPermissionAndDelegate() throws Exception {
        Method method = findMappedMethod(PostMapping.class, REGISTRATION_EXPIRY_COMPARE_PATH);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on registration expiry compare endpoint");
        assertTrue(preAuthorize.value().contains("dcc:project-code:query"));
        assertCommonResultListType(method, DccProductCatalogRegistrationExpiryCompareRespVO.class);

        DccProductCatalogRegistrationExpiryCompareReqVO reqVO =
                new DccProductCatalogRegistrationExpiryCompareReqVO();
        reqVO.setRows(List.of(new DccProductCatalogRegistrationExpiryCompareReqVO.RowKey("子公司产品", 2)));

        DccProductCatalogRegistrationExpiryCompareRespVO row =
                new DccProductCatalogRegistrationExpiryCompareRespVO();
        row.setDataSource("子公司产品");
        row.setOriginalRowNo(2);
        row.setStatus("MATCH");
        row.setLocalExpiryDate("2029-08-19");
        row.setRemoteExpiryDate("2029-08-19");
        when(productCatalogService.compareRegistrationExpiry(reqVO)).thenReturn(List.of(row));

        CommonResult<List<DccProductCatalogRegistrationExpiryCompareRespVO>> result =
                controller.compareRegistrationExpiry(reqVO);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(1, result.getData().size());
        assertEquals("MATCH", result.getData().get(0).getStatus());
        verify(productCatalogService).compareRegistrationExpiry(reqVO);
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccProductCatalogController.class.getDeclaredMethods())
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
        RequestMapping requestMapping = DccProductCatalogController.class.getAnnotation(RequestMapping.class);
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

    private void assertCommonResultPageType(Method method, Class<?> expectedRowType) {
        assertEquals(CommonResult.class, method.getReturnType());
        Type commonResultType = method.getGenericReturnType();
        assertTrue(commonResultType instanceof ParameterizedType);
        Type pageResultType = ((ParameterizedType) commonResultType).getActualTypeArguments()[0];
        assertTrue(pageResultType instanceof ParameterizedType);
        ParameterizedType pageResultParameterizedType = (ParameterizedType) pageResultType;
        assertEquals(PageResult.class, pageResultParameterizedType.getRawType());
        assertEquals(expectedRowType, pageResultParameterizedType.getActualTypeArguments()[0]);
    }

    private void assertCommonResultListType(Method method, Class<?> expectedRowType) {
        assertEquals(CommonResult.class, method.getReturnType());
        Type commonResultType = method.getGenericReturnType();
        assertTrue(commonResultType instanceof ParameterizedType);
        Type listType = ((ParameterizedType) commonResultType).getActualTypeArguments()[0];
        assertTrue(listType instanceof ParameterizedType);
        ParameterizedType listParameterizedType = (ParameterizedType) listType;
        assertEquals(List.class, listParameterizedType.getRawType());
        assertEquals(expectedRowType, listParameterizedType.getActualTypeArguments()[0]);
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
