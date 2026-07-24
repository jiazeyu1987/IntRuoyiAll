package cn.iocoder.yudao.module.dcc.controller.admin.log;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.log.vo.DccControlledFileLogRespVO;
import cn.iocoder.yudao.module.dcc.service.log.DccControlledFileLogQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileLogControllerTest extends BaseMockitoUnitTest {

    private static final String PAGE_PATH = "/dcc/controlled-file-logs/page";

    @Mock
    private DccControlledFileLogQueryService logQueryService;

    @InjectMocks
    private DccControlledFileLogController controller;

    @Test
    void getLogPage_mapsUnifiedEndpointPermissionAndDelegates() throws Exception {
        Method method = findMappedMethod(GetMapping.class, PAGE_PATH);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on unified DCC log endpoint");
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:log:query"));

        DccControlledFileLogPageReqVO reqVO = new DccControlledFileLogPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setLogType("PROJECT_CODE_CHANGE");
        DccControlledFileLogRespVO row = new DccControlledFileLogRespVO();
        row.setId("PROJECT_CODE_CHANGE:7001");
        row.setLogType("PROJECT_CODE_CHANGE");
        PageResult<DccControlledFileLogRespVO> pageResult = new PageResult<>(List.of(row), 1L);
        when(logQueryService.getLogPage(reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<DccControlledFileLogRespVO>> result =
                controller.getLogPage(reqVO);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(pageResult, result.getData());
        verify(logQueryService).getLogPage(reqVO);
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccControlledFileLogController.class.getDeclaredMethods())
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
        RequestMapping requestMapping = DccControlledFileLogController.class.getAnnotation(RequestMapping.class);
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
