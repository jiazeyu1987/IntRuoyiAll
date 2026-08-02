package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolTimelineEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDefectReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceParameterRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeBindingDisableReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderAddReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRemoveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderActiveOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderAllocationTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderBatchRecordTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderOrderProcessTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationLineReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderReportAllocationPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamLeaderSubmissionReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamDeviceStatusUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamEmployeeProfileSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProcessDefectReasonSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProcessDeviceBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesTeamProcessEmployeeBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesWorkOrderAbnormalReportReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonCatalogService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProcessDeviceParameterRuleService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderAddReqBO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderRemoveReqBO;
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
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesWorkOrderAbnormalReportService;
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
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;
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
    private MesTeamEmployeeBindingService employeeBindingService;
    @Mock
    private MesDefectReasonCatalogService defectReasonCatalogService;
    @Mock
    private MesProcessDeviceParameterRuleService deviceParameterRuleService;
    @Mock
    private MesTeamLeaderActiveOrderService activeOrderService;
    @Mock
    private MesTeamLeaderReportConfirmationService reportConfirmationService;
    @Mock
    private MesTeamLeaderRuntimeConfigService runtimeConfigService;
    @Mock
    private MesTeamLeaderTraceService traceService;

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
                .setReviewRemark("已复核");

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
    }

    @Test
    void maintenanceRequestsInjectCurrentLeaderUserIntoServiceCommands() {
        when(employeeBindingService.addEmployeeBinding(org.mockito.ArgumentMatchers.any())).thenReturn(8201L);
        when(defectReasonCatalogService.createReason(org.mockito.ArgumentMatchers.any())).thenReturn(8301L);
        when(deviceParameterRuleService.saveRule(org.mockito.ArgumentMatchers.any())).thenReturn(8401L);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            assertEquals(8201L, controller.addEmployeeBinding(new MesTeamEmployeeBindingSaveReqVO()
                    .setProcessId(6001L)
                    .setEmployeeUserId(2001L)).getData());
            controller.disableEmployeeBinding(new MesTeamEmployeeBindingDisableReqVO().setBindingId(8201L));
            assertEquals(8301L, controller.createDefectReason(new MesTeamDefectReasonSaveReqVO()
                    .setProcessId(6001L)
                    .setReasonType("LOSS")
                    .setReasonCode("LOSS-001")
                    .setReasonName("损耗")).getData());
            assertEquals(8401L, controller.saveDeviceParameterRule(new MesTeamDeviceParameterRuleSaveReqVO()
                    .setProcessId(6001L)
                    .setDeviceId(7001L)
                    .setParameterCode("pressure")
                    .setParameterName("压力")
                    .setLowerLimit(new BigDecimal("20"))
                    .setUpperLimit(new BigDecimal("40"))
                    .setValueType("DECIMAL")).getData());
        }

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingSaveReqBO>
                bindingCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingSaveReqBO.class);
        verify(employeeBindingService).addEmployeeBinding(bindingCaptor.capture());
        assertEquals(3001L, bindingCaptor.getValue().getLeaderUserId());

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingDisableReqBO>
                disableCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamEmployeeBindingDisableReqBO.class);
        verify(employeeBindingService).disableEmployeeBinding(disableCaptor.capture());
        assertEquals(3001L, disableCaptor.getValue().getLeaderUserId());

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonSaveReqBO>
                reasonCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesDefectReasonSaveReqBO.class);
        verify(defectReasonCatalogService).createReason(reasonCaptor.capture());
        assertEquals(3001L, reasonCaptor.getValue().getLeaderUserId());

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProcessDeviceParameterRuleSaveReqBO>
                ruleCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProcessDeviceParameterRuleSaveReqBO.class);
        verify(deviceParameterRuleService).saveRule(ruleCaptor.capture());
        assertEquals(3001L, ruleCaptor.getValue().getLeaderUserId());
    }

    @Test
    void activeOrderRequestsInjectCurrentLeaderUserAndExposeOnlyActivePool() {
        when(activeOrderService.addActiveOrder(org.mockito.ArgumentMatchers.any())).thenReturn(8101L);
        when(activeOrderService.listActiveOrders(3001L)).thenReturn(List.of(MesProcessPoolActiveOrderDO.builder()
                .id(8101L)
                .leaderUserId(3001L)
                .workOrderId(9001L)
                .routeId(922119L)
                .routeVersionId(448L)
                .erpFixedQuantitySnapshot(new BigDecimal("200"))
                .activeStatus("ACTIVE")
                .businessStatus("ACTIVE")
                .joinedAt(LocalDateTime.of(2026, 7, 31, 8, 30))
                .version(0)
                .build()));

        CommonResult<List<MesTeamLeaderActiveOrderRespVO>> listResponse;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(3001L);
            assertEquals(8101L, controller.addActiveOrder(new MesTeamLeaderActiveOrderAddReqVO()
                    .setWorkOrderId(9001L)
                    .setRouteId(922119L)
                    .setRouteVersionId(448L)).getData());
            controller.removeActiveOrder(new MesTeamLeaderActiveOrderRemoveReqVO().setActiveOrderId(8101L));
            listResponse = controller.getActiveOrderList();
        }

        ArgumentCaptor<MesTeamLeaderActiveOrderAddReqBO> addCaptor =
                ArgumentCaptor.forClass(MesTeamLeaderActiveOrderAddReqBO.class);
        verify(activeOrderService).addActiveOrder(addCaptor.capture());
        assertEquals(3001L, addCaptor.getValue().getLeaderUserId());
        assertEquals(9001L, addCaptor.getValue().getWorkOrderId());
        assertEquals(922119L, addCaptor.getValue().getRouteId());
        assertEquals(448L, addCaptor.getValue().getRouteVersionId());

        ArgumentCaptor<MesTeamLeaderActiveOrderRemoveReqBO> removeCaptor =
                ArgumentCaptor.forClass(MesTeamLeaderActiveOrderRemoveReqBO.class);
        verify(activeOrderService).removeActiveOrder(removeCaptor.capture());
        assertEquals(3001L, removeCaptor.getValue().getLeaderUserId());
        assertEquals(8101L, removeCaptor.getValue().getActiveOrderId());

        assertEquals(1, listResponse.getData().size());
        assertEquals(9001L, listResponse.getData().get(0).getWorkOrderId());
        assertEquals(922119L, listResponse.getData().get(0).getRouteId());
        assertEquals(448L, listResponse.getData().get(0).getRouteVersionId());
        assertEquals(new BigDecimal("200"), listResponse.getData().get(0).getErpFixedQuantitySnapshot());
        assertEquals("ACTIVE", listResponse.getData().get(0).getActiveStatus());
        assertEquals("ACTIVE", listResponse.getData().get(0).getBusinessStatus());
        assertEquals(0, listResponse.getData().get(0).getVersion());
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
        assertEquals(1, confirmCaptor.getValue().getAllocations().size());
        MesTeamLeaderReportAllocationLineReqBO line = confirmCaptor.getValue().getAllocations().get(0);
        assertEquals(8101L, line.getActiveOrderId());
        assertEquals(new BigDecimal("80"), line.getAllocatedQuantity());
    }

    @Test
    void runtimeConfigRequestsInjectCurrentLeaderUserAndCarryDeviceDefaults() {
        when(runtimeConfigService.createEmployee(org.mockito.ArgumentMatchers.any())).thenReturn(8801L);
        when(runtimeConfigService.bindEmployeeToProcess(org.mockito.ArgumentMatchers.any())).thenReturn(8201L);
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
            assertEquals(8201L, controller.saveProcessEmployeeBinding(new MesTeamProcessEmployeeBindingSaveReqVO()
                    .setProcessId(6001L)
                    .setEmployeeProfileId(8801L)).getData());
            assertEquals(7001L, controller.createTeamDevice(new MesTeamDeviceSaveReqVO()
                    .setDeviceCode("D-001")
                    .setDeviceName("压力泵")
                    .setDeviceStatus("REPAIRING")).getData());
            controller.updateTeamDeviceStatus(new MesTeamDeviceStatusUpdateReqVO()
                    .setDeviceId(7001L)
                    .setDeviceStatus("ENABLED"));
            assertEquals(7201L, controller.saveProcessDeviceBinding(new MesTeamProcessDeviceBindingSaveReqVO()
                    .setProcessId(6001L)
                    .setDeviceId(7001L)).getData());
            assertEquals(8401L, controller.saveRuntimeDeviceParameterRule(new MesTeamDeviceParameterRuleSaveReqVO()
                    .setProcessId(6001L)
                    .setDeviceId(7001L)
                    .setParameterCode("pressure")
                    .setParameterName("压力")
                    .setUnit("MPa")
                    .setLowerLimit(new BigDecimal("10"))
                    .setUpperLimit(new BigDecimal("20"))
                    .setDefaultValue(new BigDecimal("15"))
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

        ArgumentCaptor<cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceParameterRuleSaveReqBO>
                ruleCaptor = ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamDeviceParameterRuleSaveReqBO.class);
        verify(runtimeConfigService).saveDeviceParameterRule(ruleCaptor.capture());
        assertEquals(3001L, ruleCaptor.getValue().getLeaderUserId());
        assertEquals("MPa", ruleCaptor.getValue().getUnit());
        assertEquals(new BigDecimal("15"), ruleCaptor.getValue().getDefaultValue());
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
        assertEndpoint("addEmployeeBinding", new Class[]{MesTeamEmployeeBindingSaveReqVO.class}, PostMapping.class,
                new String[]{"/employee-binding/add"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("disableEmployeeBinding", new Class[]{MesTeamEmployeeBindingDisableReqVO.class},
                PutMapping.class, new String[]{"/employee-binding/disable"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("createDefectReason", new Class[]{MesTeamDefectReasonSaveReqVO.class}, PostMapping.class,
                new String[]{"/defect-reason/create"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("saveDeviceParameterRule", new Class[]{MesTeamDeviceParameterRuleSaveReqVO.class},
                PostMapping.class, new String[]{"/device-parameter-rule/save"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("addActiveOrder", new Class[]{MesTeamLeaderActiveOrderAddReqVO.class}, PostMapping.class,
                new String[]{"/active-order/add"}, "mes:pro-process-pool-team-leader:maintain");
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
        assertEndpoint("saveProcessEmployeeBinding", new Class[]{MesTeamProcessEmployeeBindingSaveReqVO.class},
                PostMapping.class, new String[]{"/process-employee-binding/save"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("createTeamDevice", new Class[]{MesTeamDeviceSaveReqVO.class}, PostMapping.class,
                new String[]{"/team-device/create"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("updateTeamDeviceStatus", new Class[]{MesTeamDeviceStatusUpdateReqVO.class}, PutMapping.class,
                new String[]{"/team-device/status/update"}, "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("saveProcessDeviceBinding", new Class[]{MesTeamProcessDeviceBindingSaveReqVO.class},
                PostMapping.class, new String[]{"/process-device-binding/save"},
                "mes:pro-process-pool-team-leader:maintain");
        assertEndpoint("saveRuntimeDeviceParameterRule", new Class[]{MesTeamDeviceParameterRuleSaveReqVO.class},
                PostMapping.class, new String[]{"/runtime-device-parameter-rule/save"},
                "mes:pro-process-pool-team-leader:maintain");
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

        assertNoClientLeaderUserField(MesTeamLeaderSubmissionPageReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderSubmissionReviewReqVO.class);
        assertNoClientLeaderUserField(MesWorkOrderAbnormalReportReqVO.class);
        assertNoClientLeaderUserField(MesTeamEmployeeBindingSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamEmployeeBindingDisableReqVO.class);
        assertNoClientLeaderUserField(MesTeamDefectReasonSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamDeviceParameterRuleSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderActiveOrderAddReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderActiveOrderRemoveReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderReportAllocationPreviewReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderReportAllocationConfirmReqVO.class);
        assertNoClientLeaderUserField(MesTeamLeaderReportAllocationLineReqVO.class);
        assertNoClientLeaderUserField(MesTeamEmployeeProfileSaveReqVO.class);
        assertNoClientLeaderUserField(MesTeamProcessEmployeeBindingSaveReqVO.class);
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

    private void requireGetter(Class<?> type, String getterName) throws NoSuchMethodException {
        assertNotNull(type.getMethod(getterName));
    }
}
