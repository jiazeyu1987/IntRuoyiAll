package cn.iocoder.yudao.module.mes.controller.admin.pro.task;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.service.md.client.MesMdClientService;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdProductionLineService;
import cn.iocoder.yudao.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import cn.iocoder.yudao.module.mes.service.pro.task.MesProTaskService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProTaskControllerHistoryDisplayTest {

    @Mock
    private MesProTaskService taskService;
    @Mock
    private MesProWorkOrderService workOrderService;
    @Mock
    private MesMdWorkstationService workstationService;
    @Mock
    private MesMdProductionLineService productionLineService;
    @Mock
    private MesProProcessService processService;
    @Mock
    private MesMdItemService itemService;
    @Mock
    private MesMdClientService clientService;
    @Mock
    private MesMdUnitMeasureService unitMeasureService;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;

    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProReplanExplanationSnapshotMapper replanExplanationSnapshotMapper;
    @Mock
    private cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper scheduleOrderMapper;

    @InjectMocks
    private MesProTaskController controller;

    @Test
    void allEffectiveOrdersQueryOnlyLinkedWorkOrdersAndPreserveFilter() {
        var snapshot = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProReplanExplanationSnapshotDO();
        snapshot.setSnapshotJson("{\"orders\":[{\"scheduleOrderId\":7}]}");
        org.mockito.Mockito.lenient().when(replanExplanationSnapshotMapper.selectLatest()).thenReturn(snapshot);
        var order = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO();
        order.setId(7L);
        var historicalOrder = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO();
        historicalOrder.setId(17L);
        when(scheduleOrderMapper.selectList()).thenReturn(List.of(order, historicalOrder));
        var ext = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO();
        ext.setTaskId(8L); ext.setScheduleOrderId(7L); ext.setScheduleOrderProcessId(9L);
        var historicalExt = new cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO();
        historicalExt.setTaskId(18L); historicalExt.setScheduleOrderId(17L); historicalExt.setScheduleOrderProcessId(19L);
        when(taskScheduleExtMapper.selectListByScheduleOrderIds(Set.of(7L, 17L))).thenReturn(List.of(ext, historicalExt));
        MesProTaskDO task = new MesProTaskDO(); task.setId(8L); task.setWorkOrderId(10L);
        MesProTaskDO historicalTask = new MesProTaskDO(); historicalTask.setId(18L); historicalTask.setWorkOrderId(20L);
        when(taskService.getTaskList(Set.of(8L, 18L))).thenReturn(List.of(task, historicalTask));
        var request = new cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO();
        request.setCode("FILTER");
        when(workOrderService.getWorkOrderPageByIds(request, Set.of(10L, 20L)))
                .thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(), 0L));
        assertTrue(controller.listGanttTaskList(request).getData().isEmpty());
        assertEquals("FILTER", request.getCode());
        verify(workOrderService).getWorkOrderPageByIds(request, Set.of(10L, 20L));
        org.mockito.Mockito.verify(workOrderService, org.mockito.Mockito.never()).getWorkOrderPage(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verifyNoInteractions(replanExplanationSnapshotMapper);
        verify(taskScheduleExtMapper).selectListByScheduleOrderIds(Set.of(7L, 17L));
        org.mockito.Mockito.verify(taskService, org.mockito.Mockito.never()).getTaskListByWorkOrderIds(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void emptyScheduleScopeDoesNotQueryWholeWorkOrderPool() {
        when(scheduleOrderMapper.selectList()).thenReturn(List.of());
        assertTrue(controller.listGanttTaskList(new cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO()).getData().isEmpty());
        org.mockito.Mockito.verifyNoInteractions(workOrderService, taskService, taskScheduleExtMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildTaskRespVOList_usesIgnoreDeletedRouteProcessesForHistoryRows() throws Exception {
        MesProTaskDO task = new MesProTaskDO();
        task.setId(1L);
        task.setRouteId(11L);
        task.setProcessId(22L);

        MesProRouteProcessDO deletedRouteProcess = new MesProRouteProcessDO();
        deletedRouteProcess.setRouteId(11L);
        deletedRouteProcess.setProcessId(22L);
        deletedRouteProcess.setCheckFlag(Boolean.TRUE);

        when(workOrderService.getWorkOrderMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(workstationService.getWorkstationMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(processService.getProcessMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(itemService.getItemMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(unitMeasureService.getUnitMeasureMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(clientService.getClientMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(routeProcessService.getRouteProcessListByRouteIdsIgnoreDeleted(Set.of(11L)))
                .thenReturn(List.of(deletedRouteProcess));

        Method method = MesProTaskController.class.getDeclaredMethod("buildTaskRespVOList", List.class);
        method.setAccessible(true);

        List<?> result = (List<?>) method.invoke(controller, List.of(task));
        assertEquals(1, result.size());

        Object vo = result.get(0);
        Method getCheckFlag = vo.getClass().getMethod("getCheckFlag");

        assertTrue((Boolean) getCheckFlag.invoke(vo));
        verify(routeProcessService).getRouteProcessListByRouteIdsIgnoreDeleted(Set.of(11L));
    }
}
