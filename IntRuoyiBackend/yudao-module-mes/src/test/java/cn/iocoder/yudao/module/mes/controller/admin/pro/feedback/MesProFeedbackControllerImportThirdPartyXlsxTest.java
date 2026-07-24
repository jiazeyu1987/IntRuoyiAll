package cn.iocoder.yudao.module.mes.controller.admin.pro.feedback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.service.pro.feedback.MesProFeedbackService;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportResult;
import cn.iocoder.yudao.module.mes.service.pro.feedback.importer.ThirdPartyFeedbackImportService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
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
class MesProFeedbackControllerImportThirdPartyXlsxTest {

    @Mock
    private MesProFeedbackService feedbackService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdUnitMeasureService unitMeasureService;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProTaskService taskService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ThirdPartyFeedbackImportService thirdPartyFeedbackImportService;

    @InjectMocks
    private MesProFeedbackController controller;

    @Test
    void importThirdPartyXlsx_delegatesMultipartPayloadAndReturnsSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "third-party-feedback.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});
        ThirdPartyFeedbackImportResult result = new ThirdPartyFeedbackImportResult();
        result.setSheetCount(2);
        result.setImportedCount(6);
        result.setSubmittedCount(6);
        when(thirdPartyFeedbackImportService.importWorkbook(eq(file))).thenReturn(result);

        CommonResult<ThirdPartyFeedbackImportResult> response = controller.importThirdPartyXlsx(file);

        assertEquals(0, response.getCode());
        assertTrue(response.isSuccess());
        assertSame(result, response.getData());
        verify(thirdPartyFeedbackImportService).importWorkbook(eq(file));
    }

    @Test
    void importDirectWorkReportXlsx_delegatesMultipartPayloadAndReturnsSummary() {
        MockMultipartFile file = new MockMultipartFile("file", "李萍.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});
        ThirdPartyFeedbackImportResult result = new ThirdPartyFeedbackImportResult();
        result.setSheetCount(1);
        result.setImportedCount(1);
        result.setSubmittedCount(1);
        result.setSkippedRows(2);
        when(thirdPartyFeedbackImportService.importDirectWorkReportWorkbook(eq(file))).thenReturn(result);

        CommonResult<ThirdPartyFeedbackImportResult> response = controller.importDirectWorkReportXlsx(file);

        assertEquals(0, response.getCode());
        assertTrue(response.isSuccess());
        assertSame(result, response.getData());
        verify(thirdPartyFeedbackImportService).importDirectWorkReportWorkbook(eq(file));
    }

    @Test
    void simulateThirdPartyXlsxImport_delegatesToServiceAndReturnsSummary() {
        ThirdPartyFeedbackImportResult result = new ThirdPartyFeedbackImportResult();
        result.setSheetCount(1);
        result.setImportedCount(1);
        result.setPendingCount(1);
        when(thirdPartyFeedbackImportService.simulateImportWorkbook(3)).thenReturn(result);

        CommonResult<ThirdPartyFeedbackImportResult> response = controller.simulateThirdPartyXlsxImport(3);

        assertEquals(0, response.getCode());
        assertTrue(response.isSuccess());
        assertSame(result, response.getData());
        verify(thirdPartyFeedbackImportService).simulateImportWorkbook(3);
    }

    @Test
    void importThirdPartyXlsx_exposesExpectedPermissionAndRequestParam() throws Exception {
        Method method = MesProFeedbackController.class.getDeclaredMethod("importThirdPartyXlsx",
                org.springframework.web.multipart.MultipartFile.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/import-third-party-xlsx"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('mes:pro-feedback:create')", preAuthorize.value());

        RequestParam fileParam = method.getParameters()[0].getAnnotation(RequestParam.class);
        assertEquals("file", fileParam.value());
    }

    @Test
    void importDirectWorkReportXlsx_exposesExpectedPermissionAndRequestParam() throws Exception {
        Method method = MesProFeedbackController.class.getDeclaredMethod("importDirectWorkReportXlsx",
                org.springframework.web.multipart.MultipartFile.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/import-direct-work-report-xlsx"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('mes:pro-feedback:create')", preAuthorize.value());

        RequestParam fileParam = method.getParameters()[0].getAnnotation(RequestParam.class);
        assertEquals("file", fileParam.value());
    }

    @Test
    void simulateThirdPartyXlsxImport_exposesExpectedPermissionAndPostMapping() throws Exception {
        Method method = MesProFeedbackController.class.getDeclaredMethod("simulateThirdPartyXlsxImport", Integer.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertArrayEquals(new String[]{"/simulate-import-third-party-xlsx"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermission('mes:pro-feedback:create')", preAuthorize.value());

        RequestParam processCountParam = method.getParameters()[0].getAnnotation(RequestParam.class);
        assertEquals("processCount", processCountParam.value());
    }
}
