package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccBrowserExtensionBlacklistSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePageReqVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBrowserSettingsService;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PERSONAL_PAGE_DISABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DccControlledFileBrowserPageControllerTest extends BaseMockitoUnitTest {

    private static final String BROWSER_PAGE_PATH = "/dcc/controlled-files/browser-page";

    @Mock
    private DccControlledFileQueryService queryService;
    @Mock
    private DccControlledFileBrowserSettingsService browserSettingsService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void browserPage_mapsStaticPathBeforeNumericDetailPath() throws Exception {
        Method method = findMappedMethod(GetMapping.class, BROWSER_PAGE_PATH);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on browser page endpoint");
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:query"));

        Method detailMethod = DccControlledFileController.class.getDeclaredMethod("getControlledFile", Long.class);
        String detailPath = detailMethod.getAnnotation(GetMapping.class).value()[0];
        assertEquals("/{id:\\d+}", detailPath, "detail route must not consume static browser-page path");

        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();
        PageResult<?> pageResult = new PageResult<>(List.of(), 0L);
        when(queryService.getControlledFileBrowserPage(99L, reqVO)).thenReturn((PageResult) pageResult);

        CommonResult<?> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            result = (CommonResult<?>) method.invoke(controller, reqVO);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertEquals(pageResult, result.getData());
        verify(queryService).getControlledFileBrowserPage(99L, reqVO);
    }

    @Test
    void legacyControlledFilePage_failsFastBecausePersonalFileEntryIsRetired() {
        DccControlledFilePageReqVO reqVO = new DccControlledFilePageReqVO();

        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> controller.getControlledFilePage(reqVO));

            assertEquals(CONTROLLED_FILE_PERSONAL_PAGE_DISABLED.getCode(), exception.getCode());
        }
        verifyNoInteractions(queryService);
    }

    @Test
    void browserExtensionBlacklistEndpoints_requireDocControlRole() throws Exception {
        Method getMethod = findMappedMethod(GetMapping.class, "/dcc/controlled-files/browser-extension-blacklist");
        PreAuthorize getPreAuthorize = getMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(getPreAuthorize, "Missing @PreAuthorize on browser extension blacklist get endpoint");
        assertTrue(getPreAuthorize.value().contains("@ss.hasRole('doc_control')"));
        DccBrowserExtensionBlacklistRespVO respVO = new DccBrowserExtensionBlacklistRespVO();
        respVO.setExtensionPatterns(List.of("*.db", "*.pyc"));
        when(browserSettingsService.getExtensionBlacklist()).thenReturn(respVO);

        CommonResult<?> getResult = (CommonResult<?>) getMethod.invoke(controller);

        assertTrue(Boolean.TRUE.equals(getResult.isSuccess()));
        assertEquals(respVO, getResult.getData());
        verify(browserSettingsService).getExtensionBlacklist();

        Method saveMethod = findMappedMethod(PutMapping.class, "/dcc/controlled-files/browser-extension-blacklist");
        PreAuthorize savePreAuthorize = saveMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(savePreAuthorize, "Missing @PreAuthorize on browser extension blacklist save endpoint");
        assertTrue(savePreAuthorize.value().contains("@ss.hasRole('doc_control')"));
        DccBrowserExtensionBlacklistSaveReqVO reqVO = new DccBrowserExtensionBlacklistSaveReqVO();
        reqVO.setExtensionPatterns(List.of("*.db", "*.pyc"));

        CommonResult<?> saveResult = (CommonResult<?>) saveMethod.invoke(controller, reqVO);

        assertTrue(Boolean.TRUE.equals(saveResult.isSuccess()));
        verify(browserSettingsService).saveExtensionBlacklist(reqVO);
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
