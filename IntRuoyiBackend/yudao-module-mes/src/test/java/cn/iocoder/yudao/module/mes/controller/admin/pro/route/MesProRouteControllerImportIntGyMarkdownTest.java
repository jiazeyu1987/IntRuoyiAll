package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.IntGyRouteMarkdownImportResult;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.IntGyRouteMarkdownImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteControllerImportIntGyMarkdownTest {

    @Mock
    private MesProRouteService routeService;
    @Mock
    private IntGyRouteMarkdownImportService routeMarkdownImportService;
    @InjectMocks
    private MesProRouteController controller;

    @Test
    void importIntGyMarkdown_delegatesMultipartPayloadAndReturnsSummary() throws Exception {
        String markdown = "# IntGY export";
        MockMultipartFile file = new MockMultipartFile("file", "route.md", "text/markdown",
                markdown.getBytes(StandardCharsets.UTF_8));
        IntGyRouteMarkdownImportResult result = new IntGyRouteMarkdownImportResult();
        result.setRouteCount(2);
        result.setProcessCreatedCount(51);
        result.setProcessReusedCount(0);
        result.setRouteProcessCount(51);
        when(routeMarkdownImportService.importMarkdown(eq(markdown), eq(1), eq("{\"A\":[\"P1\"]}")))
                .thenReturn(result);

        CommonResult<IntGyRouteMarkdownImportResult> response = controller.importIntGyMarkdown(
                file, 1, "{\"A\":[\"P1\"]}");

        assertEquals(0, response.getCode());
        assertTrue(response.isSuccess());
        assertSame(result, response.getData());
        verify(routeMarkdownImportService).importMarkdown(eq(markdown), eq(1), eq("{\"A\":[\"P1\"]}"));
    }

    @Test
    void importIntGyMarkdown_exposesExpectedMappingPermissionAndRequestParams() throws Exception {
        Method method = MesProRouteController.class.getDeclaredMethod("importIntGyMarkdown",
                org.springframework.web.multipart.MultipartFile.class, Integer.class, String.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[] { "/import-intgy-md" }, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('mes:pro-route:create')", preAuthorize.value());

        RequestParam fileParam = method.getParameters()[0].getAnnotation(RequestParam.class);
        assertEquals("file", fileParam.value());

        RequestParam statusParam = method.getParameters()[1].getAnnotation(RequestParam.class);
        assertEquals("processStatus", statusParam.value());
        assertTrue(statusParam.required());

        RequestParam jsonParam = method.getParameters()[2].getAnnotation(RequestParam.class);
        assertEquals("checkProcessCodesByRouteCodeJson", jsonParam.value());
        assertTrue(!jsonParam.required());
    }

}
