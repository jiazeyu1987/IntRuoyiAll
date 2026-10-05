package cn.iocoder.yudao.module.mes.service.pro.processpool;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesSharedProductionReportCorrectionGuardTest {
    MesSharedProductionReportCorrectionGuard guard;
    MesProcessPoolReportAllocationMapper allocations;
    MesProcessPoolActiveOrderMapper orders;
    MesProcessPoolActiveOrderCompletionReceiptMapper receipts;
    MesReportAllocationReleaseStateService releases;
    MesProEdhrNonconformanceReviewService freezes;
    MesProProcessPoolEventDO event;MesProcessPoolActiveOrderDO source,target;MesProcessPoolReportAllocationDO allocation;
    @BeforeEach void setup(){
        TenantContextHolder.setTenantId(1L);guard=new MesSharedProductionReportCorrectionGuard();
        allocations=mock(MesProcessPoolReportAllocationMapper.class);orders=mock(MesProcessPoolActiveOrderMapper.class);
        receipts=mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);releases=mock(MesReportAllocationReleaseStateService.class);
        freezes=mock(MesProEdhrNonconformanceReviewService.class);
        for(var e:Map.of("allocations",allocations,"orders",orders,"receipts",receipts,"releases",releases,"freezes",freezes).entrySet())ReflectionTestUtils.setField(guard,e.getKey(),e.getValue());
        event=MesProProcessPoolEventDO.builder().id(176L).workOrderId(274L).routeId(98L).routeProcessId(91L).processId(15L).rawPayload("{\"activeOrderId\":413,\"pressure\":20}").build();event.setTenantId(1L);
        source=order(413L,274L);target=order(414L,275L);
        when(orders.selectByIdForUpdate(413L)).thenReturn(source);when(orders.selectByIdForUpdate(414L)).thenReturn(target);
        allocation=new MesProcessPoolReportAllocationDO().setId(5L).setEventId(176L).setActiveOrderId(414L).setWorkOrderId(275L).setRouteProcessId(91L).setProcessId(15L).setLifecycleStatus("CURRENT").setAllocatedQuantity(BigDecimal.ONE);allocation.setTenantId(1L);
        when(allocations.selectListByEventIdForUpdate(176L)).thenReturn(List.of(allocation));
        when(releases.findReleaseApplicationLockedActiveOrderIdsForUpdate(any())).thenReturn(Set.of());
    }
    static MesProcessPoolActiveOrderDO order(long id,long work){var o=MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(work).routeId(98L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();o.setTenantId(1L);return o;}
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void crossOrderSharedParameterCanChangeOnlyWhileBothOrdersAreEditable(){assertDoesNotThrow(()->guard.assertEditable(event));verify(freezes).ensureWorkOrderNotFrozen(274L,"共享报工正文更正");verify(freezes).ensureWorkOrderNotFrozen(275L,"共享报工正文更正");}
    @org.junit.jupiter.params.ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(strings={"sourceFreeze","targetFreeze","completed","receipt","release","foreignTenant","wrongProcess","wrongWorkOrder"})
    void sharedNonQuantityChangeCannotBypassAnyCurrentConsumer(String defect){
        switch(defect){
            case "sourceFreeze"->doThrow(new IllegalStateException("source frozen")).when(freezes).ensureWorkOrderNotFrozen(274L,"共享报工正文更正");
            case "targetFreeze"->doThrow(new IllegalStateException("target frozen")).when(freezes).ensureWorkOrderNotFrozen(275L,"共享报工正文更正");
            case "completed"->target.setBusinessStatus("COMPLETED");
            case "receipt"->when(receipts.selectByActiveOrderIdForUpdate(414L)).thenReturn(new MesProcessPoolActiveOrderCompletionReceiptDO());
            case "release"->when(releases.findReleaseApplicationLockedActiveOrderIdsForUpdate(any())).thenReturn(Set.of(414L));
            case "foreignTenant"->target.setTenantId(2L);
            case "wrongProcess"->allocation.setProcessId(16L);
            case "wrongWorkOrder"->allocation.setWorkOrderId(276L);
            default->throw new IllegalArgumentException(defect);
        }
        assertThrows(RuntimeException.class,()->guard.assertEditable(event));
    }
    @Test void zeroAllocationDoesNotFreezeAnUnconsumingNeighbor(){allocation.setAllocatedQuantity(BigDecimal.ZERO);target.setBusinessStatus("COMPLETED");assertDoesNotThrow(()->guard.assertEditable(event));verify(orders,never()).selectByIdForUpdate(414L);}
    /** Real guard with explicitly open fixture rows for existing correction/signature transaction tests. */
    public static MesSharedProductionReportCorrectionGuard openFixture(){return openFixture(980008L,922119L);}
    public static MesSharedProductionReportCorrectionGuard openFixture(long workOrderId,long routeId){
        var guard=new MesSharedProductionReportCorrectionGuard();var allocations=mock(MesProcessPoolReportAllocationMapper.class);
        var orders=mock(MesProcessPoolActiveOrderMapper.class);var receipts=mock(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
        var releases=mock(MesReportAllocationReleaseStateService.class);var freezes=mock(MesProEdhrNonconformanceReviewService.class);
        org.mockito.Mockito.lenient().when(allocations.selectListByEventIdForUpdate(anyLong())).thenReturn(List.of());
        var source=order(413L,workOrderId).setRouteId(routeId);org.mockito.Mockito.lenient().when(orders.selectByIdForUpdate(413L)).thenReturn(source);
        org.mockito.Mockito.lenient().when(releases.findReleaseApplicationLockedActiveOrderIdsForUpdate(any())).thenReturn(Set.of());
        for(var e:Map.of("allocations",allocations,"orders",orders,"receipts",receipts,"releases",releases,"freezes",freezes).entrySet())ReflectionTestUtils.setField(guard,e.getKey(),e.getValue());return guard;
    }
}
