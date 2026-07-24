package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderCreateFromWorkOrdersReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderPreflightSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderProcessWipRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderUpdatePriorityReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.scheduleorder.MesProScheduleOrderService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderControllerTest {

    @Mock
    private MesProScheduleOrderService scheduleOrderService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private MesProScheduleOrderController controller;

    @Test
    void updatePriority_delegatesToServiceAndReturnsTrue() {
        MesProScheduleOrderUpdatePriorityReqVO reqVO = new MesProScheduleOrderUpdatePriorityReqVO();
        reqVO.setId(10L);
        reqVO.setPriorityNo(3);

        CommonResult<Boolean> response = controller.updatePriority(reqVO);

        assertEquals(Boolean.TRUE, response.getData());
        verify(scheduleOrderService).updatePriority(10L, 3);
    }

    @Test
    void getScheduleOrderPage_backfillsWorkOrderCodeFromLinkedWorkOrderWhenScheduleCodeMissing() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SCH-LEGACY-20260702-0001")
                .workOrderId(20L)
                .erpWorkOrderCode(null)
                .productId(30L)
                .quantity(new BigDecimal("10.000000"))
                .promiseDate(LocalDate.of(2026, 7, 2))
                .routeId(40L)
                .build();

        when(scheduleOrderService.getScheduleOrderPage(any()))
                .thenReturn(new PageResult<>(List.of(scheduleOrder), 1L));
        when(itemService.getItemMap(Set.of(30L)))
                .thenReturn(Map.of(30L, MesMdItemDO.builder()
                        .id(30L)
                        .code("ITEM-001")
                        .name("产品")
                        .specification("S1")
                        .build()));
        when(routeService.getRouteMap(Set.of(40L)))
                .thenReturn(Map.of(40L, MesProRouteDO.builder()
                        .id(40L)
                        .code("ROUTE-001")
                        .name("工艺路线")
                        .build()));
        when(workOrderService.getWorkOrderMap(Set.of(20L)))
                .thenReturn(Map.of(20L, MesProWorkOrderDO.builder()
                        .id(20L)
                        .code("MO-LEGACY-001")
                        .build()));
        when(scheduleOrderService.getScheduleOrderProcessListByScheduleOrderIds(Set.of(10L)))
                .thenReturn(List.of());
        when(scheduleOrderService.calculateProcessAggregateProgressSummary(new BigDecimal("10.000000"), List.of()))
                .thenReturn(new MesProScheduleOrderService.ProgressSummary(
                        new BigDecimal("10.000000"),
                        BigDecimal.ZERO.setScale(6),
                        new BigDecimal("10.000000"),
                        BigDecimal.ZERO.setScale(6)));
        when(processService.getProcessMap(Set.of())).thenReturn(Map.of());

        CommonResult<PageResult<MesProScheduleOrderRespVO>> response =
                controller.getScheduleOrderPage(new MesProScheduleOrderPageReqVO());

        MesProScheduleOrderRespVO vo = response.getData().getList().get(0);
        assertEquals("MO-LEGACY-001", vo.getErpWorkOrderCode());
        verify(workOrderService).getWorkOrderMap(Set.of(20L));
    }

    @Test
    void getScheduleOrderPage_returnsProductionMaterialListSummaryByWorkOrderId() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SCH-WO-001-20260703-0001")
                .workOrderId(20L)
                .erpWorkOrderCode("WO-001")
                .productId(30L)
                .quantity(new BigDecimal("10.000000"))
                .promiseDate(LocalDate.of(2026, 7, 3))
                .build();

        when(scheduleOrderService.getScheduleOrderPage(any()))
                .thenReturn(new PageResult<>(List.of(scheduleOrder), 1L));
        when(itemService.getItemMap(Set.of(30L)))
                .thenReturn(Map.of(30L, MesMdItemDO.builder()
                        .id(30L)
                        .code("ITEM-001")
                        .name("产品")
                        .specification("S1")
                        .build()));
        when(routeService.getRouteMap(Set.of())).thenReturn(Map.of());
        when(productionMaterialListMapper.selectListByWorkOrderIds(Set.of(20L))).thenReturn(List.of(
                MesKingdeeProductionMaterialListDO.builder().workOrderId(20L).sourceBillNo("PPBOM-001").build(),
                MesKingdeeProductionMaterialListDO.builder().workOrderId(20L).sourceBillNo("PPBOM-002").build(),
                MesKingdeeProductionMaterialListDO.builder().workOrderId(20L).sourceBillNo("PPBOM-002").build()));
        when(scheduleOrderService.getScheduleOrderProcessListByScheduleOrderIds(Set.of(10L)))
                .thenReturn(List.of());
        when(scheduleOrderService.calculateProcessAggregateProgressSummary(new BigDecimal("10.000000"), List.of()))
                .thenReturn(new MesProScheduleOrderService.ProgressSummary(
                        new BigDecimal("10.000000"),
                        BigDecimal.ZERO.setScale(6),
                        new BigDecimal("10.000000"),
                        BigDecimal.ZERO.setScale(6)));
        when(processService.getProcessMap(Set.of())).thenReturn(Map.of());

        CommonResult<PageResult<MesProScheduleOrderRespVO>> response =
                controller.getScheduleOrderPage(new MesProScheduleOrderPageReqVO());

        MesProScheduleOrderRespVO vo = response.getData().getList().get(0);
        assertEquals(2, vo.getProductionMaterialListCount());
        assertEquals("PPBOM-001、PPBOM-002", vo.getProductionMaterialListSummary());
    }

    @Test
    void getScheduleOrderPage_returnsProgressAggregatedFromProcessSnapshots() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SCH-WO-001-20260612-0001")
                .workOrderId(20L)
                .erpWorkOrderCode("WO-001")
                .productId(30L)
                .quantity(new BigDecimal("100.000000"))
                .promiseDate(LocalDate.of(2026, 6, 30))
                .routeId(40L)
                .build();
        MesProScheduleOrderProcessDO firstProcess = MesProScheduleOrderProcessDO.builder()
                .id(101L)
                .scheduleOrderId(10L)
                .plannedQuantity(new BigDecimal("100.000000"))
                .reportedQuantity(new BigDecimal("25.000000"))
                .remainingQuantity(new BigDecimal("75.000000"))
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(102L)
                .scheduleOrderId(10L)
                .plannedQuantity(new BigDecimal("100.000000"))
                .reportedQuantity(new BigDecimal("75.000000"))
                .remainingQuantity(new BigDecimal("25.000000"))
                .build();

        when(scheduleOrderService.getScheduleOrderPage(any()))
                .thenReturn(new PageResult<>(List.of(scheduleOrder), 1L));
        when(itemService.getItemMap(Set.of(30L)))
                .thenReturn(Map.of(30L, MesMdItemDO.builder()
                        .id(30L)
                        .code("ITEM-001")
                        .name("产品")
                        .specification("S1")
                        .build()));
        when(routeService.getRouteMap(Set.of(40L)))
                .thenReturn(Map.of(40L, MesProRouteDO.builder()
                        .id(40L)
                        .code("ROUTE-001")
                        .name("工艺路线")
                        .build()));
        when(scheduleOrderService.getScheduleOrderProcessListByScheduleOrderIds(Set.of(10L)))
                .thenReturn(List.of(firstProcess, secondProcess));
        when(scheduleOrderService.calculateProcessAggregateProgressSummary(any(), any()))
                .thenReturn(new MesProScheduleOrderService.ProgressSummary(
                        new BigDecimal("200.000000"),
                        new BigDecimal("100.000000"),
                        new BigDecimal("100.000000"),
                        new BigDecimal("50.000000")));
        when(scheduleOrderService.calculateProcessProgressMetrics(10L, List.of(firstProcess, secondProcess)))
                .thenReturn(Map.of(
                        101L, new MesProScheduleOrderService.ProcessProgressMetrics(
                                new BigDecimal("25.000000"),
                                new BigDecimal("10.000000"),
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                new BigDecimal("25.000000"),
                                new BigDecimal("75.000000"),
                                new BigDecimal("25.000000")),
                        102L, new MesProScheduleOrderService.ProcessProgressMetrics(
                                new BigDecimal("75.000000"),
                                BigDecimal.ZERO.setScale(6),
                                new BigDecimal("5.000000"),
                                new BigDecimal("2.000000"),
                                new BigDecimal("77.000000"),
                                new BigDecimal("23.000000"),
                                new BigDecimal("75.000000"))));

        CommonResult<PageResult<MesProScheduleOrderRespVO>> response =
                controller.getScheduleOrderPage(new MesProScheduleOrderPageReqVO());

        MesProScheduleOrderRespVO vo = response.getData().getList().get(0);
        assertEquals(new BigDecimal("50.000000"), vo.getProgressPercent());
        assertEquals(new BigDecimal("100.000000"), vo.getEffectiveCompletedQuantity());
        assertEquals(new BigDecimal("10.000000"), vo.getPendingApprovalQuantity());
        assertEquals(new BigDecimal("5.000000"), vo.getPendingInspectionQuantity());
        assertEquals(new BigDecimal("2.000000"), vo.getOverReportedQuantity());
        assertEquals("ITEM-001", vo.getProductCode());
        assertEquals("工艺路线", vo.getRouteName());
        verify(scheduleOrderService).getScheduleOrderProcessListByScheduleOrderIds(Set.of(10L));
    }

    @Test
    void getScheduleOrderPage_manualFinishedKeepsLockedSummaryAndRealProcessMetrics() {
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SCH-WO-001-20260612-0001")
                .workOrderId(20L)
                .erpWorkOrderCode("WO-001")
                .productId(30L)
                .quantity(new BigDecimal("100.000000"))
                .totalQuantity(new BigDecimal("100.000000"))
                .completedQuantity(new BigDecimal("100.000000"))
                .uncompletedQuantity(BigDecimal.ZERO.setScale(6))
                .progressPercent(new BigDecimal("100.000000"))
                .status(3)
                .manualFinished(Boolean.TRUE)
                .manualFinishedReason("排产员人工完成")
                .promiseDate(LocalDate.of(2026, 6, 30))
                .routeId(40L)
                .build();
        MesProScheduleOrderProcessDO firstProcess = MesProScheduleOrderProcessDO.builder()
                .id(101L)
                .scheduleOrderId(10L)
                .routeProcessId(201L)
                .processId(301L)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100.000000"))
                .reportedQuantity(new BigDecimal("25.000000"))
                .remainingQuantity(new BigDecimal("75.000000"))
                .build();
        MesProScheduleOrderProcessDO secondProcess = MesProScheduleOrderProcessDO.builder()
                .id(102L)
                .scheduleOrderId(10L)
                .routeProcessId(202L)
                .processId(302L)
                .sort(2)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100.000000"))
                .reportedQuantity(BigDecimal.ZERO.setScale(6))
                .remainingQuantity(new BigDecimal("100.000000"))
                .build();

        when(scheduleOrderService.getScheduleOrderPage(any()))
                .thenReturn(new PageResult<>(List.of(scheduleOrder), 1L));
        when(itemService.getItemMap(Set.of(30L)))
                .thenReturn(Map.of(30L, MesMdItemDO.builder()
                        .id(30L)
                        .code("ITEM-001")
                        .name("产品")
                        .specification("S1")
                        .build()));
        when(routeService.getRouteMap(Set.of(40L)))
                .thenReturn(Map.of(40L, MesProRouteDO.builder()
                        .id(40L)
                        .code("ROUTE-001")
                        .name("工艺路线")
                        .build()));
        when(scheduleOrderService.getScheduleOrderProcessListByScheduleOrderIds(Set.of(10L)))
                .thenReturn(List.of(firstProcess, secondProcess));
        when(scheduleOrderService.calculateProcessProgressMetrics(10L, List.of(firstProcess, secondProcess)))
                .thenReturn(Map.of(
                        101L, new MesProScheduleOrderService.ProcessProgressMetrics(
                                new BigDecimal("25.000000"),
                                new BigDecimal("10.000000"),
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                new BigDecimal("25.000000"),
                                new BigDecimal("75.000000"),
                                new BigDecimal("25.000000")),
                        102L, new MesProScheduleOrderService.ProcessProgressMetrics(
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                new BigDecimal("5.000000"),
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                new BigDecimal("100.000000"),
                                BigDecimal.ZERO.setScale(6))));
        when(processService.getProcessMap(Set.of(301L, 302L)))
                .thenReturn(Map.of(
                        301L, MesProProcessDO.builder().id(301L).code("PROC-001").name("切割").build(),
                        302L, MesProProcessDO.builder().id(302L).code("PROC-002").name("打磨").build()));

        CommonResult<PageResult<MesProScheduleOrderRespVO>> response =
                controller.getScheduleOrderPage(new MesProScheduleOrderPageReqVO());

        MesProScheduleOrderRespVO vo = response.getData().getList().get(0);
        assertEquals(new BigDecimal("100.000000"), vo.getProgressPercent());
        assertEquals(new BigDecimal("100.000000"), vo.getCompletedQuantity());
        assertEquals(BigDecimal.ZERO.setScale(6), vo.getUncompletedQuantity());
        assertEquals(new BigDecimal("25.000000"), vo.getEffectiveCompletedQuantity());
        assertEquals(new BigDecimal("10.000000"), vo.getPendingApprovalQuantity());
        assertEquals(new BigDecimal("5.000000"), vo.getPendingInspectionQuantity());
        assertEquals(301L, vo.getCurrentProcessId());
        assertEquals(new BigDecimal("25.000000"), vo.getCurrentProcessProgressPercent());
    }

    @Test
    void getAdmissionDiff_delegatesToServiceAndUsesDedicatedPermission() throws NoSuchMethodException {
        MesProScheduleOrderAdmissionDiffPageRespVO serviceResult = new MesProScheduleOrderAdmissionDiffPageRespVO();
        serviceResult.setTotal(1L);
        serviceResult.setSummary(new MesProScheduleOrderAdmissionDiffSummaryRespVO());
        serviceResult.setList(List.of(new MesProScheduleOrderAdmissionDiffRespVO()));
        when(scheduleOrderService.getAdmissionDiff(any())).thenReturn(serviceResult);

        CommonResult<MesProScheduleOrderAdmissionDiffPageRespVO> response =
                controller.getAdmissionDiff(new MesProScheduleOrderAdmissionDiffPageReqVO());

        assertEquals(1L, response.getData().getTotal());
        verify(scheduleOrderService).getAdmissionDiff(any());
        PreAuthorize preAuthorize = MesProScheduleOrderController.class
                .getMethod("getAdmissionDiff", MesProScheduleOrderAdmissionDiffPageReqVO.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:admission-diff')", preAuthorize.value());
    }

    @Test
    void getProcessList_returnsPerProcessFeedbackHistoryAndSummary() {
        MesProScheduleOrderProcessDO cutting = MesProScheduleOrderProcessDO.builder()
                .id(701L)
                .scheduleOrderId(10L)
                .routeProcessId(801L)
                .processId(301L)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100.000000"))
                .build();
        MesProScheduleOrderProcessDO welding = MesProScheduleOrderProcessDO.builder()
                .id(702L)
                .scheduleOrderId(10L)
                .routeProcessId(802L)
                .processId(302L)
                .sort(2)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100.000000"))
                .build();
        MesProFeedbackDO firstFeedback = MesProFeedbackDO.builder()
                .id(901L)
                .code("FB-001")
                .scheduleOrderId(10L)
                .scheduleOrderProcessId(701L)
                .processId(301L)
                .feedbackTime(LocalDateTime.of(2026, 7, 1, 10, 30))
                .feedbackQuantity(new BigDecimal("30.000000"))
                .qualifiedQuantity(new BigDecimal("30.000000"))
                .unqualifiedQuantity(BigDecimal.ZERO.setScale(6))
                .feedbackUserId(1001L)
                .status(MesProFeedbackStatusEnum.FINISHED.getStatus())
                .build();
        MesProFeedbackDO secondFeedback = MesProFeedbackDO.builder()
                .id(902L)
                .code("FB-002")
                .scheduleOrderId(10L)
                .scheduleOrderProcessId(701L)
                .processId(301L)
                .feedbackTime(LocalDateTime.of(2026, 7, 2, 14, 20))
                .feedbackQuantity(new BigDecimal("25.000000"))
                .qualifiedQuantity(new BigDecimal("24.000000"))
                .unqualifiedQuantity(new BigDecimal("1.000000"))
                .feedbackUserId(1002L)
                .status(MesProFeedbackStatusEnum.APPROVING.getStatus())
                .build();
        AdminUserRespDTO zhangSan = new AdminUserRespDTO();
        zhangSan.setId(1001L);
        zhangSan.setNickname("张三");
        AdminUserRespDTO liSi = new AdminUserRespDTO();
        liSi.setId(1002L);
        liSi.setNickname("李四");

        when(scheduleOrderService.getScheduleOrderProcessList(10L)).thenReturn(List.of(cutting, welding));
        when(scheduleOrderService.calculateProcessProgressMetrics(10L, List.of(cutting, welding)))
                .thenReturn(Map.of(
                        701L, new MesProScheduleOrderService.ProcessProgressMetrics(
                                new BigDecimal("55.000000"),
                                new BigDecimal("25.000000"),
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                new BigDecimal("55.000000"),
                                new BigDecimal("45.000000"),
                                new BigDecimal("55.000000")),
                        702L, new MesProScheduleOrderService.ProcessProgressMetrics(
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                BigDecimal.ZERO.setScale(6),
                                new BigDecimal("100.000000"),
                                BigDecimal.ZERO.setScale(6))));
        when(processService.getProcessMap(Set.of(301L, 302L))).thenReturn(Map.of(
                301L, MesProProcessDO.builder().id(301L).code("PROC-CUT").name("切割").build(),
                302L, MesProProcessDO.builder().id(302L).code("PROC-WELD").name("焊接").build()));
        when(scheduleOrderService.getProgressFeedbackList(10L)).thenReturn(List.of(firstFeedback, secondFeedback));
        when(adminUserApi.getUserMap(Set.of(1001L, 1002L))).thenReturn(Map.of(1001L, zhangSan, 1002L, liSi));

        CommonResult<List<MesProScheduleOrderProcessRespVO>> response = controller.getProcessList(10L);

        MesProScheduleOrderProcessRespVO cuttingResp = response.getData().get(0);
        assertEquals(2, cuttingResp.getFeedbackCount());
        assertEquals(LocalDateTime.of(2026, 7, 2, 14, 20), cuttingResp.getLatestFeedbackTime());
        assertEquals(new BigDecimal("55.000000"), cuttingResp.getReportedQuantity());
        assertEquals(2, cuttingResp.getFeedbackHistoryList().size());
        assertEquals("FB-001", cuttingResp.getFeedbackHistoryList().get(0).getCode());
        assertEquals(new BigDecimal("30.000000"), cuttingResp.getFeedbackHistoryList().get(0).getFeedbackQuantity());
        assertEquals("张三", cuttingResp.getFeedbackHistoryList().get(0).getFeedbackUserNickname());
        assertEquals("已完成", cuttingResp.getFeedbackHistoryList().get(0).getStatusName());
        assertEquals("FB-002", cuttingResp.getFeedbackHistoryList().get(1).getCode());
        assertEquals("李四", cuttingResp.getFeedbackHistoryList().get(1).getFeedbackUserNickname());
        assertEquals("审批中", cuttingResp.getFeedbackHistoryList().get(1).getStatusName());

        MesProScheduleOrderProcessRespVO weldingResp = response.getData().get(1);
        assertEquals(0, weldingResp.getFeedbackCount());
        assertEquals(null, weldingResp.getLatestFeedbackTime());
        assertEquals(List.of(), weldingResp.getFeedbackHistoryList());
    }

    @Test
    void createFromWorkOrders_delegatesBatchRequestAndUsesCreatePermission() throws NoSuchMethodException {
        MesProScheduleOrderCreateFromWorkOrdersReqVO reqVO = new MesProScheduleOrderCreateFromWorkOrdersReqVO();
        reqVO.setWorkOrderIds(List.of(100L, 101L));
        when(scheduleOrderService.createFromWorkOrders(reqVO)).thenReturn(List.of(900L, 901L));

        CommonResult<List<Long>> response = controller.createFromWorkOrders(reqVO);

        assertEquals(List.of(900L, 901L), response.getData());
        verify(scheduleOrderService).createFromWorkOrders(reqVO);
        PreAuthorize preAuthorize = MesProScheduleOrderController.class
                .getMethod("createFromWorkOrders", MesProScheduleOrderCreateFromWorkOrdersReqVO.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:create')", preAuthorize.value());
    }

    @Test
    void preflight_delegatesToServiceAndUsesDedicatedPermission() throws NoSuchMethodException {
        MesProScheduleOrderPreflightRespVO serviceResult = new MesProScheduleOrderPreflightRespVO();
        serviceResult.setResult("PASS");
        serviceResult.setSummary(new MesProScheduleOrderPreflightSummaryRespVO());
        when(scheduleOrderService.preflight(any())).thenReturn(serviceResult);

        CommonResult<MesProScheduleOrderPreflightRespVO> response =
                controller.preflight(new MesProScheduleOrderPreflightReqVO());

        assertEquals("PASS", response.getData().getResult());
        verify(scheduleOrderService).preflight(any());
        PreAuthorize preAuthorize = MesProScheduleOrderController.class
                .getMethod("preflight", MesProScheduleOrderPreflightReqVO.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:preflight')", preAuthorize.value());
    }

    @Test
    void getProcessWipStatistics_returnsProcessOrderCountsAndUsesQueryPermission() throws NoSuchMethodException {
        MesProScheduleOrderProcessWipRespVO clean = MesProScheduleOrderProcessWipRespVO.builder()
                .processId(301L)
                .processCode("PROC-CLEAN")
                .processName("清洗")
                .wipOrderCount(5L)
                .scheduleOrderIds(List.of(10L, 11L, 12L, 13L, 14L))
                .build();
        when(scheduleOrderService.getProcessWipStatistics()).thenReturn(List.of(clean));

        CommonResult<List<MesProScheduleOrderProcessWipRespVO>> response = controller.getProcessWipStatistics();

        assertEquals(1, response.getData().size());
        assertEquals("清洗", response.getData().get(0).getProcessName());
        assertEquals(5L, response.getData().get(0).getWipOrderCount());
        assertEquals(List.of(10L, 11L, 12L, 13L, 14L), response.getData().get(0).getScheduleOrderIds());
        verify(scheduleOrderService).getProcessWipStatistics();
        PreAuthorize preAuthorize = MesProScheduleOrderController.class
                .getMethod("getProcessWipStatistics")
                .getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:query')", preAuthorize.value());
    }

    @Test
    void exportScheduleOrderExcel_usesExportPermissionAndAccessLog() throws NoSuchMethodException {
        var method = MesProScheduleOrderController.class
                .getMethod("exportScheduleOrderExcel", MesProScheduleOrderPageReqVO.class, HttpServletResponse.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:export')", preAuthorize.value());
        ApiAccessLog accessLog = method.getAnnotation(ApiAccessLog.class);
        assertNotNull(accessLog);
        assertArrayEquals(new cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum[]{EXPORT},
                accessLog.operateType());
    }

    @Test
    void manualFinish_delegatesToServiceAndUsesDedicatedPermission() throws NoSuchMethodException {
        MesProScheduleOrderActionReqVO reqVO = new MesProScheduleOrderActionReqVO();
        reqVO.setId(10L);
        reqVO.setReason("排产员确认完成");

        CommonResult<Boolean> response = controller.manualFinish(reqVO);

        assertEquals(Boolean.TRUE, response.getData());
        verify(scheduleOrderService).manualFinish(reqVO);
        PreAuthorize preAuthorize = MesProScheduleOrderController.class
                .getMethod("manualFinish", MesProScheduleOrderActionReqVO.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:manual-finish')", preAuthorize.value());
    }

    @Test
    void revokeManualFinish_delegatesToServiceAndUsesDedicatedPermission() throws NoSuchMethodException {
        MesProScheduleOrderActionReqVO reqVO = new MesProScheduleOrderActionReqVO();
        reqVO.setId(10L);
        reqVO.setReason("管理员撤销误操作");

        CommonResult<Boolean> response = controller.revokeManualFinish(reqVO);

        assertEquals(Boolean.TRUE, response.getData());
        verify(scheduleOrderService).revokeManualFinish(reqVO);
        PreAuthorize preAuthorize = MesProScheduleOrderController.class
                .getMethod("revokeManualFinish", MesProScheduleOrderActionReqVO.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:revoke-complete')", preAuthorize.value());
    }

    @Test
    void exportScheduleOrderExcel_usesSelectedColumnsAllRowsAndExportPermission() throws Exception {
        MesProScheduleOrderPageReqVO reqVO = new MesProScheduleOrderPageReqVO();
        reqVO.setCode("SCH-EXPORT");
        reqVO.setExportColumns(List.of("erpWorkOrderCode", "productCode", "quantityProgress"));
        MesProScheduleOrderDO scheduleOrder = MesProScheduleOrderDO.builder()
                .id(10L)
                .code("SCH-EXPORT-001")
                .workOrderId(20L)
                .erpWorkOrderCode("WO-EXPORT-001")
                .productId(30L)
                .quantity(new BigDecimal("10.000000"))
                .promiseDate(LocalDate.of(2026, 7, 8))
                .build();
        when(scheduleOrderService.getScheduleOrderPage(reqVO))
                .thenReturn(new PageResult<>(List.of(scheduleOrder), 1L));
        when(itemService.getItemMap(Set.of(30L)))
                .thenReturn(Map.of(30L, MesMdItemDO.builder()
                        .id(30L).code("ITEM-EXPORT").name("导出产品").specification("S1").build()));
        when(routeService.getRouteMap(Set.of())).thenReturn(Map.of());
        when(productionMaterialListMapper.selectListByWorkOrderIds(Set.of(20L))).thenReturn(List.of());
        when(scheduleOrderService.getScheduleOrderProcessListByScheduleOrderIds(Set.of(10L))).thenReturn(List.of());
        when(scheduleOrderService.calculateProcessAggregateProgressSummary(new BigDecimal("10.000000"), List.of()))
                .thenReturn(new MesProScheduleOrderService.ProgressSummary(
                        new BigDecimal("10.000000"),
                        BigDecimal.ZERO.setScale(6),
                        new BigDecimal("10.000000"),
                        BigDecimal.ZERO.setScale(6)));
        when(processService.getProcessMap(Set.of())).thenReturn(Map.of());

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportScheduleOrderExcel(reqVO, response);

        assertEquals(PageParam.PAGE_SIZE_NONE, reqVO.getPageSize());
        assertEquals("application/vnd.ms-excel;charset=UTF-8", response.getContentType());
        assertNotNull(response.getHeader("Content-Disposition"));
        verify(scheduleOrderService).getScheduleOrderPage(reqVO);
        PreAuthorize preAuthorize = MesProScheduleOrderController.class
                .getMethod("exportScheduleOrderExcel", MesProScheduleOrderPageReqVO.class, jakarta.servlet.http.HttpServletResponse.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-schedule-order:export')", preAuthorize.value());
    }

    @Test
    void exportScheduleOrderExcel_rejectsUnsupportedColumn() {
        MesProScheduleOrderPageReqVO reqVO = new MesProScheduleOrderPageReqVO();
        reqVO.setExportColumns(List.of("erpWorkOrderCode", "notAllowed"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> controller.exportScheduleOrderExcel(reqVO, new MockHttpServletResponse()));

        assertEquals("排产工单导出列不支持: notAllowed", exception.getMessage());
    }

    @Test
    void exportScheduleOrderExcel_rejectsEmptyColumnList() {
        MesProScheduleOrderPageReqVO reqVO = new MesProScheduleOrderPageReqVO();
        reqVO.setExportColumns(List.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> controller.exportScheduleOrderExcel(reqVO, new MockHttpServletResponse()));

        assertEquals("请至少选择一个导出列", exception.getMessage());
    }

    @Test
    void exportScheduleOrderExcel_rejectsBlankColumn() {
        MesProScheduleOrderPageReqVO reqVO = new MesProScheduleOrderPageReqVO();
        reqVO.setExportColumns(List.of("erpWorkOrderCode", " "));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> controller.exportScheduleOrderExcel(reqVO, new MockHttpServletResponse()));

        assertEquals("排产工单导出列不能为空", exception.getMessage());
    }
}
