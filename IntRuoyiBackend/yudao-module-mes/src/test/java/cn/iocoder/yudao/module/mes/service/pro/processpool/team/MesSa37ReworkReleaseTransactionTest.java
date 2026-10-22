package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.*;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.service.pro.MesSa36Sa38MapperFixture;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real rework, progress, process aggregation, completion/backfill and release persistence.
 * ERP loss/issue readers, audit, notifications and downstream batch opening are explicit boundaries. */
class MesSa37ReworkReleaseTransactionTest {
    private MesSa36Sa38MapperFixture db;
    private MesProcessPoolActiveOrderMapper orders;
    private MesProcessPoolActiveOrderProcessSnapshotMapper snapshots;
    private MesProcessPoolReportAllocationMapper allocations;
    private MesProProcessPoolEventMapper events;
    private MesProcessPoolSubmissionReviewMapper reviews;
    private MesPqcInspectionTaskMapper tasks;
    private MesPqcProcessInspectionAggregateDetailMapper details;
    private MesProcessPoolOrderProcessCompletionMapper completions;
    private MesProcessPoolActiveOrderCompletionReceiptMapper receipts;
    private MesProcessPoolActiveOrderCompletionBackfillMapper backfills;
    private MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    private MesProEdhrBatchExecutionMapper batches;
    private MesProWorkOrderMapper workOrders;
    private MesProcessPoolActiveOrderPickListBindingMapper bindings;
    private MesProcessPoolActiveOrderPickListBindingItemMapper bindingItems;
    private MesTeamLeaderOrderProcessCompletionService aggregation;
    private MesTeamLeaderActiveOrderCompletionBackfillPortImpl backfill;
    private MesTeamLeaderActiveOrderCompletionProgressPortImpl progress;
    private MesTeamLeaderActiveOrderReleaseApplicationServiceImpl application;
    private MesReportAllocationOrderChangeService orderChanges;
    private AutoCloseable batchContext;
    private final LocalDateTime early = LocalDateTime.of(2026,10,5,8,0);

