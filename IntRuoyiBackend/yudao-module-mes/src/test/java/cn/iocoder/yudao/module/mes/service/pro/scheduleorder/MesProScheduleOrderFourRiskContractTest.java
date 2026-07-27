package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderOperationLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderOperationLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProFeedbackStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderFourRiskContractTest {

    @InjectMocks
    private MesProScheduleOrderServiceImpl scheduleOrderService;

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProFeedbackMapper feedbackMapper;
    @Mock
    private MesProScheduleOrderOperationLogMapper scheduleOrderOperationLogMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskMapper taskMapper;

    @Test
    void syncFeedbackProgress_shouldUseAttributedFeedbackAndKeepOverReportedQuantityVisible() {
        MesProScheduleOrderDO order = MesProScheduleOrderDO.builder()
                .id(501L)
                .code("SCH-RISK-001")
                .quantity(new BigDecimal("100.000000"))
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .build();
        MesProScheduleOrderProcessDO process = MesProScheduleOrderProcessDO.builder()
                .id(701L)
                .scheduleOrderId(501L)
                .processId(11L)
                .sort(1)
                .enabled(Boolean.TRUE)
                .plannedQuantity(new BigDecimal("100.000000"))
                .planDate(LocalDate.of(2026, 6, 24))
                .build();
        when(scheduleOrderMapper.selectById(501L)).thenReturn(order);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(501L)).thenReturn(List.of(process));
        when(feedbackMapper.selectProgressListByScheduleOrderId(501L)).thenReturn(List.of(
                feedback(9001L, "120.000000", MesProFeedbackStatusEnum.FINISHED.getStatus()),
                feedback(9002L, "30.000000", MesProFeedbackStatusEnum.APPROVING.getStatus()),
                feedback(9003L, "20.000000", MesProFeedbackStatusEnum.UNCHECK.getStatus())
        ));

        scheduleOrderService.syncFeedbackProgress(501L);

        verify(scheduleOrderProcessMapper).updateProgress(701L, new BigDecimal("170.000000"),
                BigDecimal.ZERO.setScale(6), new BigDecimal("100.000000"));
        verify(scheduleOrderMapper).updateProgressSummary(501L, new BigDecimal("100.000000"),
                new BigDecimal("100.000000"), BigDecimal.ZERO.setScale(6), new BigDecimal("100.000000"),
                MesProScheduleOrderStatusEnum.FINISHED.getStatus());
        ArgumentCaptor<MesProScheduleOrderOperationLogDO> logCaptor =
                ArgumentCaptor.forClass(MesProScheduleOrderOperationLogDO.class);
        verify(scheduleOrderOperationLogMapper).insert(logCaptor.capture());
        assertTrue(logCaptor.getValue().getOperationType().contains("SYNC_PROGRESS"));
        String afterSnapshotJson = logCaptor.getValue().getAfterSnapshotJson();
        assertTrue(afterSnapshotJson.contains("\"reportedQuantity\":170.000000"));
        assertTrue(afterSnapshotJson.contains("\"overReportedQuantity\":70.000000"));
        assertTrue(afterSnapshotJson.contains("\"progressPercent\":100.000000"));
    }

    private MesProFeedbackDO feedback(Long id, String quantity, Integer status) {
        return MesProFeedbackDO.builder()
                .id(id)
                .scheduleOrderId(501L)
                .scheduleOrderProcessId(701L)
                .processId(11L)
                .feedbackQuantity(new BigDecimal(quantity))
                .feedbackTime(LocalDateTime.of(2026, 6, 24, 9, 0))
                .status(status)
                .build();
    }
}
