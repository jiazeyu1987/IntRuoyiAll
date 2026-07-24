package cn.iocoder.yudao.module.mes.service.dv.machinery;

import cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.machinery.MesDvMachineryProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesDvMachineryProcessServiceImplTest {

    @Mock
    private MesDvMachineryProcessMapper machineryProcessMapper;
    @Mock
    private MesProRouteProcessService routeProcessService;
    @InjectMocks
    private MesDvMachineryProcessServiceImpl machineryProcessService;

    @Test
    @SuppressWarnings("unchecked")
    void getMachineryProcessListByMachineryIdsAndProcessIds_mapsLegacyBindingToCurrentIdentity()
            throws Exception {
        when(routeProcessService.getProcessIdentityMap(List.of(901L)))
                .thenReturn(Map.of(900L, 901L, 901L, 901L));
        when(machineryProcessMapper.selectListByMachineryIds(List.of(11L))).thenReturn(List.of(
                MesDvMachineryProcessDO.builder()
                        .id(1L)
                        .machineryId(11L)
                        .processId(900L)
                        .build()));

        Method method = MesDvMachineryProcessServiceImpl.class.getMethod(
                "getMachineryProcessListByMachineryIdsAndProcessIds", Collection.class, Collection.class);
        List<MesDvMachineryProcessDO> result = (List<MesDvMachineryProcessDO>) method.invoke(
                machineryProcessService, List.of(11L), List.of(901L));

        assertEquals(1, result.size());
        assertEquals(901L, result.get(0).getProcessId());
    }

    @Test
    void getMachineryProcessListByMachineryIdsAndProcessIds_prefersExplicitTargetCapacityOverLegacyAlias() {
        when(routeProcessService.getProcessIdentityMap(List.of(922851L)))
                .thenReturn(Map.of(900851L, 922851L, 922851L, 922851L));
        when(machineryProcessMapper.selectListByMachineryIds(List.of(47L))).thenReturn(List.of(
                MesDvMachineryProcessDO.builder()
                        .id(1L)
                        .machineryId(47L)
                        .processId(900851L)
                        .standardHourlyCapacity(new BigDecimal("12"))
                        .build(),
                MesDvMachineryProcessDO.builder()
                        .id(2L)
                        .machineryId(47L)
                        .processId(922851L)
                        .standardHourlyCapacity(new BigDecimal("20"))
                        .build()));

        List<MesDvMachineryProcessDO> result =
                machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                        List.of(47L), List.of(922851L));

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(922851L, result.get(0).getProcessId());
        assertEquals(0, result.get(0).getStandardHourlyCapacity().compareTo(new BigDecimal("20")));
    }

    @Test
    void getMachineryProcessListByMachineryIdsAndProcessIds_ignoresActiveSiblingProcessForRequestedTarget() {
        when(routeProcessService.getProcessIdentityMap(List.of(922919L)))
                .thenReturn(Map.of(922851L, 922919L, 922919L, 922919L));
        when(machineryProcessMapper.selectListByMachineryIds(List.of(47L))).thenReturn(List.of(
                MesDvMachineryProcessDO.builder()
                        .id(852L)
                        .machineryId(47L)
                        .processId(922851L)
                        .standardHourlyCapacity(new BigDecimal("25.714286"))
                        .build(),
                MesDvMachineryProcessDO.builder()
                        .id(915L)
                        .machineryId(47L)
                        .processId(922896L)
                        .standardHourlyCapacity(new BigDecimal("61.904762"))
                        .build(),
                MesDvMachineryProcessDO.builder()
                        .id(954L)
                        .machineryId(47L)
                        .processId(922919L)
                        .standardHourlyCapacity(new BigDecimal("25.714286"))
                        .build()));

        List<MesDvMachineryProcessDO> result =
                machineryProcessService.getMachineryProcessListByMachineryIdsAndProcessIds(
                        List.of(47L), List.of(922919L));

        assertEquals(1, result.size());
        assertEquals(954L, result.get(0).getId());
        assertEquals(922919L, result.get(0).getProcessId());
        assertEquals(0, result.get(0).getStandardHourlyCapacity().compareTo(new BigDecimal("25.714286")));
    }
}