    @BeforeEach
    void setUp() throws Exception {
        db = new MesSa36Sa38MapperFixture();
        for (Class<?> type : List.of(MesProcessPoolActiveOrderDO.class,
                MesProcessPoolActiveOrderProcessSnapshotDO.class, MesProcessPoolReportAllocationDO.class,
                MesProProcessPoolEventDO.class, MesProcessPoolSubmissionReviewDO.class, MesPqcInspectionTaskDO.class,
                MesPqcProcessInspectionAggregateDetailDO.class, MesProcessPoolOrderProcessCompletionDO.class,
                MesProcessPoolActiveOrderCompletionReceiptDO.class, MesProcessPoolActiveOrderCompletionBackfillDO.class,
                MesProcessPoolActiveOrderReleaseApplicationDO.class, MesProEdhrWorkTaskDO.class,
                MesProEdhrBatchExecutionDO.class, MesProEdhrBatchExecutionOriginDO.class,
                MesProEdhrNonconformanceReviewDO.class, MesProWorkOrderDO.class,
                MesProcessPoolActiveOrderPickListBindingDO.class, MesProcessPoolActiveOrderPickListBindingItemDO.class,
                MesProcessPoolReportAllocationStateDO.class, MesProcessPoolReportAllocationAdjustmentAuditDO.class)) {
            db.table(type,type.getAnnotation(TableName.class).value());
        }
        // The formal work-task schema has this column although the DO relies on tenant interception.
        db.jdbc.execute("ALTER TABLE mes_pro_edhr_work_task ADD COLUMN tenant_id BIGINT");
        db.register(MesProcessPoolActiveOrderMapper.class, MesProcessPoolActiveOrderProcessSnapshotMapper.class,
                MesProcessPoolReportAllocationMapper.class, MesProProcessPoolEventMapper.class,
                MesProcessPoolSubmissionReviewMapper.class, MesPqcInspectionTaskMapper.class,
                MesPqcProcessInspectionAggregateDetailMapper.class, MesProcessPoolOrderProcessCompletionMapper.class,
                MesProcessPoolActiveOrderCompletionReceiptMapper.class, MesProcessPoolActiveOrderCompletionBackfillMapper.class,
                MesProcessPoolActiveOrderReleaseApplicationMapper.class, MesProEdhrWorkTaskMapper.class,
                MesProEdhrBatchExecutionMapper.class, MesProEdhrBatchExecutionOriginMapper.class, MesProWorkOrderMapper.class,
                MesProcessPoolActiveOrderPickListBindingMapper.class, MesProcessPoolActiveOrderPickListBindingItemMapper.class,
                MesProcessPoolReportAllocationStateMapper.class, MesProcessPoolReportAllocationAdjustmentAuditMapper.class);
        orders=db.mapper(MesProcessPoolActiveOrderMapper.class); snapshots=db.mapper(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
        allocations=db.mapper(MesProcessPoolReportAllocationMapper.class); events=db.mapper(MesProProcessPoolEventMapper.class);
        reviews=db.mapper(MesProcessPoolSubmissionReviewMapper.class); tasks=db.mapper(MesPqcInspectionTaskMapper.class);
        details=db.mapper(MesPqcProcessInspectionAggregateDetailMapper.class); completions=db.mapper(MesProcessPoolOrderProcessCompletionMapper.class);
        receipts=db.mapper(MesProcessPoolActiveOrderCompletionReceiptMapper.class); backfills=db.mapper(MesProcessPoolActiveOrderCompletionBackfillMapper.class);
        applications=db.mapper(MesProcessPoolActiveOrderReleaseApplicationMapper.class); batches=db.mapper(MesProEdhrBatchExecutionMapper.class);
        workOrders=db.mapper(MesProWorkOrderMapper.class); bindings=db.mapper(MesProcessPoolActiveOrderPickListBindingMapper.class);
        bindingItems=db.mapper(MesProcessPoolActiveOrderPickListBindingItemMapper.class);
        batchContext=db.enableBatchPersistence();
        aggregation=new MesTeamLeaderOrderProcessCompletionService(allocations,workOrders,completions,
                new MesTeamLeaderOrderProcessTargetService(snapshots),mock(MesProScheduleOrderMapper.class),
                mock(MesProScheduleOrderProcessMapper.class),snapshots,events,orders);
        orderChanges=new MesReportAllocationOrderChangeService(orders,events,allocations,
                db.mapper(MesProcessPoolReportAllocationStateMapper.class),db.mapper(MesProcessPoolReportAllocationAdjustmentAuditMapper.class),
                mock(MesReportAllocationReleaseStateService.class),new MesTeamLeaderOrderProcessTargetService(snapshots),
                mock(MesReportAllocationQuantityFragmentService.class),aggregation,mock(MesProductionReportManagementSummaryService.class));
        progress=new MesTeamLeaderActiveOrderCompletionProgressPortImpl(snapshots,allocations,tasks,events);
        var loss=mock(MesTeamLeaderActiveOrderReleaseLossSourceReader.class);
        when(loss.read(any())).thenAnswer(call -> {
            var command=call.<MesTeamLeaderActiveOrderReleaseLossReportPlanCommand>getArgument(0);
            var line=allocations.selectListByActiveOrderIdForUpdate(command.getActiveOrderId()).get(0);
            return new MesTeamLeaderActiveOrderReleaseLossSourceReadResult().setBlockers(List.of())
                    .setProcessSources(command.getProcessSnapshots().stream().map(s ->
                            new MesTeamLeaderActiveOrderReleaseLossSourceReadResult.ProcessLossSource()
                                    .setSnapshot(s).setAllocation(line).setReview(reviews.selectById(line.getReviewId()))
                                    .setEvent(events.selectById(line.getEventId()))
                                    .setFeedback(MesProFeedbackDO.builder().id(501L).workOrderId(30L).routeId(40L).processId(1L)
                                            .unqualifiedQuantity(BigDecimal.ZERO).build())
                                    .setFormalLossQuantity(BigDecimal.ZERO).setHasActualLoss(false).setZeroLossConfirmed(true)
                                    .setLossDecision("NO_LOSS").setReplenishmentSources(List.of()).setLossDetails(List.of())).toList());
        });
        var issues=mock(MesWmProductIssueMapper.class);var issueItems=mock(MesWmProductIssueDetailMapper.class);
        when(issues.selectListByWorkOrderIdForUpdate(30L)).thenReturn(List.of(MesWmProductIssueDO.builder()
                .id(901L).workOrderId(30L).code("ISSUE-30")
                .status(cn.iocoder.yudao.module.mes.enums.wm.MesWmProductIssueStatusEnum.FINISHED.getStatus()).build()));
        when(issueItems.selectListByIssueIdForUpdate(901L)).thenReturn(List.of(MesWmProductIssueDetailDO.builder()
                .id(902L).issueId(901L).lineId(903L).materialStockId(904L).itemId(1001L).quantity(BigDecimal.ONE)
                .batchId(905L).batchCode("RAW-30").build()));
        backfill=new MesTeamLeaderActiveOrderCompletionBackfillPortImpl(snapshots,allocations,completions,tasks,details,
                workOrders,loss,backfills,issues,issueItems,bindings,bindingItems,events,reviews);
        var completion=new MesTeamLeaderActiveOrderCompletionServiceImpl(orders,receipts,progress,backfill,
                mock(MesTeamLeaderActiveOrderPickListCompletionSourceService.class),mock(MesActiveOrderTransferTraceService.class),
                mock(MesPqcProcessInspectionAggregationService.class));
        var audit=mock(GxpAuditService.class);var sequence=new AtomicLong(8000);
        when(audit.append(any())).thenAnswer(c -> new GxpAuditAppendResult(sequence.incrementAndGet(),sequence.get(),"audit-"+sequence.get(),false));
        var states=mock(MesReleaseAffectedStateCollector.class);
        when(states.withAffected(any(),any())).thenAnswer(c -> c.getArgument(0));
        ReflectionTestUtils.setField(completion,"gxpAuditService",audit);
        ReflectionTestUtils.setField(completion,"affectedStateCollector",states);
        ReflectionTestUtils.setField(completion,"handoffService",mock(MesActiveOrderHandoffService.class));
        var persistence=new MesTeamLeaderActiveOrderReleaseApplicationPersistenceService(applications,
                db.mapper(MesProEdhrWorkTaskMapper.class),mock(MesReleaseFlowAuditRecorder.class));
        ReflectionTestUtils.setField(persistence,"notificationService",mock(MesReleaseTaskNotificationService.class));
        var candidates=mock(MesProductionReleaseRequiredCandidateResolver.class);
        when(candidates.resolveRequiredCandidates(1L,MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER))
                .thenReturn(new MesProductionReleaseRoleCandidates(90L,MesProductionReleaseRoleCodes.PQC_RELEASE_OWNER,List.of(91L),"candidate-hash"));
        var generation=new MesTeamLeaderActiveOrderReleaseGenerationService(orders,workOrders,snapshots,completions,tasks,
                details,allocations,applications,db.mapper(MesProEdhrWorkTaskMapper.class),persistence,candidates,
                new MesTeamLeaderActiveOrderReleaseSourceSnapshotHasher(),bindings,mock(MesProEdhrNonconformanceReviewService.class));
        var batchBoundary=mock(MesTeamLeaderActiveOrderCompletionBatchExecutionService.class);
        when(batchBoundary.openOrCreate(anyLong(),anyLong(),anyLong(),anyString())).thenAnswer(call -> {
            var receipt=receipts.selectById(call.<Long>getArgument(2));
            assertEquals("BACKFILL_SUCCEEDED",receipt.getReceiptStatus());
            var batch=new MesProEdhrBatchExecutionDO().setWorkOrderId(30L).setRouteId(40L).setRouteVersionId(41L)
                    .setBatchCode("BATCH-30");batch.setTenantId(1L);batches.insert(batch);return batch.getId();
        });
        application=new MesTeamLeaderActiveOrderReleaseApplicationServiceImpl(generation,completion,receipts,batches,
                applications,batchBoundary,db.mapper(MesProEdhrBatchExecutionOriginMapper.class));
        ReflectionTestUtils.setField(application,"gxpAuditService",audit);
        ReflectionTestUtils.setField(application,"affectedStates",states);
        ReflectionTestUtils.setField(application,"handoffService",mock(MesActiveOrderHandoffService.class));
    }

    @AfterEach
    void restoreBatchContext() throws Exception {
        if (batchContext != null) batchContext.close();
    }

    @ParameterizedTest
    @CsvSource({"0,false","1,false","0,true","1,true"})
    void reworkUsesFormalCycleDespiteOlderOrEqualApprovalAndReleasesThroughRealBackfill(int laterHours, boolean sameSource) {
        TenantUtils.execute(1L, () -> db.transaction.executeWithoutResult(status -> {
            seed(laterHours,sameSource);
            String oldAllocation=JsonUtils.toJsonString(allocations.selectById(201L));
            String oldSnapshot=JsonUtils.toJsonString(snapshots.selectById(101L));
            String oldReceipt=JsonUtils.toJsonString(receipts.selectById(301L));
            String oldSignature=JsonUtils.toJsonString(reviews.selectById(602L));
            var rework=new MesActiveOrderReworkCycleService(orders,snapshots,tasks);
            Long next=rework.start(10L,30L,700L,early.plusDays(1));
            var current=allocation(202L,next,401L,601L,early);
            allocations.insert(current);
            var task=tasks.selectListByActiveOrderId(next).get(0);
            task.setTaskStatus("CONFIRMED").setActualInspectionQuantity(1).setSubmittedEventId(450L);tasks.updateById(task);
            details.insert(MesPqcProcessInspectionAggregateDetailDO.builder().id(801L).activeOrderId(next).workOrderId(30L)
                    .routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L).pqcTaskId(task.getId())
                    .actualInspectionQuantity(1).sourcePqcRecordId(901L).sourcePieceDetailId(1801L).eventId(450L).reviewId(650L).build());
            var binding=MesProcessPoolActiveOrderPickListBindingDO.builder().id(8801L).activeOrderId(next).workOrderId(30L)
                    .pickListId(9901L).sourceSnapshotHash("pick-hash").build();bindings.insert(binding);
            bindingItems.insert(MesProcessPoolActiveOrderPickListBindingItemDO.builder().id(8811L).bindingId(8801L).pickListItemId(9911L).build());
            aggregation.reconcileAffectedAllocations(events.selectById(401L),List.of(current));
            assertCurrentSources("[202]","10");
            // A second source can join the current cycle even when its confirmation is earlier.
            current.setAllocatedQuantity(new BigDecimal("5"));allocations.updateById(current);
            var second=allocation(204L,next,402L,602L,early.minusMinutes(1));
            second.setAllocatedQuantity(new BigDecimal("5"));allocations.insert(second);
            aggregation.reconcileAffectedAllocations(events.selectById(402L),List.of(second));
            assertCurrentSources("[204,202]","10");
            orderChanges.reduceWorkOrderAllocations(30L,new BigDecimal("5"),20L,"SA37 reduce current cycle");
            assertCurrentSources("[202]","5");
            // Removing the last current allocation must clear progress rather than resurrect B0.
            orderChanges.invalidateActiveOrder(next,20L,"SA37 remove last current allocation");
            assertCurrentSources("[]","0");
            var restored=allocation(203L,next,401L,601L,early);allocations.insert(restored);
            aggregation.applyConfirmedAllocations(events.selectById(401L),List.of(restored));
            assertCurrentSources("[203]","10");
            assertTrue(progress.read(20L,orders.selectById(next)).isDoubleComplete());
            var completion=completions.selectListByWorkOrderIds(List.of(30L)).get(0);
            completion.setSourceAllocationIdsJson("[201]");completions.updateById(completion);
            var mismatch=assertThrows(ServiceException.class, () -> backfill.prepare(20L,orders.selectById(next),
                    new MesTeamLeaderActiveOrderCompletionCommand().setConfirmNoReplenishmentInfo(true)));
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING.getCode(),mismatch.getCode());
            assertTrue(mismatch.getMessage().contains("PRODUCTION_COMPLETION_SOURCE_ALLOCATIONS"));
            aggregation.reconcileAffectedAllocations(events.selectById(401L),List.of(restored));
            var result=application.apply(20L,new MesTeamLeaderActiveOrderReleaseApplyCommand().setActiveOrderId(next)
                    .setIdempotencyKey("sa37-release").setConfirmNoReplenishmentInfo(true));
            var receipt=receipts.selectByActiveOrderIdForUpdate(next);
            assertEquals("BACKFILL_SUCCEEDED",receipt.getReceiptStatus());
            assertNotNull(backfills.selectById(receipt.getBatchRecordId()));
            assertNotNull(backfills.selectById(receipt.getProcessInspectionId()));
            assertEquals(result.getBatchExecutionId(),applications.selectById(result.getApplicationId()).getBatchExecutionId());
            assertEquals(next,applications.selectById(result.getApplicationId()).getActiveOrderId());
            assertNotNull(batches.selectById(result.getBatchExecutionId()));
            assertCurrentSources("[203]","10");
            assertEquals(oldAllocation,JsonUtils.toJsonString(allocations.selectById(201L)));
            assertEquals(oldSnapshot,JsonUtils.toJsonString(snapshots.selectById(101L)));
            assertEquals(oldReceipt,JsonUtils.toJsonString(receipts.selectById(301L)));
            assertEquals(oldSignature,JsonUtils.toJsonString(reviews.selectById(602L)));
            assertEquals("REMOVED",orders.selectById(10L).getActiveStatus());
            assertEquals("REWORKED",orders.selectById(10L).getBusinessStatus());
            assertEquals(result.getApplicationId(),application.apply(20L,new MesTeamLeaderActiveOrderReleaseApplyCommand()
                    .setActiveOrderId(next).setIdempotencyKey("sa37-release").setConfirmNoReplenishmentInfo(true)).getApplicationId());
        }));
    }

    private void assertCurrentSources(String ids,String quantity) {
        var completion=completions.selectListByWorkOrderIds(List.of(30L)).get(0);
        assertEquals(ids,completion.getSourceAllocationIdsJson());
        assertEquals(0,new BigDecimal(quantity).compareTo(completion.getConfirmedQuantity()));
    }

    @ParameterizedTest
    @CsvSource({"none","duplicate","version_pending"})
    void formalCycleAuthorityRejectsAmbiguityAndAllowsVersionInvalidation(String state) {
        TenantUtils.execute(1L, () -> db.transaction.executeWithoutResult(status -> {
            seed(0,false);
            if ("version_pending".equals(state)) {
                orders.updateById(MesProcessPoolActiveOrderDO.builder().id(10L).activeStatus("VERSION_UPGRADE_PENDING").build());
                aggregation.reconcileAffectedAllocations(events.selectById(402L),List.of(allocations.selectById(201L)));
                assertCurrentSources("[201]","10");
                orderChanges.invalidateActiveOrder(10L,20L,"SA37 version upgrade invalidation");
                assertCurrentSources("[]","0");
                return;
            }
            if ("none".equals(state)) {
                orders.updateById(MesProcessPoolActiveOrderDO.builder().id(10L).activeStatus("REMOVED").build());
            } else {
                var other=orders.selectById(10L);other.setId(11L);orders.insert(other);
                var snapshot=snapshots.selectById(101L);snapshot.setId(102L).setActiveOrderId(11L);snapshots.insert(snapshot);
            }
            var ex=assertThrows(ServiceException.class, () -> aggregation.reconcileAffectedAllocations(
                    events.selectById(402L),List.of(allocations.selectById(201L))));
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ORDER_PROCESS_TARGET_REQUIRED.getCode(),ex.getCode());
            assertTrue(completions.selectListByWorkOrderIds(List.of(30L)).isEmpty());
            assertEquals("CURRENT",allocations.selectById(201L).getLifecycleStatus());
        }));
    }

    private void seed(int laterHours,boolean sameSource) {
        var order=MesProcessPoolActiveOrderDO.builder().id(10L).leaderUserId(20L).workOrderId(30L).routeId(40L)
                .routeVersionId(41L).qaRegulationVersionId(42L).erpFixedQuantitySnapshot(BigDecimal.TEN)
                .activeStatus("ACTIVE").businessStatus("COMPLETED").version(2).build();order.setTenantId(1L);orders.insert(order);
        var snapshot=MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(101L).activeOrderId(10L).workOrderId(30L)
                .routeId(40L).routeVersionId(41L).routeProcessId(101L).processId(1L).erpFixedQuantitySnapshot(BigDecimal.TEN)
                .productionQuantityFactorSnapshot(BigDecimal.ONE).plannedQuantitySnapshot(BigDecimal.TEN)
                .productionConfigSnapshotJson("{\"outputMaterialIds\":[]}").build();snapshot.setTenantId(1L);snapshots.insert(snapshot);
        tasks.insert(MesPqcInspectionTaskDO.builder().id(701L).activeOrderId(10L).workOrderId(30L).routeId(40L)
                .routeVersionId(41L).routeProcessId(101L).processId(1L).regulationVersionId(42L).taskStatus("CONFIRMED")
                .plannedInspectionQuantity(1).actualInspectionQuantity(1).businessDate(LocalDate.of(2026,10,5)).submittedEventId(451L).build());
        var work=MesProWorkOrderDO.builder().id(30L).code("B-30").productId(1001L).quantity(BigDecimal.TEN).batchCode("BATCH-30").build();
        work.setTenantId(1L);workOrders.insert(work);
        for(long id:List.of(401L,402L)) {
            var event=MesProProcessPoolEventDO.builder().id(id).eventType("PRODUCTION_SUBMIT").workOrderId(id==401L?31L:30L)
                    .routeId(40L).routeProcessId(101L).processId(1L).reportOutputQuantity(BigDecimal.TEN)
                    .rawPayload("{\"outputQuantity\":10}").serverSubmitTime(early).build();event.setTenantId(1L);events.insert(event);
            var review=MesProcessPoolSubmissionReviewDO.builder().id(id+200).eventId(id).leaderUserId(20L).leaderType("PRODUCTION")
                    .reviewStatus("APPROVED").reviewedAt(id==401L?early:early.plusHours(laterHours)).reviewRound(0)
                    .reviewSignatureId(id+300).reviewSignatureUserId(20L).reviewSignatureSnapshotJson("{\"signatureId\":"+(id+300)
                            +",\"actorId\":20,\"processPoolEventId\":"+id+",\"actionType\":\"TEAM_LEADER_REVIEW\","
                            +"\"eventType\":\"PRODUCTION_SUBMIT\",\"leaderType\":\"PRODUCTION\",\"reviewStatus\":\"APPROVED\"}").build();
            review.setTenantId(1L);reviews.insert(review);
        }
        allocations.insert(allocation(201L,10L,sameSource?401L:402L,sameSource?601L:602L,early.plusHours(laterHours)));
        var receipt=MesProcessPoolActiveOrderCompletionReceiptDO.builder().id(301L).activeOrderId(10L).workOrderId(30L)
                .leaderUserId(20L).receiptStatus("BACKFILL_SUCCEEDED").receiptHash("old-receipt").build();receipt.setTenantId(1L);receipts.insert(receipt);
    }

    private MesProcessPoolReportAllocationDO allocation(Long id,Long cycle,Long event,Long review,LocalDateTime at) {
        var row=MesProcessPoolReportAllocationDO.builder().id(id).activeOrderId(cycle).workOrderId(30L).leaderUserId(20L)
                .eventId(event).reviewId(review).routeProcessId(101L).processId(1L).allocatedQuantity(BigDecimal.TEN)
                .confirmedAt(at).lifecycleStatus("CURRENT").build();row.setTenantId(1L);return row;
    }
}
