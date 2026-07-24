package cn.iocoder.yudao.module.mes.service.pro.task;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProTaskServiceImplTest {

    @InjectMocks
    private MesProTaskServiceImpl taskService;

    @Mock
    private MesProTaskMapper taskMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdUnitMeasureService unitMeasureService;
    @Mock
    private MesProRouteService routeService;
    @Mock
    private MesMdAutoCodeRecordService autoCodeRecordService;

    @Test
    void lockTask_shouldCreateScheduleExtWhenMissing() {
        MesProTaskDO task = MesProTaskDO.builder()
                .id(1L)
                .status(MesProTaskStatusEnum.PREPARE.getStatus())
                .build();
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskScheduleExtMapper.selectByTaskId(1L)).thenReturn(null);

        taskService.lockTask(1L, "planner-lock");

        ArgumentCaptor<MesProTaskScheduleExtDO> insertCaptor = ArgumentCaptor.forClass(MesProTaskScheduleExtDO.class);
        verify(taskScheduleExtMapper).insert(insertCaptor.capture());
        MesProTaskScheduleExtDO ext = insertCaptor.getValue();
        assertEquals(1L, ext.getTaskId());
        assertEquals(Boolean.TRUE, ext.getLocked());
        assertEquals("planner-lock", ext.getLockedReason());
        assertEquals("MANUAL", ext.getScheduleSource());
    }

    @Test
    void lockTask_shouldRejectFinishedTask() {
        MesProTaskDO task = MesProTaskDO.builder()
                .id(2L)
                .status(MesProTaskStatusEnum.FINISHED.getStatus())
                .build();
        when(taskMapper.selectById(2L)).thenReturn(task);

        assertThrows(ServiceException.class, () -> taskService.lockTask(2L, "planner-lock"));
        verify(taskScheduleExtMapper, never()).insert(any(MesProTaskScheduleExtDO.class));
        verify(taskScheduleExtMapper, never()).updateById(any(MesProTaskScheduleExtDO.class));
    }

    @Test
    void unlockTask_shouldClearLockedState() {
        MesProTaskDO task = MesProTaskDO.builder()
                .id(3L)
                .status(MesProTaskStatusEnum.PREPARE.getStatus())
                .build();
        MesProTaskScheduleExtDO ext = MesProTaskScheduleExtDO.builder()
                .id(30L)
                .taskId(3L)
                .scheduleSource("AUTO")
                .locked(Boolean.TRUE)
                .lockedReason("planner-lock")
                .build();
        when(taskMapper.selectById(3L)).thenReturn(task);
        when(taskScheduleExtMapper.selectByTaskId(3L)).thenReturn(ext);

        taskService.unlockTask(3L);

        ArgumentCaptor<MesProTaskScheduleExtDO> updateCaptor = ArgumentCaptor.forClass(MesProTaskScheduleExtDO.class);
        verify(taskScheduleExtMapper).updateById(updateCaptor.capture());
        MesProTaskScheduleExtDO update = updateCaptor.getValue();
        assertEquals(30L, update.getId());
        assertFalse(update.getLocked());
        assertEquals(null, update.getLockedReason());
    }

    @Test
    void updateProducedQuantity_shouldMarkPrepareTaskInProgress() {
        MesProTaskDO task = MesProTaskDO.builder()
                .id(4L)
                .status(MesProTaskStatusEnum.PREPARE.getStatus())
                .build();
        when(taskMapper.selectById(4L)).thenReturn(task);

        taskService.updateProducedQuantity(4L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO);

        ArgumentCaptor<MesProTaskDO> updateCaptor = ArgumentCaptor.forClass(MesProTaskDO.class);
        verify(taskMapper).updateById(updateCaptor.capture());
        MesProTaskDO update = updateCaptor.getValue();
        assertEquals(4L, update.getId());
        assertEquals(MesProTaskStatusEnum.IN_PROGRESS.getStatus(), update.getStatus());
        verify(taskMapper).updateProducedQuantity(4L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO);
    }
}
