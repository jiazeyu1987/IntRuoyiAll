package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDefectReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceParameterRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeDisplayNameUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamFormalEmployeeLinkReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamFormalUserCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesPqcLeaderPersonnelLinkReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesPqcLeaderPersonnelStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderAddReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRemoveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderTransferTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderAllocationTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderBatchRecordTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderOrderProcessTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderProcessConfigListReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationLineReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesProductionExecutionTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeProfileSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamMaintenanceAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProcessDefectReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProcessDeviceBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProductionEmployeeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamTemporaryEmployeeCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamTemporarySignaturePasswordResetReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesWorkOrderAbnormalReportReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonCatalogService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamFormalUserCandidateBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcLeaderPersonnelService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderTransferTraceService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceParameterRuleSaveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigDevice;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigParameter;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderProcessConfigService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderAddReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCandidateBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRemoveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRow;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportAllocationLineReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportAllocationPreview;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportAllocationPreviewReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportConfirmationReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderReportConfirmationService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderRuntimeConfigService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderTraceService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderWorkbenchService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamProcessDeviceBindingSaveReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesWorkOrderAbnormalReportReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesWorkOrderAbnormalReportService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessPoolTeamLeaderControllerTest {

    @Mock
    private MesTeamLeaderWorkbenchService workbenchService;
    @Mock
    private MesTeamLeaderSubmissionReviewService submissionReviewService;
    @Mock
    private MesWorkOrderAbnormalReportService abnormalReportService;
    @Mock
    private MesDefectReasonCatalogService defectReasonCatalogService;
    @Mock
    private MesTeamLeaderProcessConfigService processConfigService;
    @Mock
    private MesTeamLeaderActiveOrderService activeOrderService;
    @Mock
    private MesTeamLeaderReportConfirmationService reportConfirmationService;
    @Mock
    private MesTeamLeaderRuntimeConfigService runtimeConfigService;
    @Mock
    private MesPqcLeaderPersonnelService pqcPersonnelService;
    @Mock
    private MesTeamLeaderTraceService traceService;
    @Mock
    private MesActiveOrderTransferTraceService activeOrderTransferTraceService;

    @InjectMocks
    private MesProcessPoolTeamLeaderController controller;

    @Test
    void getSubmissionPage_delegatesCurrentLoginLeaderAndScopedFilters() {
        MesTeamLeaderSubmissionPageReqVO reqVO = new MesTeamLeaderSubmissionPageReqVO();
        reqVO.setLeaderType("PRODUCTION");
        reqVO.setSubmitDate(LocalDate.of(2026, 7, 30));
        reqVO.setProcessId(6001L);
        PageResult<ProcessPoolTimelineEventRespVO> pageResult =
                new PageResult<>(Collections.emptyList(), 0L);
        when(workbenchService.getSubmissionPage(3001L, "PRODUCTION", reqVO)).thenReturn(pageResult);

        CommonResult<PageResult<ProcessPoolTimelineEventRespVO>> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            response = controller.getSubmissionPage(reqVO);
        }

        assertEquals(pageResult, response.getData());
        verify(workbenchService).getSubmissionPage(3001L, "PRODUCTION", reqVO);
    }

    @Test
    void reviewSubmission_usesLoginUserAsLeaderNotClientProvidedUser() {
        when(submissionReviewService.reviewSubmission(org.mockito.ArgumentMatchers.any())).thenReturn(9101L);

        MesTeamLeaderSubmissionReviewReqVO reqVO = new MesTeamLeaderSubmissionReviewReqVO()
                .setEventId(1001L)
                .setLeaderType("PQC")
                .setReviewStatus("APPROVED")
                .setReviewRemark("已复核")
                .setReviewSignatureId(9101L)
                .setReviewSignatureEmployeeUserId(3002L)
                .setReviewSignatureSnapshotJson("{\"signature\":\"review\"}");

        CommonResult<Long> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3002L);
            response = controller.reviewSubmission(reqVO);
        }

        assertEquals(9101L, response.getData());
        ArgumentCaptor<MesTeamLeaderSubmissionReviewReqBO> captor =
                ArgumentCaptor.forClass(MesTeamLeaderSubmissionReviewReqBO.class);
        verify(submissionReviewService).reviewSubmission(captor.capture());
        assertEquals(3002L, captor.getValue().getLeaderUserId());
        assertEquals("PQC", captor.getValue().getLeaderType());
        assertEquals(1001L, captor.getValue().getEventId());
        assertEquals("APPROVED", captor.getValue().getReviewStatus());
        assertEquals(9101L, captor.getValue().getReviewSignatureId());
        assertEquals(3002L, captor.getValue().getReviewSignatureUserId());
        assertEquals("{\"signature\":\"review\"}", captor.getValue().getReviewSignatureSnapshotJson());
    }

    @Test
    void markAndReportWorkOrderAbnormal_acceptsDescriptionOnly() {
        when(abnormalReportService.markAndReport(org.mockito.ArgumentMatchers.any())).thenReturn(8101L);

        MesWorkOrderAbnormalReportReqVO reqVO = new MesWorkOrderAbnormalReportReqVO()
                .setWorkOrderId(5001L)
                .setAbnormalDescription("设备停机，影响工单交付");

        CommonResult<Long> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            response = controller.markAndReportWorkOrderAbnormal(reqVO);
        }

        assertEquals(8101L, response.getData());
        ArgumentCaptor<MesWorkOrderAbnormalReportReqBO> captor =
                ArgumentCaptor.forClass(MesWorkOrderAbnormalReportReqBO.class);
        verify(abnormalReportService).markAndReport(captor.capture());
        assertEquals(5001L, captor.getValue().getWorkOrderId());
        assertEquals(3001L, captor.getValue().getMarkerUserId());
        assertEquals("设备停机，影响工单交付", captor.getValue().getAbnormalDescription());
    }

    @Test
    void maintenanceRequestsInjectCurrentLeaderUserIntoServiceCommands() {
        when(defectReasonCatalogService.createReason(org.mockito.ArgumentMatchers.any())).thenReturn(8301L);
        when(runtimeConfigService.bindDeviceToProcess(org.mockito.ArgumentMatchers.any())).thenReturn(8101L);
        when(runtimeConfigService.saveDeviceParameterRule(org.mockito.ArgumentMatchers.any())).thenReturn(8401L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            assertEquals(8301L, controller.createDefectReason(new MesTeamDefectReasonSaveReqVO()
                    .setProcessId(6001L)
                    .setReasonType("LOSS")
                    .setReasonCode("LOSS-001")
                    .setReasonName("损耗")).getData());
            assertEquals(8101L, controller.saveProcessConfigDeviceBinding(new MesTeamProcessDeviceBindingSaveReqVO()
                    .setRouteProcessId(7101L)
                    .setDeviceId(7001L)).getData());
            assertEquals(8401L, controller.saveProcessConfigDeviceParameterRule(new MesTeamDeviceParameterRuleSaveReqVO()
                    .setRouteProcessId(7101L)
                    .setDeviceId(7001L)
                    .setParameterCode("pressure")
                    .setParameterName("压力")
                    .setLowerLimit(new BigDecimal("20"))
                    .setUpperLimit(new BigDecimal("40"))
                    .setTargetValue(new BigDecimal("30"))
                    .setValueType("DECIMAL")).getData());
        }

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonSaveReqBO>
                reasonCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonSaveReqBO.class);
        verify(defectReasonCatalogService).createReason(reasonCaptor.capture());
        assertEquals(3001L, reasonCaptor.getValue().getLeaderUserId());

        ArgumentCaptor<MesTeamProcessDeviceBindingSaveReqBO> deviceBindingCaptor =
                ArgumentCaptor.forClass(MesTeamProcessDeviceBindingSaveReqBO.class);
        verify(runtimeConfigService).bindDeviceToProcess(deviceBindingCaptor.capture());
        assertEquals(3001L, deviceBindingCaptor.getValue().getLeaderUserId());
        assertEquals(7101L, deviceBindingCaptor.getValue().getRouteProcessId());
        assertEquals(7001L, deviceBindingCaptor.getValue().getDeviceId());

        ArgumentCaptor<MesTeamDeviceParameterRuleSaveReqBO> ruleCaptor =
                ArgumentCaptor.forClass(MesTeamDeviceParameterRuleSaveReqBO.class);
        verify(runtimeConfigService).saveDeviceParameterRule(ruleCaptor.capture());
        assertEquals(3001L, ruleCaptor.getValue().getLeaderUserId());
        assertEquals(7101L, ruleCaptor.getValue().getRouteProcessId());
        assertEquals(new BigDecimal("30"), ruleCaptor.getValue().getTargetValue());
    }

    @Test
    void activeOrderRequestsInjectCurrentLeaderUserAndExposeOnlyActivePool() {
        when(activeOrderService.addActiveOrder(org.mockito.ArgumentMatchers.any())).thenReturn(8101L);
        when(activeOrderService.listActiveOrders(3001L)).thenReturn(List.of(new MesTeamLeaderActiveOrderRow()
                .setId(8101L)
                .setLeaderUserId(3001L)
                .setWorkOrderId(9001L)
                .setRouteId(922119L)
                .setRouteName("按压式球囊扩充压力泵工艺路线")
                .setRouteVersionId(448L)
                .setRouteVersionNo("V1")
                .setErpFixedQuantitySnapshot(new BigDecimal("200"))
                .setActiveStatus("ACTIVE")
                .setBusinessStatus("ACTIVE")
                .setJoinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .setVersion(0)));

        CommonResult<List<MesTeamLeaderActiveOrderRespVO>> listResponse;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            assertEquals(8101L, controller.addActiveOrder(new MesTeamLeaderActiveOrderAddReqVO()
                    .setWorkOrderId(9001L)).getData());
            controller.removeActiveOrder(new MesTeamLeaderActiveOrderRemoveReqVO().setActiveOrderId(8101L));
            listResponse = controller.getActiveOrderList();
        }

        ArgumentCaptor<MesTeamLeaderActiveOrderAddReqBO> addCaptor =
                ArgumentCaptor.forClass(MesTeamLeaderActiveOrderAddReqBO.class);
        verify(activeOrderService).addActiveOrder(addCaptor.capture());
        assertEquals(3001L, addCaptor.getValue().getLeaderUserId());
        assertEquals(9001L, addCaptor.getValue().getWorkOrderId());

        ArgumentCaptor<MesTeamLeaderActiveOrderRemoveReqBO> removeCaptor =
                ArgumentCaptor.forClass(MesTeamLeaderActiveOrderRemoveReqBO.class);
        verify(activeOrderService).removeActiveOrder(removeCaptor.capture());
        assertEquals(3001L, removeCaptor.getValue().getLeaderUserId());
        assertEquals(8101L, removeCaptor.getValue().getActiveOrderId());

        assertEquals(1, listResponse.getData().size());
        assertEquals(9001L, listResponse.getData().get(0).getWorkOrderId());
        assertEquals(922119L, listResponse.getData().get(0).getRouteId());
        assertEquals(448L, listResponse.getData().get(0).getRouteVersionId());
        assertEquals("按压式球囊扩充压力泵工艺路线", listResponse.getData().get(0).getRouteName());
        assertEquals("V1", listResponse.getData().get(0).getRouteVersionNo());
        assertEquals(new BigDecimal("200"), listResponse.getData().get(0).getErpFixedQuantitySnapshot());
        assertEquals("ACTIVE", listResponse.getData().get(0).getActiveStatus());
        assertEquals("ACTIVE", listResponse.getData().get(0).getBusinessStatus());
        assertEquals(0, listResponse.getData().get(0).getVersion());
    }

    @Test
    void activeOrderCandidateEndpointReturnsWorkOrderCodeOptions() {
        when(activeOrderService.searchActiveOrderCandidates("WO-9")).thenReturn(List.of(
                MesTeamLeaderActiveOrderCandidateBO.builder()
                        .workOrderId(9001L)
                        .workOrderCode("WO-9001")
                        .eligible(true)
                        .build(),
                MesTeamLeaderActiveOrderCandidateBO.builder()
                        .workOrderId(9002L)
                        .workOrderCode("WO-9002")
                        .eligible(false)
                        .ineligibleReason("缺少已发布QA规程")
                        .build()));

        List<MesTeamLeaderActiveOrderCandidateRespVO> candidates =
                controller.searchActiveOrderCandidates("WO-9").getData();

        assertEquals(2, candidates.size());
        assertEquals(9001L, candidates.get(0).getWorkOrderId());
        assertEquals("WO-9001", candidates.get(0).getWorkOrderCode());
        assertEquals(Boolean.TRUE, candidates.get(0).getEligible());
        assertEquals(null, candidates.get(0).getIneligibleReason());
        assertEquals(9002L, candidates.get(1).getWorkOrderId());
        assertEquals("WO-9002", candidates.get(1).getWorkOrderCode());
        assertEquals(Boolean.FALSE, candidates.get(1).getEligible());
        assertEquals("缺少已发布QA规程", candidates.get(1).getIneligibleReason());
        verify(activeOrderService).searchActiveOrderCandidates("WO-9");
    }

    @Test
    void pqcFormalCandidateEndpointDelegatesToPqcPermissionCandidateService() {
        when(pqcPersonnelService.searchFormalInspectorCandidates(3001L, "王")).thenReturn(List.of(
                new MesTeamFormalUserCandidateBO(2001L, "王检验")));

        CommonResult<List<MesTeamFormalUserCandidateRespVO>> response;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            response = controller.searchPqcFormalEmployeeCandidates("王");
        }

        assertEquals(1, response.getData().size());
        assertEquals(2001L, response.getData().get(0).getSystemUserId());
        assertEquals("王检验", response.getData().get(0).getDisplayName());
        verify(pqcPersonnelService).searchFormalInspectorCandidates(3001L, "王");
        verify(runtimeConfigService, never()).searchFormalUserCandidates(3001L, "王");
    }

    @Test
    void pqcFormalCandidateEndpointAcceptsMissingKeywordForEmptyDropdown() throws Exception {
        Method method = MesProcessPoolTeamLeaderController.class.getDeclaredMethod(
                "searchPqcFormalEmployeeCandidates", String.class);
        RequestParam keywordParam = method.getParameters()[0].getAnnotation(RequestParam.class);

        assertNotNull(keywordParam);
        assertEquals("keyword", keywordParam.value());
        assertFalse(keywordParam.required());
    }

    @Test
    void reportAllocationRequestsInjectCurrentLeaderUserAndNeverAcceptClientLeaderUser() {
        when(reportConfirmationService.previewFifoAllocation(org.mockito.ArgumentMatchers.any()))
                .thenReturn(MesTeamLeaderReportAllocationPreview.builder()
                        .totalAllocatedQuantity(new BigDecimal("80"))
                        .lines(List.of())
                        .build());
        when(reportConfirmationService.confirmSubmission(org.mockito.ArgumentMatchers.any())).thenReturn(7001L);

        MesTeamLeaderReportAllocationPreviewRespVO previewResponse;
        CommonResult<Long> confirmResponse;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            previewResponse = controller.previewReportFifoAllocation(
                    new MesTeamLeaderReportAllocationPreviewReqVO()
                            .setEventId(1001L)
                            .setLeaderType("PRODUCTION")).getData();
            confirmResponse = controller.confirmReportAllocation(
                    new MesTeamLeaderReportAllocationConfirmReqVO()
                            .setEventId(1001L)
                            .setLeaderType("PRODUCTION")
                            .setAllocationMode("MANUAL")
                            .setReviewRemark("现场调整")
                            .setReviewSignatureId(9201L)
                            .setReviewSignatureEmployeeUserId(3001L)
                            .setReviewSignatureSnapshotJson("{\"signature\":\"confirm\"}")
                            .setAllocations(List.of(new MesTeamLeaderReportAllocationLineReqVO()
                                    .setActiveOrderId(8101L)
                                    .setAllocatedQuantity(new BigDecimal("80")))));
        }

        assertEquals(new BigDecimal("80"), previewResponse.getTotalAllocatedQuantity());
        assertEquals(7001L, confirmResponse.getData());

        ArgumentCaptor<MesTeamLeaderReportAllocationPreviewReqBO> previewCaptor =
                ArgumentCaptor.forClass(MesTeamLeaderReportAllocationPreviewReqBO.class);
        verify(reportConfirmationService).previewFifoAllocation(previewCaptor.capture());
        assertEquals(3001L, previewCaptor.getValue().getLeaderUserId());
        assertEquals(1001L, previewCaptor.getValue().getEventId());

        ArgumentCaptor<MesTeamLeaderReportConfirmationReqBO> confirmCaptor =
                ArgumentCaptor.forClass(MesTeamLeaderReportConfirmationReqBO.class);
        verify(reportConfirmationService).confirmSubmission(confirmCaptor.capture());
        assertEquals(3001L, confirmCaptor.getValue().getLeaderUserId());
        assertEquals("PRODUCTION", confirmCaptor.getValue().getLeaderType());
        assertEquals("MANUAL", confirmCaptor.getValue().getAllocationMode());
        assertEquals(9201L, confirmCaptor.getValue().getReviewSignatureId());
        assertEquals(3001L, confirmCaptor.getValue().getReviewSignatureUserId());
        assertEquals("{\"signature\":\"confirm\"}", confirmCaptor.getValue().getReviewSignatureSnapshotJson());
        assertEquals(1, confirmCaptor.getValue().getAllocations().size());
        MesTeamLeaderReportAllocationLineReqBO line = confirmCaptor.getValue().getAllocations().get(0);
        assertEquals(8101L, line.getActiveOrderId());
        assertEquals(new BigDecimal("80"), line.getAllocatedQuantity());
    }

    @Test
    void runtimeConfigRequestsInjectCurrentLeaderUserAndCarryRouteProcessTargets() {
        when(runtimeConfigService.createEmployee(org.mockito.ArgumentMatchers.any())).thenReturn(8801L);
        when(runtimeConfigService.createDevice(org.mockito.ArgumentMatchers.any())).thenReturn(7001L);
        when(runtimeConfigService.bindDeviceToProcess(org.mockito.ArgumentMatchers.any())).thenReturn(7201L);
        when(runtimeConfigService.saveDeviceParameterRule(org.mockito.ArgumentMatchers.any())).thenReturn(8401L);
        when(runtimeConfigService.saveProcessDefectReason(org.mockito.ArgumentMatchers.any())).thenReturn(8301L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            assertEquals(8801L, controller.createEmployeeProfile(new MesTeamEmployeeProfileSaveReqVO()
                    .setEmployeeCode("TMP-001")
                    .setEmployeeName("临时工甲")
                    .setEmployeeType("TEMPORARY")).getData());
            assertEquals(7001L, controller.createTeamDevice(new MesTeamDeviceSaveReqVO()
                    .setDeviceCode("D-001")
                    .setDeviceName("压力泵")
                    .setDeviceStatus("REPAIRING")).getData());
            controller.updateTeamDeviceStatus(new MesTeamDeviceStatusUpdateReqVO()
                    .setDeviceId(7001L)
                    .setDeviceStatus("ENABLED"));
            assertEquals(7201L, controller.saveProcessConfigDeviceBinding(new MesTeamProcessDeviceBindingSaveReqVO()
                    .setRouteProcessId(7101L)
                    .setDeviceId(7001L)).getData());
            assertEquals(8401L, controller.saveProcessConfigDeviceParameterRule(new MesTeamDeviceParameterRuleSaveReqVO()
                    .setRouteProcessId(7101L)
                    .setDeviceId(7001L)
                    .setParameterCode("pressure")
                    .setParameterName("压力")
                    .setUnit("MPa")
                    .setLowerLimit(new BigDecimal("10"))
                    .setUpperLimit(new BigDecimal("20"))
                    .setTargetValue(new BigDecimal("15"))
                    .setStandardText("10-20MPa，目标15MPa")
                    .setValueType("DECIMAL")).getData());
            assertEquals(8301L, controller.saveProcessDefectReason(new MesTeamProcessDefectReasonSaveReqVO()
                    .setProcessId(6001L)
                    .setReasonType("LOSS")
                    .setReasonCode("LOSS-001")
                    .setReasonName("正常损耗")).getData());
        }

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeProfileSaveReqBO>
                employeeCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeProfileSaveReqBO.class);
        verify(runtimeConfigService).createEmployee(employeeCaptor.capture());
        assertEquals(3001L, employeeCaptor.getValue().getLeaderUserId());
        assertEquals("TEMPORARY", employeeCaptor.getValue().getEmployeeType());

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceSaveReqBO>
                deviceCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceSaveReqBO.class);
        verify(runtimeConfigService).createDevice(deviceCaptor.capture());
        assertEquals(3001L, deviceCaptor.getValue().getLeaderUserId());
        assertEquals("REPAIRING", deviceCaptor.getValue().getDeviceStatus());

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceStatusUpdateReqBO>
                deviceStatusCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceStatusUpdateReqBO.class);
        verify(runtimeConfigService).updateDeviceStatus(deviceStatusCaptor.capture());
        assertEquals(3001L, deviceStatusCaptor.getValue().getLeaderUserId());
        assertEquals("ENABLED", deviceStatusCaptor.getValue().getDeviceStatus());

        ArgumentCaptor<MesTeamProcessDeviceBindingSaveReqBO> deviceBindingCaptor =
                ArgumentCaptor.forClass(MesTeamProcessDeviceBindingSaveReqBO.class);
        verify(runtimeConfigService).bindDeviceToProcess(deviceBindingCaptor.capture());
        assertEquals(3001L, deviceBindingCaptor.getValue().getLeaderUserId());
        assertEquals(7101L, deviceBindingCaptor.getValue().getRouteProcessId());

        ArgumentCaptor<MesTeamDeviceParameterRuleSaveReqBO> ruleCaptor =
                ArgumentCaptor.forClass(MesTeamDeviceParameterRuleSaveReqBO.class);
        verify(runtimeConfigService).saveDeviceParameterRule(ruleCaptor.capture());
        assertEquals(3001L, ruleCaptor.getValue().getLeaderUserId());
        assertEquals(7101L, ruleCaptor.getValue().getRouteProcessId());
        assertEquals("MPa", ruleCaptor.getValue().getUnit());
        assertEquals(new BigDecimal("15"), ruleCaptor.getValue().getTargetValue());
        assertEquals("10-20MPa，目标15MPa", ruleCaptor.getValue().getStandardText());
    }

    @Test
    void productionPersonnelManagementEndpointsUseCurrentLeaderScopeAndAuditTrace() {
        when(runtimeConfigService.listEmployeeProfiles(3001L, null)).thenReturn(List.of(
                MesProcessPoolTeamEmployeeProfileDO.builder()
                        .id(8801L)
                        .leaderUserId(3001L)
                        .systemUserId(2001L)
                        .employeeCode("USER-2001")
                        .employeeName("张三")
                        .displayName("张三-A")
                        .employeeType("FORMAL")
                        .enabled(Boolean.TRUE)
                        .build()));
        when(runtimeConfigService.searchFormalUserCandidates(3001L, "张")).thenReturn(List.of(
                new MesTeamFormalUserCandidateBO(2001L, "张三")));
        when(runtimeConfigService.createTemporaryEmployee(org.mockito.ArgumentMatchers.any())).thenReturn(8802L);
        when(runtimeConfigService.linkFormalEmployee(org.mockito.ArgumentMatchers.any())).thenReturn(8803L);
        when(runtimeConfigService.listEmployeeAuditRecords(3001L, 8801L)).thenReturn(List.of(
                MesProcessPoolTeamMaintenanceAuditDO.builder()
                        .id(9901L)
                        .leaderUserId(3001L)
                        .operatorUserId(3001L)
                        .actionType("RENAME_EMPLOYEE")
                        .targetType("TEAM_EMPLOYEE_PROFILE")
                        .targetId(8801L)
                        .resultStatus("SUCCESS")
                        .changeSummary("修改生产人员显示名：张三-A")
                        .auditTime(LocalDateTime.of(2026, 8, 5, 10, 30))
                        .build()));

        CommonResult<List<MesTeamProductionEmployeeRespVO>> listResponse;
        CommonResult<List<MesTeamFormalUserCandidateRespVO>> candidatesResponse;
        CommonResult<List<MesTeamMaintenanceAuditRespVO>> auditResponse;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            listResponse = controller.getProductionPersonnelList(null);
            candidatesResponse = controller.searchFormalEmployeeCandidates("张");
            assertEquals(8802L, controller.createTemporaryEmployee(new MesTeamTemporaryEmployeeCreateReqVO()
                    .setDisplayName("临时工甲")
                    .setSignaturePassword("sign-123")).getData());
            assertEquals(8803L, controller.linkFormalEmployee(new MesTeamFormalEmployeeLinkReqVO()
                    .setSystemUserId(2001L)
                    .setDisplayName("张三-A")).getData());
            controller.updateEmployeeDisplayName(new MesTeamEmployeeDisplayNameUpdateReqVO()
                    .setEmployeeProfileId(8801L)
                    .setDisplayName("张三-B"));
            controller.updateEmployeeStatus(new MesTeamEmployeeStatusUpdateReqVO()
                    .setEmployeeProfileId(8801L)
                    .setEnabled(Boolean.FALSE));
            controller.resetTemporarySignaturePassword(new MesTeamTemporarySignaturePasswordResetReqVO()
                    .setEmployeeProfileId(8802L)
                    .setSignaturePassword("new-sign"));
            auditResponse = controller.getEmployeeAuditList(8801L);
        }

        assertEquals(1, listResponse.getData().size());
        assertEquals(8801L, listResponse.getData().get(0).getId());
        assertEquals("张三-A", listResponse.getData().get(0).getDisplayName());
        assertEquals("FORMAL", listResponse.getData().get(0).getEmployeeType());
        assertEquals(Boolean.TRUE, listResponse.getData().get(0).getEnabled());
        assertEquals("SYSTEM_USER", listResponse.getData().get(0).getSignaturePasswordManagedBy());

        assertEquals(1, candidatesResponse.getData().size());
        assertEquals(2001L, candidatesResponse.getData().get(0).getSystemUserId());
        assertEquals("张三", candidatesResponse.getData().get(0).getDisplayName());

        assertEquals(1, auditResponse.getData().size());
        assertEquals("RENAME_EMPLOYEE", auditResponse.getData().get(0).getActionType());
        assertEquals("SUCCESS", auditResponse.getData().get(0).getResultStatus());

        verify(runtimeConfigService).listEmployeeProfiles(3001L, null);
        verify(runtimeConfigService).searchFormalUserCandidates(3001L, "张");
        verify(runtimeConfigService).createTemporaryEmployee(org.mockito.ArgumentMatchers.argThat(req ->
                req.getLeaderUserId().equals(3001L)
                        && req.getDisplayName().equals("临时工甲")
                        && req.getSignaturePassword().equals("sign-123")));
        verify(runtimeConfigService).linkFormalEmployee(org.mockito.ArgumentMatchers.argThat(req ->
                req.getLeaderUserId().equals(3001L)
                        && req.getSystemUserId().equals(2001L)
                        && req.getDisplayName().equals("张三-A")));
        verify(runtimeConfigService).renameEmployee(org.mockito.ArgumentMatchers.argThat(req ->
                req.getLeaderUserId().equals(3001L)
                        && req.getEmployeeProfileId().equals(8801L)
                        && req.getDisplayName().equals("张三-B")));
        verify(runtimeConfigService).updateEmployeeEnabled(org.mockito.ArgumentMatchers.argThat(req ->
                req.getLeaderUserId().equals(3001L)
                        && req.getEmployeeProfileId().equals(8801L)
                        && Boolean.FALSE.equals(req.getEnabled())));
        verify(runtimeConfigService).resetTemporaryEmployeeSignaturePassword(org.mockito.ArgumentMatchers.argThat(req ->
                req.getLeaderUserId().equals(3001L)
                        && req.getEmployeeProfileId().equals(8802L)
                        && req.getSignaturePassword().equals("new-sign")));
        verify(runtimeConfigService).listEmployeeAuditRecords(3001L, 8801L);
    }

    @Test
    void p6TraceEndpointsExposeAllocationCompletionAndBatchRecordBackfillEvidence() {
        when(traceService.getAllocationTrace(1001L, 9001L, 5001L, 6001L))
                .thenReturn(new MesTeamLeaderAllocationTraceRespVO()
                        .setEventId(1001L)
                        .setWorkOrderId(9001L)
                        .setRouteProcessId(5001L)
                        .setProcessId(6001L)
                        .setTotalAllocatedQuantity(new BigDecimal("80"))
                        .setLines(List.of(new MesTeamLeaderAllocationTraceRespVO.Line()
                                .setAllocationId(7101L)
                                .setActiveOrderId(8101L)
                                .setWorkOrderId(9001L)
                                .setAllocatedQuantity(new BigDecimal("80"))
                                .setAllocationMode("FIFO"))));
        when(traceService.getOrderProcessTrace(9001L, 5001L, 6001L))
                .thenReturn(new MesTeamLeaderOrderProcessTraceRespVO()
                        .setWorkOrderId(9001L)
                        .setRouteProcessId(5001L)
                        .setProcessId(6001L)
                        .setTargetQuantity(new BigDecimal("80"))
                        .setConfirmedQuantity(new BigDecimal("80"))
                        .setCompletionStatus("COMPLETED")
                        .setBackfillStatus("SUCCESS")
                        .setBackfillExecutionId(8801L));
        when(traceService.getBatchRecordTrace(9001L, 5001L, 6001L))
                .thenReturn(new MesTeamLeaderBatchRecordTraceRespVO()
                        .setWorkOrderId(9001L)
                        .setRouteProcessId(5001L)
                        .setProcessId(6001L)
                        .setExecutionId(8801L)
                        .setBatchRecordReportId("BR-FORM-A")
                        .setFieldAuditLastBatchId(9901L)
                        .setCells(List.of(new MesTeamLeaderBatchRecordTraceRespVO.Cell()
                                .setFieldPath("report.pressure")
                                .setFieldKey("pressure")
                                .setRowIndex(6)
                                .setColumnIndex(2)
                                .setValueType("NUMBER")
                                .setValueDisplay("15"))));

        MesTeamLeaderAllocationTraceRespVO allocation = controller.getReportAllocationTrace(
                1001L, 9001L, 5001L, 6001L).getData();
        MesTeamLeaderOrderProcessTraceRespVO completion = controller.getOrderProcessTrace(
                9001L, 5001L, 6001L).getData();
        MesTeamLeaderBatchRecordTraceRespVO backfill = controller.getBatchRecordTrace(
                9001L, 5001L, 6001L).getData();

        assertEquals(new BigDecimal("80"), allocation.getTotalAllocatedQuantity());
        assertEquals(1, allocation.getLines().size());
        assertEquals("COMPLETED", completion.getCompletionStatus());
        assertEquals("SUCCESS", completion.getBackfillStatus());
        assertEquals(8801L, completion.getBackfillExecutionId());
        assertEquals(8801L, backfill.getExecutionId());
        assertEquals(1, backfill.getCells().size());
        assertEquals("pressure", backfill.getCells().get(0).getFieldKey());
        assertEquals("15", backfill.getCells().get(0).getValueDisplay());
        verify(traceService).getAllocationTrace(1001L, 9001L, 5001L, 6001L);
        verify(traceService).getOrderProcessTrace(9001L, 5001L, 6001L);
        verify(traceService).getBatchRecordTrace(9001L, 5001L, 6001L);
    }

    @Test
    void p0ProductionExecutionTraceEndpointDelegatesToUnifiedTraceService() {
        when(traceService.getProductionExecutionTrace(1001L))
                .thenReturn(new MesProductionExecutionTraceRespVO()
                        .setProcessPoolEventId(1001L)
                        .setComplete(false)
                        .setSections(List.of(new MesProductionExecutionTraceRespVO.Section()
                                .setSectionKey("quality")
                                .setStatus("BLOCKED")
                                .setBlockers(List.of(new MesProductionExecutionTraceRespVO.Blocker()
                                        .setCode("PQC_EVENT_MISSING"))))));

        MesProductionExecutionTraceRespVO trace = controller.getProductionExecutionTrace(1001L).getData();

        assertEquals(1001L, trace.getProcessPoolEventId());
        assertFalse(trace.getComplete());
        assertEquals("quality", trace.getSections().get(0).getSectionKey());
        assertEquals("PQC_EVENT_MISSING", trace.getSections().get(0).getBlockers().get(0).getCode());
        verify(traceService).getProductionExecutionTrace(1001L);
    }

    @Test
    void activeOrderTransferTraceEndpointExposesFormalShipmentAndBatchSourcesReadOnly() {
        when(activeOrderTransferTraceService.listByActiveOrder(8101L)).thenReturn(List.of(
                MesProcessPoolActiveOrderTransferTraceDO.builder()
                        .id(7201L)
                        .activeOrderId(8101L)
                        .workOrderId(9001L)
                        .routeId(922119L)
                        .routeVersionId(448L)
                        .sourceType(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SHIPMENT)
                        .direction("OUT")
                        .transferId(5001L)
                        .transferLineId(5002L)
                        .transferDetailId(5003L)
                        .materialStockId(6001L)
                        .batchId(7001L)
                        .itemId(8001L)
                        .quantity(new BigDecimal("15.000000"))
                        .sourceObjectType("WM_TRANSFER_DETAIL")
                        .sourceObjectId("5003")
                        .sourceObjectCode("TR-9001")
                        .sourceStatus("SHIPPED")
                        .sourceOccurredAt(LocalDateTime.of(2026, 8, 3, 10, 15))
                        .idempotencyKey("transfer-9001-line-2-batch-3")
                        .sourceSnapshotJson("{\"transferNo\":\"TR-9001\"}")
                        .build()));

        List<MesTeamLeaderActiveOrderTransferTraceRespVO> traces =
                controller.getActiveOrderTransferTrace(8101L).getData();

        assertEquals(1, traces.size());
        MesTeamLeaderActiveOrderTransferTraceRespVO trace = traces.get(0);
        assertEquals(7201L, trace.getId());
        assertEquals(8101L, trace.getActiveOrderId());
        assertEquals(9001L, trace.getWorkOrderId());
        assertEquals(922119L, trace.getRouteId());
        assertEquals(448L, trace.getRouteVersionId());
        assertEquals(MesProcessPoolActiveOrderTransferTraceDO.SOURCE_TYPE_SHIPMENT, trace.getSourceType());
        assertEquals("OUT", trace.getDirection());
        assertEquals(5001L, trace.getTransferId());
        assertEquals(5002L, trace.getTransferLineId());
        assertEquals(5003L, trace.getTransferDetailId());
        assertEquals(6001L, trace.getMaterialStockId());
        assertEquals(7001L, trace.getBatchId());
        assertEquals(8001L, trace.getItemId());
        assertEquals(new BigDecimal("15.000000"), trace.getQuantity());
        assertEquals("WM_TRANSFER_DETAIL", trace.getSourceObjectType());
        assertEquals("5003", trace.getSourceObjectId());
        assertEquals("TR-9001", trace.getSourceObjectCode());
        assertEquals("SHIPPED", trace.getSourceStatus());
        assertEquals(LocalDateTime.of(2026, 8, 3, 10, 15), trace.getSourceOccurredAt());
        assertEquals("transfer-9001-line-2-batch-3", trace.getIdempotencyKey());
        assertEquals("{\"transferNo\":\"TR-9001\"}", trace.getSourceSnapshotJson());
        verify(activeOrderTransferTraceService).listByActiveOrder(8101L);
    }

    @Test
    void processConfigListExposesUnifiedRouteProcessRowsWithDeviceParameterStats() {
        MesTeamLeaderProcessConfigListReqVO reqVO = new MesTeamLeaderProcessConfigListReqVO()
                .setRouteKeyword("PCU")
                .setDeviceKeyword("压力泵");
        when(processConfigService.listProcessConfigs(3001L, reqVO)).thenReturn(List.of(
                new MesTeamLeaderProcessConfigRow()
                        .setRouteId(9001L)
                        .setRouteCode("R-PCU")
                        .setRouteName("PCU 路线")
                        .setRouteProcessId(7101L)
                        .setProcessId(6001L)
                        .setProcessCode("P-CLEAN")
                        .setProcessName("精洗")
                        .setSort(10)
                        .setLossReasons(List.of())
                        .setDevices(List.of(new MesTeamLeaderProcessConfigDevice()
                                .setBindingId(8101L)
                                .setDeviceId(7001L)
                                .setDeviceCode("D-001")
                                .setDeviceName("压力泵")
                                .setDeviceStatus("ENABLED")
                                .setMapped(Boolean.TRUE)
                                .setParameters(List.of(new MesTeamLeaderProcessConfigParameter()
                                        .setRuleId(8401L)
                                        .setParameterCode("pressure")
                                        .setParameterName("压力")
                                        .setUnit("MPa")
                                        .setValueType("DECIMAL")
                                        .setStandardText("20-40MPa，目标30MPa")
                                        .setLowerLimit(new BigDecimal("20"))
                                        .setTargetValue(new BigDecimal("30"))
                                        .setUpperLimit(new BigDecimal("40"))
                                        .setEnabled(Boolean.TRUE)
                                        .setActualAverage(new BigDecimal("28.500000"))
                                        .setSampleCount(2)
                                        .setStatisticsWindowDays(30)
                                        .setStatisticsStartTime(LocalDateTime.of(2026, 7, 7, 16, 0))
                                        .setStatisticsEndTime(LocalDateTime.of(2026, 8, 6, 16, 0))))))));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            var response = controller.getProcessConfigList(reqVO).getData();
            assertEquals(1, response.size());
            assertEquals(7101L, response.get(0).getRouteProcessId());
            assertEquals("PCU 路线", response.get(0).getRouteName());
            assertEquals(1, response.get(0).getDevices().size());
            assertEquals("压力泵", response.get(0).getDevices().get(0).getDeviceName());
            assertEquals(new BigDecimal("30"),
                    response.get(0).getDevices().get(0).getParameters().get(0).getTargetValue());
            assertEquals(new BigDecimal("28.500000"),
                    response.get(0).getDevices().get(0).getParameters().get(0).getActualAverage());
            assertEquals("20-40MPa，目标30MPa",
                    response.get(0).getDevices().get(0).getParameters().get(0).getStandardText());
        }
        verify(processConfigService).listProcessConfigs(3001L, reqVO);
    }

    @Test
    void processConfigListQueryRejectsAllOversizedKeywords() {
        MesTeamLeaderProcessConfigListReqVO reqVO = new MesTeamLeaderProcessConfigListReqVO()
                .setRouteKeyword("R".repeat(129))
                .setProcessKeyword("P".repeat(129))
                .setLossReasonKeyword("L".repeat(129))
                .setDeviceKeyword("D".repeat(129))
                .setParameterKeyword("S".repeat(129));
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<String> invalidFields = validator.validate(reqVO).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("routeKeyword", "processKeyword", "lossReasonKeyword",
                "deviceKeyword", "parameterKeyword"), invalidFields);
    }

    @Test
    void mappingsAndPermissions_matchTeamLeaderWorkbenchContract() throws Exception {
        RequestMapping requestMapping = MesProcessPoolTeamLeaderController.class.getAnnotation(RequestMapping.class);
        assertNotNull(requestMapping);
        assertArrayEquals(new String[]{"/mes/pro/process-pool/team-leader"}, requestMapping.value());

        assertEndpoint("getSubmissionPage", new Class[]{MesTeamLeaderSubmissionPageReqVO.class}, GetMapping.class,
                new String[]{"/submission/page"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("getSubmissionDetail", new Class[]{Long.class, String.class}, GetMapping.class,
                new String[]{"/submission/detail"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("reviewSubmission", new Class[]{MesTeamLeaderSubmissionReviewReqVO.class}, PostMapping.class,
                new String[]{"/submission/review"}, "mes:pro-process-pool-team-leader:review");
        assertEndpoint("markAndReportWorkOrderAbnormal", new Class[]{MesWorkOrderAbnormalReportReqVO.class},
                PostMapping.class, new String[]{"/work-order/abnormal/report"},
                "mes:pro-process-pool-team-leader:abnormal");
        assertNull(findFieldOrNull(MesWorkOrderAbnormalReportReqVO.class, "routeProcessId"));
        assertNull(findFieldOrNull(MesWorkOrderAbnormalReportReqVO.class, "processId"));
        assertNull(findFieldOrNull(MesWorkOrderAbnormalReportReqVO.class, "sourceEventId"));
        assertNull(findFieldOrNull(MesWorkOrderAbnormalReportReqVO.class, "abnormalReasonCode"));
        assertEndpoint("createDefectReason", new Class[]{MesTeamDefectReasonSaveReqVO.class}, PostMapping.class,
                new String[]{"/defect-reason/create"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("getProcessConfigList", new Class[]{MesTeamLeaderProcessConfigListReqVO.class}, GetMapping.class,
                new String[]{"/process-config/list"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("saveProcessConfigDeviceBinding", new Class[]{MesTeamProcessDeviceBindingSaveReqVO.class},
                PostMapping.class, new String[]{"/process-config/device-binding/save"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("saveProcessConfigDeviceParameterRule", new Class[]{MesTeamDeviceParameterRuleSaveReqVO.class},
                PostMapping.class, new String[]{"/process-config/device-parameter-rule/save"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("addActiveOrder", new Class[]{MesTeamLeaderActiveOrderAddReqVO.class}, PostMapping.class,
                new String[]{"/active-order/add"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("searchActiveOrderCandidates", new Class[]{String.class}, GetMapping.class,
                new String[]{"/active-order/candidates"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("removeActiveOrder", new Class[]{MesTeamLeaderActiveOrderRemoveReqVO.class}, PutMapping.class,
                new String[]{"/active-order/remove"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("getActiveOrderList", new Class[]{}, GetMapping.class,
                new String[]{"/active-order/list"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("previewReportFifoAllocation",
                new Class[]{MesTeamLeaderReportAllocationPreviewReqVO.class}, PostMapping.class,
                new String[]{"/submission/allocation/preview-fifo"},
                "mes:pro-process-pool-team-leader:review");
        assertEndpoint("confirmReportAllocation",
                new Class[]{MesTeamLeaderReportAllocationConfirmReqVO.class}, PostMapping.class,
                new String[]{"/submission/allocation/confirm"},
                "mes:pro-process-pool-team-leader:review");
        assertEndpoint("createEmployeeProfile", new Class[]{MesTeamEmployeeProfileSaveReqVO.class}, PostMapping.class,
                new String[]{"/employee-profile/create"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("getPqcPersonnelList", new Class[]{Boolean.class}, GetMapping.class,
                new String[]{"/pqc-personnel/list"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("searchPqcFormalEmployeeCandidates", new Class[]{String.class}, GetMapping.class,
                new String[]{"/pqc-personnel/formal-candidates"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("linkPqcFormalEmployee", new Class[]{MesPqcLeaderPersonnelLinkReqVO.class},
                PostMapping.class, new String[]{"/pqc-personnel/formal/link"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("updatePqcPersonnelStatus", new Class[]{MesPqcLeaderPersonnelStatusUpdateReqVO.class},
                PutMapping.class, new String[]{"/pqc-personnel/status/update"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("getProductionPersonnelList", new Class[]{Boolean.class}, GetMapping.class,
                new String[]{"/employee-profile/list"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("searchFormalEmployeeCandidates", new Class[]{String.class}, GetMapping.class,
                new String[]{"/employee-profile/formal-candidates"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("createTemporaryEmployee", new Class[]{MesTeamTemporaryEmployeeCreateReqVO.class},
                PostMapping.class, new String[]{"/employee-profile/temporary/create"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("linkFormalEmployee", new Class[]{MesTeamFormalEmployeeLinkReqVO.class},
                PostMapping.class, new String[]{"/employee-profile/formal/link"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("updateEmployeeDisplayName", new Class[]{MesTeamEmployeeDisplayNameUpdateReqVO.class},
                PutMapping.class, new String[]{"/employee-profile/display-name/update"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("updateEmployeeStatus", new Class[]{MesTeamEmployeeStatusUpdateReqVO.class},
                PutMapping.class, new String[]{"/employee-profile/status/update"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("resetTemporarySignaturePassword",
                new Class[]{MesTeamTemporarySignaturePasswordResetReqVO.class}, PutMapping.class,
                new String[]{"/employee-profile/temp-signature-password/reset"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("getEmployeeAuditList", new Class[]{Long.class}, GetMapping.class,
                new String[]{"/employee-profile/audit/list"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("createTeamDevice", new Class[]{MesTeamDeviceSaveReqVO.class}, PostMapping.class,
                new String[]{"/team-device/create"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("updateTeamDeviceStatus", new Class[]{MesTeamDeviceStatusUpdateReqVO.class}, PutMapping.class,
                new String[]{"/team-device/status/update"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("saveProcessDefectReason", new Class[]{MesTeamProcessDefectReasonSaveReqVO.class},
                PostMapping.class, new String[]{"/process-defect-reason/save"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("getReportAllocationTrace",
                new Class[]{Long.class, Long.class, Long.class, Long.class}, GetMapping.class,
                new String[]{"/submission/allocation/trace"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("getOrderProcessTrace", new Class[]{Long.class, Long.class, Long.class}, GetMapping.class,
                new String[]{"/order-process/trace"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("getBatchRecordTrace", new Class[]{Long.class, Long.class, Long.class}, GetMapping.class,
                new String[]{"/batch-record/trace"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("getProductionExecutionTrace", new Class[]{Long.class}, GetMapping.class,
                new String[]{"/production-execution/trace"}, "mes:pro-process-pool-team-leader:query");
        assertEndpoint("getActiveOrderTransferTrace", new Class[]{Long.class}, GetMapping.class,
                new String[]{"/active-order/transfer-trace"}, "mes:pro-process-pool-team-leader:query");

        assertNoClientLeaderUserField(MesTeamLeaderSubmissionPageReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderSubmissionReviewReqVO.class);
        assertNoClientLeaderUserField(MesWorkOrderAbnormalReportReqVO.class);
        assertNoClientLeaderUserField(MesTeamDefectReasonSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamDeviceParameterRuleSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderActiveOrderAddReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderActiveOrderRemoveReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderReportAllocationPreviewReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderReportAllocationConfirmReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderReportAllocationLineReqVO.class);
        assertNoClientLeaderUserField(MesTeamEmployeeProfileSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamTemporaryEmployeeCreateReqVO.class);
        assertNoClientLeaderUserField(MesTeamFormalEmployeeLinkReqVO.class);
        assertNoClientLeaderUserField(MesTeamEmployeeDisplayNameUpdateReqVO.class);
        assertNoClientLeaderUserField(MesTeamEmployeeStatusUpdateReqVO.class);
        assertNoClientLeaderUserField(MesTeamTemporarySignaturePasswordResetReqVO.class);
        assertNoClientLeaderUserField(MesTeamDeviceSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamDeviceStatusUpdateReqVO.class);
        assertNoClientLeaderUserField(MesTeamProcessDeviceBindingSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamProcessDefectReasonSaveReqVO.class);

        requireGetter(ProcessPoolTimelineDetailRespVO.class, "getOriginalPayloadJson");
    }

    private void assertEndpoint(String methodName, Class<?>[] parameterTypes,
                                Class<? extends java.lang.annotation.Annotation> mappingType,
                                String[] expectedPath, String expectedPermission) throws Exception {
        Method method = MesProcessPoolTeamLeaderController.class.getDeclaredMethod(methodName, parameterTypes);
        Object mapping = method.getAnnotation(mappingType);
        assertNotNull(mapping);
        String[] value = (String[]) mappingType.getMethod("value").invoke(mapping);
        assertArrayEquals(expectedPath, value);
        assertEquals("@ss.hasPermission('" + expectedPermission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private void assertNoClientLeaderUserField(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            assertFalse("leaderUserId".equals(field.getName()),
                    "Client request VO must not accept leaderUserId: " + type.getName());
        }
    }

    private Field findFieldOrNull(Class<?> type, String name) {
        for (Field field : type.getDeclaredFields()) {
            if (name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private void requireGetter(Class<?> type, String getterName) throws NoSuchMethodException {
        assertNotNull(type.getMethod(getterName));
    }
}
