package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackImportRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDailyCompareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackImportRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderOperationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleCapacityModeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderProgressServiceTest {

    @InjectMocks
    private MesProScheduleOrderServiceImpl scheduleOrderService;

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProScheduleOrderOperationLogMapper scheduleOrderOperationLogMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProFeedbackImportRecordMapper importRecordMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskMapper taskMapper;

    @Test
    void syncFeedbackProgress_shouldIgnoreAppliedDirectWorkReportProgressRecordsForCompletion() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("100.000000")).build();
        MesProScheduleOrderProcessDO process = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(process));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "30.000000", LocalDateTime.of(2026, 6, 10, 9, 0))
        ));
        lenient().when(importRecordMapper.selectAppliedDirectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                MesProFeedbackImportRecordDO.builder()
                        .id(8001L)
                        .scheduleOrderId(501L)
                        .scheduleOrderProcessId(701L)
                        .progressSourceType(MesProFeedbackImportRecordDO.PROGRESS_SOURCE_TYPE_DIRECT_WORK_REPORT)
                        .progressQuantity(new BigDecimal("50.000000"))
                        .build()
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderProcessMapper).updateProgress(701L, new BigDecimal("30.000000"),
                new BigDecimal("70.000000"), new BigDecimal("30.000000"));
        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("100.000000"),
                new BigDecimal("30.000000"), new BigDecimal("70.000000"), new BigDecimal("30.000000"),
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
    }

    @Test
    void syncFeedbackProgress_shouldWriteAggregateProcessTotalsBackToOrderSummary() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("100.000000")).build();
        MesProScheduleOrderProcessDO first = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        MesProScheduleOrderProcessDO second = process(702L, 501L, 12L, 2, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(first, second));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "30.000000", LocalDateTime.of(2026, 6, 10, 9, 0)),
                feedback(9002L, 501L, 701L, 11L, "20.000000", LocalDateTime.of(2026, 6, 10, 10, 0)),
                feedback(9003L, 501L, 702L, 12L, "25.000000", LocalDateTime.of(2026, 6, 10, 11, 0))
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderProcessMapper).updateProgress(701L, new BigDecimal("50.000000"),
                new BigDecimal("50.000000"), new BigDecimal("50.000000"));
        verify(scheduleOrderProcessMapper).updateProgress(702L, new BigDecimal("25.000000"),
                new BigDecimal("75.000000"), new BigDecimal("25.000000"));
        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("200.000000"),
                new BigDecimal("75.000000"), new BigDecimal("125.000000"), new BigDecimal("37.500000"),
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
    }

    @Test
    void calculateProcessAggregateProgressSummary_shouldUseProcessQuantityTotalsForHalfOfOneInTenProcesses() {
        List<MesProScheduleOrderProcessDO> processes = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(index -> MesProScheduleOrderProcessDO.builder()
                        .id((long) index)
                        .scheduleOrderId(501L)
                        .processId(700L + index)
                        .sort(index)
                        .enabled(Boolean.TRUE)
                        .plannedQuantity(new BigDecimal("100.000000"))
                        .reportedQuantity(index == 1 ? new BigDecimal("50.000000") : BigDecimal.ZERO.setScale(6))
                        .build())
                .toList();

        MesProScheduleOrderService.ProgressSummary summary =
                scheduleOrderService.calculateProcessAggregateProgressSummary(new BigDecimal("100.000000"), processes);

        assertEquals(new BigDecimal("1000.000000"), summary.totalQuantity());
        assertEquals(new BigDecimal("50.000000"), summary.completedQuantity());
        assertEquals(new BigDecimal("950.000000"), summary.uncompletedQuantity());
        assertEquals(new BigDecimal("5.000000"), summary.progressPercent());
    }

    @Test
    void calculateProcessAggregateProgressSummary_shouldUseProcessQuantityTotalsForOneFinishedInTenProcesses() {
        List<MesProScheduleOrderProcessDO> processes = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(index -> MesProScheduleOrderProcessDO.builder()
                        .id((long) index)
                        .scheduleOrderId(501L)
                        .processId(800L + index)
                        .sort(index)
                        .enabled(Boolean.TRUE)
                        .plannedQuantity(new BigDecimal("100.000000"))
                        .reportedQuantity(index == 1 ? new BigDecimal("100.000000") : BigDecimal.ZERO.setScale(6))
                        .build())
                .toList();

        MesProScheduleOrderService.ProgressSummary summary =
                scheduleOrderService.calculateProcessAggregateProgressSummary(new BigDecimal("100.000000"), processes);

        assertEquals(new BigDecimal("1000.000000"), summary.totalQuantity());
        assertEquals(new BigDecimal("100.000000"), summary.completedQuantity());
        assertEquals(new BigDecimal("900.000000"), summary.uncompletedQuantity());
        assertEquals(new BigDecimal("10.000000"), summary.progressPercent());
    }

    @Test
    void calculateProcessAggregateProgressSummary_shouldUseScheduleOrderQuantityPerProcessAndCapOverReport() {
        List<MesProScheduleOrderProcessDO> processes = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(index -> MesProScheduleOrderProcessDO.builder()
                        .id((long) index)
                        .scheduleOrderId(501L)
                        .processId(900L + index)
                        .sort(index)
                        .enabled(Boolean.TRUE)
                        .plannedQuantity(new BigDecimal("1200.000000"))
                        .reportedQuantity(index == 1 ? new BigDecimal("1200.000000") : BigDecimal.ZERO.setScale(6))
                        .build())
                .toList();

        MesProScheduleOrderService.ProgressSummary summary =
                scheduleOrderService.calculateProcessAggregateProgressSummary(new BigDecimal("1000.000000"), processes);

        assertEquals(new BigDecimal("10000.000000"), summary.totalQuantity());
        assertEquals(new BigDecimal("1000.000000"), summary.completedQuantity());
        assertEquals(new BigDecimal("9000.000000"), summary.uncompletedQuantity());
        assertEquals(new BigDecimal("10.000000"), summary.progressPercent());
    }

    @Test
    void syncFeedbackProgress_shouldIgnoreStaleAggregateTotalQuantityWhenCalculatingOrderProgress() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L)
                .quantity(new BigDecimal("1000.000000"))
                .totalQuantity(new BigDecimal("12000.000000"))
                .build();
        List<MesProScheduleOrderProcessDO> processes = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(index -> process(700L + index, 501L, 900L + index, index,
                        "FINITE_HOURLY", "1200.000000", LocalDate.of(2026, 6, 10)))
                .toList();
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(processes);
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 901L, "1200.000000", LocalDateTime.of(2026, 6, 10, 9, 0))
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("10000.000000"),
                new BigDecimal("1000.000000"), new BigDecimal("9000.000000"), new BigDecimal("10.000000"),
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
    }

    @Test
    void syncFeedbackProgress_shouldCountApprovingFeedbackCreatedByImportAttributionForCompletedQuantity() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("100.000000")).build();
        MesProScheduleOrderProcessDO process = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(process));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "12.000000", LocalDateTime.of(2026, 6, 10, 9, 0))
                        .setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus())
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderProcessMapper).updateProgress(701L, new BigDecimal("12.000000"),
                new BigDecimal("88.000000"), new BigDecimal("12.000000"));
        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("100.000000"),
                new BigDecimal("12.000000"), new BigDecimal("88.000000"), new BigDecimal("12.000000"),
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
    }

    @Test
    void syncFeedbackProgress_shouldCapProcessProgressWhenReportedQuantityExceedsPlan() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("1000.000000")).build();
        MesProScheduleOrderProcessDO process = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "1000.000000",
                LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(process));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "900.000000", LocalDateTime.of(2026, 6, 10, 9, 0))
                        .setStatus(MesProFeedbackStatusEnum.FINISHED.getStatus()),
                feedback(9002L, 501L, 701L, 11L, "200.000000", LocalDateTime.of(2026, 6, 10, 10, 0))
                        .setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus())
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderProcessMapper).updateProgress(701L, new BigDecimal("1100.000000"),
                BigDecimal.ZERO.setScale(6), new BigDecimal("100.000000"));
        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("1000.000000"),
                new BigDecimal("1000.000000"), BigDecimal.ZERO.setScale(6), new BigDecimal("100.000000"),
                MesProScheduleOrderStatusEnum.FINISHED.getStatus());
    }

    @Test
    void getDailyCompare_shouldComparePlanAndActualByDayWithNormalAheadBehindStatus() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("100.000000")).build();
        MesProScheduleOrderProcessDO first = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "60.000000",
                LocalDate.of(2026, 6, 10));
        MesProScheduleOrderProcessDO second = process(702L, 501L, 12L, 2, "FINITE_HOURLY", "40.000000",
                LocalDate.of(2026, 6, 11));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(first, second));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(argThat(ids -> containsOnly(ids, 701L, 702L))))
                .thenReturn(List.of(
                taskExt(8001L, 501L, 701L),
                taskExt(8002L, 501L, 702L)
        ));
        when(taskMapper.selectListByIds(argThat(ids -> containsOnly(ids, 8001L, 8002L)))).thenReturn(List.of(
                task(8001L, "60.000000", LocalDateTime.of(2026, 6, 10, 8, 0),
                        LocalDateTime.of(2026, 6, 10, 12, 0)),
                task(8002L, "40.000000", LocalDateTime.of(2026, 6, 11, 20, 0),
                        LocalDateTime.of(2026, 6, 12, 4, 0))
        ));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "60.000000", LocalDateTime.of(2026, 6, 10, 9, 0)),
                feedback(9002L, 501L, 702L, 12L, "25.000000", LocalDateTime.of(2026, 6, 11, 22, 0)),
                feedback(9003L, 501L, 702L, 12L, "10.000000", LocalDateTime.of(2026, 6, 12, 2, 0)),
                feedback(9004L, 501L, 702L, 12L, "5.000000", LocalDateTime.of(2026, 6, 13, 9, 0))
        ));

        List<MesProScheduleOrderDailyCompareDO> result = scheduleOrderService.getDailyCompare(501L,
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 13));

        assertEquals(4, result.size());
        assertCompare(result.get(0), LocalDate.of(2026, 6, 10), "60.000000", "60.000000", "0.000000",
                MesProScheduleDailyCompareStatusEnum.NORMAL.getStatus());
        assertCompare(result.get(1), LocalDate.of(2026, 6, 11), "20.000000", "25.000000", "5.000000",
                MesProScheduleDailyCompareStatusEnum.AHEAD.getStatus());
        assertCompare(result.get(2), LocalDate.of(2026, 6, 12), "20.000000", "10.000000", "-10.000000",
                MesProScheduleDailyCompareStatusEnum.BEHIND.getStatus());
        assertCompare(result.get(3), LocalDate.of(2026, 6, 13), "0.000000", "5.000000", "5.000000",
                MesProScheduleDailyCompareStatusEnum.NO_PLAN.getStatus());
    }

    @Test
    void syncFeedbackProgress_shouldNotMarkOrderCompletedWhenOnlyOneOfTwoProcessesReportsFirst() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("100.000000")).build();
        MesProScheduleOrderProcessDO first = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        MesProScheduleOrderProcessDO second = process(702L, 501L, 12L, 2,
                MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), "100.000000", LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(first, second));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9003L, 501L, 702L, 12L, "100.000000", LocalDateTime.of(2026, 6, 10, 11, 0))
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderProcessMapper).updateProgress(701L, BigDecimal.ZERO.setScale(6),
                new BigDecimal("100.000000"), BigDecimal.ZERO.setScale(6));
        verify(scheduleOrderProcessMapper).updateProgress(702L, new BigDecimal("100.000000"),
                BigDecimal.ZERO.setScale(6), new BigDecimal("100.000000"));
        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("200.000000"),
                new BigDecimal("100.000000"), new BigDecimal("100.000000"), new BigDecimal("50.000000"),
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
    }

    @Test
    void syncFeedbackProgress_shouldAggregateAllEnabledProcessCompletion() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("100.000000")).build();
        MesProScheduleOrderProcessDO keyProcess = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10), Boolean.TRUE);
        MesProScheduleOrderProcessDO infiniteProcess = process(702L, 501L, 12L, 2,
                MesProScheduleCapacityModeEnum.INFINITE_FORMULA.getMode(), "100.000000",
                LocalDate.of(2026, 6, 10), Boolean.FALSE);
        MesProScheduleOrderProcessDO ordinaryProcess = process(703L, 501L, 13L, 3, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10), Boolean.FALSE);
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(
                keyProcess, infiniteProcess, ordinaryProcess));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "80.000000", LocalDateTime.of(2026, 6, 10, 9, 0)),
                feedback(9002L, 501L, 702L, 12L, "60.000000", LocalDateTime.of(2026, 6, 10, 10, 0))
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("300.000000"),
                new BigDecimal("140.000000"), new BigDecimal("160.000000"), new BigDecimal("46.666667"),
                MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus());
    }

    @Test
    void syncFeedbackProgress_shouldMarkScheduleOrderFinishedWhenSerialProgressCompleted() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .quantity(new BigDecimal("100.000000"))
                .build();
        MesProScheduleOrderProcessDO first = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        MesProScheduleOrderProcessDO second = process(702L, 501L, 12L, 2, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(first, second));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "100.000000", LocalDateTime.of(2026, 6, 10, 9, 0)),
                feedback(9002L, 501L, 702L, 12L, "100.000000", LocalDateTime.of(2026, 6, 10, 11, 0))
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("200.000000"),
                new BigDecimal("200.000000"), BigDecimal.ZERO.setScale(6), new BigDecimal("100.000000"),
                MesProScheduleOrderStatusEnum.FINISHED.getStatus());
    }

    @Test
    void syncFeedbackProgress_shouldKeepManualFinishedSummaryLockedAtOneHundredPercent() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L)
                .status(MesProScheduleOrderStatusEnum.FINISHED.getStatus())
                .manualFinished(Boolean.TRUE)
                .quantity(new BigDecimal("100.000000"))
                .build();
        MesProScheduleOrderProcessDO first = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(first));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "20.000000", LocalDateTime.of(2026, 6, 10, 9, 0))
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("100.000000"),
                new BigDecimal("100.000000"), BigDecimal.ZERO.setScale(6), new BigDecimal("100.000000"),
                MesProScheduleOrderStatusEnum.FINISHED.getStatus());
    }

    @Test
    void getDailyCompare_shouldFailFastWhenScheduledTaskHasInvalidTimeInsteadOfUsingProcessPlanDate() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("100.000000")).build();
        MesProScheduleOrderProcessDO process = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "60.000000",
                LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(process));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of());
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(argThat(ids -> ids != null && ids.contains(701L))))
                .thenReturn(List.of(taskExt(8001L, 501L, 701L)));
        when(taskMapper.selectListByIds(argThat(ids -> ids != null && ids.contains(8001L)))).thenReturn(List.of(
                task(8001L, "60.000000", LocalDateTime.of(2026, 6, 10, 8, 0),
                        LocalDateTime.of(2026, 6, 10, 8, 0))
        ));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> scheduleOrderService.getDailyCompare(501L,
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10)));
        assertEquals("排产任务缺少有效的计划数量或开始结束时间，无法生成日报表计划量，taskId=8001", ex.getMessage());
    }

    @Test
    void calculateProcessProgressMetrics_shouldIgnoreAppliedDirectWorkReportProgressRecordsForCompletion() {
        MesProScheduleOrderProcessDO process = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "30.000000", LocalDateTime.of(2026, 6, 10, 9, 0))
        ));
        lenient().when(importRecordMapper.selectAppliedDirectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                MesProFeedbackImportRecordDO.builder()
                        .id(8001L)
                        .scheduleOrderId(501L)
                        .scheduleOrderProcessId(701L)
                        .progressSourceType(MesProFeedbackImportRecordDO.PROGRESS_SOURCE_TYPE_DIRECT_WORK_REPORT)
                        .progressQuantity(new BigDecimal("120.000000"))
                        .build()
        ));

        var metrics = scheduleOrderService.calculateProcessProgressMetrics(501L, List.of(process)).get(701L);

        assertEquals(new BigDecimal("30.000000"), metrics.effectiveCompletedQuantity());
        assertEquals(BigDecimal.ZERO.setScale(6), metrics.pendingApprovalQuantity());
        assertEquals(BigDecimal.ZERO.setScale(6), metrics.pendingInspectionQuantity());
        assertEquals(BigDecimal.ZERO.setScale(6), metrics.overReportedQuantity());
        assertEquals(new BigDecimal("30.000000"), metrics.reportedQuantity());
        assertEquals(new BigDecimal("70.000000"), metrics.remainingQuantity());
        assertEquals(new BigDecimal("30.000000"), metrics.progressPercent());
    }

    @Test
    void calculateProcessProgressMetrics_shouldExposeApprovalInspectionAndOverReportedBreakdown() {
        MesProScheduleOrderProcessDO process = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "100.000000",
                LocalDate.of(2026, 6, 10));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, 501L, 701L, 11L, "40.000000", LocalDateTime.of(2026, 6, 10, 9, 0))
                        .setStatus(MesProFeedbackStatusEnum.FINISHED.getStatus()),
                feedback(9002L, 501L, 701L, 11L, "15.000000", LocalDateTime.of(2026, 6, 10, 10, 0))
                        .setStatus(MesProFeedbackStatusEnum.APPROVING.getStatus()),
                feedback(9003L, 501L, 701L, 11L, "55.000000", LocalDateTime.of(2026, 6, 10, 11, 0))
                        .setStatus(MesProFeedbackStatusEnum.UNCHECK.getStatus())
        ));

        var metrics = scheduleOrderService.calculateProcessProgressMetrics(501L, List.of(process)).get(701L);

        assertEquals(new BigDecimal("100.000000"), metrics.effectiveCompletedQuantity());
        assertEquals(new BigDecimal("15.000000"), metrics.pendingApprovalQuantity());
        assertEquals(new BigDecimal("55.000000"), metrics.pendingInspectionQuantity());
        assertEquals(new BigDecimal("10.000000"), metrics.overReportedQuantity());
        assertEquals(new BigDecimal("110.000000"), metrics.reportedQuantity());
        assertEquals(BigDecimal.ZERO.setScale(6), metrics.remainingQuantity());
        assertEquals(new BigDecimal("100.000000"), metrics.progressPercent());
    }

    @Test
    void getDailyCompare_shouldMarkNoFeedbackWhenPlanExistsButNoActualFeedback() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L).quantity(new BigDecimal("100.000000")).build();
        MesProScheduleOrderProcessDO process = process(701L, 501L, 11L, 1, "FINITE_HOURLY", "40.000000",
                LocalDate.of(2026, 6, 10));
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(process));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(argThat(ids -> ids != null && ids.contains(701L))))
                .thenReturn(List.of(taskExt(8001L, 501L, 701L)));
        when(taskMapper.selectListByIds(argThat(ids -> ids != null && ids.contains(8001L)))).thenReturn(List.of(
                task(8001L, "40.000000", LocalDateTime.of(2026, 6, 10, 8, 0),
                        LocalDateTime.of(2026, 6, 10, 12, 0))
        ));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of());

        List<MesProScheduleOrderDailyCompareDO> result = scheduleOrderService.getDailyCompare(501L,
                LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10));

        assertEquals(1, result.size());
        assertCompare(result.get(0), LocalDate.of(2026, 6, 10), "40.000000", "0.000000", "-40.000000",
                MesProScheduleDailyCompareStatusEnum.NO_FEEDBACK.getStatus());
    }

    private MesProScheduleOrderProcessDO process(Long id, Long scheduleOrderId, Long processId, Integer sort,
                                                 String capacityMode, String plannedQuantity, LocalDate planDate) {
        return process(id, scheduleOrderId, processId, sort, capacityMode, plannedQuantity, planDate, Boolean.FALSE);
    }

    private MesProScheduleOrderProcessDO process(Long id, Long scheduleOrderId, Long processId, Integer sort,
                                                 String capacityMode, String plannedQuantity, LocalDate planDate,
                                                 Boolean keyProcessFlag) {
        return MesProScheduleOrderProcessDO.builder()
                .id(id)
                .scheduleOrderId(scheduleOrderId)
                .processId(processId)
                .sort(sort)
                .enabled(Boolean.TRUE)
                .capacityMode(capacityMode)
                .plannedQuantity(new BigDecimal(plannedQuantity))
                .planDate(planDate)
                .keyProcessFlag(keyProcessFlag)
                .build();
    }

    private MesProFeedbackDO feedback(Long id, Long scheduleOrderId, Long scheduleOrderProcessId, Long processId,
                                       String quantity, LocalDateTime feedbackTime) {
        return MesProFeedbackDO.builder()
                .id(id)
                .scheduleOrderId(scheduleOrderId)
                .scheduleOrderProcessId(scheduleOrderProcessId)
                .processId(processId)
                .feedbackQuantity(new BigDecimal(quantity))
                .feedbackTime(feedbackTime)
                .status(MesProFeedbackStatusEnum.FINISHED.getStatus())
                .build();
    }

    private MesProTaskScheduleExtDO taskExt(Long taskId, Long scheduleOrderId, Long scheduleOrderProcessId) {
        return MesProTaskScheduleExtDO.builder()
                .taskId(taskId)
                .scheduleOrderId(scheduleOrderId)
                .scheduleOrderProcessId(scheduleOrderProcessId)
                .build();
    }

    private MesProTaskDO task(Long id, String quantity, LocalDateTime startTime, LocalDateTime endTime) {
        return MesProTaskDO.builder()
                .id(id)
                .quantity(new BigDecimal(quantity))
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    private boolean containsOnly(Collection<Long> ids, Long first, Long second) {
        return ids != null && ids.size() == 2 && ids.contains(first) && ids.contains(second);
    }

    private void assertCompare(MesProScheduleOrderDailyCompareDO compare, LocalDate planDate, String planned,
                               String actual, String diff, Integer status) {
        assertEquals(planDate, compare.getPlanDate());
        assertEquals(new BigDecimal(planned), compare.getPlannedQuantity());
        assertEquals(new BigDecimal(actual), compare.getActualQuantity());
        assertEquals(new BigDecimal(diff), compare.getDiffQuantity());
        assertEquals(status, compare.getStatus());
    }

}
