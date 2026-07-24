package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.Sheet1RouteExcelImportResult;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.Sheet1RouteExcelImportService;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteControllerImportSheet1XlsxTest {

    @Mock
    private MesProRouteService routeService;
    @Mock
    private Sheet1RouteExcelImportService sheet1RouteExcelImportService;
    @InjectMocks
    private MesProRouteController controller;

    @Test
    void importSheet1Xlsx_delegatesMultipartPayloadAndReturnsSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "route.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});
        Sheet1RouteExcelImportResult result = new Sheet1RouteExcelImportResult();
        result.setRouteCount(2);
        result.setProcessCreatedCount(33);
        result.setProcessReusedCount(0);
        result.setRouteProcessCount(50);
        when(sheet1RouteExcelImportService.importExcel(eq(file), eq(0))).thenReturn(result);

        CommonResult<Sheet1RouteExcelImportResult> response = controller.importSheet1Xlsx(file, 0);

        assertEquals(0, response.getCode());
        assertTrue(response.isSuccess());
        assertSame(result, response.getData());
        verify(sheet1RouteExcelImportService).importExcel(eq(file), eq(0));
    }

    @Test
    void importSheet1Xlsx_exposesExpectedMappingPermissionAndRequestParams() throws Exception {
        Method method = MesProRouteController.class.getDeclaredMethod("importSheet1Xlsx",
                org.springframework.web.multipart.MultipartFile.class, Integer.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/import-sheet1-xlsx"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('mes:pro-route:create')", preAuthorize.value());

        RequestParam fileParam = method.getParameters()[0].getAnnotation(RequestParam.class);
        assertEquals("file", fileParam.value());

        RequestParam statusParam = method.getParameters()[1].getAnnotation(RequestParam.class);
        assertEquals("processStatus", statusParam.value());
        assertTrue(statusParam.required());
    }
}
