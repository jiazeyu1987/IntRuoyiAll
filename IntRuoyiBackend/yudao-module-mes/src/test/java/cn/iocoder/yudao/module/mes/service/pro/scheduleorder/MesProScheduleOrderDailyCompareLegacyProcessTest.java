package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDailyCompareDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleDailyCompareStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderDailyCompareLegacyProcessTest {

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskMapper taskMapper;
    @InjectMocks
    private MesProScheduleOrderServiceImpl scheduleOrderService;

    @Test
    void getDailyCompare_sameScheduleProcessWithLegacyAndCurrentProcessIds_aggregatesOneRow() {
        LocalDate planDate = LocalDate.of(2026, 7, 10);
        when(scheduleOrderMapper.selectById(100L)).thenReturn(MesProScheduleOrderDO.builder().id(100L).build());
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(100L)).thenReturn(List.of(
                MesProScheduleOrderProcessDO.builder()
                        .id(200L)
                        .scheduleOrderId(100L)
                        .processId(900L)
                        .enabled(Boolean.TRUE)
                        .build()));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(anyCollection())).thenReturn(List.of(
                MesProTaskScheduleExtDO.builder()
                        .taskId(300L)
                        .scheduleOrderProcessId(200L)
                        .build()));
        when(taskMapper.selectListByIds(List.of(300L))).thenReturn(List.of(
                MesProTaskDO.builder()
                        .id(300L)
                        .quantity(new BigDecimal("10"))
                        .startTime(planDate.atTime(8, 0))
                        .endTime(planDate.atTime(9, 0))
                        .build()));
        when(feedbackMapper.selectProgressListByScheduleOrderId(100L)).thenReturn(List.of(
                MesProFeedbackDO.builder()
                        .id(400L)
                        .scheduleOrderId(100L)
                        .scheduleOrderProcessId(200L)
                        .processId(901L)
                        .feedbackQuantity(new BigDecimal("10"))
                        .feedbackTime(LocalDateTime.of(2026, 7, 10, 8, 30))
                        .status(MesProFeedbackStatusEnum.FINISHED.getStatus())
                        .build()));

        List<MesProScheduleOrderDailyCompareDO> result =
                scheduleOrderService.getDailyCompare(100L, planDate, planDate);

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).getScheduleOrderProcessId());
        assertEquals(900L, result.get(0).getProcessId());
        assertEquals(new BigDecimal("10.000000"), result.get(0).getPlannedQuantity());
        assertEquals(new BigDecimal("10.000000"), result.get(0).getActualQuantity());
        assertEquals(MesProScheduleDailyCompareStatusEnum.NORMAL.getStatus(), result.get(0).getStatus());
    }
}
