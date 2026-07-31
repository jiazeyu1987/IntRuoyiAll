package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportBatchDeleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportDeleteAllRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportCellRulesRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportImportPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportRenameReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportSignatureCellMarkersReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordImportResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordImportPreflightResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordImportRouteProductOption;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportView;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordVersionApprovalResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordReportControllerTest {

    @Mock
    private MesProBatchRecordReportService reportService;

    @InjectMocks
    private MesProBatchRecordReportController controller;

    @Test
    void importEndpointsAndPageEndpoints_delegateToService() {
        MockMultipartFile docFile = new MockMultipartFile("file", "pilot.doc", "application/msword",
                new byte[]{1, 2, 3});
        MesProBatchRecordImportResult importResult = MesProBatchRecordImportResult.builder()
                .importedCount(1)
                .createdCount(1)
                .updatedCount(0)
                .routeId(1001L)
                .routeCode("ROUTE-IMPORT-1")
                .routeName("测试批记录")
                .routeProcessCount(2)
                .batchRecordRouteBindingCount(2)
                .boundProductNameCount(1)
                .boundProductCodeCount(2)
                .skippedProductNames(List.of("无编码产品"))
                .reports(List.of(MesProBatchRecordReportView.builder()
                        .batchRecordName("测试批记录")
                        .batchRecordDefinitionId(10L)
                        .batchRecordVersionId(20L)
                        .productName("球囊扩张压力泵")
                        .versionNo("V2.0")
                        .versionStatus("APPROVED")
                        .formSlotType("MAIN")
                        .sourceTableIndex(1)
                        .reportId("report-1")
                        .reportCode("CODE-1")
                        .reportName("Report 1")
                        .sourceFileName("pilot.doc")
                        .build()))
                .build();
        when(reportService.importPilotDoc(any())).thenReturn(importResult);
        when(reportService.importImage(any())).thenReturn(importResult);
        when(reportService.recognizeUploadedRoute(any(), eq("B"), eq("测试批记录"), eq("UPGRADE"), isNull(Long.class),
                isNull(String.class),
                eq(List.of("球囊扩张压力泵")), eq(true), eq(List.of(101L)), eq(List.of("球囊扩张压力泵")),
                eq(false), isNull(Long.class), isNull(Long.class), isNull(Long.class)))
                .thenReturn(importResult);
        when(reportService.existsBatchRecordName("B", "测试批记录")).thenReturn(true);
        when(reportService.getBatchRecordNameOptions()).thenReturn(List.of("测试批记录", "棘突球囊"));
        when(reportService.getProductNameOptions("压力", true)).thenReturn(List.of("球囊扩张压力泵"));
        when(reportService.getGeneratedReportPage(any()))
                .thenReturn(new PageResult<>(List.of(MesProBatchRecordReportView.builder()
                        .batchRecordName("测试批记录")
                        .batchRecordDefinitionId(10L)
                        .batchRecordVersionId(20L)
                        .productName("球囊扩张压力泵")
                        .versionNo("V2.0")
                        .versionStatus("APPROVED")
                        .formSlotType("MAIN")
                        .sourceTableIndex(1)
                        .reportId("report-1")
                        .reportCode("CODE-1")
                        .reportName("Report 1")
                        .sourceFileName("pilot.doc")
                        .build()), 1L));
        when(reportService.getDesignerPath("report-1")).thenReturn("/jmreport/view/report-1?tenantId=1");
        when(reportService.getEditPath("report-1")).thenReturn("/jmreport/index/report-1?tenantId=1");
        when(reportService.deleteAllGeneratedReports("PROD"))
                .thenReturn(new BatchRecordReportDeleteAllRespVO()
                        .setDeletedReportCount(3)
                        .setDeletedMetadataCount(2)
                        .setSkippedBoundReportCount(1));
        when(reportService.deleteGeneratedReportsByBatchRecordName("测试批记录", false))
                .thenReturn(new BatchRecordReportDeleteAllRespVO()
                        .setDeletedReportCount(2)
                        .setDeletedMetadataCount(2)
                        .setSkippedBoundReportCount(0));
        when(reportService.deleteGeneratedReports(List.of("report-1", "report-2"), true))
                .thenReturn(new BatchRecordReportDeleteAllRespVO()
                        .setDeletedReportCount(2)
                        .setDeletedMetadataCount(2)
                        .setSkippedBoundReportCount(0)
                        .setUnboundRouteProcessCount(1));

        CommonResult<BatchRecordReportImportRespVO> docResult = controller.importPilotDoc(docFile);
        CommonResult<BatchRecordReportImportRespVO> imageResult = controller.importImage(docFile);
        CommonResult<BatchRecordReportImportRespVO> uploadedRouteResult =
                controller.recognizeUploadedRoute(docFile, "B", "测试批记录", true,
                        null, null, null,
                        List.of("球囊扩张压力泵"), true, List.of(101L), List.of("球囊扩张压力泵"),
                        false, null, null);
        CommonResult<Boolean> existsResult = controller.existsBatchRecordName("B", "测试批记录");
        CommonResult<List<String>> namesResult = controller.getBatchRecordNameOptions();
        CommonResult<List<String>> productNamesResult = controller.getProductNameOptions("压力", true);
        CommonResult<PageResult<BatchRecordReportRespVO>> pageResult =
                controller.getGeneratedReportPage(new BatchRecordReportPageReqVO());
        CommonResult<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportDesignerPathRespVO>
                designerPathResult = controller.getDesignerPath("report-1");
        CommonResult<cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordreport.vo.BatchRecordReportDesignerPathRespVO>
                editPathResult = controller.getEditPath("report-1");
        CommonResult<Boolean> renameResult = controller.renameGeneratedReport(new BatchRecordReportRenameReqVO() {{
            setReportId("report-1");
            setReportName("新名称");
        }});
        CommonResult<Boolean> deleteResult = controller.deleteGeneratedReport("report-1");
        CommonResult<BatchRecordReportDeleteAllRespVO> batchDeleteResult =
                controller.deleteGeneratedReports(new BatchRecordReportBatchDeleteReqVO() {{
                    setReportIds(List.of("report-1", "report-2"));
                    setForceUnbind(true);
                }});
        CommonResult<BatchRecordReportDeleteAllRespVO> deleteByNameResult =
                controller.deleteGeneratedReportsByBatchRecordName("测试批记录", false);
        CommonResult<BatchRecordReportDeleteAllRespVO> deleteAllResult = controller.deleteAllGeneratedReports("PROD");

        assertTrue(docResult.isSuccess());
        assertTrue(imageResult.isSuccess());
        assertTrue(uploadedRouteResult.isSuccess());
        assertEquals(1, docResult.getData().getCreatedCount());
        assertEquals(1, imageResult.getData().getCreatedCount());
        assertEquals(1, uploadedRouteResult.getData().getCreatedCount());
        assertEquals(1001L, uploadedRouteResult.getData().getRouteId());
        assertEquals("ROUTE-IMPORT-1", uploadedRouteResult.getData().getRouteCode());
        assertEquals("测试批记录", uploadedRouteResult.getData().getRouteName());
        assertEquals(2, uploadedRouteResult.getData().getRouteProcessCount());
        assertEquals(2, uploadedRouteResult.getData().getBatchRecordRouteBindingCount());
        assertEquals(1, uploadedRouteResult.getData().getBoundProductNameCount());
        assertEquals(2, uploadedRouteResult.getData().getBoundProductCodeCount());
        assertEquals(List.of("无编码产品"), uploadedRouteResult.getData().getSkippedProductNames());
        assertTrue(existsResult.getData());
        assertEquals(List.of("测试批记录", "棘突球囊"), namesResult.getData());
        assertEquals(List.of("球囊扩张压力泵"), productNamesResult.getData());
        assertEquals(1L, pageResult.getData().getTotal());
        assertEquals("测试批记录", pageResult.getData().getList().get(0).getBatchRecordName());
        assertEquals(10L, pageResult.getData().getList().get(0).getBatchRecordDefinitionId());
        assertEquals(20L, pageResult.getData().getList().get(0).getBatchRecordVersionId());
        assertEquals("球囊扩张压力泵", pageResult.getData().getList().get(0).getProductName());
        assertEquals("V2.0", pageResult.getData().getList().get(0).getVersionNo());
        assertEquals("APPROVED", pageResult.getData().getList().get(0).getVersionStatus());
        assertEquals("MAIN", pageResult.getData().getList().get(0).getFormSlotType());
        assertEquals("/jmreport/view/report-1?tenantId=1", designerPathResult.getData().getPath());
        assertEquals("/jmreport/index/report-1?tenantId=1", editPathResult.getData().getPath());
        assertTrue(renameResult.getData());
        assertTrue(deleteResult.getData());
        assertEquals(2, batchDeleteResult.getData().getDeletedReportCount());
        assertEquals(1, batchDeleteResult.getData().getUnboundRouteProcessCount());
        assertEquals(2, deleteByNameResult.getData().getDeletedReportCount());
        assertEquals(2, deleteByNameResult.getData().getDeletedMetadataCount());
        assertEquals(3, deleteAllResult.getData().getDeletedReportCount());
        assertEquals(2, deleteAllResult.getData().getDeletedMetadataCount());
        assertEquals(1, deleteAllResult.getData().getSkippedBoundReportCount());
        verify(reportService).importPilotDoc(docFile);
        verify(reportService).importImage(docFile);
        verify(reportService).recognizeUploadedRoute(docFile, "B", "测试批记录", "UPGRADE", null, null,
                List.of("球囊扩张压力泵"), true, List.of(101L), List.of("球囊扩张压力泵"),
                false, null, null, null);
        verify(reportService).existsBatchRecordName("B", "测试批记录");
        verify(reportService).getBatchRecordNameOptions();
        verify(reportService).getProductNameOptions("压力", true);
        verify(reportService).getGeneratedReportPage(any());
        verify(reportService).getDesignerPath("report-1");
        verify(reportService).getEditPath("report-1");
        verify(reportService).renameGeneratedReport("report-1", "新名称");
        verify(reportService).deleteGeneratedReport("report-1");
        verify(reportService).deleteGeneratedReports(List.of("report-1", "report-2"), true);
        verify(reportService).deleteGeneratedReportsByBatchRecordName("测试批记录", false);
        verify(reportService).deleteAllGeneratedReports("PROD");
    }

    @Test
    void contractMappings_matchImageImportAndExistingReportEndpoints() throws Exception {
        Method importDocMethod = MesProBatchRecordReportController.class.getDeclaredMethod("importPilotDoc",
                org.springframework.web.multipart.MultipartFile.class);
        assertArrayEquals(new String[]{"/import"}, importDocMethod.getAnnotation(PostMapping.class).value());
        assertEquals("file", importDocMethod.getParameters()[0].getAnnotation(RequestParam.class).value());

        Method importImageMethod = MesProBatchRecordReportController.class.getDeclaredMethod("importImage",
                org.springframework.web.multipart.MultipartFile.class);
        assertArrayEquals(new String[]{"/import-image"}, importImageMethod.getAnnotation(PostMapping.class).value());
        assertEquals("file", importImageMethod.getParameters()[0].getAnnotation(RequestParam.class).value());

        Method pageMethod = MesProBatchRecordReportController.class.getDeclaredMethod("getGeneratedReportPage",
                BatchRecordReportPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, pageMethod.getAnnotation(GetMapping.class).value());

        Method deleteMethod = MesProBatchRecordReportController.class.getDeclaredMethod("deleteGeneratedReport",
                String.class);
        assertArrayEquals(new String[]{"/delete"}, deleteMethod.getAnnotation(DeleteMapping.class).value());

        Method batchDeleteMethod = MesProBatchRecordReportController.class.getDeclaredMethod("deleteGeneratedReports",
                BatchRecordReportBatchDeleteReqVO.class);
        assertArrayEquals(new String[]{"/delete-batch"}, batchDeleteMethod.getAnnotation(DeleteMapping.class).value());

        Method editPathMethod = MesProBatchRecordReportController.class.getDeclaredMethod("getEditPath",
                String.class);
        assertArrayEquals(new String[]{"/edit-path"}, editPathMethod.getAnnotation(GetMapping.class).value());

        Method renameMethod = MesProBatchRecordReportController.class.getDeclaredMethod("renameGeneratedReport",
                BatchRecordReportRenameReqVO.class);
        assertArrayEquals(new String[]{"/rename"}, renameMethod.getAnnotation(PutMapping.class).value());

        Method deleteAllMethod = MesProBatchRecordReportController.class.getDeclaredMethod("deleteAllGeneratedReports",
                String.class);
        assertArrayEquals(new String[]{"/delete-all"}, deleteAllMethod.getAnnotation(DeleteMapping.class).value());
        assertEquals("confirm", deleteAllMethod.getParameters()[0].getAnnotation(RequestParam.class).value());

        Method deleteByNameMethod = MesProBatchRecordReportController.class.getDeclaredMethod(
                "deleteGeneratedReportsByBatchRecordName", String.class, Boolean.class);
        assertArrayEquals(new String[]{"/delete-by-batch-record-name"},
                deleteByNameMethod.getAnnotation(DeleteMapping.class).value());
        assertEquals("batchRecordName",
                deleteByNameMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("forceUnbind",
                deleteByNameMethod.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("false",
                deleteByNameMethod.getParameters()[1].getAnnotation(RequestParam.class).defaultValue());
    }

    @Test
    void contractMappings_exposeFixedRouteRecognitionEndpoint() throws Exception {
        Method recognizeMethod = MesProBatchRecordReportController.class.getDeclaredMethod(
                "recognizeFixedRoute", String.class);
        assertArrayEquals(new String[]{"/recognize-fixed"}, recognizeMethod.getAnnotation(PostMapping.class).value());
        assertEquals("routeKey", recognizeMethod.getParameters()[0].getAnnotation(RequestParam.class).value());

        Method pageMethod = MesProBatchRecordReportController.class.getDeclaredMethod("getGeneratedReportPage",
                BatchRecordReportPageReqVO.class);
        assertSame(BatchRecordReportPageReqVO.class, pageMethod.getParameters()[0].getType());
        assertEquals(String.class, BatchRecordReportPageReqVO.class.getDeclaredField("batchRecordName").getType());
    }

    @Test
    void contractMappings_exposeUploadedRouteRecognitionEndpoint() throws Exception {
        Method recognizeMethod = MesProBatchRecordReportController.class.getDeclaredMethod(
                "recognizeUploadedRoute", MultipartFile.class, String.class, String.class, Boolean.class,
                String.class, Long.class, String.class, List.class, Boolean.class, List.class, List.class,
                Boolean.class, Long.class, Long.class);
        assertArrayEquals(new String[]{"/recognize-uploaded"}, recognizeMethod.getAnnotation(PostMapping.class).value());
        assertEquals("file", recognizeMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("routeKey", recognizeMethod.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("batchRecordName", recognizeMethod.getParameters()[2].getAnnotation(RequestParam.class).value());
        assertEquals("upgrade", recognizeMethod.getParameters()[3].getAnnotation(RequestParam.class).value());
        assertEquals("importAction", recognizeMethod.getParameters()[4].getAnnotation(RequestParam.class).value());
        assertEquals("expectedSourceVersionId", recognizeMethod.getParameters()[5].getAnnotation(RequestParam.class).value());
        assertEquals("expectedTargetVersionNo", recognizeMethod.getParameters()[6].getAnnotation(RequestParam.class).value());
        assertEquals("productNames", recognizeMethod.getParameters()[7].getAnnotation(RequestParam.class).value());
        assertEquals("rebuildBatchRecord", recognizeMethod.getParameters()[8].getAnnotation(RequestParam.class).value());
        assertEquals("true", recognizeMethod.getParameters()[8].getAnnotation(RequestParam.class).defaultValue());
        assertEquals("selectedRouteProductIds", recognizeMethod.getParameters()[9].getAnnotation(RequestParam.class).value());
        assertEquals("selectedProductNames", recognizeMethod.getParameters()[10].getAnnotation(RequestParam.class).value());
        assertEquals("routeUpgradeConfirmed", recognizeMethod.getParameters()[11].getAnnotation(RequestParam.class).value());
        assertEquals("false", recognizeMethod.getParameters()[11].getAnnotation(RequestParam.class).defaultValue());
        assertEquals("expectedRouteId", recognizeMethod.getParameters()[12].getAnnotation(RequestParam.class).value());
        assertEquals("expectedRouteVersionId", recognizeMethod.getParameters()[13].getAnnotation(RequestParam.class).value());

        Method preflightMethod = MesProBatchRecordReportController.class.getDeclaredMethod(
                "preflightUploadedRoute", String.class, String.class, List.class);
        assertArrayEquals(new String[]{"/recognize-uploaded/preflight"},
                preflightMethod.getAnnotation(GetMapping.class).value());
        assertEquals("routeKey", preflightMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("batchRecordName", preflightMethod.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("productNames", preflightMethod.getParameters()[2].getAnnotation(RequestParam.class).value());

        Method existsMethod = MesProBatchRecordReportController.class.getDeclaredMethod(
                "existsBatchRecordName", String.class, String.class);
        assertArrayEquals(new String[]{"/exists"}, existsMethod.getAnnotation(GetMapping.class).value());
        assertEquals("routeKey", existsMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("batchRecordName", existsMethod.getParameters()[1].getAnnotation(RequestParam.class).value());

        Method namesMethod = MesProBatchRecordReportController.class.getDeclaredMethod("getBatchRecordNameOptions");
        assertArrayEquals(new String[]{"/batch-record-names"}, namesMethod.getAnnotation(GetMapping.class).value());

        Method productNamesMethod = MesProBatchRecordReportController.class.getDeclaredMethod(
                "getProductNameOptions", String.class, Boolean.class);
        assertArrayEquals(new String[]{"/product-name-options"},
                productNamesMethod.getAnnotation(GetMapping.class).value());
        assertEquals("keyword",
                productNamesMethod.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("latestVersionOnly",
                productNamesMethod.getParameters()[1].getAnnotation(RequestParam.class).value());
        assertEquals("false",
                productNamesMethod.getParameters()[1].getAnnotation(RequestParam.class).defaultValue());
    }

    @Test
    void contractMappings_exposeSignatureCellMarkerEndpoints() throws Exception {
        Method getMarkers = MesProBatchRecordReportController.class.getDeclaredMethod(
                "getSignatureCellMarkers", String.class);
        assertArrayEquals(new String[]{"/signature-cell-markers"}, getMarkers.getAnnotation(GetMapping.class).value());
        assertEquals("reportId", getMarkers.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-template:query')",
                getMarkers.getAnnotation(PreAuthorize.class).value());

        Method saveMarkers = MesProBatchRecordReportController.class.getDeclaredMethod(
                "saveSignatureCellMarkers", BatchRecordReportSignatureCellMarkersReqVO.class);
        assertArrayEquals(new String[]{"/signature-cell-markers"}, saveMarkers.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-template:update')",
                saveMarkers.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void contractMappings_exposeCellRuleEndpointsWithTemplatePermissions() throws Exception {
        when(reportService.getCellRules("report-typed"))
                .thenReturn(new BatchRecordReportCellRulesRespVO().setReportId("report-typed"));

        CommonResult<BatchRecordReportCellRulesRespVO> result = controller.getCellRules("report-typed");

        assertTrue(result.isSuccess());
        assertEquals("report-typed", result.getData().getReportId());
        verify(reportService).getCellRules("report-typed");

        Method getRules = MesProBatchRecordReportController.class.getDeclaredMethod("getCellRules", String.class);
        assertArrayEquals(new String[]{"/cell-rules"}, getRules.getAnnotation(GetMapping.class).value());
        assertEquals("reportId", getRules.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-template:query')",
                getRules.getAnnotation(PreAuthorize.class).value());

        Method saveRules = MesProBatchRecordReportController.class.getDeclaredMethod(
                "saveCellRules", BatchRecordReportCellRulesReqVO.class);
        assertArrayEquals(new String[]{"/cell-rules"}, saveRules.getAnnotation(PutMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-template:update')",
                saveRules.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void contractMappings_exposesVersionApprovalSubmitEndpointWithApprovalPermission() throws Exception {
        Method submitApproval = MesProBatchRecordReportController.class.getDeclaredMethod(
                "submitBatchRecordVersionApproval", Long.class);
        assertArrayEquals(new String[]{"/version-approval/submit"},
                submitApproval.getAnnotation(PostMapping.class).value());
        assertEquals("versionId", submitApproval.getParameters()[0].getAnnotation(RequestParam.class).value());
        assertEquals("@ss.hasPermission('mes:pro-batch-record-template:version-approve')",
                submitApproval.getAnnotation(PreAuthorize.class).value());
    }

    @Test
    void importResponseAndApprovalResponse_exposePhaseOneVersionContract() {
        MesProBatchRecordImportResult importResult = MesProBatchRecordImportResult.builder()
                .importedCount(1)
                .createdCount(1)
                .updatedCount(0)
                .batchRecordDefinitionId(10L)
                .batchRecordVersionId(20L)
                .sourceBatchRecordVersionId(19L)
                .versionNo("V2.0")
                .versionStatus("PENDING_APPROVAL")
                .approvalInstanceId("BRV-20-1")
                .reports(List.of())
                .build();
        when(reportService.recognizeUploadedRoute(any(), eq("B"), eq("测试批记录"), eq("UPGRADE"), isNull(Long.class),
                isNull(String.class),
                eq(List.of("球囊扩张压力泵")), eq(true), eq(List.<Long>of()), eq(List.of("球囊扩张压力泵")),
                eq(false), isNull(Long.class), isNull(Long.class), isNull(Long.class)))
                .thenReturn(importResult);

        CommonResult<BatchRecordReportImportRespVO> importResponse = controller.recognizeUploadedRoute(
                new MockMultipartFile("file", "phase-one.doc", "application/msword", new byte[]{1}),
                "B", "测试批记录", true, null, null, null,
                List.of("球囊扩张压力泵"), true, List.of(), List.of("球囊扩张压力泵"),
                false, null, null);

        assertTrue(importResponse.isSuccess());
        assertEquals(10L, importResponse.getData().getBatchRecordDefinitionId());
        assertEquals(20L, importResponse.getData().getBatchRecordVersionId());
        assertEquals(19L, importResponse.getData().getSourceBatchRecordVersionId());
        assertEquals("V2.0", importResponse.getData().getVersionNo());
        assertEquals("PENDING_APPROVAL", importResponse.getData().getVersionStatus());
        assertEquals("BRV-20-1", importResponse.getData().getApprovalInstanceId());

        MesProBatchRecordVersionApprovalResult approvalResult = MesProBatchRecordVersionApprovalResult.builder()
                .definitionId(10L)
                .versionId(20L)
                .versionStatus("PENDING_APPROVAL")
                .approvalInstanceId("BRV-20-1")
                .processedResult("SUBMITTED")
                .build();
        assertEquals(10L, approvalResult.definitionId());
        assertEquals(20L, approvalResult.versionId());
        assertEquals("PENDING_APPROVAL", approvalResult.versionStatus());
        assertEquals("SUBMITTED", approvalResult.processedResult());
    }

    @Test
    void preflightUploadedRoute_returnsBatchAndRouteVersionContract() {
        MesProBatchRecordImportPreflightResult preflight = MesProBatchRecordImportPreflightResult.builder()
                .batchRecordName("测试批记录")
                .routeKey("B")
                .batchRecordDefinitionId(10L)
                .currentBatchRecordVersionId(20L)
                .currentBatchRecordVersionNo("V1.0")
                .currentBatchRecordVersionStatus("APPROVED")
                .latestBatchRecordVersionId(21L)
                .latestBatchRecordVersionNo("V2.0")
                .latestBatchRecordVersionStatus("PENDING_APPROVAL")
                .currentBatchRecordHasMainReports(true)
                .currentRouteId(1001L)
                .currentRouteCode("ROUTE-IMPORT-1")
                .currentRouteName("测试批记录")
                .currentRouteVersionId(2001L)
                .currentRouteVersionNo("V3")
                .hasHistoricalReferences(true)
                .allowedActions(List.of("UPGRADE"))
                .recommendedAction("UPGRADE")
                .nextVersionNo("V3.0")
                .referenceBlockers(List.of(MesProBatchRecordImportPreflightResult.ReferenceBlocker.builder()
                        .versionNo("V2.0")
                        .referenceName("存在批记录执行")
                        .count(1L)
                        .cleanupEntrance("eDHR 批记录 > 批次执行")
                        .cleanupAction("删除或作废执行记录")
                        .build()))
                .routeProductOptions(List.of(MesProBatchRecordImportRouteProductOption.builder()
                        .optionKey("ROUTE_PRODUCT:101")
                        .routeProductId(101L)
                        .routeId(1001L)
                        .routeCode("ROUTE-IMPORT-1")
                        .routeName("测试批记录")
                        .routeVersionId(2001L)
                        .routeVersionNo("V3")
                        .productId(301L)
                        .productCode("BRP-001")
                        .productName("球囊扩张压力泵")
                        .existing(true)
                        .build()))
                .build();
        when(reportService.preflightUploadedRoute("B", "测试批记录", List.of("球囊扩张压力泵")))
                .thenReturn(preflight);

        CommonResult<BatchRecordReportImportPreflightRespVO> response =
                controller.preflightUploadedRoute("B", "测试批记录", List.of("球囊扩张压力泵"));

        assertTrue(response.isSuccess());
        assertEquals("V1.0", response.getData().getCurrentBatchRecordVersionNo());
        assertEquals(21L, response.getData().getLatestBatchRecordVersionId());
        assertEquals("V2.0", response.getData().getLatestBatchRecordVersionNo());
        assertEquals("PENDING_APPROVAL", response.getData().getLatestBatchRecordVersionStatus());
        assertEquals(true, response.getData().getCurrentBatchRecordHasMainReports());
        assertEquals("V3", response.getData().getCurrentRouteVersionNo());
        assertEquals(true, response.getData().getHasHistoricalReferences());
        assertEquals(List.of("UPGRADE"), response.getData().getAllowedActions());
        assertEquals("UPGRADE", response.getData().getRecommendedAction());
        assertEquals("V3.0", response.getData().getNextVersionNo());
        assertEquals("存在批记录执行", response.getData().getReferenceBlockers().get(0).getReferenceName());
        assertEquals(1, response.getData().getRouteProductOptions().size());
        assertEquals("ROUTE_PRODUCT:101", response.getData().getRouteProductOptions().get(0).getOptionKey());
        assertEquals("球囊扩张压力泵", response.getData().getRouteProductOptions().get(0).getProductName());
        verify(reportService).preflightUploadedRoute("B", "测试批记录", List.of("球囊扩张压力泵"));
    }
}
