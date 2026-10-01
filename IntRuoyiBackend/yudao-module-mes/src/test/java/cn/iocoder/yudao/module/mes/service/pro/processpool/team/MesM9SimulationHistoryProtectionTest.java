package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesM9SimulationHistoryProtectionTest {

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> twoCandidateSourceCases() {
        return java.util.stream.Stream.of("reset", "cleanup").flatMap(entry ->
                java.util.stream.Stream.of("legal", "crossReviewEvent", "crossTaskEvent")
                        .map(scenario -> org.junit.jupiter.params.provider.Arguments.of(entry, scenario)));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("twoCandidateSourceCases")
    void sameRunCandidatesMustNotAuthorizeCrossEventAggregateSources(String entry, String scenario) {
        var cleanup = dependency(MesTeamLeaderDataCleanupMapper.class);
        var events = dependency(MesProProcessPoolEventMapper.class);
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(101L)).thenReturn(parent());
        var tasks = new ArrayList<MesPqcInspectionTaskDO>();
        var pieces = new ArrayList<MesPqcInspectionPieceDetailDO>();
        var records = new ArrayList<cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO>();
        var reviews = new ArrayList<MesProcessPoolSubmissionReviewDO>();
        var aggregates = new ArrayList<MesPqcProcessInspectionAggregateDetailDO>();
        var allocations = new ArrayList<MesProcessPoolReportAllocationDO>();
        var eventIds = List.of(501L, 502L, 551L, 552L);
        for (int i = 0; i < 2; i++) {
            long taskId = 301L + i, eventId = 501L + i, productionId = 551L + i;
            var task = MesPqcInspectionTaskDO.builder().id(taskId).activeOrderId(101L).workOrderId(201L)
                    .routeId(701L).routeVersionId(702L).routeProcessId(703L).processId(704L)
                    .qaProcessId(705L).regulationVersionId(706L).inspectionType("PATROL")
                    .businessDate(java.time.LocalDate.of(2026, 9, 29)).shiftCode("DAY").roundNo(i + 1)
                    .actualInspectionQuantity(1).taskStatus("CONFIRMED").submittedEventId(eventId).build();
            var piece = MesPqcInspectionPieceDetailDO.builder().id(401L + i).taskId(taskId)
                    .sampleNo(1).itemCode("DIM").itemName("Dimension").inspectionMethod("MEASURE")
                    .standardText("1").resultType("NUMBER").measuredValue("1").judgement("PASS").build();
            var event = MesProProcessPoolEventDO.builder().id(eventId).workOrderId(201L)
                    .routeId(701L).qaProcessId(705L).eventType("PQC_INSPECTION")
                    .feedbackSourceType("MES_PQC_INSPECTION_TASK").feedbackSourceId(taskId)
                    .recordbookSourceType("MES_PQC_INSPECTION_TASK").recordbookSourceId(taskId).build();
            var production = MesProProcessPoolEventDO.builder().id(productionId).workOrderId(201L)
                    .routeId(701L).qaProcessId(705L).eventType("PRODUCTION_SUBMIT").build();
            var review = MesProcessPoolSubmissionReviewDO.builder().id(711L + i).eventId(eventId)
                    .leaderType("PQC").reviewStatus("APPROVED").leaderUserId(3001L).build();
            var record = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO
                    .builder().id(611L + i).eventId(eventId).workOrderId(201L).routeId(701L).qaProcessId(705L)
                    .productionSubmitEventId(productionId).processInspectionReviewId(review.getId())
                    .processInspectionAggregationStatus("AGGREGATED").build();
            var allocation = MesProcessPoolReportAllocationDO.builder().id(901L + i)
                    .activeOrderId(101L).eventId(productionId).build();
            for (Object row : List.of(task, piece, event, production, review, record, allocation)) {
                ReflectionTestUtils.setField(row, "tenantId", 92820L);
                ReflectionTestUtils.setField(row, "simulated", true);
                ReflectionTestUtils.setField(row, "simulationRunId", "M9-owned");
                ReflectionTestUtils.setField(row, "simulationStage", "LATEST_VERSION_COPY");
            }
            // Validate the unmodified chain with the real producer before constructing its projection.
            Class<?> producer = MesPqcProcessInspectionAggregationServiceImpl.class;
            ReflectionTestUtils.invokeMethod(producer, "validatePqcEvent", record, event, eventId);
            ReflectionTestUtils.invokeMethod(producer, "validatePqcTask", record, event, task, eventId);
            ReflectionTestUtils.invokeMethod(producer, "validatePieceDetails", record, task, List.of(piece), eventId);
            MesPqcProcessInspectionAggregateDetailDO aggregate = ReflectionTestUtils.invokeMethod(producer,
                    "buildAggregateDetail", record, event, task, piece, review.getId(),
                    java.time.LocalDateTime.of(2026, 9, 29, 8, 0));
            assertNotNull(aggregate);
            aggregate.setId(811L + i);
            assertEquals(productionId, aggregate.getProductionSubmitEventId());
            tasks.add(task); pieces.add(piece); records.add(record); reviews.add(review);
            aggregates.add(aggregate); allocations.add(allocation);
            when(events.selectByIdForUpdate(eventId)).thenReturn(event);
            when(events.selectByIdForUpdate(productionId)).thenReturn(production);
            when(events.selectListPqcByTaskId("MES_PQC_INSPECTION_TASK", taskId)).thenReturn(List.of(event));
            when(cleanup.selectSimulationEventAllocationsForUpdate(92820L, productionId)).thenReturn(List.of(allocation));
            when(dependency(MesProcessPoolReportAllocationMapper.class).selectAllListByEventIdForUpdate(productionId))
                    .thenReturn(List.of(allocation));
        }
        if (scenario.equals("crossReviewEvent")) {
            records.get(0).setProcessInspectionReviewId(reviews.get(1).getId());
            aggregates.get(0).setReviewId(reviews.get(1).getId());
        } else if (scenario.equals("crossTaskEvent")) {
            aggregates.get(0).setPqcTaskId(tasks.get(1).getId());
            aggregates.get(0).setSourcePieceDetailId(pieces.get(1).getId());
        }
        when(cleanup.selectSimulationAllocationsForUpdate(92820L, 101L)).thenReturn(allocations);
        when(cleanup.selectCleanupTasksForUpdate(92820L, 101L)).thenReturn(tasks);
        when(cleanup.selectCleanupPiecesForUpdate(92820L, List.of(301L, 302L))).thenReturn(pieces);
        when(cleanup.selectSimulationPqcRecordsForUpdate(eq(92820L), anyCollection())).thenReturn(records);
        when(cleanup.selectCleanupReviewsForUpdate(eq(92820L), anyCollection())).thenReturn(reviews);
        when(cleanup.selectCleanupAggregatesForUpdate(92820L, 101L)).thenReturn(aggregates);
        var target = spy(service);
        if (entry.equals("reset")) {
            stubFixedResetOwner();
            when(cleanup.selectEventIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(eventIds);
            doReturn(MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(102L).workOrderId(201L)
                    .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build()).when(target).addActiveOrder(any());
        }
        org.junit.jupiter.api.function.Executable action = () -> {
            if (entry.equals("reset")) target.resetFixedSimulationActiveOrder(3001L);
            else target.cleanupLatestSimulationActiveOrder(3001L, 101L);
        };
        if (scenario.equals("legal")) {
            assertDoesNotThrow(action);
            verify(cleanup).selectCleanupAggregatesForUpdate(92820L, 101L);
            verify(cleanup).selectCleanupPiecesForUpdate(92820L, List.of(301L, 302L));
            verify(cleanup).selectSimulationPqcRecordsForUpdate(eq(92820L), anyCollection());
            if (entry.equals("reset")) verify(cleanup).deletePqcPieceDetails(92820L, List.of(301L, 302L));
            else verify(dependency(MesPqcInspectionPieceDetailMapper.class)).deleteByTaskIds(List.of(301L, 302L));
        } else {
            rejectWrites = true;
            var error = assertThrows(ServiceException.class, action);
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                    .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
            verify(dependency(GxpAuditService.class), never()).append(any());
        }
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> aggregateSourceCases() {
        return java.util.stream.Stream.of("reset", "cleanup").flatMap(entry ->
                java.util.stream.Stream.of("legal", "sourcePqcRecordId", "sourcePieceDetailId", "eventId",
                                "reviewId", "productionSubmitEventId", "pqcTaskId")
                        .map(source -> org.junit.jupiter.params.provider.Arguments.of(entry, source)));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("aggregateSourceCases")
    void aggregateMustPreserveItsActualPqcSourceChain(String entry, String source) {
        stubOwnedEvent("PQC_INSPECTION", false);
        stubSimulationPiece("legal");
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        // Match the formal producer's event -> task identity before varying an aggregate source.
        var event = MesProProcessPoolEventDO.builder().id(501L).workOrderId(201L)
                .eventType("PQC_INSPECTION").feedbackSourceType("MES_PQC_INSPECTION_TASK")
                .feedbackSourceId(301L).recordbookSourceType("MES_PQC_INSPECTION_TASK")
                .recordbookSourceId(301L).simulated(true).simulationStage("LATEST_VERSION_COPY")
                .simulationRunId("M9-owned").build();
        event.setTenantId(92820L);
        when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(501L)).thenReturn(event);
        var task = MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(101L).workOrderId(201L)
                .submittedEventId(501L).simulated(true).simulationStage("LATEST_VERSION_COPY")
                .simulationRunId("M9-owned").build();
        task.setTenantId(92820L);
        when(mapper.selectCleanupTasksForUpdate(92820L, 101L)).thenReturn(List.of(task));
        when(dependency(MesPqcInspectionTaskMapper.class).selectListByActiveOrderIdForUpdate(101L))
                .thenReturn(List.of(task));
        if (entry.equals("reset")) {
            stubFixedResetOwner();
            when(mapper.selectEventIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(List.of(501L));
        }
        var review = MesProcessPoolSubmissionReviewDO.builder().id(701L).eventId(501L)
                .reviewStatus("APPROVED").leaderUserId(3001L).build();
        var record = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO
                .builder().id(602L).eventId(501L).workOrderId(201L)
                .processInspectionReviewId(701L).processInspectionAggregationStatus("AGGREGATED").build();
        // The formal aggregate writer copies the optional production source from the PQC record.
        // This legal PQC chain has no production binding; an unrelated non-null ID cannot be inferred.
        var aggregate = MesPqcProcessInspectionAggregateDetailDO.builder().id(801L)
                .sourcePqcRecordId(602L).sourcePieceDetailId(401L).eventId(501L).reviewId(701L)
                .pqcTaskId(301L).activeOrderId(101L).workOrderId(201L).build();
        for (Object row : List.of(review, record, aggregate)) {
            ReflectionTestUtils.setField(row, "tenantId", 92820L);
            ReflectionTestUtils.setField(row, "simulated", true);
            ReflectionTestUtils.setField(row, "simulationRunId", "M9-owned");
            ReflectionTestUtils.setField(row, "simulationStage", "LATEST_VERSION_COPY");
        }
        if (!source.equals("legal")) ReflectionTestUtils.setField(aggregate, source, 999L);
        when(mapper.selectCleanupReviewsForUpdate(eq(92820L), anyCollection())).thenReturn(List.of(review));
        when(mapper.selectSimulationPqcRecordsForUpdate(eq(92820L), anyCollection())).thenReturn(List.of(record));
        when(mapper.selectCleanupAggregatesForUpdate(92820L, 101L)).thenReturn(List.of(aggregate));
        var target = spy(service);
        if (entry.equals("reset")) {
            doReturn(MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(102L).workOrderId(201L)
                    .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build()).when(target).addActiveOrder(any());
        }
        org.junit.jupiter.api.function.Executable action = () -> {
            if (entry.equals("reset")) target.resetFixedSimulationActiveOrder(3001L);
            else target.cleanupLatestSimulationActiveOrder(3001L, 101L);
        };
        if (source.equals("legal")) {
            assertDoesNotThrow(action);
            verify(mapper).selectCleanupAggregatesForUpdate(92820L, 101L);
            verify(mapper).selectSimulationPqcRecordsForUpdate(eq(92820L), anyCollection());
            verify(mapper).selectCleanupPiecesForUpdate(92820L, List.of(301L));
        } else {
            rejectWrites = true;
            var error = assertThrows(ServiceException.class, action);
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                    .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"emptySnapshot", "foreignSnapshot", "matchingSnapshot", "softDeleted"})
    void resetDeletesPiecesOnlyForValidatedLockedTaskSet(String scenario) {
        stubSimulationPiece("legal");
        stubFixedResetOwner();
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        var task = MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(101L).workOrderId(201L)
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        task.setTenantId(92820L);
        task.setDeleted(scenario.equals("softDeleted"));
        when(mapper.selectCleanupTasksForUpdate(92820L, 101L)).thenReturn(List.of(task));
        when(mapper.selectPqcTaskIds(92820L, List.of(101L))).thenReturn(
                scenario.equals("emptySnapshot") || scenario.equals("softDeleted") ? List.of()
                        : scenario.equals("foreignSnapshot") ? List.of(999L) : List.of(301L));
        var target = spy(service);
        doReturn(MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(102L).workOrderId(201L)
                .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build()).when(target).addActiveOrder(any());
        assertDoesNotThrow(() -> target.resetFixedSimulationActiveOrder(3001L));
        verify(mapper).selectCleanupTasksForUpdate(92820L, 101L);
        verify(mapper).deletePqcPieceDetails(92820L, List.of(301L));
        verify(mapper, never()).deletePqcPieceDetails(92820L, List.of(999L));
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> snapshotRouteCases() {
        return java.util.stream.Stream.of("reset", "cleanup").flatMap(entry ->
                java.util.stream.Stream.of("legal", "foreignRouteVersion", "missingRouteVersion")
                        .map(source -> org.junit.jupiter.params.provider.Arguments.of(entry, source)));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("snapshotRouteCases")
    void snapshotMustBelongToOwnersFrozenRouteVersion(String entry, String source) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var owner = parent();
        owner.setRouteId(701L);
        owner.setRouteVersionId(702L);
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(101L)).thenReturn(owner);
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        if (entry.equals("reset")) {
            stubFixedResetOwner();
            when(dependency(MesProcessPoolActiveOrderMapper.class)
                    .selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(owner));
            when(mapper.selectEventIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(List.of(501L));
        }
        var snapshot = MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(603L).activeOrderId(101L)
                .workOrderId(201L).routeVersionId(source.equals("missingRouteVersion") ? null
                        : source.equals("foreignRouteVersion") ? 999L : 702L)
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        snapshot.setTenantId(92820L);
        when(mapper.selectSimulationSnapshotsForUpdate(eq(92820L), anyCollection())).thenReturn(List.of(snapshot));
        var target = spy(service);
        if (entry.equals("reset")) {
            doReturn(MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(102L).workOrderId(201L)
                    .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build()).when(target).addActiveOrder(any());
        }
        org.junit.jupiter.api.function.Executable action = () -> {
            if (entry.equals("reset")) target.resetFixedSimulationActiveOrder(3001L);
            else target.cleanupLatestSimulationActiveOrder(3001L, 101L);
        };
        if (source.equals("legal")) {
            assertDoesNotThrow(action);
            verify(mapper).selectSimulationSnapshotsForUpdate(eq(92820L), anyCollection());
        } else {
            rejectWrites = true;
            var error = assertThrows(ServiceException.class, action);
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                    .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"receipt", "backfill", "releaseApplication", "nonconformance"})
    void cleanupMustRetainParentsOfFormalDownstreamHistory(String kind) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        switch (kind) {
            case "receipt" -> when(mapper.selectResetCompletionReceiptIdsForUpdate(92820L, List.of(101L))).thenReturn(List.of(1101L));
            case "backfill" -> when(mapper.selectResetCompletionBackfillIdsForUpdate(92820L, List.of(101L))).thenReturn(List.of(1102L));
            case "releaseApplication" -> when(mapper.selectResetReleaseApplicationIdsForUpdate(92820L, List.of(101L))).thenReturn(List.of(1103L));
            case "nonconformance" -> when(mapper.selectResetNonconformanceIdsForUpdate(92820L, List.of(101L))).thenReturn(List.of(1104L));
            default -> throw new AssertionError(kind);
        }
        rejectWrites = true;
        var error = assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings={"resetRevision", "cleanupRevision", "resetTransfer", "resetMaintenance"})
    void fixedResetAllowsOwnHistoryWhileOrdinaryCleanupRequiresSimulationProvenance(String kind) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        boolean reset = kind.startsWith("reset");
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        if (reset) {
            stubFixedResetOwner();
            when(mapper.selectEventIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(List.of(501L));
        }
        if (kind.endsWith("Revision")) {
            when(mapper.selectUnprovenSimulationRevisionIdsForUpdate(eq(92820L), anyCollection()))
                    .thenReturn(List.of(1201L));
        } else if (kind.endsWith("Transfer")) {
            when(mapper.selectUnprovenSimulationTransferIdsForUpdate(eq(92820L), anyCollection()))
                    .thenReturn(List.of(1202L));
        } else {
            when(mapper.selectUnprovenSimulationMaintenanceIdsForUpdate(eq(92820L), anyCollection()))
                    .thenReturn(List.of(1203L));
        }
        if (reset) {
            assertDoesNotThrow(() -> resetTargetWithSuccessfulReadd().resetFixedSimulationActiveOrder(3001L));
            verify(mapper).deleteEventRevisions(92820L, List.of(501L));
            verify(mapper).deleteTransferTraces(92820L, List.of(101L));
            verify(mapper, never()).deleteActiveOrderAudits(anyLong(), anyCollection());
        } else {
            rejectWrites = true;
            var error = assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                    .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"transfer", "maintenance"})
    void ordinaryCleanupRetainsHistoricalTablesOutsideItsDeletionScope(String history) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        if (history.equals("transfer")) {
            when(mapper.selectUnprovenSimulationTransferIdsForUpdate(eq(92820L), anyCollection()))
                    .thenReturn(List.of(1202L));
        } else {
            when(mapper.selectUnprovenSimulationMaintenanceIdsForUpdate(eq(92820L), anyCollection()))
                    .thenReturn(List.of(1203L));
        }

        assertDoesNotThrow(() -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));

        verify(dependency(MesProProcessPoolEventMapper.class)).deleteActiveOrderRuntimeEventsByIds(Set.of(501L));
        verify(mapper, never()).deleteTransferTraces(anyLong(), anyCollection());
        verify(mapper, never()).deleteActiveOrderAudits(anyLong(), anyCollection());
        verify(dependency(MesProcessPoolTeamMaintenanceAuditMapper.class), never()).deleteById(anyLong());
        verify(dependency(MesProcessPoolActiveOrderTransferTraceMapper.class), never()).deleteById(anyLong());
    }


    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> g2Candidates() {
        return java.util.stream.Stream.of("reset", "cleanup").flatMap(entry ->
                java.util.stream.Stream.of("fragment", "pqcRecord", "snapshot", "binding", "bindingItem")
                        .flatMap(table -> java.util.stream.Stream.of("formal", "differentRun", "missingIdentity",
                                        "differentStage", "foreignSource", "foreignTenant", "legal")
                                .map(scenario -> org.junit.jupiter.params.provider.Arguments.of(entry, table, scenario))));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("g2Candidates")
    void g2ChildNeedsItsOwnIdentityAndConsistentSource(String entry, String table, String scenario) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        if (entry.equals("reset")) {
            stubFixedResetOwner();
            when(dependency(MesTeamLeaderDataCleanupMapper.class)
                    .selectEventIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(List.of(501L));
        }
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        Object row;
        String sourceField;
        switch (table) {
            case "fragment" -> {
                row = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO
                        .builder().id(601L).eventId(501L).productionSubmitEventId(501L).workOrderId(201L).build();
                sourceField = "workOrderId";
                when(mapper.selectSimulationFragmentsForUpdate(eq(92820L), anyCollection(), eq(true)))
                        .thenReturn(List.of((cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolQuantityFragmentDO) row));
            }
            case "pqcRecord" -> {
                row = cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO
                        .builder().id(602L).eventId(501L).productionSubmitEventId(501L).workOrderId(201L).build();
                sourceField = "productionSubmitEventId";
                when(mapper.selectSimulationPqcRecordsForUpdate(eq(92820L), anyCollection()))
                        .thenReturn(List.of((cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO) row));
            }
            case "snapshot" -> {
                row = MesProcessPoolActiveOrderProcessSnapshotDO.builder().id(603L).activeOrderId(101L)
                        .workOrderId(201L).routeVersionId(702L).build();
                sourceField = "activeOrderId";
                when(mapper.selectSimulationSnapshotsForUpdate(eq(92820L), anyCollection()))
                        .thenReturn(List.of((MesProcessPoolActiveOrderProcessSnapshotDO) row));
            }
            case "binding" -> {
                row = MesProcessPoolActiveOrderPickListBindingDO.builder().id(604L).activeOrderId(101L)
                        .workOrderId(201L).pickListId(704L).sourceSnapshotHash("source-hash")
                        .requestPayloadHash("request-hash").build();
                sourceField = "workOrderId";
                when(mapper.selectSimulationBindingsForUpdate(eq(92820L), anyCollection()))
                        .thenReturn(List.of((MesProcessPoolActiveOrderPickListBindingDO) row));
            }
            case "bindingItem" -> {
                var binding = MesProcessPoolActiveOrderPickListBindingDO.builder().id(604L).activeOrderId(101L)
                        .workOrderId(201L).pickListId(704L).sourceSnapshotHash("source-hash")
                        .requestPayloadHash("request-hash").simulated(true).simulationRunId("M9-owned")
                        .simulationStage("LATEST_VERSION_COPY").build();
                binding.setTenantId(92820L);
                when(mapper.selectSimulationBindingsForUpdate(eq(92820L), anyCollection())).thenReturn(List.of(binding));
                row = MesProcessPoolActiveOrderPickListBindingItemDO.builder().id(605L).bindingId(604L)
                        .pickListItemId(705L).itemSnapshotHash("item-hash").build();
                sourceField = "bindingId";
                when(mapper.selectSimulationBindingItemsForUpdate(eq(92820L), anyCollection()))
                        .thenReturn(List.of((MesProcessPoolActiveOrderPickListBindingItemDO) row));
            }
            default -> throw new AssertionError(table);
        }
        ReflectionTestUtils.setField(row, "tenantId", scenario.equals("foreignTenant") ? 92821L : 92820L);
        ReflectionTestUtils.setField(row, "simulated", !scenario.equals("formal"));
        ReflectionTestUtils.setField(row, "simulationRunId",
                scenario.equals("missingIdentity") ? null : scenario.equals("differentRun") ? "other-run" : "M9-owned");
        ReflectionTestUtils.setField(row, "simulationStage",
                scenario.equals("differentStage") ? "OTHER" : "LATEST_VERSION_COPY");
        if (scenario.equals("foreignSource")) ReflectionTestUtils.setField(row, sourceField, 999L);
        var subject = service;
        if (entry.equals("reset")) {
            subject = spy(service);
            doReturn(MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(102L).workOrderId(201L)
                    .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build()).when(subject).addActiveOrder(any());
        }
        final var target = subject;
        org.junit.jupiter.api.function.Executable action = () -> {
            if (entry.equals("reset")) target.resetFixedSimulationActiveOrder(3001L);
            else target.cleanupLatestSimulationActiveOrder(3001L, 101L);
        };
        if (scenario.equals("legal") || (entry.equals("reset")
                && Set.of("formal", "differentRun", "missingIdentity", "differentStage").contains(scenario))) {
            assertDoesNotThrow(action);
            // A positive must consume the candidate, not pass because the new query was never reached.
            switch (table) {
                case "fragment" -> verify(mapper).selectSimulationFragmentsForUpdate(eq(92820L), anyCollection(), eq(true));
                case "pqcRecord" -> verify(mapper).selectSimulationPqcRecordsForUpdate(eq(92820L), anyCollection());
                case "snapshot" -> verify(mapper).selectSimulationSnapshotsForUpdate(eq(92820L), anyCollection());
                case "binding" -> verify(mapper).selectSimulationBindingsForUpdate(eq(92820L), anyCollection());
                case "bindingItem" -> verify(mapper).selectSimulationBindingItemsForUpdate(eq(92820L), anyCollection());
                default -> throw new AssertionError(table);
            }
        } else {
            rejectWrites = true;
            var error = assertThrows(ServiceException.class, action);
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                    .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
        }
    }

    MesTeamLeaderActiveOrderServiceImpl service;
    final Map<Class<?>, Object> dependencies = new HashMap<>();
    boolean rejectWrites;

    @BeforeEach
    void setUp() throws Exception {
        TenantContextHolder.setTenantId(92820L);
        var ctor = MesTeamLeaderActiveOrderServiceImpl.class.getConstructors()[0];
        Object[] args = Arrays.stream(ctor.getParameterTypes()).map(this::createMock).toArray();
        service = (MesTeamLeaderActiveOrderServiceImpl) ctor.newInstance(args);
        for (var field : MesTeamLeaderActiveOrderServiceImpl.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(jakarta.annotation.Resource.class)) {
                ReflectionTestUtils.setField(service, field.getName(), createMock(field.getType()));
            }
        }
    }

    Object createMock(Class<?> type) {
        return dependencies.computeIfAbsent(type, key -> mock(key, call -> {
            String name = call.getMethod().getName();
            if (rejectWrites && (name.startsWith("delete") || name.startsWith("update") || name.startsWith("insert"))) {
                fail("Unexpected first write: " + key.getSimpleName() + "." + name);
            }
            return org.mockito.Answers.RETURNS_DEFAULTS.answer(call);
        }));
    }

    @SuppressWarnings("unchecked")
    <T> T dependency(Class<T> type) { return (T) createMock(type); }

    @AfterEach
    void tearDown() { TenantContextHolder.clear(); }

    MesProcessPoolActiveOrderDO parent() {
        var row = MesProcessPoolActiveOrderDO.builder().id(101L).workOrderId(201L).leaderUserId(3001L)
                .routeId(701L).routeVersionId(702L)
                .activeStatus("ACTIVE").businessStatus("ACTIVE").simulated(true)
                .simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        row.setTenantId(92820L);
        return row;
    }

    @ParameterizedTest
    @ValueSource(strings={"formal", "missingRun", "differentOwner", "missingParent"})
    void fixedResetRequiresOwnershipButNotSimulationTags(String scenario) {
        var workOrder = MesProWorkOrderDO.builder().id(201L).build();
        workOrder.setTenantId(92820L);
        when(dependency(MesProWorkOrderMapper.class).selectByTenantIdAndCodeForUpdate(eq(92820L), anyString())).thenReturn(workOrder);
        var order = parent();
        if (scenario.equals("formal")) order.setSimulated(false);
        if (scenario.equals("missingRun")) order.setSimulationRunId(null);
        if (scenario.equals("differentOwner")) order.setLeaderUserId(3002L);
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(scenario.equals("missingParent") ? List.of() : List.of(order));
        if (scenario.equals("formal") || scenario.equals("missingRun")) {
            var result = assertDoesNotThrow(() -> resetTargetWithSuccessfulReadd().resetFixedSimulationActiveOrder(3001L));
            assertEquals(201L, result.getWorkOrderId());
            verify(dependency(MesTeamLeaderDataCleanupMapper.class)).deleteActiveOrders(92820L, List.of(101L));
        } else {
            rejectWrites = true;
            assertThrows(ServiceException.class, () -> service.resetFixedSimulationActiveOrder(3001L));
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"formalEvent", "differentRunEvent", "differentStageEvent", "missingEvent", "formalAllocation"})
    void simulationParentCannotAuthorizeDeletionOfUnprovenChildren(String scenario) {
        var parent = parent();
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(101L)).thenReturn(parent);
        var allocation = MesProcessPoolReportAllocationDO.builder().id(401L).activeOrderId(101L).eventId(501L)
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        allocation.setTenantId(92820L);
        if (scenario.equals("formalAllocation")) allocation.setSimulated(false);
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectSimulationAllocationsForUpdate(92820L, 101L))
                .thenReturn(List.of(allocation));
        var event = MesProProcessPoolEventDO.builder().id(501L).workOrderId(201L).eventType("PRODUCTION_SUBMIT")
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        event.setTenantId(92820L);
        if (scenario.equals("formalEvent")) event.setSimulated(false);
        if (scenario.equals("differentRunEvent")) event.setSimulationRunId("M9-other");
        if (scenario.equals("differentStageEvent")) event.setSimulationStage("OTHER");
        when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(501L))
                .thenReturn(scenario.equals("missingEvent") ? null : event);
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
    }

    @Test
    void ownedNonEmptyProductionEvidenceStillCleansUp() {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        assertDoesNotThrow(() -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
        verify(dependency(MesProProcessPoolEventMapper.class)).deleteActiveOrderRuntimeEventsByIds(Set.of(501L));
        verify(dependency(MesProcessPoolReportAllocationMapper.class)).deleteAllByActiveOrderId(101L);
        verify(dependency(MesProWorkOrderMapper.class)).deleteById(201L);
        verify(dependency(MesProWorkOrderMapper.class), never()).deleteById(202L);
    }

    @ParameterizedTest
    @ValueSource(strings={"formal", "differentRun", "differentTenant"})
    void simulationEventDoesNotAuthorizeDeletingUnprovenReview(String scenario) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var review = MesProcessPoolSubmissionReviewDO.builder().id(601L).eventId(501L)
                .simulated(!scenario.equals("formal")).simulationStage("LATEST_VERSION_COPY")
                .simulationRunId(scenario.equals("differentRun") ? "M9-other" : "M9-owned").build();
        review.setTenantId(scenario.equals("differentTenant") ? 92821L : 92820L);
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectCleanupReviewsForUpdate(92820L, Set.of(501L)))
                .thenReturn(List.of(review));
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
    }

    @Test
    void ownedSimulationReviewStillCleansUp() {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var review = MesProcessPoolSubmissionReviewDO.builder().id(601L).eventId(501L)
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        review.setTenantId(92820L);
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectCleanupReviewsForUpdate(92820L, Set.of(501L)))
                .thenReturn(List.of(review));
        assertDoesNotThrow(() -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
        verify(dependency(MesProcessPoolSubmissionReviewMapper.class)).deleteByEventIds(Set.of(501L));
    }

    @Test
    void simulatedParentCannotDeleteOrphanFormalAggregate() {
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(101L)).thenReturn(parent());
        var aggregate = MesPqcProcessInspectionAggregateDetailDO.builder().id(701L)
                .activeOrderId(101L).workOrderId(201L).eventId(999L).pqcTaskId(998L)
                .simulated(false).build();
        aggregate.setTenantId(92820L);
        when(dependency(MesTeamLeaderDataCleanupMapper.class)
                .selectCleanupAggregatesForUpdate(92820L, 101L)).thenReturn(List.of(aggregate));
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
    }

    @ParameterizedTest
    @ValueSource(strings={"missingEvent", "foreignAllocation"})
    void simulatedParentCannotDeleteCompletionOutsideEvidenceClosure(String scenario) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var completion = MesProcessPoolOrderProcessCompletionDO.builder().id(801L).workOrderId(201L)
                .lastEventId(scenario.equals("missingEvent") ? 999L : 501L)
                .sourceEventIdsJson(scenario.equals("missingEvent") ? "[999]" : "[501]")
                .sourceAllocationIdsJson(scenario.equals("foreignAllocation") ? "[999]" : "[401]")
                .build();
        completion.setTenantId(92820L);
        when(dependency(MesTeamLeaderDataCleanupMapper.class)
                .selectCleanupCompletionsForUpdate(92820L, 201L)).thenReturn(List.of(completion));
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
    }

    @Test
    void sameRunCompletionEvidenceClosureStillCleansUp() {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var completion = MesProcessPoolOrderProcessCompletionDO.builder().id(801L).workOrderId(201L)
                .lastEventId(501L).sourceEventIdsJson("[501]").sourceAllocationIdsJson("[401]").build();
        completion.setTenantId(92820L);
        when(dependency(MesTeamLeaderDataCleanupMapper.class)
                .selectCleanupCompletionsForUpdate(92820L, 201L)).thenReturn(List.of(completion));
        assertDoesNotThrow(() -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
        verify(dependency(MesProcessPoolOrderProcessCompletionMapper.class)).deleteByWorkOrderId(201L);
    }

    @ParameterizedTest
    @ValueSource(strings={"PRODUCTION_SUBMIT", "PQC_INSPECTION"})
    void sharedEventCannotBeDeletedBySimulationCleanup(String eventType) {
        stubOwnedEvent(eventType, true);
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
    }

    private void stubOwnedEvent(String eventType, boolean shared) {
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(101L)).thenReturn(parent());
        var allocation = MesProcessPoolReportAllocationDO.builder().id(401L).activeOrderId(101L).eventId(501L)
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        allocation.setTenantId(92820L);
        var event = MesProProcessPoolEventDO.builder().id(501L).workOrderId(201L).eventType(eventType)
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        event.setTenantId(92820L);
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectSimulationAllocationsForUpdate(92820L, 101L))
                .thenReturn(List.of(allocation));
        when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(501L)).thenReturn(event);
        var other = MesProcessPoolReportAllocationDO.builder().id(402L).activeOrderId(102L).eventId(501L)
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        other.setTenantId(92820L);
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectSimulationEventAllocationsForUpdate(92820L, 501L))
                .thenReturn(shared ? List.of(allocation, other) : List.of(allocation));
        when(dependency(MesProcessPoolReportAllocationMapper.class).selectAllListByEventIdForUpdate(501L))
                .thenReturn(shared ? List.of(allocation, other) : List.of(allocation));
    }

    @Test
    void emptyOwnedSimulationCopyStillCleansUp() {
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(101L)).thenReturn(parent());
        assertDoesNotThrow(() -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
        verify(dependency(MesProWorkOrderMapper.class)).deleteById(201L);
    }

    @ParameterizedTest
    @ValueSource(strings={"formal", "differentRun", "missingIdentity", "softDeletedFormal"})
    void simulationTaskCannotAuthorizeDeletingUnprovenPiece(String scenario) {
        stubSimulationPiece(scenario);
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
    }

    @Test
    void nonEmptySameRunSimulationPieceStillCleansUp() {
        stubSimulationPiece("owned");
        assertDoesNotThrow(() -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
        verify(dependency(MesPqcInspectionPieceDetailMapper.class)).deleteByTaskIds(List.of(301L));
    }

    private void stubSimulationPiece(String scenario) {
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(101L)).thenReturn(parent());
        var task = MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(101L).workOrderId(201L)
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        task.setTenantId(92820L);
        when(dependency(MesPqcInspectionTaskMapper.class).selectListByActiveOrderIdForUpdate(101L))
                .thenReturn(List.of(task));
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectCleanupTasksForUpdate(92820L, 101L))
                .thenReturn(List.of(task));
        var piece = MesPqcInspectionPieceDetailDO.builder().id(401L).taskId(301L)
                .simulated(!scenario.equals("formal") && !scenario.equals("softDeletedFormal"))
                .simulationStage("LATEST_VERSION_COPY")
                .simulationRunId(scenario.equals("differentRun") ? "M9-other" : "M9-owned").build();
        piece.setTenantId(92820L);
        if (scenario.equals("missingIdentity")) piece.setSimulationRunId(null);
        if (scenario.equals("softDeletedFormal")) piece.setDeleted(true);
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectCleanupPiecesForUpdate(92820L, List.of(301L)))
                .thenReturn(List.of(piece));
    }

    @ParameterizedTest
    @ValueSource(strings={"formalEvent", "formalBatch"})
    void fixedResetMayClearFormalEventOrBatchBelongingToFixedWorkOrder(String scenario) {
        var workOrder = MesProWorkOrderDO.builder().id(201L).build();
        workOrder.setTenantId(92820L);
        when(dependency(MesProWorkOrderMapper.class).selectByTenantIdAndCodeForUpdate(eq(92820L), anyString()))
                .thenReturn(workOrder);
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(parent()));
        if (scenario.equals("formalEvent")) {
            when(dependency(MesTeamLeaderDataCleanupMapper.class).selectEventIdsByWorkOrderIds(92820L, List.of(201L)))
                    .thenReturn(List.of(501L));
            var event = MesProProcessPoolEventDO.builder().id(501L).workOrderId(201L)
                    .eventType("PRODUCTION_SUBMIT").simulated(false).build();
            event.setTenantId(92820L);
            when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(501L)).thenReturn(event);
        } else {
            when(dependency(MesTeamLeaderDataCleanupMapper.class).selectBatchExecutionIdsByWorkOrderIds(92820L, List.of(201L)))
                    .thenReturn(List.of(901L));
        }
        assertDoesNotThrow(() -> resetTargetWithSuccessfulReadd().resetFixedSimulationActiveOrder(3001L));
        if (scenario.equals("formalEvent")) {
            verify(dependency(MesTeamLeaderDataCleanupMapper.class)).deleteEvents(92820L, List.of(501L));
        } else {
            verify(dependency(MesTeamLeaderDataCleanupMapper.class)).deleteBatchExecutions(92820L, List.of(901L));
        }
    }

    private void stubFixedResetOwner() {
        var workOrder = MesProWorkOrderDO.builder().id(201L).build();
        workOrder.setTenantId(92820L);
        when(dependency(MesProWorkOrderMapper.class).selectByTenantIdAndCodeForUpdate(eq(92820L), anyString()))
                .thenReturn(workOrder);
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(parent()));
    }

    @ParameterizedTest
    @ValueSource(strings={"task", "piece", "aggregate", "completion", "feedback"})
    void fixedResetAllowsOwnedTaskPiecesButRejectsIncompleteSourceEvidence(String kind) {
        stubFixedResetOwner();
        var cleanup = dependency(MesTeamLeaderDataCleanupMapper.class);
        if (kind.equals("task") || kind.equals("piece")) {
            when(cleanup.selectPqcTaskIds(92820L, List.of(101L))).thenReturn(List.of(301L));
            when(cleanup.selectHistoricalPqcTaskIdsForUpdate(92820L, List.of(101L))).thenReturn(List.of(301L));
            var task = MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(101L).workOrderId(201L)
                    .simulated(kind.equals("piece")).simulationStage("LATEST_VERSION_COPY")
                    .simulationRunId("M9-owned").build();
            task.setTenantId(92820L);
            when(dependency(MesPqcInspectionTaskMapper.class).selectById(301L)).thenReturn(task);
            when(dependency(MesPqcInspectionTaskMapper.class).selectListByActiveOrderIdForUpdate(101L))
                    .thenReturn(List.of(task));
            when(cleanup.selectCleanupTasksForUpdate(92820L, 101L)).thenReturn(List.of(task));
            if (kind.equals("piece")) {
                var piece = MesPqcInspectionPieceDetailDO.builder().id(401L).taskId(301L).simulated(false).build();
                piece.setTenantId(92820L);
                when(cleanup.selectCleanupPiecesForUpdate(92820L, List.of(301L))).thenReturn(List.of(piece));
                when(cleanup.selectHistoricalPqcPieceIdsForUpdate(92820L, List.of(301L))).thenReturn(List.of(401L));
            }
        } else if (kind.equals("aggregate")) {
            var aggregate = MesPqcProcessInspectionAggregateDetailDO.builder().id(701L)
                    .activeOrderId(101L).workOrderId(201L).simulated(false).build();
            aggregate.setTenantId(92820L);
            when(cleanup.selectCleanupAggregatesForUpdate(92820L, 101L)).thenReturn(List.of(aggregate));
        } else if (kind.equals("completion")) {
            var completion = MesProcessPoolOrderProcessCompletionDO.builder().id(801L).workOrderId(201L)
                    .lastEventId(999L).sourceEventIdsJson("[999]").sourceAllocationIdsJson("[998]").build();
            completion.setTenantId(92820L);
            when(cleanup.selectCleanupCompletionsForUpdate(92820L, 201L)).thenReturn(List.of(completion));
        } else {
            when(cleanup.selectFeedbackIds(92820L, List.of(201L))).thenReturn(List.of(901L));
            when(dependency(MesProFeedbackMapper.class).selectListByIdsForUpdate(List.of(901L)))
                    .thenReturn(List.of(MesProFeedbackDO.builder().id(901L).workOrderId(201L).build()));
        }
        if (kind.equals("task") || kind.equals("piece")) {
            assertDoesNotThrow(() -> resetTargetWithSuccessfulReadd().resetFixedSimulationActiveOrder(3001L));
            verify(cleanup).deletePqcTasks(92820L, List.of(101L));
            if (kind.equals("piece")) verify(cleanup).deletePqcPieceDetails(92820L, List.of(301L));
        } else {
            rejectWrites = true;
            var error = assertThrows(ServiceException.class, () -> service.resetFixedSimulationActiveOrder(3001L));
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                    .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
        }
    }

    @Test
    void cleanupCannotPhysicallyDeleteTasksHiddenFromLiveQuery() {
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(101L)).thenReturn(parent());
        // The live mapper deliberately returns no rows. Full-history IDs reveal a deleted task;
        // do not feed that row into the live wrapper and pretend logical-delete coverage exists.
        when(dependency(MesPqcInspectionTaskMapper.class).selectListByActiveOrderIdForUpdate(101L))
                .thenReturn(List.of());
        when(dependency(MesTeamLeaderDataCleanupMapper.class)
                .selectHistoricalPqcTaskIdsForUpdate(92820L, List.of(101L))).thenReturn(List.of(301L));
        var deletedTask = MesPqcInspectionTaskDO.builder().id(301L).activeOrderId(101L).workOrderId(201L)
                .simulated(false).build();
        deletedTask.setTenantId(92820L);
        deletedTask.setDeleted(true);
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectCleanupTasksForUpdate(92820L, 101L))
                .thenReturn(List.of(deletedTask));
        rejectWrites = true;
        var error = assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
    }

    @Test
    void emptyOwnedFixedResetPreservesRecreationContract() {
        stubFixedResetOwner();
        var resetService = spy(service);
        // This test covers reset orchestration only, not the separately implemented add workflow.
        doReturn(MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(102L).workOrderId(201L)
                .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build()).when(resetService).addActiveOrder(any());
        var result = resetService.resetFixedSimulationActiveOrder(3001L);
        assertEquals(102L, result.getActiveOrderId());
        assertEquals(MesTeamLeaderActiveOrderAddResult.ACTION_ADD, result.getAction());
        assertEquals(0L, result.getDeletedEventCount());
        verify(dependency(MesTeamLeaderDataCleanupMapper.class)).deleteActiveOrders(92820L, List.of(101L));
        verify(dependency(MesTeamLeaderDataCleanupMapper.class), never()).deleteActiveOrders(92820L, List.of(999L));
    }

    @ParameterizedTest
    @ValueSource(strings={"receipt", "backfill", "releaseApplication", "nonconformance"})
    void fixedResetClearsItsOwnCompletedDownstreamFactsAndReaddsSameWorkOrder(String kind) {
        stubFixedResetOwner();
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        switch (kind) {
            case "receipt" -> when(mapper.selectResetCompletionReceiptIdsForUpdate(92820L, List.of(101L)))
                    .thenReturn(List.of(1101L));
            case "backfill" -> when(mapper.selectResetCompletionBackfillIdsForUpdate(92820L, List.of(101L)))
                    .thenReturn(List.of(1102L));
            case "releaseApplication" -> when(mapper.selectResetReleaseApplicationIdsForUpdate(92820L, List.of(101L)))
                    .thenReturn(List.of(1103L));
            case "nonconformance" -> when(mapper.selectResetNonconformanceIdsForUpdate(92820L, List.of(101L)))
                    .thenReturn(List.of(1104L));
            default -> throw new AssertionError(kind);
        }
        var target = resetTargetWithSuccessfulReadd();
        var result = assertDoesNotThrow(() -> target.resetFixedSimulationActiveOrder(3001L));
        assertEquals(201L, result.getWorkOrderId());
        assertEquals(102L, result.getActiveOrderId());
        verify(mapper).deleteCompletionReceipts(92820L, List.of(101L));
        verify(mapper).deleteCompletionBackfills(92820L, List.of(101L));
        verify(mapper).deleteReleaseApplications(92820L, List.of(101L));
        verify(mapper).deleteNonconformanceReviewsByActiveOrderIds(92820L, List.of(101L));
        verify(mapper).deleteActiveOrders(92820L, List.of(101L));
        verify(target).addActiveOrder(argThat(request -> request.getLeaderUserId().equals(3001L)
                && request.getWorkOrderId().equals(201L)));
    }

    private MesTeamLeaderActiveOrderServiceImpl resetTargetWithSuccessfulReadd() {
        var target = spy(service);
        doReturn(MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(102L).workOrderId(201L)
                .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build()).when(target).addActiveOrder(any());
        return target;
    }

    @Test
    void fixedResetCanRunAgainAfterReaddCreatesAnOrdinaryActiveOrder() {
        stubFixedResetOwner();
        var secondRound = parent();
        secondRound.setId(102L);
        secondRound.setSimulated(false);
        secondRound.setSimulationStage(null);
        secondRound.setSimulationRunId(null);
        when(dependency(MesProcessPoolActiveOrderMapper.class)
                .selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(parent()), List.of(secondRound));
        var target = spy(service);
        doReturn(MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(102L).workOrderId(201L)
                        .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build(),
                MesTeamLeaderActiveOrderAddResult.builder().activeOrderId(103L).workOrderId(201L)
                        .action(MesTeamLeaderActiveOrderAddResult.ACTION_ADD).build())
                .when(target).addActiveOrder(any());

        assertEquals(102L, target.resetFixedSimulationActiveOrder(3001L).getActiveOrderId());
        assertEquals(103L, target.resetFixedSimulationActiveOrder(3001L).getActiveOrderId());

        verify(dependency(MesTeamLeaderDataCleanupMapper.class)).deleteActiveOrders(92820L, List.of(101L));
        verify(dependency(MesTeamLeaderDataCleanupMapper.class)).deleteActiveOrders(92820L, List.of(102L));
        verify(target, times(2)).addActiveOrder(argThat(request -> request.getWorkOrderId().equals(201L)));
        verify(dependency(MesProWorkOrderMapper.class), never()).insert(any(MesProWorkOrderDO.class));
    }

    @Test
    void fixedResetDoesNotHideReaddFailureAndDeclaresRollbackForException() throws Exception {
        stubFixedResetOwner();
        var target = spy(service);
        var failure = new IllegalStateException("formal add prerequisite failed");
        doThrow(failure).when(target).addActiveOrder(any());

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> target.resetFixedSimulationActiveOrder(3001L)));
        var transactional = MesTeamLeaderActiveOrderServiceImpl.class
                .getMethod("resetFixedSimulationActiveOrder", Long.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertNotNull(transactional);
        assertTrue(Arrays.asList(transactional.rollbackFor()).contains(Exception.class));
        // This unit test verifies propagation and the transaction contract, not a database rollback.
    }

    @ParameterizedTest
    @ValueSource(strings={"foreignTenant", "foreignWorkOrder", "missingEvent"})
    void fixedResetRejectsOutOfScopeOrMissingEventBeforeWrites(String scope) {
        stubFixedResetOwner();
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectEventIdsByWorkOrderIds(92820L, List.of(201L)))
                .thenReturn(List.of(501L));
        var event = MesProProcessPoolEventDO.builder().id(501L)
                .workOrderId(scope.equals("foreignWorkOrder") ? 202L : 201L).eventType("PRODUCTION_SUBMIT").build();
        event.setTenantId(scope.equals("foreignTenant") ? 92821L : 92820L);
        when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(501L))
                .thenReturn(scope.equals("missingEvent") ? null : event);
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.resetFixedSimulationActiveOrder(3001L));
    }

    @ParameterizedTest
    @ValueSource(strings={"ownedBackfill", "foreignBackfill"})
    void fixedResetCompletionRequiresActualCompletionBackfillIdentity(String scope) {
        stubFixedResetOwner();
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        when(mapper.selectEventIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(List.of(501L));
        when(mapper.selectResetCompletionBackfillIdsForUpdate(92820L, List.of(101L))).thenReturn(List.of(1102L));
        var completion = MesProcessPoolOrderProcessCompletionDO.builder().id(801L).workOrderId(201L)
                .lastEventId(501L).sourceEventIdsJson("[501]").sourceAllocationIdsJson("[401]")
                .backfillExecutionId(scope.equals("ownedBackfill") ? 1102L : 999L).build();
        completion.setTenantId(92820L);
        when(mapper.selectCleanupCompletionsForUpdate(92820L, 201L)).thenReturn(List.of(completion));

        if (scope.equals("ownedBackfill")) {
            assertDoesNotThrow(() -> resetTargetWithSuccessfulReadd().resetFixedSimulationActiveOrder(3001L));
            verify(mapper).deleteOrderProcessCompletions(92820L, List.of(201L));
            verify(mapper).deleteCompletionBackfills(92820L, List.of(101L));
        } else {
            rejectWrites = true;
            assertThrows(ServiceException.class, () -> service.resetFixedSimulationActiveOrder(3001L));
        }
    }

    @Test
    void fixedResetClearsCompletedBatchReleaseAndRecordRuntimeBeforeReadd() {
        stubFixedResetOwner();
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        when(mapper.selectBatchExecutionIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(List.of(901L));
        when(mapper.selectBatchRecordExecutionIds(92820L, List.of(901L))).thenReturn(List.of(902L));
        when(mapper.selectReleaseTransactionIds(92820L, List.of(901L))).thenReturn(List.of(903L));
        when(mapper.selectRecordbookIds(92820L, List.of(901L))).thenReturn(List.of(904L));
        when(mapper.selectRecordbookEntryIds(92820L, List.of(904L))).thenReturn(List.of(905L));
        when(mapper.selectFormInstanceIds(92820L, List.of(901L))).thenReturn(List.of(906L));
        when(mapper.selectTravelerIds(92820L, List.of(901L))).thenReturn(List.of(907L));
        var target = resetTargetWithSuccessfulReadd();

        var result = assertDoesNotThrow(() -> target.resetFixedSimulationActiveOrder(3001L));

        assertEquals(201L, result.getWorkOrderId());
        assertEquals(1L, result.getDeletedBatchExecutionCount());
        assertEquals(1L, result.getDeletedRecordExecutionCount());
        var ordered = inOrder(mapper, target);
        ordered.verify(mapper).deleteExecutionAttachments(92820L, List.of(901L), List.of(902L));
        ordered.verify(mapper).deleteWorkTasks(92820L, List.of(901L));
        ordered.verify(mapper).deleteNonconformanceReviews(92820L, List.of(901L));
        ordered.verify(mapper).deleteTravelerEvents(92820L, List.of(907L));
        ordered.verify(mapper).deleteRecordbooks(92820L, List.of(904L));
        ordered.verify(mapper).deleteFormInstances(92820L, List.of(906L));
        ordered.verify(mapper).deleteReleaseTransactions(92820L, List.of(901L));
        ordered.verify(mapper).deleteExecutions(92820L, List.of(902L));
        ordered.verify(mapper).deleteBatchExecutions(92820L, List.of(901L));
        ordered.verify(mapper).deleteActiveOrders(92820L, List.of(101L));
        ordered.verify(target).addActiveOrder(argThat(request -> request.getWorkOrderId().equals(201L)));
        verify(dependency(MesProWorkOrderMapper.class), never()).deleteById(anyLong());
        verify(mapper, never()).deleteExecutionSignatures(anyLong(), anyCollection());
        verify(mapper, never()).deleteBatchSignatures(anyLong(), anyCollection());
        verify(mapper, never()).deleteBatchArchives(anyLong(), anyCollection());
        verify(mapper, never()).deleteOperationAuditEvents(anyLong(), anyCollection(), anyCollection());
        verify(mapper, never()).deleteRecordChangeEvents(anyLong(), anyCollection(), anyCollection());
        verify(mapper, never()).deleteActiveOrderAudits(anyLong(), anyCollection());
    }

    @Test
    void fixedResetClearsCompletedAndReworkCyclesOfSameOwnerAndWorkOrder() {
        stubFixedResetOwner();
        var original = parent();
        original.setActiveStatus("CLOSED");
        original.setBusinessStatus("REWORK");
        var rework = parent();
        rework.setId(103L);
        rework.setSimulated(false);
        rework.setSimulationRunId(null);
        rework.setSimulationStage(null);
        rework.setReworkSourceActiveOrderId(101L);
        rework.setReworkReviewId(1104L);
        when(dependency(MesProcessPoolActiveOrderMapper.class)
                .selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(original, rework));
        var target = resetTargetWithSuccessfulReadd();

        var result = assertDoesNotThrow(() -> target.resetFixedSimulationActiveOrder(3001L));

        assertEquals(201L, result.getWorkOrderId());
        assertEquals(2L, result.getPreviousActiveOrderCount());
        verify(dependency(MesTeamLeaderDataCleanupMapper.class)).deleteActiveOrders(92820L, List.of(101L, 103L));
        verify(target).addActiveOrder(argThat(request -> request.getWorkOrderId().equals(201L)));
    }

    @Test
    void fixedResetCompletionCanReferenceAllocationsAcrossItsOriginalAndReworkCycles() {
        stubFixedResetOwner();
        var rework = parent();
        rework.setId(103L);
        rework.setSimulated(false);
        rework.setReworkSourceActiveOrderId(101L);
        rework.setReworkReviewId(1104L);
        when(dependency(MesProcessPoolActiveOrderMapper.class)
                .selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(parent(), rework));
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        when(mapper.selectEventIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(List.of(501L, 502L));
        when(mapper.selectResetCompletionBackfillIdsForUpdate(92820L, List.of(101L, 103L)))
                .thenReturn(List.of(1102L));
        for (int index = 0; index < 2; index++) {
            long ownerId = index == 0 ? 101L : 103L;
            long eventId = 501L + index;
            var event = MesProProcessPoolEventDO.builder().id(eventId).workOrderId(201L)
                    .eventType("PRODUCTION_SUBMIT").build();
            event.setTenantId(92820L);
            when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(eventId)).thenReturn(event);
            var allocation = MesProcessPoolReportAllocationDO.builder().id(401L + index)
                    .activeOrderId(ownerId).eventId(eventId).build();
            allocation.setTenantId(92820L);
            when(mapper.selectSimulationAllocationsForUpdate(92820L, ownerId)).thenReturn(List.of(allocation));
            when(mapper.selectSimulationEventAllocationsForUpdate(92820L, eventId)).thenReturn(List.of(allocation));
        }
        var completion = MesProcessPoolOrderProcessCompletionDO.builder().id(801L).workOrderId(201L)
                .lastEventId(502L).sourceEventIdsJson("[501,502]").sourceAllocationIdsJson("[401,402]")
                .backfillExecutionId(1102L).build();
        completion.setTenantId(92820L);
        when(mapper.selectCleanupCompletionsForUpdate(92820L, 201L)).thenReturn(List.of(completion));

        assertDoesNotThrow(() -> resetTargetWithSuccessfulReadd().resetFixedSimulationActiveOrder(3001L));

        verify(mapper).deleteAllocations(92820L, List.of(101L, 103L));
        verify(mapper).deleteOrderProcessCompletions(92820L, List.of(201L));
    }

    @ParameterizedTest
    @ValueSource(strings={"foreignTenant", "foreignOwner", "foreignWorkOrder"})
    void fixedResetRejectsAnyCandidateOutsideItsTenantOwnerOrWorkOrderBeforeWrites(String scope) {
        stubFixedResetOwner();
        var outside = parent();
        outside.setId(103L);
        if (scope.equals("foreignTenant")) outside.setTenantId(92821L);
        if (scope.equals("foreignOwner")) outside.setLeaderUserId(3002L);
        if (scope.equals("foreignWorkOrder")) outside.setWorkOrderId(202L);
        when(dependency(MesProcessPoolActiveOrderMapper.class)
                .selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(parent(), outside));
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.resetFixedSimulationActiveOrder(3001L));
    }

    @ParameterizedTest
    @ValueSource(strings={"allocation", "pqcTask"})
    void fixedResetCannotDeleteEventReferencedOutsideItsCandidateOrders(String referenceType) {
        stubFixedResetOwner();
        var mapper = dependency(MesTeamLeaderDataCleanupMapper.class);
        when(mapper.selectEventIdsByWorkOrderIds(92820L, List.of(201L))).thenReturn(List.of(501L));
        var event = MesProProcessPoolEventDO.builder().id(501L).workOrderId(201L).eventType("PQC_INSPECTION")
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        event.setTenantId(92820L);
        when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(501L)).thenReturn(event);
        if (referenceType.equals("allocation")) {
            var reference = MesProcessPoolReportAllocationDO.builder().id(402L).activeOrderId(102L)
                    .eventId(501L).simulated(false).build();
            reference.setTenantId(92820L);
            when(mapper.selectSimulationEventAllocationsForUpdate(92820L, 501L))
                    .thenReturn(List.of(reference));
        } else {
            var reference = MesPqcInspectionTaskDO.builder().id(302L).activeOrderId(102L).workOrderId(202L)
                    .submittedEventId(501L).simulated(false).build();
            reference.setTenantId(92820L);
            when(mapper.selectCleanupReferencingTasksForUpdate(Set.of(501L))).thenReturn(List.of(reference));
        }
        rejectWrites = true;
        var error = assertThrows(ServiceException.class, () -> service.resetFixedSimulationActiveOrder(3001L));
        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
    }

    @Test
    void simulationPqcEventReferencedByAnotherOrdersTaskCannotBeDeleted() {
        stubOwnedEvent("PQC_INSPECTION", false);
        var foreignTask = MesPqcInspectionTaskDO.builder().id(302L).activeOrderId(102L).workOrderId(202L)
                .submittedEventId(501L).simulated(false).build();
        foreignTask.setTenantId(92820L);
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectCleanupReferencingTasksForUpdate(Set.of(501L)))
                .thenReturn(List.of(foreignTask));
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
    }

    @ParameterizedTest
    @ValueSource(strings={"reset", "cleanup"})
    void softDeletedOwnedAllocationIsIncludedAndOnlyFixedResetAllowsFormalProvenance(String entry) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        if (entry.equals("reset")) {
            stubFixedResetOwner();
            when(dependency(MesTeamLeaderDataCleanupMapper.class).selectEventIdsByWorkOrderIds(92820L, List.of(201L)))
                    .thenReturn(List.of(501L));
        }
        var historical = MesProcessPoolReportAllocationDO.builder().id(499L).activeOrderId(101L)
                .eventId(501L).simulated(false).build();
        historical.setTenantId(92820L);
        historical.setDeleted(true);
        // The live wrapper genuinely excludes this row; only the all-history query returns it.
        when(dependency(MesProcessPoolReportAllocationMapper.class).selectAllListByActiveOrderIdForUpdate(101L))
                .thenReturn(List.of());
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectSimulationAllocationsForUpdate(92820L, 101L))
                .thenReturn(List.of(historical));
        if (entry.equals("reset")) {
            assertDoesNotThrow(() -> resetTargetWithSuccessfulReadd().resetFixedSimulationActiveOrder(3001L));
            verify(dependency(MesTeamLeaderDataCleanupMapper.class)).selectSimulationAllocationsForUpdate(92820L, 101L);
            verify(dependency(MesTeamLeaderDataCleanupMapper.class)).deleteAllocations(92820L, List.of(101L));
        } else {
            rejectWrites = true;
            var error = assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
            assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                    .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"reset", "cleanup"})
    void softDeletedExternalAllocationStillProtectsItsReferencedEvent(String entry) {
        if (entry.equals("reset")) {
            stubFixedResetOwner();
            when(dependency(MesTeamLeaderDataCleanupMapper.class).selectEventIdsByWorkOrderIds(92820L, List.of(201L)))
                    .thenReturn(List.of(501L));
            var event = MesProProcessPoolEventDO.builder().id(501L).workOrderId(201L).eventType("PRODUCTION_SUBMIT")
                    .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
            event.setTenantId(92820L);
            when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(501L)).thenReturn(event);
        } else {
            stubOwnedEvent("PRODUCTION_SUBMIT", false);
        }
        var historical = MesProcessPoolReportAllocationDO.builder().id(499L).activeOrderId(102L)
                .eventId(501L).simulated(false).build();
        historical.setTenantId(92820L);
        historical.setDeleted(true);
        when(dependency(MesProcessPoolReportAllocationMapper.class).selectAllListByEventIdForUpdate(501L))
                .thenReturn(List.of());
        when(dependency(MesTeamLeaderDataCleanupMapper.class).selectSimulationEventAllocationsForUpdate(92820L, 501L))
                .thenReturn(List.of(historical));
        rejectWrites = true;
        var error = assertThrows(ServiceException.class, () -> {
            if (entry.equals("reset")) service.resetFixedSimulationActiveOrder(3001L);
            else service.cleanupLatestSimulationActiveOrder(3001L, 101L);
        });
        assertEquals(cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants
                .PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED.getCode(), error.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings={"foreignWorkOrder", "wrongSourceType", "missingFeedback"})
    void simulatedEventCannotAuthorizeUnprovenFeedbackDeletion(String scenario) {
        stubOwnedEvent("PRODUCTION_SUBMIT", false);
        var event = MesProProcessPoolEventDO.builder().id(501L).workOrderId(201L)
                .eventType("PRODUCTION_SUBMIT").feedbackSourceId(901L).feedbackSourceType("MES_PRO_FEEDBACK")
                .simulated(true).simulationStage("LATEST_VERSION_COPY").simulationRunId("M9-owned").build();
        event.setTenantId(92820L);
        if (scenario.equals("wrongSourceType")) event.setFeedbackSourceType("UNRELATED_SOURCE");
        when(dependency(MesProProcessPoolEventMapper.class).selectByIdForUpdate(501L)).thenReturn(event);
        var feedback = MesProFeedbackDO.builder().id(901L)
                .workOrderId(scenario.equals("foreignWorkOrder") ? 202L : 201L).build();
        when(dependency(MesProFeedbackMapper.class).selectListByIdsForUpdate(List.of(901L)))
                .thenReturn(scenario.equals("missingFeedback") ? List.of() : List.of(feedback));
        rejectWrites = true;
        assertThrows(ServiceException.class, () -> service.cleanupLatestSimulationActiveOrder(3001L, 101L));
    }
}
