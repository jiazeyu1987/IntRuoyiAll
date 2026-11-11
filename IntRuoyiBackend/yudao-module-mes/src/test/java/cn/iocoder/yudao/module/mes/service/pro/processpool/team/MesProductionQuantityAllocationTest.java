package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesProductionQuantityAllocationTest {
    MesReportAllocationCommandService service;
    <T> T dependency(String name,Class<T> type){return type.cast(ReflectionTestUtils.getField(service,name));}
    BigDecimal qty(String value){return new BigDecimal(value);}
    MesProcessPoolActiveOrderDO a,b;
    MesProProcessPoolEventDO event;
    MesProcessPoolReportAllocationStateDO state;
    @BeforeEach void setup() throws Exception {
        var constructor=MesReportAllocationCommandService.class.getConstructors()[0];
        service=(MesReportAllocationCommandService)constructor.newInstance(Arrays.stream(constructor.getParameterTypes()).map(t->mock(t)).toArray());
        for(var field:MesReportAllocationCommandService.class.getDeclaredFields()){
            if(field.isAnnotationPresent(jakarta.annotation.Resource.class)) ReflectionTestUtils.setField(service,field.getName(),mock(field.getType()));
        }
        MesProductionDisplayedContextFixture.attach(service);
        a=order(10L,30L);b=order(11L,31L);
        event=MesProProcessPoolEventDO.builder().id(100L).eventType("PRODUCTION_SUBMIT").eventIdempotencyKey("quantity-test")
            .actualEmployeeId(40L).signatureId(400L).deviceAccountId(40L).workOrderId(30L).routeId(50L).routeProcessId(60L).processId(70L)
            .reportOutputQuantity(qty("600")).rawPayload("{\"activeOrderProcess\":{\"activeOrderId\":10},\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":600}]}").build();
        state=MesProcessPoolReportAllocationStateDO.builder().id(200L).eventId(100L).currentVersion(1).build();
        when(dependency("eventMapper",MesProProcessPoolEventMapper.class).selectByIdForUpdate(100L)).thenReturn(event);
        when(dependency("poolQuantityService",MesReportAllocationPoolQuantityService.class).requirePoolQuantity(event)).thenReturn(qty("600"));
        when(dependency("stateMapper",MesProcessPoolReportAllocationStateMapper.class).selectByEventIdForUpdate(100L)).thenReturn(state);
        var orders=dependency("activeOrderMapper",MesProcessPoolActiveOrderMapper.class);
        when(orders.selectActiveListByLeaderForUpdate(20L)).thenReturn(List.of(a,b));
        when(orders.selectByIdForUpdate(10L)).thenReturn(a);when(orders.selectById(10L)).thenReturn(a);when(orders.selectById(11L)).thenReturn(b);
        var works=dependency("workOrderMapper",MesProWorkOrderMapper.class);
        when(works.selectListByIdsForUpdate(anyList())).thenReturn(List.of(work(30L),work(31L)));
        when(works.selectListByIds(anyList())).thenReturn(List.of(work(30L),work(31L)));
        when(works.selectById(30L)).thenReturn(work(30L));
        var targets=dependency("targetService",MesTeamLeaderOrderProcessTargetService.class);
        when(targets.requireUniqueTargetForProcess(any(),eq(70L))).thenReturn(new MesTeamLeaderOrderProcessTarget(60L,70L,qty("1000"),qty("1.1"),qty("1100")));
        when(dependency("routeStartAuthorizationService",MesRouteStartProductionLeaderAuthorizationService.class).listAuthorizedRouteProcesses(20L))
            .thenReturn(List.of(MesProRouteProcessDO.builder().id(60L).processId(70L).build()));
        var snapshots=dependency("activeOrderProcessSnapshotMapper",MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        when(snapshots.selectListByActiveOrderAndProcessForUpdate(anyLong(),eq(70L))).thenAnswer(i->List.of(snapshot(i.getArgument(0))));
        when(snapshots.selectByActiveOrderAndProcess(anyLong(),eq(60L),eq(70L))).thenAnswer(i->snapshot(i.getArgument(0)));
        var allocations=dependency("allocationMapper",MesProcessPoolReportAllocationMapper.class);
        when(allocations.selectListByEventIdForUpdate(100L)).thenReturn(List.of(MesProcessPoolReportAllocationDO.builder().id(201L)
            .eventId(100L).reviewId(300L).activeOrderId(10L).workOrderId(30L).routeProcessId(60L).processId(70L)
            .allocatedQuantity(qty("600")).lifecycleStatus("CURRENT").createdVersion(1).build()));
        when(allocations.supersedeCurrentRows(anyCollection(),eq(2))).thenReturn(1);
        when(allocations.insertBatch(anyCollection())).thenReturn(true);
        when(dependency("auditMapper",MesProcessPoolReportAllocationAdjustmentAuditMapper.class).insertBatch(anyCollection())).thenReturn(true);
        when(dependency("stateMapper",MesProcessPoolReportAllocationStateMapper.class).updateById(state)).thenReturn(1);
        when(dependency("reviewMapper",MesProcessPoolSubmissionReviewMapper.class).selectLatestByEventIdForUpdate(100L)).thenReturn(
            MesProcessPoolSubmissionReviewDO.builder().id(300L).eventId(100L).reviewRound(0).reviewStatus("APPROVED")
            .leaderUserId(20L).leaderType("PRODUCTION").reviewSignatureId(400L).reviewSignatureUserId(20L)
            .reviewSignatureSnapshotJson("{\"signature\":\"review\"}").reviewedAt(LocalDateTime.now()).build());
        when(dependency("signatureService",MesProBatchRecordExecutionSignatureService.class).recordTeamLeaderReviewSignature(any(),any(),any(),any(),any(),any())).thenReturn(401L);
        when(dependency("signatureService",MesProBatchRecordExecutionSignatureService.class).recordFieldChangeSignature(any()))
            .thenReturn(new MesProBatchRecordExecutionFieldAuditSignatureResult().setSignatureId(402L).setActorId(20L).setPasswordVerified(true).setSignedAt(LocalDateTime.now()));
    }
    MesProcessPoolActiveOrderDO order(long id,long work){return MesProcessPoolActiveOrderDO.builder().id(id).workOrderId(work).leaderUserId(20L)
        .routeId(50L).routeVersionId(51L).dccProjectCodeId(80L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();}
    MesProWorkOrderDO work(long id){return MesProWorkOrderDO.builder().id(id).productId(90L).materialSpecification("SAME").code("QTY-"+id).quantity(qty("1000")).build();}
    MesProcessPoolActiveOrderProcessSnapshotDO snapshot(long id){return MesProcessPoolActiveOrderProcessSnapshotDO.builder().activeOrderId(id).workOrderId(id==10?30L:31L)
        .routeId(50L).routeVersionId(51L).routeProcessId(60L).processId(70L).erpFixedQuantitySnapshot(qty("1000"))
        .productionQuantityFactorSnapshot(qty("1.1")).plannedQuantitySnapshot(qty("1100")).productionConfigSnapshotJson("{\"outputMaterialIds\":[501]}").build();}
    MesReportAllocationSaveCommand command(String x,String y){return MesReportAllocationSaveCommand.builder().eventId(100L).leaderUserId(20L).leaderType("PRODUCTION")
        .expectedVersion(1).allocationMode("MANUAL").idempotencyKey("correction-1").signaturePassword("test-password").reason("核对错单后更正原始600的归属")
        .allocations(List.of(MesReportAllocationSaveLine.builder().activeOrderId(10L).allocatedQuantity(qty(x)).build(),
            MesReportAllocationSaveLine.builder().activeOrderId(11L).allocatedQuantity(qty(y)).build())).build();}
    @Test void partialCorrectionPreservesSixHundredAndReconcilesBothOrders(){
        var result=MesProductionDisplayedContextFixture.save(service,command("400","200"));
        assertEquals(2,result.getVersion());assertEquals(0,qty("600").compareTo(result.getTotalAllocatedQuantity()));
        assertEquals(List.of(qty("400"),qty("200")),result.getLines().stream().map(MesReportAllocationSnapshotLine::getAllocatedQuantity).toList());
        verify(dependency("gxpAuditService",GxpAuditService.class)).append(argThat(audit->"402".equals(audit.getSignatureRecordId())));
        verify(dependency("completionService",MesTeamLeaderOrderProcessCompletionService.class)).reconcileAffectedAllocations(eq(event),argThat(rows->rows.stream().map(MesProcessPoolReportAllocationDO::getActiveOrderId).collect(java.util.stream.Collectors.toSet()).equals(Set.of(10L,11L))));
    }
    @Test void correctionCannotDiscardQuantity(){
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->MesProductionDisplayedContextFixture.save(service,command("300","200")));
        verify(dependency("allocationMapper",MesProcessPoolReportAllocationMapper.class),never()).insertBatch(anyCollection());
    }
    @Test void anOldApprovedReviewCannotReplaceTheCorrectionSignature(){
        when(dependency("signatureService",MesProBatchRecordExecutionSignatureService.class).recordFieldChangeSignature(any()))
            .thenThrow(new IllegalStateException("密码错误"));
        assertThrows(IllegalStateException.class,()->MesProductionDisplayedContextFixture.save(service,command("400","200")));
        verify(dependency("allocationMapper",MesProcessPoolReportAllocationMapper.class),never()).supersedeCurrentRows(anyCollection(),any());
    }
    @Test void fullCorrectionMovesAllSixHundredWithoutEditingOriginalEvent(){
        var command=command("400","200");
        command.setAllocations(List.of(MesReportAllocationSaveLine.builder().activeOrderId(11L).allocatedQuantity(qty("600")).build()));
        var original=event.getRawPayload();
        var result=MesProductionDisplayedContextFixture.save(service,command);
        assertEquals(1,result.getLines().size());assertEquals(11L,result.getLines().get(0).getActiveOrderId());
        assertEquals(0,qty("600").compareTo(result.getTotalAllocatedQuantity()));
        assertEquals(original,event.getRawPayload());
        verify(dependency("eventMapper",MesProProcessPoolEventMapper.class),never()).updateById(any(MesProProcessPoolEventDO.class));
    }
    @Test void unchangedApprovalCannotSkipTheFrozenCeiling(){
        var snapshots=dependency("activeOrderProcessSnapshotMapper",MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        when(snapshots.selectListByActiveOrderAndProcessForUpdate(10L,70L)).thenReturn(List.of(snapshot(10L)
            .setErpFixedQuantitySnapshot(qty("500")).setPlannedQuantitySnapshot(qty("550"))));
        var command=command("400","200");
        command.setAllocations(List.of(MesReportAllocationSaveLine.builder().activeOrderId(10L).allocatedQuantity(qty("600")).build()));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->MesProductionDisplayedContextFixture.save(service,command));
        verify(dependency("signatureService",MesProBatchRecordExecutionSignatureService.class),never())
            .recordTeamLeaderReviewSignature(any(),any(),any(),any(),any(),any());
        verify(dependency("allocationMapper",MesProcessPoolReportAllocationMapper.class),never()).attachReviewToCurrentRowsByEventId(any(),any(),any(),any());
    }
    @Test void differentProductIsRejectedBeforeAnyAllocationWrites(){
        when(dependency("workOrderMapper",MesProWorkOrderMapper.class).selectById(30L)).thenReturn(work(30L).setProductId(91L));
        assertThrows(IllegalArgumentException.class,()->MesProductionDisplayedContextFixture.save(service,command("400","200")));
        verify(dependency("allocationMapper",MesProcessPoolReportAllocationMapper.class),never()).supersedeCurrentRows(anyCollection(),any());
    }
    @Test void targetCeilingCountsItsOtherSubmissions(){
        var allocations=dependency("allocationMapper",MesProcessPoolReportAllocationMapper.class);
        when(allocations.selectListByActiveOrderIdsAndProcessForUpdate(anyCollection(),eq(70L))).thenReturn(List.of(
            MesProcessPoolReportAllocationDO.builder().eventId(101L).activeOrderId(11L).workOrderId(31L).routeProcessId(60L).processId(70L).allocatedQuantity(qty("901")).build()));
        when(dependency("eventMapper",MesProProcessPoolEventMapper.class).selectBatchIds(anyCollection())).thenReturn(List.of(
            MesProProcessPoolEventDO.builder().id(101L).eventType("PRODUCTION_SUBMIT").workOrderId(31L).routeId(50L).routeProcessId(60L).processId(70L).reportOutputQuantity(qty("901"))
            .rawPayload("{\"materialDetails\":[{\"materialId\":501,\"outputQuantity\":901}]}").build()));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->MesProductionDisplayedContextFixture.save(service,command("400","200")));
        verify(allocations,never()).insertBatch(anyCollection());
    }
}
