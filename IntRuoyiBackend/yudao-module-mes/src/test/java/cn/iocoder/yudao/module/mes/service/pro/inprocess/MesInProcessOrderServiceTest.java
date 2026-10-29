package cn.iocoder.yudao.module.mes.service.pro.inprocess;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.inprocess.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.inprocess.MesInProcessOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesInProcessOrderServiceTest {
    @Mock MesInProcessOrderMapper mapper;
    @Mock MesTeamLeaderActiveOrderService activeOrders;
    @Mock AdminUserApi users;
    @Mock MesProcessPoolReportAllocationMapper allocations;
    @Mock MesProProcessPoolEventMapper events;
    @Mock MesProcessPoolTeamDeviceMapper devices;
    @Mock MesPqcProcessInspectionAggregateDetailMapper inspections;
    @InjectMocks MesInProcessOrderService service;
    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void selectedAndMaterialEquipmentMatchButConfiguredDevicesDoNot() {
        String payload = "{\"selectedDevices\":[{\"deviceId\":\"31\",\"deviceCode\":\"EQ-1\"}],\"materialDetails\":[{\"selectedDevices\":[{\"deviceId\":32,\"deviceName\":\"设备乙\"}]}]}";
        assertTrue(MesInProcessOrderService.productionMatches(payload,"eq-1",Set.of()));
        assertTrue(MesInProcessOrderService.productionMatches(payload,"设备乙",Set.of()));
        assertTrue(MesInProcessOrderService.productionMatches(payload,"主档名",Set.of(31L)));
        assertFalse(MesInProcessOrderService.productionMatches(payload,"不存在",Set.of(99L)));
        assertFalse(MesInProcessOrderService.productionMatches("{}","设备",Set.of(31L)));
        assertThrows(IllegalStateException.class, () -> MesInProcessOrderService.productionMatches("", "设备",Set.of()));
        assertThrows(IllegalStateException.class, () -> MesInProcessOrderService.productionMatches("{\"selectedDevices\":[{\"deviceName\":\"设备\"}]}", "设备",Set.of()));
    }

    @Test void equipmentFilterRunsBeforePaginationAndIncludesPqcAcrossLeaders() {
        var first = order(1,10); var second = order(2,20); var third = order(3,30);
        var request = new MesInProcessOrderPageReqVO(); request.setEquipment("EQ"); request.setPageSize(1); request.setPageNo(2);
        when(mapper.selectCandidates(1L,request)).thenReturn(List.of(first,second,third));
        when(allocations.selectListByActiveOrderIds(any())).thenReturn(List.of(tenant(MesProcessPoolReportAllocationDO.builder().activeOrderId(2L).workOrderId(2L).eventId(9L).build())));
        when(events.selectBatchIds(any())).thenReturn(List.of(tenant(MesProProcessPoolEventDO.builder().id(9L).eventType("PRODUCTION_SUBMIT").rawPayload("{\"selectedDevices\":[{\"deviceId\":31,\"deviceCode\":\"EQ-1\"}]}").build())));
        when(devices.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(inspections.selectList(any(Wrapper.class))).thenReturn(List.of(tenant(MesPqcProcessInspectionAggregateDetailDO.builder().activeOrderId(3L).workOrderId(3L).selectedEquipmentId(7L).selectedEquipmentName("EQ检验").build())));
        var leader = new AdminUserRespDTO(); leader.setId(30L); leader.setNickname("组长丙");
        when(users.getUserMap(List.of(30L))).thenReturn(Map.of(30L,leader));
        when(activeOrders.projectActiveOrders(List.of(third))).thenReturn(List.of(new MesTeamLeaderActiveOrderRow().setId(3L).setLeaderUserId(30L)));
        var result = service.page(request);
        assertEquals(2L,result.getTotal()); assertEquals(3L,result.getList().get(0).getId());
        assertEquals("组长丙",result.getList().get(0).getLeaderName());
        verify(activeOrders).projectActiveOrders(List.of(third));
    }

    @Test void invalidTenantFailsAndEmptyPageDoesNotReadProjections() {
        var request = new MesInProcessOrderPageReqVO();
        var foreign = order(1,10); foreign.setTenantId(2L);
        when(mapper.selectCandidates(1L,request)).thenReturn(List.of(foreign));
        assertThrows(IllegalStateException.class, () -> service.page(request));
        when(mapper.selectCandidates(1L,request)).thenReturn(List.of());
        assertEquals(0L,service.page(request).getTotal());
        verifyNoInteractions(activeOrders,users,allocations,events,devices,inspections);
    }
    private <T extends cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO> T tenant(T value) {
        value.setTenantId(1L); return value;
    }
    private MesProcessPoolActiveOrderDO order(long id,long leader) {
        return tenant(MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(id).leaderUserId(leader).activeStatus("ACTIVE").businessStatus("ACTIVE").build());
    }
}
