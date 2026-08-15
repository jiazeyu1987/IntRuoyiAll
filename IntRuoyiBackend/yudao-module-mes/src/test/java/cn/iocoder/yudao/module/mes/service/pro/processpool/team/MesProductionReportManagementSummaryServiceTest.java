package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReportManagementSummaryServiceTest {

    @Mock
    private MesProProcessPoolEventMapper eventMapper;
    @Mock
    private MesProcessPoolReportAllocationMapper allocationMapper;
    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper;
    @Mock
    private MesReportAllocationPoolQuantityService poolQuantityService;
    @Mock
    private MesReportAllocationReleaseStateService releaseStateService;

    private MesProductionReportManagementSummaryService service;

    @BeforeEach
    void setUp() {
        service = new MesProductionReportManagementSummaryService(eventMapper, allocationMapper,
                releaseApplicationMapper, poolQuantityService, releaseStateService);
    }

    @Test
    void shouldInitializeNewProductionReportAsUnallocated() {
        MesProProcessPoolEventDO event = productionEvent();
        when(poolQuantityService.requireSubmittedOutputQuantity(event)).thenReturn(new BigDecimal("10"));

        service.initializeProductionEvent(event);

        assertEquals(new BigDecimal("10"), event.getReportOutputQuantity());
        assertEquals(BigDecimal.ZERO, event.getReportAllocatedQuantity());
        assertEquals(new BigDecimal("10"), event.getReportUnallocatedQuantity());
        assertEquals(MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_NOT_ALLOCATED,
                event.getReportReleaseStatus());
        assertEquals(MesProProcessPoolEventDO.REPORT_MANAGEMENT_STATUS_UNALLOCATED,
                event.getReportManagementStatus());
    }

    @Test
    void shouldArchiveFullyAllocatedAndReleasedProductionReport() {
        MesProProcessPoolEventDO event = productionEvent();
        List<MesProcessPoolReportAllocationDO> allocations = List.of(
                allocation(101L, "4"), allocation(102L, "6"));
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("10"));
        when(allocationMapper.selectListByEventId(1L)).thenReturn(allocations);
        when(releaseStateService.findReleasedActiveOrderIds(Set.of(101L, 102L)))
                .thenReturn(Set.of(101L, 102L));
        when(eventMapper.updateById(any(MesProProcessPoolEventDO.class))).thenReturn(1);

        service.refreshProductionEvent(event);

        ArgumentCaptor<MesProProcessPoolEventDO> captor = ArgumentCaptor.forClass(MesProProcessPoolEventDO.class);
        verify(eventMapper).updateById(captor.capture());
        MesProProcessPoolEventDO update = captor.getValue();
        assertEquals(new BigDecimal("10"), update.getReportAllocatedQuantity());
        assertEquals(BigDecimal.ZERO, update.getReportUnallocatedQuantity());
        assertEquals(MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_RELEASED,
                update.getReportReleaseStatus());
        assertEquals(MesProProcessPoolEventDO.REPORT_MANAGEMENT_STATUS_ARCHIVED,
                update.getReportManagementStatus());
    }

    @Test
    void shouldKeepPartiallyAllocatedProductionReportInWorkbench() {
        MesProProcessPoolEventDO event = productionEvent();
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("10"));
        when(allocationMapper.selectListByEventId(1L)).thenReturn(List.of(allocation(101L, "4")));
        when(releaseStateService.findReleasedActiveOrderIds(Set.of(101L))).thenReturn(Set.of());
        when(eventMapper.updateById(any(MesProProcessPoolEventDO.class))).thenReturn(1);

        service.refreshProductionEvent(event);

        assertEquals(new BigDecimal("4"), event.getReportAllocatedQuantity());
        assertEquals(new BigDecimal("6"), event.getReportUnallocatedQuantity());
        assertEquals(MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_NOT_RELEASED,
                event.getReportReleaseStatus());
        assertEquals(MesProProcessPoolEventDO.REPORT_MANAGEMENT_STATUS_PARTIALLY_ALLOCATED,
                event.getReportManagementStatus());
    }

    @Test
    void shouldKeepFullyAllocatedUnreleasedProductionReportPendingRelease() {
        MesProProcessPoolEventDO event = productionEvent();
        when(poolQuantityService.requirePoolQuantity(event)).thenReturn(new BigDecimal("10"));
        when(allocationMapper.selectListByEventId(1L)).thenReturn(List.of(allocation(101L, "10")));
        when(releaseStateService.findReleasedActiveOrderIds(Set.of(101L))).thenReturn(Set.of());
        when(eventMapper.updateById(any(MesProProcessPoolEventDO.class))).thenReturn(1);

        service.refreshProductionEvent(event);

        assertEquals(BigDecimal.ZERO, event.getReportUnallocatedQuantity());
        assertEquals(MesProProcessPoolEventDO.REPORT_RELEASE_STATUS_NOT_RELEASED,
                event.getReportReleaseStatus());
        assertEquals(MesProProcessPoolEventDO.REPORT_MANAGEMENT_STATUS_PENDING_RELEASE,
                event.getReportManagementStatus());
    }

    private static MesProProcessPoolEventDO productionEvent() {
        return new MesProProcessPoolEventDO().setId(1L)
                .setEventType(MesProProcessPoolEventDO.EVENT_TYPE_PRODUCTION_SUBMIT)
                .setRawPayload("{\"outputQuantity\":10}");
    }

    private static MesProcessPoolReportAllocationDO allocation(Long activeOrderId, String quantity) {
        return new MesProcessPoolReportAllocationDO().setEventId(1L).setActiveOrderId(activeOrderId)
                .setAllocatedQuantity(new BigDecimal(quantity));
    }
}
