package cn.iocoder.yudao.module.mes.service.pro.scheduleorder;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProScheduleOrderProcessPlanTest {
    @InjectMocks
    private MesProScheduleOrderServiceImpl service;
    @Mock
    private MesProScheduleOrderProcessMapper scheduleOrderProcessMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskMapper taskMapper;

    private final LocalDateTime start = LocalDateTime.of(2026, 11, 25, 8, 0);

    @Test
    void readsSplitTaskRangeOnlyThroughFormalProcessRelation() {
        MesProScheduleOrderProcessDO first = new MesProScheduleOrderProcessDO().setId(1L);
        MesProScheduleOrderProcessDO second = new MesProScheduleOrderProcessDO().setId(2L);
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(237L)).thenReturn(List.of(first, second));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(Set.of(1L, 2L)))
                .thenReturn(List.of(relation(11L, 1L), relation(12L, 1L), relation(13L, 2L), relation(14L, 1L)));
        when(taskMapper.selectListByIds(List.of(11L, 12L, 13L, 14L))).thenReturn(List.of(
                task(11L, start.plusDays(1), start.plusDays(1).plusHours(9)),
                task(12L, start, start.plusHours(10)),
                task(13L, start.plusHours(2), start.plusHours(7)),
                task(14L, start.minusDays(1), start.plusDays(4))
                        .setStatus(MesProTaskStatusEnum.CANCELED.getStatus())));

        List<MesProScheduleOrderProcessDO> result = service.getScheduleOrderProcessList(237L);

        assertEquals(start, result.get(0).getPlannedStartTime());
        assertEquals(start.plusDays(1).plusHours(9), result.get(0).getPlannedEndTime());
        assertEquals(start.plusHours(2), result.get(1).getPlannedStartTime());
        assertEquals(start.plusHours(7), result.get(1).getPlannedEndTime());
    }

    @Test
    void noCurrentTasksDoesNotReuseStaleSnapshotTimes() {
        MesProScheduleOrderProcessDO process = new MesProScheduleOrderProcessDO().setId(1L)
                .setPlannedStartTime(start).setPlannedEndTime(start.plusHours(1));
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(237L)).thenReturn(List.of(process));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(Set.of(1L))).thenReturn(List.of());

        service.getScheduleOrderProcessList(237L);

        assertNull(process.getPlannedStartTime());
        assertNull(process.getPlannedEndTime());
        verifyNoInteractions(taskMapper);
    }

    @Test
    void invalidFormalTaskTimesFailClearly() {
        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(237L))
                .thenReturn(List.of(new MesProScheduleOrderProcessDO().setId(1L)));
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(Set.of(1L)))
                .thenReturn(List.of(relation(11L, 1L)));
        when(taskMapper.selectListByIds(List.of(11L))).thenReturn(List.of(task(11L, start, null)));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.getScheduleOrderProcessList(237L));
        assertTrue(error.getMessage().contains("taskId=11"));
    }

    @Test
    void batchProjectionKeepsMissingAndCanceledProcessesUnscheduled() {
        MesProScheduleOrderProcessDO scheduled = new MesProScheduleOrderProcessDO().setId(1L);
        MesProScheduleOrderProcessDO missing = new MesProScheduleOrderProcessDO().setId(2L)
                .setPlannedStartTime(start.minusDays(3)).setPlannedEndTime(start);
        MesProScheduleOrderProcessDO canceled = new MesProScheduleOrderProcessDO().setId(3L);
        when(taskScheduleExtMapper.selectListByScheduleOrderProcessIds(Set.of(1L, 2L, 3L)))
                .thenReturn(List.of(relation(11L, 1L), relation(12L, 2L), relation(13L, 3L)));
        when(taskMapper.selectListByIds(List.of(11L, 12L, 13L))).thenReturn(List.of(
                task(11L, start, start.plusHours(8)),
                task(13L, start, start.plusHours(10)).setStatus(MesProTaskStatusEnum.CANCELED.getStatus())));

        when(scheduleOrderProcessMapper.selectListByScheduleOrderId(237L))
                .thenReturn(List.of(scheduled, missing, canceled));
        service.getScheduleOrderProcessList(237L);

        assertEquals(start, scheduled.getPlannedStartTime());
        assertEquals(start.plusHours(8), scheduled.getPlannedEndTime());
        assertNull(missing.getPlannedStartTime());
        assertNull(missing.getPlannedEndTime());
        assertNull(canceled.getPlannedStartTime());
        assertNull(canceled.getPlannedEndTime());
        verify(taskScheduleExtMapper, times(1)).selectListByScheduleOrderProcessIds(Set.of(1L, 2L, 3L));
        verify(taskMapper, times(1)).selectListByIds(List.of(11L, 12L, 13L));
    }

    private MesProTaskScheduleExtDO relation(long taskId, long processId) {
        return new MesProTaskScheduleExtDO().setTaskId(taskId).setScheduleOrderId(237L)
                .setScheduleOrderProcessId(processId);
    }

    private MesProTaskDO task(long id, LocalDateTime from, LocalDateTime to) {
        return new MesProTaskDO().setId(id).setStartTime(from).setEndTime(to);
    }
}
