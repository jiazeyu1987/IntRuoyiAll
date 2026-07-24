package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.resourceadjustment.MesProScheduleResourceAdjustmentSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProScheduleResourceAdjustmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProScheduleResourceAdjustmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProScheduleResourceAdjustmentServiceImplTest {

    @InjectMocks
    private MesProScheduleResourceAdjustmentServiceImpl service;

    @Mock
    private MesProScheduleResourceAdjustmentMapper adjustmentMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;

    @Test
    void saveAdjustment_shouldInsertNewMachineAdjustment() {
        MesProScheduleResourceAdjustmentSaveReqVO reqVO = new MesProScheduleResourceAdjustmentSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteProcessId(100L);
        reqVO.setCalendarDate(LocalDate.of(2026, 6, 10));
        reqVO.setResourceType("MACHINE");
        reqVO.setWorkstationMachineId(500L);
        reqVO.setMachineryId(600L);
        reqVO.setAvailableQuantityOverride(0);
        reqVO.setReason("设备今日维修");
        when(routeMapper.selectById(10L)).thenReturn(MesProRouteDO.builder().id(10L).build());
        when(routeProcessService.resolveCurrentRouteProcess(100L, 10L, null))
                .thenReturn(MesProRouteProcessDO.builder().id(100L).routeId(10L).build());
        when(adjustmentMapper.selectAdjustment(100L, LocalDate.of(2026, 6, 10), "MACHINE", 500L, 600L)).thenReturn(null);

        service.saveAdjustment(reqVO);

        ArgumentCaptor<MesProScheduleResourceAdjustmentDO> captor =
                ArgumentCaptor.forClass(MesProScheduleResourceAdjustmentDO.class);
        verify(adjustmentMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getAvailableQuantityOverride());
        assertEquals("设备今日维修", captor.getValue().getReason());
    }

    @Test
    void saveAdjustment_shouldUpdateExistingWorkerAdjustment() {
        MesProScheduleResourceAdjustmentSaveReqVO reqVO = new MesProScheduleResourceAdjustmentSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteProcessId(100L);
        reqVO.setCalendarDate(LocalDate.of(2026, 6, 10));
        reqVO.setResourceType("WORKER");
        reqVO.setWorkerQuantityOverride(8);
        reqVO.setSingleHourlyCapacityOverride(new BigDecimal("12.5"));
        reqVO.setShiftHoursOverride(new BigDecimal("11.0"));
        reqVO.setReason("加班加人");
        when(routeMapper.selectById(10L)).thenReturn(MesProRouteDO.builder().id(10L).build());
        when(routeProcessService.resolveCurrentRouteProcess(100L, 10L, null))
                .thenReturn(MesProRouteProcessDO.builder().id(100L).routeId(10L).build());
        when(adjustmentMapper.selectAdjustment(100L, LocalDate.of(2026, 6, 10), "WORKER", null, null))
                .thenReturn(MesProScheduleResourceAdjustmentDO.builder().id(1L).build());

        service.saveAdjustment(reqVO);

        ArgumentCaptor<MesProScheduleResourceAdjustmentDO> captor =
                ArgumentCaptor.forClass(MesProScheduleResourceAdjustmentDO.class);
        verify(adjustmentMapper).updateById(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(8, captor.getValue().getWorkerQuantityOverride());
        assertEquals(new BigDecimal("11.0"), captor.getValue().getShiftHoursOverride());
    }

    @Test
    void saveAdjustment_shouldRejectMismatchedRouteProcess() {
        MesProScheduleResourceAdjustmentSaveReqVO reqVO = new MesProScheduleResourceAdjustmentSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteProcessId(100L);
        reqVO.setCalendarDate(LocalDate.of(2026, 6, 10));
        reqVO.setResourceType("WORKER");
        when(routeMapper.selectById(10L)).thenReturn(MesProRouteDO.builder().id(10L).build());
        when(routeProcessService.resolveCurrentRouteProcess(100L, 10L, null))
                .thenReturn(MesProRouteProcessDO.builder().id(100L).routeId(11L).build());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveAdjustment(reqVO));

        assertEquals(PRO_ROUTE_FLOW_CONFIG_PROCESS_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void saveAdjustment_shouldNormalizeHistoricalRouteProcessId() {
        MesProScheduleResourceAdjustmentSaveReqVO reqVO = new MesProScheduleResourceAdjustmentSaveReqVO();
        reqVO.setRouteId(10L);
        reqVO.setRouteProcessId(99L);
        reqVO.setCalendarDate(LocalDate.of(2026, 6, 10));
        reqVO.setResourceType("MACHINE");
        reqVO.setWorkstationMachineId(500L);
        reqVO.setMachineryId(600L);
        reqVO.setAvailableQuantityOverride(1);
        reqVO.setReason("旧工序调整");
        when(routeMapper.selectById(10L)).thenReturn(MesProRouteDO.builder().id(10L).build());
        when(routeProcessService.resolveCurrentRouteProcess(99L, 10L, null))
                .thenReturn(MesProRouteProcessDO.builder().id(100L).routeId(10L).build());
        when(adjustmentMapper.selectAdjustment(100L, LocalDate.of(2026, 6, 10), "MACHINE", 500L, 600L)).thenReturn(null);

        service.saveAdjustment(reqVO);

        ArgumentCaptor<MesProScheduleResourceAdjustmentDO> captor =
                ArgumentCaptor.forClass(MesProScheduleResourceAdjustmentDO.class);
        verify(adjustmentMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getRouteProcessId());
    }

    @Test
    void getAdjustmentList_shouldReturnRouteDateAdjustments() {
        MesProScheduleResourceAdjustmentDO row = MesProScheduleResourceAdjustmentDO.builder()
                .id(1L).routeId(10L).routeProcessId(100L).build();
        when(routeMapper.selectById(10L)).thenReturn(MesProRouteDO.builder().id(10L).build());
        when(adjustmentMapper.selectListByRouteAndDate(10L, LocalDate.of(2026, 6, 10))).thenReturn(List.of(row));

        List<MesProScheduleResourceAdjustmentDO> result = service.getAdjustmentList(10L, LocalDate.of(2026, 6, 10));

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getRouteProcessId());
    }

}
