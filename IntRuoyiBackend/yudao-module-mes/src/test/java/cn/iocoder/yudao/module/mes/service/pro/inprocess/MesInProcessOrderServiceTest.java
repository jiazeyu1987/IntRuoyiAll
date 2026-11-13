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
        when(mapper.selectCandidates(eq(1L),eq(request),any(),isNull(),isNull(),anyLong(),anyInt())).thenReturn(List.of(first,second,third));
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
        when(mapper.countCandidates(eq(1L),eq(request),any())).thenReturn(1L);
        when(mapper.selectCandidates(eq(1L),eq(request),any(),isNull(),isNull(),anyLong(),anyInt())).thenReturn(List.of(foreign));
        assertThrows(IllegalStateException.class, () -> service.page(request));
        when(mapper.countCandidates(eq(1L),eq(request),any())).thenReturn(0L);
        assertEquals(0L,service.page(request).getTotal());
        verifyNoInteractions(activeOrders,users,allocations,events,devices,inspections);
    }

    @Test void crossLeaderReadRetainsTheOriginalCyclesReleaseStage() {
        var request = new MesInProcessOrderPageReqVO();
        var order = order(1,10);
        when(mapper.countCandidates(eq(1L),eq(request),any())).thenReturn(1L);
        when(mapper.selectCandidates(eq(1L),eq(request),any(),isNull(),isNull(),anyLong(),anyInt())).thenReturn(List.of(order));
        var leader = new AdminUserRespDTO(); leader.setId(10L); leader.setNickname("原生产组长");
        when(users.getUserMap(List.of(10L))).thenReturn(Map.of(10L,leader));
        for (String status : List.of("PQC_RELEASE_PENDING", "REPORT_UPLOAD_PENDING",
                "MANAGER_RELEASE_PENDING", "NONCONFORMANCE_VOID", "NONCONFORMANCE_REWORK")) {
            when(activeOrders.projectActiveOrders(List.of(order))).thenReturn(List.of(
                    new MesTeamLeaderActiveOrderRow().setId(1L).setLeaderUserId(10L)
                            .setBusinessStatus("COMPLETED").setReleaseApplicationStatus(status)));
            var result = service.page(request);
            assertEquals(status, result.getList().get(0).getReleaseApplicationStatus());
            assertEquals("COMPLETED", result.getList().get(0).getBusinessStatus());
            assertEquals("原生产组长", result.getList().get(0).getLeaderName());
        }
    }
    @Test void dynamicFiltersCountEveryWindowButRetainOnlyTheRequestedPage() {
        var request = new MesInProcessOrderPageReqVO(); request.setPageNo(2); request.setPageSize(2);
        request.setListFilters("[{\"key\":\"status\",\"operator\":\"eq\",\"value\":\"已完工待放行\"},{\"key\":\"productionProgressPercent\",\"operator\":\"gte\",\"value\":80}]");
        var candidates = java.util.stream.LongStream.iterate(205L, id -> id > 0, id -> id - 1)
                .mapToObj(id -> order(id, 10)).toList();
        when(mapper.selectCandidates(eq(1L),eq(request),any(),isNull(),isNull(),eq(0L),eq(100)))
                .thenReturn(candidates.subList(0,100));
        when(mapper.selectCandidates(eq(1L),eq(request),any(),isNull(),eq(106L),eq(0L),eq(100)))
                .thenReturn(candidates.subList(100,200));
        when(mapper.selectCandidates(eq(1L),eq(request),any(),isNull(),eq(6L),eq(0L),eq(100)))
                .thenReturn(candidates.subList(200,205));
        var leader = new AdminUserRespDTO(); leader.setId(10L); leader.setNickname("组长");
        when(users.getUserMap(List.of(10L))).thenReturn(Map.of(10L,leader));
        when(activeOrders.projectActiveOrders(any())).thenAnswer(invocation -> {
            List<MesProcessPoolActiveOrderDO> window = invocation.getArgument(0);
            assertTrue(window.size() <= 100);
            return window.stream().map(value -> new MesTeamLeaderActiveOrderRow().setId(value.getId()).setLeaderUserId(10L)
                    .setReadBlocked(value.getId()%10 == 0).setReadBlockReason("实时证据缺失")
                    .setBusinessStatus(value.getId()%3 == 0 ? "COMPLETED" : "ACTIVE")
                    .setProductionProgressPercent(java.math.BigDecimal.valueOf(100))).toList();
        });
        var result = service.page(request);
        var expected = candidates.stream().filter(value -> value.getId()%3 == 0 && value.getId()%10 != 0).toList();
        assertEquals(expected.size(), result.getTotal());
        assertEquals(expected.subList(2,4).stream().map(MesProcessPoolActiveOrderDO::getId).toList(),
                result.getList().stream().map(MesInProcessOrderRespVO::getId).toList());
        verify(activeOrders,times(3)).projectActiveOrders(any());
        verify(mapper,never()).countCandidates(any(),any(),any());
        verifyNoInteractions(allocations,events,devices,inspections);
    }

    @Test void staticQueriesCountAndFetchOnlyTheDatabasePage() {
        var request = new MesInProcessOrderPageReqVO(); request.setPageNo(3); request.setPageSize(1);
        request.setListFilters("[{\"key\":\"productName\",\"operator\":\"contains\",\"value\":\"产品\"}]");
        var value = order(21, 10);
        when(mapper.countCandidates(eq(1L),eq(request),any())).thenReturn(2000L);
        when(mapper.selectCandidates(eq(1L),eq(request),any(),isNull(),isNull(),eq(2L),eq(1)))
                .thenReturn(List.of(value));
        var leader = new AdminUserRespDTO(); leader.setId(10L); leader.setNickname("组长");
        when(users.getUserMap(List.of(10L))).thenReturn(Map.of(10L,leader));
        when(activeOrders.projectActiveOrders(List.of(value))).thenReturn(List.of(new MesTeamLeaderActiveOrderRow().setId(21L).setLeaderUserId(10L)));
        var result = service.page(request);
        assertEquals(2000L,result.getTotal()); assertEquals(21L,result.getList().get(0).getId());
        verify(activeOrders).projectActiveOrders(List.of(value));
    }

    @Test void statusFiltersUseTheSamePriorityAsTheVisibleBadge() {
        var value = new MesInProcessOrderRespVO(); value.setBusinessStatus("VERSION_UPGRADE_PENDING");
        value.setReleaseApplicationStatus("PQC_RELEASE_PENDING"); value.setAbnormal(true);
        assertEquals("升版审批中",MesInProcessOrderService.statusLabel(value));
        value.setReleaseApplicationStatus("NONCONFORMANCE_REWORK");
        assertEquals("已结束并转入返工",MesInProcessOrderService.statusLabel(value));
        value.setReadBlocked(true); assertEquals("数据异常",MesInProcessOrderService.statusLabel(value));
        value.setReadBlocked(false); value.setReleaseApplicationStatus("FUTURE"); value.setAbnormal(false); value.setBusinessStatus("ACTIVE");
        assertEquals("未知放行状态：FUTURE",MesInProcessOrderService.statusLabel(value));
    }

    @Test void unpagedAndInvalidPageRequestsFailExplicitly() {
        var request = new MesInProcessOrderPageReqVO(); request.setPageSize(-1);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.page(request));
        request.setPageSize(201);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.page(request));
        request.setPageSize(1); request.setPageNo(0);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class, () -> service.page(request));
        verifyNoInteractions(mapper,activeOrders,users,allocations,events,devices,inspections);
    }

    private <T extends cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO> T tenant(T value) {
        value.setTenantId(1L); return value;
    }
    private MesProcessPoolActiveOrderDO order(long id,long leader) {
        return tenant(MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(id).leaderUserId(leader).activeStatus("ACTIVE").businessStatus("ACTIVE").build());
    }
}
