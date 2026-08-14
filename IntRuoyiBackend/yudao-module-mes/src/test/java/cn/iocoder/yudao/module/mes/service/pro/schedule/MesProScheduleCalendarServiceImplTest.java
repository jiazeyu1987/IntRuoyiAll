package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarCapacityGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarRulesSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarSimulationAdvanceReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.holiday.MesCalHolidayDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.cal.plan.MesCalPlanShiftDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdProductionLineDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkshopDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdWorkstationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.workstation.MesMdProductionLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.cal.plan.MesCalPlanShiftMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProCapacityPlanMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanExplanationSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleCalendarSimulationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionMaterialListMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.service.cal.holiday.MesCalHolidayService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanService;
import cn.iocoder.yudao.module.mes.service.cal.plan.MesCalPlanShiftService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityMetrics;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationCapacityService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationMachineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkshopService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.component.ScheduleDefaultCompatibilityPolicy;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Lazy;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

@ExtendWith(MockitoExtension.class)
class MesProScheduleCalendarServiceImplTest {

    @InjectMocks
    private MesProScheduleCalendarServiceImpl service;

    @Mock
    private MesProScheduleCalendarRuleMapper ruleMapper;
    @Mock
    private MesProScheduleCalendarSimulationMapper simulationMapper;
    @Mock
    private MesProCapacityPlanMapper capacityPlanMapper;
    @Mock
    private MesCalHolidayService holidayService;
    @Mock
    private MesProTaskMapper taskMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Mock
    private MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesKingdeeProductionMaterialListMapper productionMaterialListMapper;
    @Mock
    private MesMdWorkstationMapper workstationMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesMdProductionLineMapper productionLineMapper;
    @Mock
    private MesWmMaterialStockMapper materialStockMapper;
    @Mock
    private MesMdProductionLineService productionLineService;
    @Mock
    private MesMdWorkstationCapacityService workstationCapacityService;
    @Mock
    private MesMdWorkstationMachineService workstationMachineService;
    @Mock
    private MesMdWorkshopService workshopService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesCalPlanService planService;
    @Mock
    private MesCalPlanShiftService planShiftService;
    @Mock
    private MesCalPlanShiftMapper planShiftMapper;
    @Spy
    private ScheduleDefaultCompatibilityPolicy scheduleDefaultCompatibilityPolicy = new ScheduleDefaultCompatibilityPolicy();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        lenient().when(routeProcessService.getProcessIdentityMap(anyCollection()))
                .thenAnswer(invocation -> identityMap(invocation.getArgument(0)));
        lenient().when(routeService.getRouteMapIgnoreDeleted(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());
        lenient().when(feedbackMapper.selectListByTaskIds(anyCollection())).thenReturn(Collections.emptyList());
        lenient().when(workstationMachineService.getWorkstationMachineListByWorkstationIds(anyCollection())).thenReturn(Collections.emptyList());
        lenient().when(workstationCapacityService.getCapacityMetrics(anyCollection(), any()))
                .thenReturn(Collections.emptyMap());
        lenient().when(productionLineMapper.selectListByIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> lineIds = invocation.getArgument(0);
            if (lineIds == null || lineIds.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, MesMdProductionLineDO> lineMap = productionLineService.getProductionLineMap(lineIds);
            if (lineMap == null || lineMap.isEmpty()) {
                return Collections.emptyList();
            }
            return lineIds.stream()
                    .map(lineMap::get)
                    .filter(Objects::nonNull)
                    .toList();
        });
        lenient().when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(anyCollection())).thenAnswer(invocation -> {
            Collection<Long> workOrderIds = invocation.getArgument(0);
            if (workOrderIds == null || workOrderIds.isEmpty()) {
                return Collections.emptyList();
            }
            List<MesProScheduleOrderDO> orders = new ArrayList<>();
            for (Long workOrderId : workOrderIds) {
                orders.add(MesProScheduleOrderDO.builder()
                        .id(workOrderId + 1000L)
                        .workOrderId(workOrderId)
                        .build());
            }
            return orders;
        });
    }

    private Map<Long, Long> identityMap(java.util.Collection<Long> processIds) {
        Map<Long, Long> result = new java.util.LinkedHashMap<>();
        processIds.stream().filter(java.util.Objects::nonNull).forEach(id -> result.put(id, id));
        return result;
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void getRules_shouldInitDefaultsWhenMissing() {
        when(ruleMapper.selectByTenantId(1L)).thenReturn(null);
        when(simulationMapper.selectByTenantId(1L)).thenReturn(null);

        var rules = service.getRules();

        assertFalse(rules.getSkipStatutoryHolidays());
        assertEquals("SINGLE", rules.getWeekendRestMode());
        assertTrue(rules.getDateShiftModeByDate().isEmpty());
        assertFalse(rules.getTemporaryFreezeEnabled());
        assertNotNull(rules.getSimulationCurrentDate());
        verify(ruleMapper).insert(any(MesProScheduleCalendarRuleDO.class));
        verify(simulationMapper).insert(any(MesProScheduleCalendarSimulationDO.class));
    }

    @Test
    void getRules_shouldSyncPastSimulationDateToToday() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.FALSE)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .build();
        MesProScheduleCalendarSimulationDO simulation = MesProScheduleCalendarSimulationDO.builder()
                .id(11L)
                .currentDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);
        when(simulationMapper.selectByTenantId(1L)).thenReturn(simulation);

        var rules = service.getRules();

        assertEquals(LocalDate.now().toString(), rules.getSimulationCurrentDate());
        ArgumentCaptor<MesProScheduleCalendarSimulationDO> captor =
                ArgumentCaptor.forClass(MesProScheduleCalendarSimulationDO.class);
        verify(simulationMapper).updateById(captor.capture());
        assertEquals(LocalDate.now(), captor.getValue().getCurrentDate().toLocalDate());
    }

    @Test
    void saveRules_shouldPersistNormalizedValues() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.TRUE)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .build();
        MesProScheduleCalendarSimulationDO simulation = MesProScheduleCalendarSimulationDO.builder()
                .id(11L)
                .currentDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);
        when(simulationMapper.selectByTenantId(1L)).thenReturn(simulation);

        MesProScheduleCalendarRulesSaveReqVO reqVO = new MesProScheduleCalendarRulesSaveReqVO();
        reqVO.setSkipStatutoryHolidays(true);
        reqVO.setWeekendRestMode("single");
        reqVO.setDateShiftModeByDate(Map.of("2026-05-20", "day", "2026-05-21", "rest"));

        var resp = service.saveRules(reqVO);

        assertEquals(10L, resp.getId());
        assertTrue(resp.getSkipStatutoryHolidays());
        assertTrue(resp.getTemporaryFreezeEnabled());
        assertEquals("SINGLE", resp.getWeekendRestMode());
        assertEquals("DAY", resp.getDateShiftModeByDate().get("2026-05-20"));
        assertEquals("REST", resp.getDateShiftModeByDate().get("2026-05-21"));
        ArgumentCaptor<MesProScheduleCalendarRuleDO> captor = ArgumentCaptor.forClass(MesProScheduleCalendarRuleDO.class);
        verify(ruleMapper).updateById(captor.capture());
        assertTrue(captor.getValue().getDateShiftModeByDateJson().contains("2026-05-20"));
        assertTrue(captor.getValue().getDateShiftModeByDateJson().contains("2026-05-21"));
        assertTrue(captor.getValue().getTemporaryFreezeEnabled());
    }

    @Test
    void saveRules_shouldRejectNightOrBothDateShiftMode() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.FALSE)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);

        MesProScheduleCalendarRulesSaveReqVO nightReq = new MesProScheduleCalendarRulesSaveReqVO();
        nightReq.setSkipStatutoryHolidays(true);
        nightReq.setWeekendRestMode("DOUBLE");
        nightReq.setDateShiftModeByDate(Map.of("2026-05-20", "NIGHT"));

        ServiceException nightEx = assertThrows(ServiceException.class, () -> service.saveRules(nightReq));

        assertEquals(PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE.getCode(), nightEx.getCode());

        MesProScheduleCalendarRulesSaveReqVO bothReq = new MesProScheduleCalendarRulesSaveReqVO();
        bothReq.setSkipStatutoryHolidays(true);
        bothReq.setWeekendRestMode("DOUBLE");
        bothReq.setDateShiftModeByDate(Map.of("2026-05-20", "BOTH"));

        ServiceException bothEx = assertThrows(ServiceException.class, () -> service.saveRules(bothReq));

        assertEquals(PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE.getCode(), bothEx.getCode());
        verify(ruleMapper, never()).updateById(any(MesProScheduleCalendarRuleDO.class));
    }

    @Test
    void getRules_shouldRejectLegacyNightOrBothDateShiftMode() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.FALSE)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{\"2026-05-20\":\"NIGHT\"}")
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);

        ServiceException nightEx = assertThrows(ServiceException.class, () -> service.getRules());

        assertEquals(PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE.getCode(), nightEx.getCode());

        rule.setDateShiftModeByDateJson("{\"2026-05-20\":\"BOTH\"}");
        ServiceException bothEx = assertThrows(ServiceException.class, () -> service.getRules());

        assertEquals(PRO_SCHEDULE_CALENDAR_INVALID_DATE_SHIFT_MODE.getCode(), bothEx.getCode());
    }

    @Test
    void saveRules_shouldRejectInvalidCalendarDate() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.FALSE)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .build();
        MesProScheduleCalendarSimulationDO simulation = MesProScheduleCalendarSimulationDO.builder()
                .id(11L)
                .currentDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);

        MesProScheduleCalendarRulesSaveReqVO reqVO = new MesProScheduleCalendarRulesSaveReqVO();
        reqVO.setSkipStatutoryHolidays(true);
        reqVO.setWeekendRestMode("DOUBLE");
        reqVO.setDateShiftModeByDate(Map.of("2026-02-31", "DAY"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveRules(reqVO));

        assertEquals(PRO_SCHEDULE_CALENDAR_INVALID_DATE.getCode(), ex.getCode());
    }

    @Test
    void advanceSimulationDays_shouldMoveDateForward() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.FALSE)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .build();
        MesProScheduleCalendarSimulationDO simulation = MesProScheduleCalendarSimulationDO.builder()
                .id(11L)
                .currentDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);
        when(simulationMapper.selectByTenantId(1L)).thenReturn(simulation);

        MesProScheduleCalendarSimulationAdvanceReqVO reqVO = new MesProScheduleCalendarSimulationAdvanceReqVO();
        reqVO.setDays(30);

        var resp = service.advanceSimulationDays(reqVO);

        assertEquals(LocalDate.now().plusDays(30).toString(), resp.getSimulationCurrentDate());
        verify(simulationMapper, atLeastOnce()).updateById(any(MesProScheduleCalendarSimulationDO.class));
    }

    @Test
    void refreshPlanCapacityForShiftHours_shouldUpdatePrimaryShiftWindowAndFutureCapacityPlans() {
        MesProScheduleCalendarSimulationDO simulation = MesProScheduleCalendarSimulationDO.builder()
                .id(11L)
                .currentDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        MesMdProductionLineDO line = MesMdProductionLineDO.builder()
                .id(600L)
                .code("LINE-01")
                .name("Line 01")
                .calendarPlanId(800L)
                .build();
        MesCalPlanShiftDO shift = MesCalPlanShiftDO.builder()
                .id(801L)
                .planId(800L)
                .sort(1)
                .name("Day")
                .startTime("08:00")
                .endTime("16:00")
                .build();
        when(simulationMapper.selectByTenantId(1L)).thenReturn(simulation);
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(line));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(shift));

        service.refreshPlanCapacityForShiftHours(new BigDecimal("10.50"));

        verify(planShiftMapper).updateEndTimeById(801L, "18:30");
        verify(capacityPlanMapper).updateCapacityMinutesByLineAndShiftFromDate(
                600L, 801L, LocalDate.now().atStartOfDay(), 630);
    }

    @Test
    void refreshPlanCapacityForShiftHours_shouldUpdateEveryShiftOnceAndEveryLineCapacity() {
        MesProScheduleCalendarSimulationDO simulation = MesProScheduleCalendarSimulationDO.builder()
                .id(11L)
                .currentDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        MesMdProductionLineDO firstLine = MesMdProductionLineDO.builder()
                .id(600L)
                .code("LINE-01")
                .name("Line 01")
                .calendarPlanId(800L)
                .build();
        MesMdProductionLineDO secondLine = MesMdProductionLineDO.builder()
                .id(601L)
                .code("LINE-02")
                .name("Line 02")
                .calendarPlanId(800L)
                .build();
        MesCalPlanShiftDO dayShift = MesCalPlanShiftDO.builder()
                .id(801L)
                .planId(800L)
                .sort(1)
                .name("Day")
                .startTime("08:00")
                .endTime("14:00")
                .build();
        MesCalPlanShiftDO nightShift = MesCalPlanShiftDO.builder()
                .id(802L)
                .planId(800L)
                .sort(2)
                .name("Night")
                .startTime("20:00")
                .endTime("02:00")
                .build();
        when(simulationMapper.selectByTenantId(1L)).thenReturn(simulation);
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(firstLine, secondLine));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(dayShift, nightShift));

        service.refreshPlanCapacityForShiftHours(new BigDecimal("8"));

        verify(planShiftMapper).updateEndTimeById(801L, "16:00");
        verify(planShiftMapper).updateEndTimeById(802L, "04:00");
        verify(planShiftMapper, times(2)).updateEndTimeById(anyLong(), anyString());
        verify(capacityPlanMapper).updateCapacityMinutesByLineAndShiftFromDate(
                600L, 801L, LocalDate.now().atStartOfDay(), 480);
        verify(capacityPlanMapper).updateCapacityMinutesByLineAndShiftFromDate(
                600L, 802L, LocalDate.now().atStartOfDay(), 480);
        verify(capacityPlanMapper).updateCapacityMinutesByLineAndShiftFromDate(
                601L, 801L, LocalDate.now().atStartOfDay(), 480);
        verify(capacityPlanMapper).updateCapacityMinutesByLineAndShiftFromDate(
                601L, 802L, LocalDate.now().atStartOfDay(), 480);
    }

    @Test
    void circularReferenceProneCollaborators_shouldUseLazyInjection() throws NoSuchFieldException {
        assertLazyInjected("workOrderService");
        assertLazyInjected("routeService");
    }

    @Test
    void getMonth_shouldAggregateDaySummary() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        MesMdItemDO materialItem = new MesMdItemDO();
        materialItem.setId(501L);
        materialItem.setCode("MAT-01");
        materialItem.setName("Material 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem, 501L, materialItem));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, new BigDecimal("5")))));
        when(materialStockMapper.selectListByItemIds(anyCollection())).thenReturn(List.of(
                MesWmMaterialStockDO.builder().id(920L).itemId(501L).quantity(new BigDecimal("3")).frozen(Boolean.FALSE).build()
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                        .shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(List.of(
                MesProScheduleIssueDO.builder()
                        .id(960L)
                        .issueType("FEEDBACK")
                        .severity("BLOCKING")
                        .workOrderId(200L)
                        .taskId(100L)
                        .calendarDate(LocalDateTime.of(2026, 5, 13, 10, 0))
                        .message("设备停机")
                        .status("OPEN")
                        .resolved(Boolean.FALSE)
                        .sourceType("FEEDBACK")
                        .sourceId(910L)
                        .build()));

        var resp = service.getMonth("2026-05");

        var day = resp.getDays().stream().filter(item -> "2026-05-13".equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(1, day.getTotalTaskCount());
        assertEquals(1, day.getTotalOrderCount());
        assertEquals(1, day.getDayShiftTaskCount());
        assertEquals(1, day.getShortageCount());
        assertTrue(resp.getCurrentScheduleStatus().getHasCurrentSchedule());
    }

    @Test
    void getMonth_shouldCountChineseSortThreeShiftAsNightShift() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-NIGHT")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 20, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 20, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L,
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        MesMdItemDO materialItem = new MesMdItemDO();
        materialItem.setId(501L);
        materialItem.setCode("MAT-01");
        materialItem.setName("Material 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem, 501L, materialItem));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(materialStockMapper.selectListByItemIds(anyCollection())).thenReturn(List.of(
                MesWmMaterialStockDO.builder().id(920L).itemId(501L).quantity(BigDecimal.TEN).frozen(Boolean.FALSE).build()
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("白班").startTime("08:00").endTime("16:00").build(),
                MesCalPlanShiftDO.builder().id(802L).planId(800L).sort(2).name("中班").startTime("16:00").endTime("20:00").build(),
                MesCalPlanShiftDO.builder().id(803L).planId(800L).sort(3).name("夜班").startTime("20:00").endTime("08:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                        .shiftId(803L).capacityMinutes(720).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                        .shiftId(803L).capacityMinutes(720).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth("2026-05");

        var day = resp.getDays().stream().filter(item -> "2026-05-13".equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(0, day.getDayShiftTaskCount());
        assertEquals(1, day.getNightShiftTaskCount());
    }

    @Test
    void getMonth_shouldCountLateTaskInWideSingleDayShiftAsNightShift() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-LATE")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 20, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 23, 30))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 20, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L,
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        MesMdItemDO materialItem = new MesMdItemDO();
        materialItem.setId(501L);
        materialItem.setCode("MAT-01");
        materialItem.setName("Material 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem, 501L, materialItem));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(materialStockMapper.selectListByItemIds(anyCollection())).thenReturn(List.of(
                MesWmMaterialStockDO.builder().id(920L).itemId(501L).quantity(BigDecimal.TEN).frozen(Boolean.FALSE).build()
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("白班").startTime("08:00").endTime("23:30").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                        .shiftId(801L).capacityMinutes(930).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth("2026-05");

        var day = resp.getDays().stream().filter(item -> "2026-05-13".equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(0, day.getDayShiftTaskCount());
        assertEquals(1, day.getNightShiftTaskCount());
    }

    @Test
    void getMonth_shouldOnlyCountTasksWithEffectiveScheduleOrder() {
        stubRuleAndSimulation();
        LocalDate today = LocalDate.now();
        MesProTaskDO activeTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-ACTIVE")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(today.atTime(8, 0))
                .endTime(today.atTime(16, 0))
                .build();
        activeTask.setUpdateTime(today.atTime(9, 0));
        MesProTaskDO staleTask = MesProTaskDO.builder()
                .id(101L)
                .code("PT-STALE")
                .workOrderId(201L)
                .workstationId(301L)
                .processId(401L)
                .itemId(501L)
                .quantity(BigDecimal.ONE)
                .startTime(today.atTime(10, 0))
                .endTime(today.atTime(18, 0))
                .build();
        staleTask.setUpdateTime(today.atTime(10, 30));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(activeTask, staleTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(2L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(staleTask);
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(anyCollection())).thenReturn(List.of(
                MesProScheduleOrderDO.builder().id(900L).workOrderId(200L).build()
        ));
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-ACTIVE").productId(500L).quantity(BigDecimal.ONE).build()
        ));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(502L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()
        ));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(
                600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()
        ));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(
                700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()
        ));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()
        ));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()
        ));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(today.atStartOfDay())
                        .shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(List.of(
                MesProScheduleIssueDO.builder()
                        .id(960L)
                        .issueType("FEEDBACK")
                        .severity("BLOCKING")
                        .workOrderId(200L)
                        .taskId(100L)
                        .calendarDate(LocalDateTime.of(2026, 5, 13, 10, 0))
                        .message("设备停机")
                        .status("OPEN")
                        .resolved(Boolean.FALSE)
                        .sourceType("FEEDBACK")
                        .sourceId(910L)
                        .build()));

        var resp = service.getMonth(today.toString().substring(0, 7));

        var day = resp.getDays().stream().filter(item -> today.toString().equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(1, day.getTotalTaskCount());
        assertEquals(1, day.getTotalOrderCount());
        assertEquals(1, resp.getCurrentScheduleStatus().getTotalTaskCount());
        assertEquals(today + " 09:00:00", resp.getCurrentScheduleStatus().getUpdatedAt());
    }

    @Test
    void getMonth_shouldIgnoreFinishedScheduleOrdersInCurrentCalendarContext() {
        stubRuleAndSimulation();
        LocalDate today = LocalDate.now();
        MesProTaskDO activeTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-ACTIVE")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(today.atTime(8, 0))
                .endTime(today.atTime(16, 0))
                .build();
        activeTask.setUpdateTime(today.atTime(9, 0));
        MesProTaskDO finishedTask = MesProTaskDO.builder()
                .id(101L)
                .code("PT-FINISHED")
                .workOrderId(201L)
                .workstationId(301L)
                .processId(401L)
                .itemId(501L)
                .quantity(BigDecimal.ONE)
                .startTime(today.atTime(10, 0))
                .endTime(today.atTime(18, 0))
                .build();
        finishedTask.setUpdateTime(today.atTime(10, 30));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(activeTask, finishedTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(2L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(finishedTask);
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(anyCollection())).thenReturn(List.of(
                MesProScheduleOrderDO.builder()
                        .id(900L)
                        .workOrderId(200L)
                        .status(MesProScheduleOrderStatusEnum.PREPARE.getStatus())
                        .build(),
                MesProScheduleOrderDO.builder()
                        .id(901L)
                        .workOrderId(201L)
                        .status(MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                        .build()
        ));
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-ACTIVE").productId(500L).quantity(BigDecimal.ONE).build()
        ));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(502L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()
        ));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(
                600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()
        ));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(
                700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()
        ));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()
        ));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()
        ));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(today.atStartOfDay())
                        .shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth(today.toString().substring(0, 7));

        var day = resp.getDays().stream().filter(item -> today.toString().equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(1, day.getTotalTaskCount());
        assertEquals(1, day.getTotalOrderCount());
        assertEquals(1, resp.getCurrentScheduleStatus().getTotalTaskCount());
        assertEquals(today + " 09:00:00", resp.getCurrentScheduleStatus().getUpdatedAt());
    }

    @Test
    void getDayDetail_shouldExcludeEffectiveTasksOutsideLatestReplanScope() {
        stubRuleAndSimulation();
        LocalDate day = LocalDate.of(2026, 7, 27);
        MesProTaskDO scopedTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-LATEST")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(new BigDecimal("100"))
                .startTime(day.atTime(8, 0))
                .endTime(day.atTime(16, 0))
                .build();
        scopedTask.setUpdateTime(day.atTime(9, 0));
        MesProTaskDO outsideTask = MesProTaskDO.builder()
                .id(101L)
                .code("PT-OUTSIDE")
                .workOrderId(201L)
                .workstationId(301L)
                .processId(401L)
                .itemId(501L)
                .quantity(new BigDecimal("12"))
                .startTime(day.atTime(8, 0))
                .endTime(day.atTime(16, 0))
                .build();
        outsideTask.setUpdateTime(day.atTime(10, 0));
        when(replanExplanationSnapshotMapper.selectLatest()).thenReturn(MesProReplanExplanationSnapshotDO.builder()
                .id(31L)
                .snapshotJson("{\"orders\":[{\"scheduleOrderId\":900}]}")
                .build());
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(scopedTask, outsideTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(2L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(outsideTask);
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(anyCollection())).thenReturn(List.of(
                MesProScheduleOrderDO.builder()
                        .id(900L)
                        .workOrderId(200L)
                        .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                        .build(),
                MesProScheduleOrderDO.builder()
                        .id(901L)
                        .workOrderId(201L)
                        .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                        .build()
        ));
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-LATEST").productId(500L).quantity(new BigDecimal("100")).build(),
                201L, MesProWorkOrderDO.builder().id(201L).code("WO-OUTSIDE").productId(501L).quantity(new BigDecimal("12")).build()
        ));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(
                        200L, Map.of(502L, BigDecimal.ONE),
                        201L, Map.of(503L, BigDecimal.ONE)
                )));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build(),
                MesMdWorkstationDO.builder().id(301L).workshopId(700L).productionLineId(600L).build()
        ));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(
                600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()
        ));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(
                700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()
        ));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(
                400L, MesProProcessDO.builder().id(400L).name("Latest Process").build(),
                401L, MesProProcessDO.builder().id(401L).name("Outside Process").build()
        ));
        MesMdItemDO scopedItem = new MesMdItemDO();
        scopedItem.setId(500L);
        scopedItem.setCode("ITEM-LATEST");
        scopedItem.setName("Latest Item");
        MesMdItemDO outsideItem = new MesMdItemDO();
        outsideItem.setId(501L);
        outsideItem.setCode("ITEM-OUTSIDE");
        outsideItem.setName("Outside Item");
        MesMdItemDO scopedMaterial = new MesMdItemDO();
        scopedMaterial.setId(502L);
        scopedMaterial.setCode("MAT-LATEST");
        scopedMaterial.setName("Latest Material");
        MesMdItemDO outsideMaterial = new MesMdItemDO();
        outsideMaterial.setId(503L);
        outsideMaterial.setCode("MAT-OUTSIDE");
        outsideMaterial.setName("Outside Material");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(
                500L, scopedItem,
                501L, outsideItem,
                502L, scopedMaterial,
                503L, outsideMaterial
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleOrderId(900L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build(),
                MesProTaskScheduleExtDO.builder().taskId(101L).scheduleOrderId(901L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()
        ));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()
        ));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(day.atStartOfDay())
                        .shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-07-27");

        var task = resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0);
        assertEquals(1, resp.getWorkshops().get(0).getTaskCount());
        assertEquals(1, resp.getWorkshops().get(0).getLines().get(0).getTaskCount());
        assertEquals(0, new BigDecimal("100").compareTo(task.getQuantity()));
        assertEquals("PT-LATEST", task.getTaskCode());
        assertEquals("WO-LATEST", task.getWorkOrderCode());
    }

    @Test
    void generateCapacityPlans_shouldCreateMissingWorkingDayCapacityRows() {
        stubCapacityGenerationRule();
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(
                MesMdProductionLineDO.builder()
                        .id(600L).code("LINE-01").name("Line 01").calendarPlanId(800L).status(0).build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("AUTO-DAY").startTime("08:00").endTime("20:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDateRange(anyCollection(), any(), any())).thenReturn(Collections.emptyList());

        MesProScheduleCalendarCapacityGenerateReqVO reqVO = new MesProScheduleCalendarCapacityGenerateReqVO();
        reqVO.setStartDate("2026-06-08");
        reqVO.setDays(1);

        var resp = service.generateCapacityPlans(reqVO);

        assertEquals("2026-06-08", resp.getStartDate());
        assertEquals("2026-06-08", resp.getEndDate());
        assertEquals(1, resp.getLineCount());
        assertEquals(1, resp.getGeneratedCount());
        assertEquals(0, resp.getSkippedExistingCount());
        assertEquals(0, resp.getSkippedRestCount());
        ArgumentCaptor<MesProCapacityPlanDO> captor = ArgumentCaptor.forClass(MesProCapacityPlanDO.class);
        verify(capacityPlanMapper).insert(captor.capture());
        assertEquals(600L, captor.getValue().getLineId());
        assertEquals(801L, captor.getValue().getShiftId());
        assertEquals(LocalDateTime.of(2026, 6, 8, 0, 0), captor.getValue().getCalendarDate());
        assertEquals(720, captor.getValue().getCapacityMinutes());
        assertTrue(captor.getValue().getEnabled());
    }

    @Test
    void generateCapacityPlans_shouldCreateConfiguredNightShiftCapacityOnWorkingDate() {
        stubCapacityGenerationRule();
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(
                MesMdProductionLineDO.builder()
                        .id(600L).code("LINE-01").name("Line 01").calendarPlanId(800L).status(0).build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("白班")
                        .startTime("08:00").endTime("20:00").build(),
                MesCalPlanShiftDO.builder().id(803L).planId(800L).sort(3).name("夜班")
                        .startTime("20:00").endTime("08:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDateRange(anyCollection(), any(), any()))
                .thenReturn(Collections.emptyList());

        MesProScheduleCalendarCapacityGenerateReqVO reqVO = new MesProScheduleCalendarCapacityGenerateReqVO();
        reqVO.setStartDate("2026-06-08");
        reqVO.setDays(1);

        var resp = service.generateCapacityPlans(reqVO);

        assertEquals(2, resp.getGeneratedCount());
        ArgumentCaptor<MesProCapacityPlanDO> captor = ArgumentCaptor.forClass(MesProCapacityPlanDO.class);
        verify(capacityPlanMapper, times(2)).insert(captor.capture());
        MesProCapacityPlanDO nightCapacity = captor.getAllValues().stream()
                .filter(capacity -> Long.valueOf(803L).equals(capacity.getShiftId()))
                .findFirst()
                .orElseThrow();
        assertEquals(LocalDateTime.of(2026, 6, 8, 0, 0), nightCapacity.getCalendarDate());
        assertEquals(720, nightCapacity.getCapacityMinutes());
        assertTrue(nightCapacity.getEnabled());
    }

    @Test
    void generateCapacityPlans_shouldSkipExistingCapacityRowsWithoutOverwrite() {
        stubCapacityGenerationRule();
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(
                MesMdProductionLineDO.builder()
                        .id(600L).code("LINE-01").name("Line 01").calendarPlanId(800L).status(0).build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("AUTO-DAY").startTime("08:00").endTime("20:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDateRange(anyCollection(), any(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder()
                        .id(950L).lineId(600L).shiftId(801L)
                        .calendarDate(LocalDateTime.of(2026, 6, 8, 0, 0))
                        .capacityMinutes(360).enabled(false).build()));

        MesProScheduleCalendarCapacityGenerateReqVO reqVO = new MesProScheduleCalendarCapacityGenerateReqVO();
        reqVO.setStartDate("2026-06-08");
        reqVO.setDays(1);

        var resp = service.generateCapacityPlans(reqVO);

        assertEquals(0, resp.getGeneratedCount());
        assertEquals(1, resp.getSkippedExistingCount());
        assertEquals(1, resp.getSkippedDetails().size());
        assertEquals("2026-06-08", resp.getSkippedDetails().get(0).getDate());
        assertEquals("LINE-01", resp.getSkippedDetails().get(0).getLineCode());
        assertEquals("AUTO-DAY", resp.getSkippedDetails().get(0).getShiftName());
        assertEquals("EXISTING_CAPACITY", resp.getSkippedDetails().get(0).getReasonCode());
        verify(capacityPlanMapper, never()).insert(any(MesProCapacityPlanDO.class));
    }

    @Test
    void generateCapacityPlans_shouldSkipRestDates() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.FALSE)
                .weekendRestMode("NONE")
                .dateShiftModeByDateJson("{\"2026-06-08\":\"REST\"}")
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);
        when(holidayService.getHolidayList(any(), any())).thenReturn(Collections.emptyList());
        when(productionLineMapper.selectListByStatus(0)).thenReturn(List.of(
                MesMdProductionLineDO.builder()
                        .id(600L).code("LINE-01").name("Line 01").calendarPlanId(800L).status(0).build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("AUTO-DAY").startTime("08:00").endTime("20:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDateRange(anyCollection(), any(), any())).thenReturn(Collections.emptyList());

        MesProScheduleCalendarCapacityGenerateReqVO reqVO = new MesProScheduleCalendarCapacityGenerateReqVO();
        reqVO.setStartDate("2026-06-08");
        reqVO.setDays(1);

        var resp = service.generateCapacityPlans(reqVO);

        assertEquals(0, resp.getGeneratedCount());
        assertEquals(1, resp.getSkippedRestCount());
        assertEquals(1, resp.getSkippedDetails().size());
        assertEquals("2026-06-08", resp.getSkippedDetails().get(0).getDate());
        assertEquals("LINE-01", resp.getSkippedDetails().get(0).getLineCode());
        assertEquals("REST_DATE", resp.getSkippedDetails().get(0).getReasonCode());
        assertEquals("排程日历规则标记为休息日", resp.getSkippedDetails().get(0).getReasonText());
        verify(capacityPlanMapper, never()).insert(any(MesProCapacityPlanDO.class));
    }

    @Test
    void getMonth_shouldLoadTaskWhenWorkstationHasNoProductionLine() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-NO-LINE")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(null).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("MANUAL").locked(false).riskStatus("LINE_MISSING").build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth("2026-05");

        var day = resp.getDays().stream().filter(item -> "2026-05-13".equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(1, day.getTotalTaskCount());
        assertEquals(1, day.getDayShiftTaskCount());
        verify(capacityPlanMapper, never()).selectListByLineIdsAndDate(anyCollection(), any());
    }

    @Test
    void getMonth_shouldLoadTaskWhenWorkstationRecordIsMissing() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-MISSING-WS")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(Collections.emptyList());
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("MANUAL").locked(false).riskStatus("WORKSTATION_MISSING").build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth("2026-05");

        var day = resp.getDays().stream().filter(item -> "2026-05-13".equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(1, day.getTotalTaskCount());
        assertEquals(1, day.getDayShiftTaskCount());
        verify(capacityPlanMapper, never()).selectListByLineIdsAndDate(anyCollection(), any());
    }

    @Test
    void getMonth_shouldLoadTaskWhenProcessRecordIsMissing() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-MISSING-PROC")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(null).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Collections.emptyMap());
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("MANUAL").locked(false).riskStatus("PROCESS_MISSING").build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth("2026-05");

        var day = resp.getDays().stream().filter(item -> "2026-05-13".equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(1, day.getTotalTaskCount());
        assertEquals(1, day.getDayShiftTaskCount());
    }

    @Test
    void getDayDetail_shouldAggregateWorkshopLineAndDailyMaterialSummary() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .routeId(350L)
                .processId(400L)
                .itemId(500L)
                .quantity(new BigDecimal("2"))
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(new BigDecimal("2")).build()));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(routeService.getRouteMapIgnoreDeleted(anyCollection())).thenReturn(Map.of(350L, MesProRouteDO.builder().id(350L).name("Route Alpha").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        MesMdItemDO materialItem = new MesMdItemDO();
        materialItem.setId(501L);
        materialItem.setCode("MAT-01");
        materialItem.setName("Material 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem, 501L, materialItem));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, new BigDecimal("5")))));
        when(materialStockMapper.selectListByItemIds(anyCollection())).thenReturn(List.of(
                MesWmMaterialStockDO.builder().id(920L).itemId(501L).quantity(new BigDecimal("3")).frozen(Boolean.FALSE).build()
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(feedbackMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProFeedbackDO.builder()
                        .id(910L)
                        .taskId(100L)
                        .feedbackQuantity(new BigDecimal("1.5"))
                        .uncheckQuantity(new BigDecimal("0.5"))
                        .status(MesProFeedbackStatusEnum.UNCHECK.getStatus())
                        .build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                        .shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(List.of(
                MesProScheduleIssueDO.builder()
                        .id(960L)
                        .issueType("FEEDBACK")
                        .severity("BLOCKING")
                        .workOrderId(200L)
                        .taskId(100L)
                        .calendarDate(LocalDateTime.of(2026, 5, 13, 10, 0))
                        .message("设备停机")
                        .status("OPEN")
                        .resolved(Boolean.FALSE)
                        .sourceType("FEEDBACK")
                        .sourceId(910L)
                        .build()));

        var resp = service.getDayDetail("2026-05-13");

        assertEquals(1, resp.getWorkshops().size());
        assertEquals(1, resp.getWorkshops().get(0).getLines().size());
        assertEquals(1, resp.getWorkshops().get(0).getLines().get(0).getTasks().size());
        assertEquals(350L, resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getRouteId());
        assertEquals("Route Alpha", resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getRouteName());
        assertEquals(0, new BigDecimal("1.5").compareTo(resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getReportedQuantity()));
        assertEquals(0, new BigDecimal("0.5").compareTo(resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getPendingInspectionQuantity()));
        assertEquals("PENDING_INSPECTION", resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getExecutionStatus());
        assertFalse(resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getScheduleOrderFrozen());
        assertEquals(1, resp.getScheduleIssueSummary().getOpenIssueCount());
        assertEquals(1, resp.getScheduleIssueSummary().getBlockingIssueCount());
        assertEquals("设备停机", resp.getScheduleIssueSummary().getItems().get(0).getMessage());
        assertEquals(1, resp.getMaterialShortageSummary().getShortageCount());
        assertEquals(0, new BigDecimal("2").compareTo(resp.getMaterialShortageSummary().getTotalShortageQty()));
        assertEquals(1, resp.getMaterialShortageSummary().getItems().size());
        assertEquals(0, new BigDecimal("5").compareTo(resp.getMaterialShortageSummary().getItems().get(0).getScheduledUsageQty()));
        assertEquals(0, new BigDecimal("3").compareTo(resp.getMaterialShortageSummary().getItems().get(0).getRemainingAvailableQty()));
        assertEquals(1, resp.getMaterialShortageSummary().getItems().get(0).getAffectedWorkOrderCount());
    }

    @Test
    void getDayDetail_shouldShowUnboundLineGroupWhenWorkstationHasNoProductionLine() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-NO-LINE")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(null).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("MANUAL").locked(false).riskStatus("LINE_MISSING").build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-13");

        assertEquals(1, resp.getWorkshops().size());
        assertEquals("Workshop 01", resp.getWorkshops().get(0).getWorkshopName());
        assertEquals(1, resp.getWorkshops().get(0).getLines().size());
        assertNull(resp.getWorkshops().get(0).getLines().get(0).getLineId());
        assertEquals("未绑定工艺路线", resp.getWorkshops().get(0).getLines().get(0).getLineName());
        assertEquals(1, resp.getWorkshops().get(0).getLines().get(0).getTasks().size());
    }

    @Test
    void getDayDetail_shouldShowMissingProcessLabelWhenProcessRecordIsMissing() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-MISSING-PROC")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(null).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Collections.emptyMap());
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("MANUAL").locked(false).riskStatus("PROCESS_MISSING").build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-13");

        assertEquals(1, resp.getWorkshops().size());
        assertEquals("Workshop 01", resp.getWorkshops().get(0).getWorkshopName());
        assertEquals(1, resp.getWorkshops().get(0).getLines().size());
        assertEquals(1, resp.getWorkshops().get(0).getLines().get(0).getTasks().size());
        assertEquals("工序不存在", resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getProcessName());
    }

    @Test
    void getDayDetail_shouldShowMissingItemLabelWhenItemRecordIsMissing() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-MISSING-ITEM")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(null).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("MANUAL").locked(false).riskStatus("ITEM_MISSING").build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-13");

        assertEquals(1, resp.getWorkshops().size());
        assertEquals(1, resp.getWorkshops().get(0).getLines().size());
        assertEquals(1, resp.getWorkshops().get(0).getLines().get(0).getTasks().size());
        assertEquals("物料不存在", resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getItemName());
    }

    @Test
    void getDayDetail_shouldShowMissingWorkstationGroupWhenWorkstationRecordIsMissing() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-MISSING-WS")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(Collections.emptyList());
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("MANUAL").locked(false).riskStatus("WORKSTATION_MISSING").build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-13");

        assertEquals(1, resp.getWorkshops().size());
        assertNull(resp.getWorkshops().get(0).getWorkshopId());
        assertEquals("工作站不存在", resp.getWorkshops().get(0).getWorkshopName());
        assertEquals(1, resp.getWorkshops().get(0).getLines().size());
        assertNull(resp.getWorkshops().get(0).getLines().get(0).getLineId());
        assertEquals("未绑定工艺路线", resp.getWorkshops().get(0).getLines().get(0).getLineName());
        assertEquals(1, resp.getWorkshops().get(0).getLines().get(0).getTasks().size());
    }

    @Test
    void getWorkOrderAnalysis_shouldBuildSingleLineAnalysis() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(200L)
                .code("WO-001")
                .productId(500L)
                .quantity(new BigDecimal("99"))
                .build();
        MesProTaskDO process1Task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-1")
                .workOrderId(200L)
                .routeId(900L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(new BigDecimal("99"))
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 9, 0))
                .build();
        MesProTaskDO process2Task = MesProTaskDO.builder()
                .id(101L)
                .code("PT-2")
                .workOrderId(200L)
                .routeId(900L)
                .workstationId(301L)
                .processId(401L)
                .itemId(500L)
                .quantity(new BigDecimal("99"))
                .startTime(LocalDateTime.of(2026, 5, 13, 9, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 12, 0))
                .build();
        MesMdWorkstationDO machineDriven = MesMdWorkstationDO.builder()
                .id(300L).name("WS-M").processId(400L).productionLineId(600L).workshopId(700L).status(0).build();
        MesMdWorkstationDO workerDriven = MesMdWorkstationDO.builder()
                .id(301L).name("WS-W").processId(401L).productionLineId(600L).workshopId(700L).status(0)
                .singleStandardHourlyCapacity(new BigDecimal("20"))
                .build();
        MesMdProductionLineDO line = MesMdProductionLineDO.builder()
                .id(600L).code("LINE-01").name("Line 01").calendarPlanId(800L).workshopId(700L).status(0).build();
        MesProProcessDO process1 = MesProProcessDO.builder().id(400L).name("设备工序").build();
        MesProProcessDO process2 = MesProProcessDO.builder().id(401L).name("人工工序").build();
        MesMdItemDO product = new MesMdItemDO();
        product.setId(500L);
        product.setCode("ITEM-01");
        product.setName("成品A");
        when(workOrderService.getWorkOrder(200L)).thenReturn(workOrder);
        when(taskMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(List.of(process1Task, process2Task));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(machineDriven, workerDriven));
        when(workstationMapper.selectListByProcessIds(anyCollection(), any())).thenReturn(List.of(machineDriven, workerDriven));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, line));
        when(routeService.getRouteMapIgnoreDeleted(anyCollection())).thenReturn(Map.of(900L,
                MesProRouteDO.builder().id(900L).code("ROUTE-900").name("球囊扩张导管").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, process1, 401L, process2));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, product));
        when(workstationCapacityService.getCapacityMetrics(anyCollection(), any())).thenReturn(Map.of(
                300L, buildCapacityMetrics(1, 1, new BigDecimal("120"), new BigDecimal("120")),
                301L, buildCapacityMetrics(2, 2, BigDecimal.ZERO, new BigDecimal("40"))
        ));

        var analysis = service.getWorkOrderAnalysis(200L);

        assertFalse(readBooleanField(analysis, "conflict"));
        assertEquals(900L, readLongField(analysis, "lineId"));
        assertEquals("ROUTE-900", readRequiredField(analysis, "lineCode", String.class));
        assertEquals("球囊扩张导管", readRequiredField(analysis, "lineName", String.class));
        assertEquals(401L, readLongField(analysis, "bottleneckProcessId"));
        @SuppressWarnings("unchecked")
        List<Object> processes = readRequiredField(analysis, "processes", List.class);
        assertEquals(2, processes.size());
    }

    @Test
    void getWorkOrderAnalysis_shouldAllowCrossLineTasksAndReturnCombinedLineSummary() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(200L)
                .code("WO-001")
                .productId(500L)
                .quantity(new BigDecimal("99"))
                .build();
        MesProTaskDO process1Task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-1")
                .workOrderId(200L)
                .routeId(900L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(new BigDecimal("99"))
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 9, 0))
                .build();
        MesProTaskDO process2Task = MesProTaskDO.builder()
                .id(101L)
                .code("PT-2")
                .workOrderId(200L)
                .routeId(900L)
                .workstationId(301L)
                .processId(401L)
                .itemId(500L)
                .quantity(new BigDecimal("99"))
                .startTime(LocalDateTime.of(2026, 5, 13, 9, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 12, 0))
                .build();
        MesMdWorkstationDO line1Process = MesMdWorkstationDO.builder()
                .id(300L).name("WS-1").processId(400L).productionLineId(600L).workshopId(700L).status(0).build();
        MesMdWorkstationDO line2Process = MesMdWorkstationDO.builder()
                .id(301L).name("WS-2").processId(401L).productionLineId(601L).workshopId(700L).status(0).build();
        MesMdProductionLineDO line1 = MesMdProductionLineDO.builder().id(600L).code("LINE-01").name("Line 01").calendarPlanId(800L).workshopId(700L).status(0).build();
        MesMdProductionLineDO line2 = MesMdProductionLineDO.builder().id(601L).code("LINE-02").name("Line 02").calendarPlanId(801L).workshopId(700L).status(0).build();
        MesMdItemDO product = new MesMdItemDO();
        product.setId(500L);
        product.setCode("ITEM-01");
        product.setName("成品A");
        when(workOrderService.getWorkOrder(200L)).thenReturn(workOrder);
        when(taskMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(List.of(process1Task, process2Task));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(line1Process, line2Process));
        when(workstationMapper.selectListByProcessIds(anyCollection(), any())).thenReturn(List.of(line1Process, line2Process));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, line1, 601L, line2));
        when(routeService.getRouteMapIgnoreDeleted(anyCollection())).thenReturn(Map.of(900L,
                MesProRouteDO.builder().id(900L).code("ROUTE-900").name("球囊扩张导管").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(
                400L, MesProProcessDO.builder().id(400L).name("工序1").build(),
                401L, MesProProcessDO.builder().id(401L).name("工序2").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, product));

        var analysis = service.getWorkOrderAnalysis(200L);

        assertFalse(readBooleanField(analysis, "conflict"));
        assertEquals(900L, readLongField(analysis, "lineId"));
        assertEquals("ROUTE-900", readRequiredField(analysis, "lineCode", String.class));
        assertEquals("球囊扩张导管", readRequiredField(analysis, "lineName", String.class));
        @SuppressWarnings("unchecked")
        List<Object> processes = readRequiredField(analysis, "processes", List.class);
        assertEquals(2, processes.size());
    }

    @Test
    void getWorkOrderAnalysis_shouldUseRouteProcessSnapshotWhenTaskHasNoWorkstation() {
        MesProWorkOrderDO workOrder = MesProWorkOrderDO.builder()
                .id(200L)
                .code("WO-ROUTE")
                .productId(500L)
                .quantity(new BigDecimal("4"))
                .build();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-ROUTE")
                .workOrderId(200L)
                .routeId(20L)
                .workstationId(null)
                .processId(400L)
                .itemId(500L)
                .quantity(new BigDecimal("4"))
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 10, 0))
                .build();
        MesMdItemDO product = new MesMdItemDO();
        product.setId(500L);
        product.setCode("ITEM-ROUTE");
        product.setName("Route Item");
        when(workOrderService.getWorkOrder(200L)).thenReturn(workOrder);
        when(taskMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(List.of(task));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, product));
        when(workstationMapper.selectListByProcessIds(anyCollection(), any())).thenReturn(Collections.emptyList());
        when(routeService.getRouteMapIgnoreDeleted(anyCollection())).thenReturn(Map.of(20L,
                MesProRouteDO.builder().id(20L).code("ROUTE-20").name("路线二十").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder()
                        .taskId(100L)
                        .scheduleOrderProcessId(601L)
                        .scheduleSource("AUTO")
                        .locked(false)
                        .riskStatus("NONE")
                        .build()));
        when(scheduleOrderProcessMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(601L)
                        .processId(400L)
                        .processName("吹球囊成型")
                        .hourlyCapacityTotal(new BigDecimal("2"))
                        .build()));

        var analysis = service.getWorkOrderAnalysis(200L);

        assertFalse(readBooleanField(analysis, "conflict"));
        assertEquals(20L, readLongField(analysis, "lineId"));
        assertEquals("ROUTE-20", readRequiredField(analysis, "lineCode", String.class));
        assertEquals("路线二十", readRequiredField(analysis, "lineName", String.class));
        @SuppressWarnings("unchecked")
        List<Object> processes = readRequiredField(analysis, "processes", List.class);
        assertEquals(1, processes.size());
        Object process = processes.get(0);
        assertEquals("吹球囊成型", readRequiredField(process, "processName", String.class));
        assertEquals("ROUTE_PROCESS", readRequiredField(process, "capacitySource", String.class));
        assertEquals(0, readRequiredField(process, "workstationCount", Integer.class));
        assertEquals(0, new BigDecimal("2").compareTo(readRequiredField(process, "effectiveHourlyCapacity", BigDecimal.class)));
        assertEquals(120, readRequiredField(process, "plannedDurationMinutes", Integer.class));
    }

    @Test
    void getDayDetail_shouldDisplayScheduleRouteNameAsLineName() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-ROUTE-LINE")
                .workOrderId(200L)
                .routeId(900L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        MesMdWorkstationDO workstation = MesMdWorkstationDO.builder()
                .id(300L).name("WS-1").processId(400L).productionLineId(600L).workshopId(700L).status(0).build();
        MesMdProductionLineDO line = MesMdProductionLineDO.builder()
                .id(600L).code("AUTO-LINE-01").name("AutoScheduleLine").calendarPlanId(800L).workshopId(700L).status(0).build();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(scheduleOrderMapper.selectEffectiveListByWorkOrderIds(anyCollection())).thenReturn(List.of(
                MesProScheduleOrderDO.builder().id(10L).workOrderId(200L).status(1).build()));
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(workstation));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, line));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(routeService.getRouteMapIgnoreDeleted(anyCollection())).thenReturn(Map.of(900L,
                MesProRouteDO.builder().id(900L).code("ROUTE-900").name("球囊扩张导管").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder()
                        .id(70L)
                        .lineId(600L)
                        .calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                        .capacityMinutes(480)
                        .enabled(Boolean.TRUE)
                        .build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-13");

        assertEquals(1, resp.getWorkshops().size());
        assertEquals(1, resp.getWorkshops().get(0).getLines().size());
        var lineItem = resp.getWorkshops().get(0).getLines().get(0);
        assertEquals(900L, lineItem.getLineId());
        assertEquals("ROUTE-900", lineItem.getLineCode());
        assertEquals("球囊扩张导管", lineItem.getLineName());
        assertEquals("球囊扩张导管", lineItem.getTasks().get(0).getRouteName());
    }

    private MesMdWorkstationCapacityMetrics buildCapacityMetrics(int configuredWorkerCount,
                                                                 int currentWorkerCount,
                                                                 BigDecimal machineryStandardHourlyCapacity,
                                                                 BigDecimal effectiveHourlyCapacity) {
        return MesMdWorkstationCapacityMetrics.builder()
                .configuredWorkerCount(configuredWorkerCount)
                .currentWorkerCount(currentWorkerCount)
                .machineryStandardHourlyCapacity(machineryStandardHourlyCapacity)
                .todayCapacity(effectiveHourlyCapacity)
                .build();
    }

    private boolean readBooleanField(Object target, String fieldName) {
        return readRequiredField(target, fieldName, Boolean.class);
    }

    private long readLongField(Object target, String fieldName) {
        return readRequiredField(target, fieldName, Long.class);
    }

    private <T> T readRequiredField(Object target, String fieldName, Class<T> type) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            assertNotNull(value, "Expected field '" + fieldName + "' to be populated");
            return type.cast(value);
        } catch (NoSuchFieldException e) {
            fail("Expected field '" + fieldName + "' on " + target.getClass().getSimpleName());
            return null;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private <T> T readOptionalField(Object target, String fieldName, Class<T> type) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value == null ? null : type.cast(value);
        } catch (NoSuchFieldException e) {
            fail("Expected field '" + fieldName + "' on " + target.getClass().getSimpleName());
            return null;
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void getMonth_shouldCountLaterDayShortageAfterPriorFirstOperationUsage() {
        stubRuleAndSimulation();
        MesProTaskDO firstTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        firstTask.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        MesProTaskDO secondTask = MesProTaskDO.builder()
                .id(101L)
                .code("PT-0002")
                .workOrderId(201L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .build();
        secondTask.setUpdateTime(LocalDateTime.of(2026, 5, 14, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(firstTask, secondTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(2L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(secondTask);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build(),
                201L, MesProWorkOrderDO.builder().id(201L).code("WO-002").productId(500L).quantity(BigDecimal.ONE).build()
        ));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        MesMdItemDO materialItem = new MesMdItemDO();
        materialItem.setId(501L);
        materialItem.setCode("MAT-01");
        materialItem.setName("Material 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem, 501L, materialItem));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(
                        200L, Map.of(501L, new BigDecimal("3")),
                        201L, Map.of(501L, new BigDecimal("4"))
                )));
        when(materialStockMapper.selectListByItemIds(anyCollection())).thenReturn(List.of(
                MesWmMaterialStockDO.builder().id(920L).itemId(501L).quantity(new BigDecimal("5")).frozen(Boolean.FALSE).build()
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build(),
                MesProTaskScheduleExtDO.builder().taskId(101L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()
        ));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth("2026-05");

        var firstDay = resp.getDays().stream().filter(item -> "2026-05-13".equals(item.getDate())).findFirst().orElseThrow();
        var secondDay = resp.getDays().stream().filter(item -> "2026-05-14".equals(item.getDate())).findFirst().orElseThrow();
        assertEquals(0, firstDay.getShortageCount());
        assertEquals(1, secondDay.getShortageCount());
    }

    @Test
    void getDayDetail_shouldSubtractPriorAllocatedUsageWhenComputingRemainingAvailableQty() {
        stubRuleAndSimulation();
        MesProTaskDO firstTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        firstTask.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        MesProTaskDO secondTask = MesProTaskDO.builder()
                .id(101L)
                .code("PT-0002")
                .workOrderId(201L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .build();
        secondTask.setUpdateTime(LocalDateTime.of(2026, 5, 14, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(firstTask, secondTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(2L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(secondTask);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build(),
                201L, MesProWorkOrderDO.builder().id(201L).code("WO-002").productId(500L).quantity(BigDecimal.ONE).build()
        ));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        MesMdItemDO materialItem = new MesMdItemDO();
        materialItem.setId(501L);
        materialItem.setCode("MAT-01");
        materialItem.setName("Material 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem, 501L, materialItem));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(
                        200L, Map.of(501L, new BigDecimal("3")),
                        201L, Map.of(501L, new BigDecimal("4"))
                )));
        when(materialStockMapper.selectListByItemIds(anyCollection())).thenReturn(List.of(
                MesWmMaterialStockDO.builder().id(920L).itemId(501L).quantity(new BigDecimal("5")).frozen(Boolean.FALSE).build()
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build(),
                MesProTaskScheduleExtDO.builder().taskId(101L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()
        ));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-14");

        assertEquals(1, resp.getMaterialShortageSummary().getItems().size());
        assertEquals(0, new BigDecimal("4").compareTo(resp.getMaterialShortageSummary().getItems().get(0).getScheduledUsageQty()));
        assertEquals(0, new BigDecimal("2").compareTo(resp.getMaterialShortageSummary().getItems().get(0).getRemainingAvailableQty()));
        assertEquals(0, new BigDecimal("2").compareTo(resp.getMaterialShortageSummary().getItems().get(0).getShortageQty()));
        assertEquals(2, resp.getMaterialShortageSummary().getItems().get(0).getAffectedWorkOrderCount());
    }

    @Test
    void getDayDetail_shouldExposeCurrentShortageAsCumulativeDemandThroughSelectedDay() {
        stubRuleAndSimulation();
        MesProTaskDO firstTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        firstTask.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        MesProTaskDO secondTask = MesProTaskDO.builder()
                .id(101L)
                .code("PT-0002")
                .workOrderId(201L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .build();
        secondTask.setUpdateTime(LocalDateTime.of(2026, 5, 14, 9, 0));
        MesProTaskDO thirdTask = MesProTaskDO.builder()
                .id(102L)
                .code("PT-0003")
                .workOrderId(202L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 15, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 15, 16, 0))
                .build();
        thirdTask.setUpdateTime(LocalDateTime.of(2026, 5, 15, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(firstTask, secondTask, thirdTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(3L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(thirdTask);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build(),
                201L, MesProWorkOrderDO.builder().id(201L).code("WO-002").productId(500L).quantity(BigDecimal.ONE).build(),
                202L, MesProWorkOrderDO.builder().id(202L).code("WO-003").productId(500L).quantity(BigDecimal.ONE).build()
        ));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        MesMdItemDO materialItem = new MesMdItemDO();
        materialItem.setId(501L);
        materialItem.setCode("MAT-01");
        materialItem.setName("Material 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem, 501L, materialItem));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(
                        200L, Map.of(501L, new BigDecimal("3")),
                        201L, Map.of(501L, new BigDecimal("4")),
                        202L, Map.of(501L, new BigDecimal("2"))
                )));
        when(materialStockMapper.selectListByItemIds(anyCollection())).thenReturn(List.of(
                MesWmMaterialStockDO.builder().id(920L).itemId(501L).quantity(new BigDecimal("5")).frozen(Boolean.FALSE).build()
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build(),
                MesProTaskScheduleExtDO.builder().taskId(101L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build(),
                MesProTaskScheduleExtDO.builder().taskId(102L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()
        ));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(952L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var firstDay = service.getDayDetail("2026-05-13");
        assertEquals(0, firstDay.getMaterialShortageSummary().getShortageCount());
        assertTrue(firstDay.getMaterialShortageSummary().getItems().isEmpty());

        var thirdDay = service.getDayDetail("2026-05-15");

        assertEquals(1, thirdDay.getMaterialShortageSummary().getShortageCount());
        assertEquals(1, thirdDay.getMaterialShortageSummary().getItems().size());
        var shortage = thirdDay.getMaterialShortageSummary().getItems().get(0);
        assertEquals(0, new BigDecimal("2").compareTo(shortage.getScheduledUsageQty()));
        assertEquals(0, new BigDecimal("0").compareTo(shortage.getRemainingAvailableQty()));
        assertEquals(0, new BigDecimal("9").compareTo(shortage.getRequiredQty()));
        assertEquals(0, new BigDecimal("5").compareTo(shortage.getAvailableQty()));
        assertEquals(0, new BigDecimal("4").compareTo(shortage.getShortageQty()));
    }

    @Test
    void getDayDetail_shouldExposeMaterialDemandByWorkOrderAndTotalThroughSelectedDay() {
        stubRuleAndSimulation();
        MesProTaskDO firstTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        firstTask.setUpdateTime(LocalDateTime.of(2026, 5, 13, 9, 0));
        MesProTaskDO secondTask = MesProTaskDO.builder()
                .id(101L)
                .code("PT-0002")
                .workOrderId(201L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 16, 0))
                .build();
        secondTask.setUpdateTime(LocalDateTime.of(2026, 5, 14, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(firstTask, secondTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(2L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(secondTask);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(BigDecimal.ONE).build(),
                201L, MesProWorkOrderDO.builder().id(201L).code("WO-002").productId(500L).quantity(BigDecimal.ONE).build()
        ));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        MesMdItemDO productItem = new MesMdItemDO();
        productItem.setId(500L);
        productItem.setCode("ITEM-01");
        productItem.setName("Item 01");
        MesMdItemDO materialItem = new MesMdItemDO();
        materialItem.setId(501L);
        materialItem.setCode("MAT-01");
        materialItem.setName("Material 01");
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, productItem, 501L, materialItem));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(
                        200L, Map.of(501L, new BigDecimal("3")),
                        201L, Map.of(501L, new BigDecimal("4"))
                )));
        when(materialStockMapper.selectListByItemIds(anyCollection())).thenReturn(List.of(
                MesWmMaterialStockDO.builder().id(920L).itemId(501L).quantity(new BigDecimal("5")).frozen(Boolean.FALSE).build()
        ));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build(),
                MesProTaskScheduleExtDO.builder().taskId(101L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()
        ));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var firstDay = service.getDayDetail("2026-05-13");

        assertEquals(1, firstDay.getMaterialDemandSummary().getWorkOrderCount());
        assertEquals(1, firstDay.getMaterialDemandSummary().getMaterialCount());
        assertEquals(1, firstDay.getMaterialDemandSummary().getWorkOrderItems().size());
        assertEquals("WO-001", firstDay.getMaterialDemandSummary().getWorkOrderItems().get(0).getWorkOrderCode());
        assertEquals(0, new BigDecimal("3").compareTo(firstDay.getMaterialDemandSummary().getTotalItems().get(0).getRequiredQty()));
        assertEquals(0, new BigDecimal("0").compareTo(firstDay.getMaterialDemandSummary().getTotalItems().get(0).getShortageQty()));

        var secondDay = service.getDayDetail("2026-05-14");

        assertEquals(2, secondDay.getMaterialDemandSummary().getWorkOrderCount());
        assertEquals(2, secondDay.getMaterialDemandSummary().getWorkOrderItems().size());
        assertEquals(0, new BigDecimal("7").compareTo(secondDay.getMaterialDemandSummary().getTotalItems().get(0).getRequiredQty()));
        assertEquals(0, new BigDecimal("5").compareTo(secondDay.getMaterialDemandSummary().getTotalItems().get(0).getAvailableQty()));
        assertEquals(0, new BigDecimal("2").compareTo(secondDay.getMaterialDemandSummary().getTotalItems().get(0).getShortageQty()));
        assertEquals(2, secondDay.getMaterialDemandSummary().getTotalItems().get(0).getAffectedWorkOrderCount());
    }

    @Test
    void getMonth_shouldReturnEmptyCalendarWhenCurrentScheduleMissing() {
        stubRuleAndSimulation();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(Collections.emptyList());
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(0L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(null);

        var response = service.getMonth("2026-05");

        assertEquals("2026-05", response.getMonth());
        assertNotNull(response.getCurrentScheduleStatus());
        assertFalse(response.getCurrentScheduleStatus().getHasCurrentSchedule());
        assertEquals(0, response.getCurrentScheduleStatus().getTotalTaskCount());
        assertEquals(31, response.getDays().size());
        assertTrue(response.getDays().stream().allMatch(day -> day.getTotalTaskCount() == 0));
        assertTrue(response.getDays().stream().allMatch(day -> day.getTotalOrderCount() == 0));
        assertTrue(response.getDays().stream().allMatch(day -> day.getShortageCount() == 0));
    }

    @Test
    void getDayDetail_shouldReturnEmptyDetailWhenCurrentScheduleMissing() {
        stubRuleAndSimulation();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(Collections.emptyList());
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(0L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(null);

        var response = service.getDayDetail("2026-05-13");

        assertEquals("2026-05-13", response.getDate());
        assertEquals(0, response.getDayShiftTaskCount());
        assertEquals(0, response.getNightShiftTaskCount());
        assertNotNull(response.getWorkshops());
        assertTrue(response.getWorkshops().isEmpty());
        assertNotNull(response.getMaterialShortageSummary());
        assertEquals(0, response.getMaterialShortageSummary().getShortageCount());
        assertEquals(0, response.getMaterialShortageSummary().getItems().size());
        assertNotNull(response.getScheduleIssueSummary());
        assertEquals(0, response.getScheduleIssueSummary().getOpenIssueCount());
        assertEquals(0, response.getScheduleIssueSummary().getBlockingIssueCount());
        assertEquals(0, response.getScheduleIssueSummary().getItems().size());
    }

    @Test
    void getMonth_shouldFailFastWhenCapacityMissing() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").status(0).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, new MesMdItemDO().setId(500L).setCode("ITEM-01").setName("Item 01")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(Collections.emptyList());
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(Collections.emptyList());
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.getMonth("2026-05"));

        assertEquals(PRO_AUTO_SCHEDULE_CAPACITY_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void getMonth_shouldReuseDayCapacityWhenNightShiftCapacityRowMissing() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-NIGHT")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 20, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 2, 0))
                .build();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(
                600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").status(0).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(
                700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(
                400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(
                500L, new MesMdItemDO().setId(500L).setCode("ITEM-01").setName("Item 01")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("15:00").build(),
                MesCalPlanShiftDO.builder().id(802L).planId(800L).sort(2).name("Night").startTime("20:00").endTime("08:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(801L).capacityMinutes(420).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(801L).capacityMinutes(420).enabled(true).build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var response = service.getMonth("2026-05");

        var day = response.getDays().stream()
                .filter(item -> "2026-05-13".equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, day.getNightShiftTaskCount());
    }

    @Test
    void getMonth_shouldNotRequireNextDayCapacityForCrossDayNightShift() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-NIGHT")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 20, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 2, 0))
                .build();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(
                600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").status(0).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(
                700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(
                400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(
                500L, new MesMdItemDO().setId(500L).setCode("ITEM-01").setName("Item 01")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("15:00").build(),
                MesCalPlanShiftDO.builder().id(802L).planId(800L).sort(2).name("Night").startTime("20:00").endTime("08:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(801L).capacityMinutes(420).enabled(true).build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var response = service.getMonth("2026-05");

        var day = response.getDays().stream()
                .filter(item -> "2026-05-13".equals(item.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, day.getNightShiftTaskCount());
    }

    @Test
    void getMonth_shouldRenewFutureCapacityCoverageBeforeFailFastValidation() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .build();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L,
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").status(0).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L,
                new MesMdItemDO().setId(500L).setCode("ITEM-01").setName("Item 01")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()
        ));
        when(capacityPlanMapper.selectListByLineIdsAndDateRange(anyCollection(), any(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder()
                        .id(950L).lineId(600L).shiftId(801L)
                        .calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                        .capacityMinutes(480).enabled(true).build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder()
                        .id(950L).lineId(600L).shiftId(801L)
                        .calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                        .capacityMinutes(480).enabled(true).build(),
                MesProCapacityPlanDO.builder()
                        .id(951L).lineId(600L).shiftId(801L)
                        .calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0))
                        .capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth("2026-05");

        assertEquals(31, resp.getDays().size());
        ArgumentCaptor<MesProCapacityPlanDO> captor = ArgumentCaptor.forClass(MesProCapacityPlanDO.class);
        verify(capacityPlanMapper).insert(captor.capture());
        assertEquals(600L, captor.getValue().getLineId());
        assertEquals(801L, captor.getValue().getShiftId());
        assertEquals(LocalDateTime.of(2026, 5, 14, 0, 0), captor.getValue().getCalendarDate());
        assertEquals(480, captor.getValue().getCapacityMinutes());
    }

    @Test
    void getMonth_shouldNotRequireCapacityForRestDateWithinTaskSpan() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-REST-SPAN")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 15, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 17, 10, 0))
                .build();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L,
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(productionLineMapper.selectListByIds(anyCollection())).thenReturn(List.of(
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").status(0).build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L,
                new MesMdItemDO().setId(500L).setCode("ITEM-01").setName("Item 01")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("16:00").build()));
        when(capacityPlanMapper.selectListByLineIdsAndDateRange(anyCollection(), any(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder()
                        .id(950L).lineId(600L).shiftId(801L)
                        .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                        .capacityMinutes(480).enabled(true).build()));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder()
                        .id(950L).lineId(600L).shiftId(801L)
                        .calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0))
                        .capacityMinutes(480).enabled(true).build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getMonth("2026-05");

        assertEquals(31, resp.getDays().size());
        var workingDay = resp.getDays().stream()
                .filter(day -> "2026-05-15".equals(day.getDate()))
                .findFirst()
                .orElseThrow();
        var saturdayRest = resp.getDays().stream()
                .filter(day -> "2026-05-16".equals(day.getDate()))
                .findFirst()
                .orElseThrow();
        var sundayRest = resp.getDays().stream()
                .filter(day -> "2026-05-17".equals(day.getDate()))
                .findFirst()
                .orElseThrow();
        assertEquals("DAY", workingDay.getDateShiftMode());
        assertEquals(1, workingDay.getTotalTaskCount());
        assertEquals("REST", saturdayRest.getDateShiftMode());
        assertEquals(0, saturdayRest.getTotalTaskCount());
        assertEquals(0, saturdayRest.getTotalOrderCount());
        assertEquals(0, saturdayRest.getDayShiftTaskCount());
        assertEquals(0, saturdayRest.getNightShiftTaskCount());
        assertEquals("REST", sundayRest.getDateShiftMode());
        assertEquals(0, sundayRest.getTotalTaskCount());
        assertEquals(0, sundayRest.getTotalOrderCount());
        assertEquals(0, sundayRest.getDayShiftTaskCount());
        assertEquals(0, sundayRest.getNightShiftTaskCount());
    }

    @Test
    void getMonth_shouldFailFastWhenProductionMaterialListMissing() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 13, 16, 0))
                .build();
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").build()
        ));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.getMonth("2026-05"));

        assertEquals(PRO_SCHEDULE_CALENDAR_PRODUCTION_MATERIAL_REQUIRED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("WO-001"));
    }

    @Test
    void getMonth_shouldAllowEmptyRequestedMonthWhenScheduleExistsElsewhere() {
        stubRuleAndSimulation();
        MesProTaskDO latestTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-0001")
                .workOrderId(200L)
                .startTime(LocalDateTime.of(2026, 6, 1, 8, 0))
                .endTime(LocalDateTime.of(2026, 6, 1, 16, 0))
                .build();
        latestTask.setUpdateTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(Collections.emptyList());
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(latestTask);

        var resp = service.getMonth("2026-05");

        assertTrue(resp.getCurrentScheduleStatus().getHasCurrentSchedule());
        assertEquals(1, resp.getCurrentScheduleStatus().getTotalTaskCount());
        assertEquals(31, resp.getDays().size());
        assertEquals(0, resp.getDays().stream().mapToInt(item -> item.getTotalTaskCount()).sum());
    }

    @Test
    void getDayDetail_shouldIncludeTaskOnSecondOccupiedDay() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-OVERNIGHT")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(BigDecimal.ONE)
                .startTime(LocalDateTime.of(2026, 5, 13, 20, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 2, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 20, 30));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L, MesProWorkOrderDO.builder().id(200L).code("WO-001").build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L, MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L, MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L, MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L, new MesMdItemDO().setId(500L).setCode("ITEM-01").setName("Item 01")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(2).name("Night").startTime("20:00").endTime("08:00").build()
        ));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-14");

        assertEquals(1, resp.getWorkshops().size());
        assertEquals(1, resp.getWorkshops().get(0).getLines().get(0).getTasks().size());
        assertEquals("PT-OVERNIGHT", resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0).getTaskCode());
    }

    @Test
    void getDayDetail_shouldExposeDailyAllocatedQuantityForCrossDayTask() {
        stubRuleAndSimulation();
        MesProTaskDO task = MesProTaskDO.builder()
                .id(100L)
                .code("PT-DAILY-QTY")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(new BigDecimal("60"))
                .startTime(LocalDateTime.of(2026, 5, 13, 20, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 2, 0))
                .build();
        task.setUpdateTime(LocalDateTime.of(2026, 5, 13, 20, 30));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(task));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(task);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(200L,
                MesProWorkOrderDO.builder().id(200L).code("WO-001").productId(500L).quantity(new BigDecimal("60")).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(200L, Map.of(501L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L,
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L,
                new MesMdItemDO().setId(500L).setCode("ITEM-01").setName("Item 01")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder().taskId(100L).scheduleSource("AUTO").locked(false).riskStatus("NONE").build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(2).name("Night").startTime("20:00").endTime("08:00").build()
        ));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(801L).capacityMinutes(480).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-14");

        var detailTask = resp.getWorkshops().get(0).getLines().get(0).getTasks().get(0);
        assertEquals(0, new BigDecimal("60").compareTo(detailTask.getQuantity()));
        assertEquals(0, new BigDecimal("20").compareTo(detailTask.getDailyQuantity()));
    }

    @Test
    void getDayDetail_shouldAggregateDailyProcessCapacityUtilization() {
        stubRuleAndSimulation();
        MesProTaskDO crossDayTask = MesProTaskDO.builder()
                .id(100L)
                .code("PT-CROSS")
                .workOrderId(200L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(new BigDecimal("60"))
                .startTime(LocalDateTime.of(2026, 5, 13, 20, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 2, 0))
                .build();
        crossDayTask.setUpdateTime(LocalDateTime.of(2026, 5, 13, 20, 30));
        MesProTaskDO sameDayTask = MesProTaskDO.builder()
                .id(101L)
                .code("PT-SAME")
                .workOrderId(201L)
                .workstationId(300L)
                .processId(400L)
                .itemId(500L)
                .quantity(new BigDecimal("30"))
                .startTime(LocalDateTime.of(2026, 5, 14, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 14, 10, 0))
                .build();
        sameDayTask.setUpdateTime(LocalDateTime.of(2026, 5, 14, 8, 30));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(crossDayTask, sameDayTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(2L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(sameDayTask);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(
                200L, MesProWorkOrderDO.builder().id(200L).code("WO-CROSS").productId(500L).quantity(new BigDecimal("60")).build(),
                201L, MesProWorkOrderDO.builder().id(201L).code("WO-SAME").productId(500L).quantity(new BigDecimal("30")).build()
        ));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(
                        200L, Map.of(501L, BigDecimal.ONE),
                        201L, Map.of(501L, BigDecimal.ONE)
                )));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(300L).workshopId(700L).productionLineId(600L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(600L,
                MesMdProductionLineDO.builder().id(600L).calendarPlanId(800L).code("LINE-01").name("Line 01").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(700L,
                MesMdWorkshopDO.builder().id(700L).code("WS-01").name("Workshop 01").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(400L,
                MesProProcessDO.builder().id(400L).name("Cut").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(500L,
                new MesMdItemDO().setId(500L).setCode("ITEM-01").setName("Item 01"), 501L,
                new MesMdItemDO().setId(501L).setCode("MAT-01").setName("Material 01")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder()
                        .taskId(100L)
                        .scheduleOrderId(900L)
                        .scheduleOrderProcessId(3000L)
                        .scheduleSource("AUTO")
                        .locked(false)
                        .riskStatus("NONE")
                        .build(),
                MesProTaskScheduleExtDO.builder()
                        .taskId(101L)
                        .scheduleOrderId(901L)
                        .scheduleOrderProcessId(3001L)
                        .scheduleSource("AUTO")
                        .locked(false)
                        .riskStatus("NONE")
                        .build()));
        when(scheduleOrderProcessMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(3000L)
                        .scheduleOrderId(900L)
                        .processId(400L)
                        .sort(1)
                        .processName("Cut")
                        .shiftCapacityTotal(new BigDecimal("100"))
                        .capacitySource("MACHINE")
                        .build(),
                MesProScheduleOrderProcessDO.builder()
                        .id(3001L)
                        .scheduleOrderId(901L)
                        .processId(400L)
                        .sort(1)
                        .processName("Cut")
                        .shiftCapacityTotal(new BigDecimal("100"))
                        .capacitySource("MACHINE")
                        .build()));
        when(planShiftService.getPlanShiftListByPlanId(800L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(801L).planId(800L).sort(1).name("Day").startTime("08:00").endTime("20:00").build(),
                MesCalPlanShiftDO.builder().id(802L).planId(800L).sort(2).name("Night").startTime("20:00").endTime("08:00").build()
        ));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(950L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 13, 0, 0)).shiftId(802L).capacityMinutes(360).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(951L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(801L).capacityMinutes(720).enabled(true).build(),
                MesProCapacityPlanDO.builder().id(952L).lineId(600L).calendarDate(LocalDateTime.of(2026, 5, 14, 0, 0)).shiftId(802L).capacityMinutes(720).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-14");

        assertEquals(1, resp.getProcessCapacitySummary().getItems().size());
        var summary = resp.getProcessCapacitySummary().getItems().get(0);
        assertEquals(400L, summary.getProcessId());
        assertEquals("Cut", summary.getProcessName());
        assertEquals(2, summary.getTaskCount());
        assertEquals(2, summary.getWorkOrderCount());
        assertEquals(0, new BigDecimal("100").compareTo(summary.getMaxCapacity()));
        assertEquals(0, new BigDecimal("50").compareTo(summary.getScheduledQuantity()));
        assertEquals(0, new BigDecimal("50").compareTo(summary.getRemainingCapacity()));
        assertEquals(0, new BigDecimal("50.000000").compareTo(summary.getUtilizationRate()));
    }

    @Test
    void getDayDetail_shouldUseRouteProcessCapacityWhenTaskHasNoProductionLine() {
        stubRuleAndSimulation();
        MesProTaskDO routeProcessTask = MesProTaskDO.builder()
                .id(120L)
                .code("PT-ROUTE-CAPACITY")
                .workOrderId(220L)
                .routeId(920L)
                .processId(420L)
                .itemId(520L)
                .quantity(new BigDecimal("420"))
                .startTime(LocalDateTime.of(2026, 7, 23, 8, 0))
                .endTime(LocalDateTime.of(2026, 7, 23, 18, 30))
                .build();
        routeProcessTask.setUpdateTime(LocalDateTime.of(2026, 7, 23, 8, 30));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(routeProcessTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(routeProcessTask);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(220L,
                MesProWorkOrderDO.builder().id(220L).code("WO-ROUTE-CAPACITY")
                        .productId(520L).quantity(new BigDecimal("420")).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(220L, Map.of(521L, BigDecimal.ONE))));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(420L,
                MesProProcessDO.builder().id(420L).name("Route Leak Test").build()));
        when(routeService.getRouteMapIgnoreDeleted(anyCollection())).thenReturn(Map.of(920L,
                MesProRouteDO.builder().id(920L).code("ROUTE-920").name("Route 920").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(
                520L, new MesMdItemDO().setId(520L).setCode("ITEM-ROUTE-CAPACITY").setName("Route Capacity Item"),
                521L, new MesMdItemDO().setId(521L).setCode("MAT-ROUTE-CAPACITY").setName("Route Capacity Material")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder()
                        .taskId(120L)
                        .scheduleOrderId(1220L)
                        .scheduleOrderProcessId(3020L)
                        .scheduleSource("AUTO")
                        .locked(false)
                        .riskStatus("NONE")
                        .build()));
        when(scheduleOrderProcessMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(3020L)
                        .scheduleOrderId(1220L)
                        .processId(420L)
                        .processName("Route Leak Test")
                        .sort(1)
                        .capacitySource("ROUTE_PROCESS")
                        .shiftCapacityTotal(new BigDecimal("420"))
                        .build()));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-07-23");

        assertEquals(1, resp.getProcessCapacitySummary().getItems().size());
        var summary = resp.getProcessCapacitySummary().getItems().get(0);
        assertEquals(420L, summary.getProcessId());
        assertEquals("Route Leak Test", summary.getProcessName());
        assertEquals(0, new BigDecimal("420").compareTo(summary.getMaxCapacity()));
        assertEquals(0, new BigDecimal("420").compareTo(summary.getScheduledQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getRemainingCapacity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getOverCapacity()));
        assertEquals(0, new BigDecimal("100.000000").compareTo(summary.getUtilizationRate()));
    }

    @Test
    void getDayDetail_shouldExposeProcessCapacityOverrun() {
        stubRuleAndSimulation();
        MesProTaskDO overrunTask = MesProTaskDO.builder()
                .id(110L)
                .code("PT-OVERRUN")
                .workOrderId(210L)
                .workstationId(310L)
                .processId(410L)
                .itemId(510L)
                .quantity(new BigDecimal("120"))
                .startTime(LocalDateTime.of(2026, 5, 15, 8, 0))
                .endTime(LocalDateTime.of(2026, 5, 15, 12, 0))
                .build();
        overrunTask.setUpdateTime(LocalDateTime.of(2026, 5, 15, 8, 30));
        when(taskMapper.selectListByStartTimeRange(isNull(), any())).thenReturn(List.of(overrunTask));
        when(taskMapper.selectCurrentScheduleCount()).thenReturn(1L);
        when(taskMapper.selectLatestUpdatedTask()).thenReturn(overrunTask);
        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Map.of(210L,
                MesProWorkOrderDO.builder().id(210L).code("WO-OVERRUN").productId(510L).quantity(new BigDecimal("120")).build()));
        when(productionMaterialListMapper.selectListByWorkOrderIds(anyCollection()))
                .thenReturn(buildProductionMaterialListRows(Map.of(210L, Map.of(511L, BigDecimal.ONE))));
        when(workstationMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesMdWorkstationDO.builder().id(310L).workshopId(710L).productionLineId(610L).build()));
        when(productionLineService.getProductionLineMap(anyCollection())).thenReturn(Map.of(610L,
                MesMdProductionLineDO.builder().id(610L).calendarPlanId(810L).code("LINE-OVERRUN").name("Line Overrun").build()));
        when(workshopService.getWorkshopMap(anyCollection())).thenReturn(Map.of(710L,
                MesMdWorkshopDO.builder().id(710L).code("WS-OVERRUN").name("Workshop Overrun").build()));
        when(processService.getProcessMap(anyCollection())).thenReturn(Map.of(410L,
                MesProProcessDO.builder().id(410L).name("Overrun Process").build()));
        when(itemService.getItemMap(anyCollection())).thenReturn(Map.of(510L,
                new MesMdItemDO().setId(510L).setCode("ITEM-OVERRUN").setName("Item Overrun"), 511L,
                new MesMdItemDO().setId(511L).setCode("MAT-OVERRUN").setName("Material Overrun")));
        when(taskScheduleExtMapper.selectListByTaskIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder()
                        .taskId(110L)
                        .scheduleOrderId(910L)
                        .scheduleOrderProcessId(3010L)
                        .scheduleSource("AUTO")
                        .locked(false)
                        .riskStatus("NONE")
                        .build()));
        when(scheduleOrderProcessMapper.selectByIds(anyCollection())).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(3010L)
                        .scheduleOrderId(910L)
                        .processId(410L)
                        .sort(1)
                        .processName("Overrun Process")
                        .shiftCapacityTotal(new BigDecimal("100"))
                        .capacitySource("MACHINE")
                        .build()));
        when(planShiftService.getPlanShiftListByPlanId(810L)).thenReturn(List.of(
                MesCalPlanShiftDO.builder().id(811L).planId(810L).sort(1).name("Day").startTime("08:00").endTime("20:00").build()
        ));
        when(capacityPlanMapper.selectListByLineIdsAndDate(anyCollection(), any())).thenReturn(List.of(
                MesProCapacityPlanDO.builder().id(960L).lineId(610L).calendarDate(LocalDateTime.of(2026, 5, 15, 0, 0)).shiftId(811L).capacityMinutes(720).enabled(true).build()
        ));
        when(scheduleIssueMapper.selectListByWorkOrderIds(anyCollection())).thenReturn(Collections.emptyList());

        var resp = service.getDayDetail("2026-05-15");

        assertEquals(1, resp.getProcessCapacitySummary().getItems().size());
        var summary = resp.getProcessCapacitySummary().getItems().get(0);
        assertEquals(0, new BigDecimal("100").compareTo(summary.getMaxCapacity()));
        assertEquals(0, new BigDecimal("120").compareTo(summary.getScheduledQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getRemainingCapacity()));
        assertEquals(0, new BigDecimal("20").compareTo(summary.getOverCapacity()));
        assertEquals(0, new BigDecimal("120.000000").compareTo(summary.getUtilizationRate()));
    }

    private void stubRuleAndSimulation() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.FALSE)
                .weekendRestMode("DOUBLE")
                .dateShiftModeByDateJson("{}")
                .build();
        MesProScheduleCalendarSimulationDO simulation = MesProScheduleCalendarSimulationDO.builder()
                .id(11L)
                .currentDate(LocalDateTime.of(2026, 5, 13, 0, 0))
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);
        when(simulationMapper.selectByTenantId(1L)).thenReturn(simulation);
        when(holidayService.getHolidayList(any(), any())).thenReturn(Collections.emptyList());
    }

    private void assertLazyInjected(String fieldName) throws NoSuchFieldException {
        Field field = MesProScheduleCalendarServiceImpl.class.getDeclaredField(fieldName);
        assertNotNull(field.getAnnotation(Lazy.class), fieldName + " must use @Lazy injection");
    }

    private void stubCapacityGenerationRule() {
        MesProScheduleCalendarRuleDO rule = MesProScheduleCalendarRuleDO.builder()
                .id(10L)
                .skipStatutoryHolidays(Boolean.FALSE)
                .temporaryFreezeEnabled(Boolean.FALSE)
                .weekendRestMode("NONE")
                .dateShiftModeByDateJson("{}")
                .build();
        when(ruleMapper.selectByTenantId(1L)).thenReturn(rule);
        when(holidayService.getHolidayList(any(), any())).thenReturn(Collections.emptyList());
    }

    private List<MesKingdeeProductionMaterialListDO> buildProductionMaterialListRows(
            Map<Long, Map<Long, BigDecimal>> demandByWorkOrderId) {
        List<MesKingdeeProductionMaterialListDO> rows = new ArrayList<>();
        long sequence = 1L;
        for (Map.Entry<Long, Map<Long, BigDecimal>> workOrderEntry : demandByWorkOrderId.entrySet()) {
            Long workOrderId = workOrderEntry.getKey();
            for (Map.Entry<Long, BigDecimal> materialEntry : workOrderEntry.getValue().entrySet()) {
                rows.add(MesKingdeeProductionMaterialListDO.builder()
                        .id(sequence++)
                        .workOrderId(workOrderId)
                        .childMaterialId(materialEntry.getKey())
                        .childMaterialCode("MAT-" + materialEntry.getKey())
                        .requiredQuantity(materialEntry.getValue())
                        .build());
            }
        }
        return rows;
    }

}
