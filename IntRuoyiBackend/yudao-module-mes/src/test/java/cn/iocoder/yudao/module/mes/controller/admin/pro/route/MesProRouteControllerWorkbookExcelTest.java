package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.MesProRoutePageReqVO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookExportService;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookImportResult;
import cn.iocoder.yudao.module.mes.service.pro.route.importer.MesProRouteWorkbookImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProRouteControllerWorkbookExcelTest {

    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProRouteWorkbookExportService routeWorkbookExportService;
    @Mock
    private MesProRouteWorkbookImportService routeWorkbookImportService;
    @InjectMocks
    private MesProRouteController controller;

    @Test
    void exportRouteImportWorkbook_writesXlsxResponse() throws Exception {
        byte[] workbookBytes = new byte[] {1, 2, 3};
        when(routeWorkbookExportService.exportWorkbook(any(MesProRoutePageReqVO.class))).thenReturn(workbookBytes);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.exportRouteImportWorkbook(new MesProRoutePageReqVO(), response);

        assertArrayEquals(workbookBytes, response.getContentAsByteArray());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8",
                response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("filename="));
    }

    @Test
    void importRouteWorkbookXlsx_delegatesMultipartPayloadAndReturnsSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "route.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});
        MesProRouteWorkbookImportResult result = new MesProRouteWorkbookImportResult();
        result.setRouteCount(1);
        result.setRouteProcessCount(2);
        result.setRouteProductCount(3);
        result.setRouteProductBomCount(4);
        result.setRouteCodes(List.of("ROUTE-001"));
        when(routeWorkbookImportService.importWorkbook(eq(file))).thenReturn(result);

        CommonResult<MesProRouteWorkbookImportResult> response = controller.importRouteWorkbookXlsx(file);

        assertEquals(0, response.getCode());
        assertSame(result, response.getData());
        verify(routeWorkbookImportService).importWorkbook(eq(file));
    }

    @Test
    void workbookEndpoints_exposeExpectedMappingPermissionsAndRequestParams() throws Exception {
        Method exportMethod = MesProRouteController.class.getDeclaredMethod(
                "exportRouteImportWorkbook", MesProRoutePageReqVO.class, jakarta.servlet.http.HttpServletResponse.class);
        assertArrayEquals(new String[] {"/export-import-xlsx"}, exportMethod.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:export')",
                exportMethod.getAnnotation(PreAuthorize.class).value());

        Method importMethod = MesProRouteController.class.getDeclaredMethod(
                "importRouteWorkbookXlsx", org.springframework.web.multipart.MultipartFile.class);
        assertArrayEquals(new String[] {"/import-workbook-xlsx"}, importMethod.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-route:create')",
                importMethod.getAnnotation(PreAuthorize.class).value());
        RequestParam fileParam = importMethod.getParameters()[0].getAnnotation(RequestParam.class);
        assertEquals("file", fileParam.value());
    }
}
