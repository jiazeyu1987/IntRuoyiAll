package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.calendar.MesProScheduleCalendarDayDetailRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesProcessSharedCapacityTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 26);

    @Test
    void singleOrderReplanCountsOtherOrderWithoutWorkstationAndDoesNotDoubleCountPreservedTask() {
        var tasks = mock(MesProTaskMapper.class);
        var orders = mock(MesProScheduleOrderMapper.class);
        var processes = mock(MesProScheduleOrderProcessMapper.class);
        var service = new MesProAutoScheduleServiceImpl();
        ReflectionTestUtils.setField(service, "taskMapper", tasks);
        ReflectionTestUtils.setField(service, "scheduleOrderMapper", orders);
        ReflectionTestUtils.setField(service, "scheduleOrderProcessMapper", processes);
        var computation = new SchedulePlanner.ScheduleComputation();
        computation.requestStartTime = DAY.atTime(8, 0);
        computation.routeProcessesByWorkOrderId.put(1L, List.of(
                MesProRouteProcessDO.builder().id(10L).processId(1001L).build()));
        var existing = MesProTaskDO.builder().id(20L).workOrderId(2L).processId(1001L)
                .quantity(new BigDecimal("600")).startTime(DAY.atTime(8, 0)).endTime(DAY.atTime(14, 0)).build();
        var replacing = MesProTaskDO.builder().id(21L).workOrderId(1L).processId(1001L)
                .quantity(new BigDecimal("450")).startTime(DAY.atTime(8, 0)).endTime(DAY.atTime(12, 30)).build();
        computation.preservedTasks.add(existing);
        computation.replaceableScopeTasks.add(replacing);
        when(tasks.selectListByProcessIds(any())).thenReturn(List.of(existing, replacing));
        when(orders.selectEffectiveListByWorkOrderIds(any())).thenReturn(List.of(
                MesProScheduleOrderDO.builder().id(30L).workOrderId(2L).build()));
        var snapshot = MesProScheduleOrderProcessDO.builder().id(40L).scheduleOrderId(30L)
                .processId(1001L).shiftCapacityTotal(new BigDecimal("1050")).build();
        when(processes.selectListByScheduleOrderIds(any())).thenReturn(List.of(snapshot));

        ReflectionTestUtils.invokeMethod(service, "initializeProcessCapacityLedger", computation);

        assertTrue(computation.issues.isEmpty());
        assertEquals(0, new BigDecimal("450").compareTo(
                computation.processCapacityLedger.remaining(1001L, DAY, snapshot)));
        assertEquals(0, new BigDecimal("1050").compareTo(
                computation.processCapacityLedger.remaining(1002L, DAY, snapshot)));
        verify(tasks).selectListByProcessIds(java.util.Set.of(1001L));
        verify(tasks, never()).selectListByWorkstationIds(any());
    }

    @Test
    void calendarSharesOneCapacityAcrossLinesAndUsesIdInsteadOfName() throws Exception {
        var service = new MesProScheduleCalendarServiceImpl();
        List<Object> rows = List.of(row(1L, 1001L, "粗洗", 10L, "600"),
                row(2L, 1001L, "粗洗改名", 11L, "450"), row(3L, 1002L, "粗洗", 10L, "210"));
        MesProScheduleCalendarDayDetailRespVO.ProcessCapacitySummary summary =
                ReflectionTestUtils.invokeMethod(service, "buildProcessCapacitySummary", rows, DAY);
        assertNotNull(summary);
        assertEquals(2, summary.getProcessCount());
        var shared = summary.getItems().stream().filter(item -> item.getProcessId().equals(1001L)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("1050").compareTo(shared.getMaxCapacity()));
        assertEquals(0, new BigDecimal("1050").compareTo(shared.getScheduledQuantity()));
        assertEquals(0, new BigDecimal("100").compareTo(shared.getUtilizationRate()));
        assertEquals(0, BigDecimal.ZERO.compareTo(shared.getRemainingCapacity()));
        var separate = summary.getItems().stream().filter(item -> item.getProcessId().equals(1002L)).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("20").compareTo(separate.getUtilizationRate()));
    }

    private Object row(Long taskId, Long processId, String name, Long lineId, String quantity) throws Exception {
        Class<?> rowClass = Class.forName(MesProScheduleCalendarServiceImpl.class.getName() + "$TaskCalendarRow");
        var builderMethod = rowClass.getDeclaredMethod("builder");
        builderMethod.setAccessible(true);
        Object builder = builderMethod.invoke(null);
        ReflectionTestUtils.invokeMethod(builder, "taskId", taskId);
        ReflectionTestUtils.invokeMethod(builder, "workOrderId", taskId);
        ReflectionTestUtils.invokeMethod(builder, "processId", processId);
        ReflectionTestUtils.invokeMethod(builder, "processName", name);
        ReflectionTestUtils.invokeMethod(builder, "resourceLineId", lineId);
        ReflectionTestUtils.invokeMethod(builder, "quantity", new BigDecimal(quantity));
        ReflectionTestUtils.invokeMethod(builder, "shiftCapacityTotal", new BigDecimal("1050"));
        ReflectionTestUtils.invokeMethod(builder, "startTime", DAY.atTime(8, 0));
        ReflectionTestUtils.invokeMethod(builder, "endTime", DAY.atTime(18, 30));
        return ReflectionTestUtils.invokeMethod(builder, "build");
    }
}
