package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleReplanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedule.vo.MesProAutoScheduleSummaryRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProNightlyReplanServiceImplTest {

    @InjectMocks
    private MesProNightlyReplanServiceImpl nightlyReplanService;

    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProAutoScheduleService autoScheduleService;

    @Test
    void executeNightlyReplan_shouldReplanUnfinishedScheduleOrdersAndPreserveLockedScope() {
        MesProScheduleOrderDO scheduledOrder = MesProScheduleOrderDO.builder()
                .id(1001L)
                .status(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus())
                .build();
        MesProScheduleOrderDO inProgressOrder = MesProScheduleOrderDO.builder()
                .id(1002L)
                .status(MesProScheduleOrderStatusEnum.IN_PROGRESS.getStatus())
                .build();
        when(scheduleOrderMapper.selectListForNightlyReplan()).thenReturn(List.of(scheduledOrder, inProgressOrder));
        MesProAutoScheduleApplyRespVO applyRespVO = new MesProAutoScheduleApplyRespVO();
        MesProAutoScheduleSummaryRespVO summary = new MesProAutoScheduleSummaryRespVO();
        summary.setGeneratedTaskCount(6);
        summary.setPreservedTaskCount(2);
        summary.setBlockingIssueCount(1);
        summary.setShortageCount(1);
        applyRespVO.setSummary(summary);
        MesProAutoScheduleReplanPreviewRespVO previewRespVO = new MesProAutoScheduleReplanPreviewRespVO();
        previewRespVO.setCalendarContextToken("nightly-calendar-token");
        when(autoScheduleService.replanPreview(org.mockito.ArgumentMatchers.any())).thenReturn(previewRespVO);
        when(autoScheduleService.replanApplyForNightly(org.mockito.ArgumentMatchers.any())).thenReturn(applyRespVO);

        MesProNightlyReplanResult result = nightlyReplanService.executeNightlyReplan(LocalDateTime.of(2026, 6, 10, 2, 0));

        assertEquals(2, result.getScheduleOrderCount());
        assertEquals(6, result.getGeneratedTaskCount());
        assertEquals(2, result.getPreservedTaskCount());
        assertEquals(1, result.getBlockingIssueCount());
        assertEquals(1, result.getShortageCount());
        assertEquals("夜间重排完成：排产工单 2，生成任务 6，保护任务 2，阻塞 1，短缺 1", result.toJobMessage());

        ArgumentCaptor<MesProAutoScheduleReplanReqVO> captor = ArgumentCaptor.forClass(MesProAutoScheduleReplanReqVO.class);
        verify(autoScheduleService).replanApplyForNightly(captor.capture());
        MesProAutoScheduleReplanReqVO reqVO = captor.getValue();
        assertEquals(List.of(1001L, 1002L), reqVO.getScheduleOrderIds());
        assertEquals(LocalDateTime.of(2026, 6, 10, 2, 0), reqVO.getStartTime());
        assertEquals("PLANNED", reqVO.getRuntimeCapacityBasis());
        assertTrue(reqVO.getPreserveManualLockedTasks());
        assertEquals("nightly-calendar-token", reqVO.getCalendarContextToken());
        verify(autoScheduleService).replanPreview(captor.capture());
        MesProAutoScheduleReplanReqVO previewReqVO = captor.getValue();
        assertEquals(reqVO.getScheduleOrderIds(), previewReqVO.getScheduleOrderIds());
        assertEquals(reqVO.getStartTime(), previewReqVO.getStartTime());
    }

    @Test
    void executeNightlyReplan_shouldReturnEmptyResultWhenNoUnfinishedScheduleOrders() {
        when(scheduleOrderMapper.selectListForNightlyReplan()).thenReturn(Collections.emptyList());

        MesProNightlyReplanResult result = nightlyReplanService.executeNightlyReplan(LocalDateTime.of(2026, 6, 10, 2, 0));

        assertEquals(0, result.getScheduleOrderCount());
        assertEquals("夜间重排完成：没有待重排排产工单", result.toJobMessage());
        verify(autoScheduleService, never()).replanPreview(org.mockito.ArgumentMatchers.any());
        verify(autoScheduleService, never()).replanApplyForNightly(org.mockito.ArgumentMatchers.any());
    }

}
