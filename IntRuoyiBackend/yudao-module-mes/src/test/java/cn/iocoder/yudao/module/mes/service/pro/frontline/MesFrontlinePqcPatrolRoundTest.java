package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesFrontlinePqcProcessRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcInspectionItemPlan;
import cn.iocoder.yudao.module.mes.service.pro.processpool.pqc.MesPqcQaSources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_QA_SOURCE_SNAPSHOT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PQC_QA_SOURCE_SNAPSHOT_REQUIRED;

class MesFrontlinePqcPatrolRoundTest {
    private final Map<Class<?>, Object> dependencies = new HashMap<>();
    private MesFrontlinePqcContextServiceImpl service;
    private MesProcessPoolActiveOrderDO order;
    private final List<MesPqcInspectionTaskDO> tasks = new ArrayList<>();
    @SuppressWarnings("unchecked")
    private <T> T dependency(Class<T> type) { return (T) dependencies.get(type); }

    @BeforeEach
    void setup() throws Exception {
        var constructor = MesFrontlinePqcContextServiceImpl.class.getConstructors()[0];
        Object[] args = Arrays.stream(constructor.getParameterTypes()).map(type -> {
            Object dependency = mock(type); dependencies.put(type, dependency); return dependency;
        }).toArray();
        service = spy((MesFrontlinePqcContextServiceImpl) constructor.newInstance(args));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "roundReleaseStateService", mock(cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService.class));
        doReturn(mock(MesFrontlineEmployeeCandidate.class)).when(service).requirePqcEmployee(10L, 10L);
        order = MesProcessPoolActiveOrderDO.builder().id(1L).workOrderId(2L).routeId(3L).routeVersionId(4L)
                .qaRegulationVersionId(100L).pqcQaSourceVersionIdsJson("[100,200]")
                .erpFixedQuantitySnapshot(new BigDecimal("101")).activeStatus("ACTIVE").businessStatus("ACTIVE").build();
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectByIdForUpdate(1L)).thenReturn(order);
        when(dependency(MesProWorkOrderMapper.class).selectById(2L)).thenReturn(MesProWorkOrderDO.builder().id(2L).productId(5L)
                .status(cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum.CONFIRMED.getStatus()).build());
        when(dependency(MesPqcInspectionTaskMapper.class).selectListByActiveOrderIdForUpdate(1L)).thenAnswer(call -> tasks);
        when(dependency(MesQaInspectionRegulationVersionMapper.class).selectById(anyLong()))
                .thenReturn(MesQaInspectionRegulationVersionDO.builder().versionNo("V1").build());
        doReturn(List.of(process(100L, 101L), process(200L, 201L))).when(service).buildProcesses(eq(order), anyList());
        when(dependency(MesPqcInspectionTaskMapper.class).insert(any(MesPqcInspectionTaskDO.class))).thenAnswer(call -> {
            MesPqcInspectionTaskDO task = call.getArgument(0); task.setId(1000L + tasks.size()); tasks.add(task); return 1;
        });
    }

    @Test
    void createsExactlyRequestedRoundsAcrossAllProcessesAndRejectsDuplicateRequest() {
        assertEquals(1, service.createPatrolRound(10L, 1L, 0));
        assertEquals(2, tasks.size());
        assertEquals(2, service.createPatrolRound(10L, 1L, 1));
        assertEquals(3, service.createPatrolRound(10L, 1L, 2));
        assertEquals(6, tasks.size());
        assertThrows(ServiceException.class, () -> service.createPatrolRound(10L, 1L, 2));
        assertEquals(6, tasks.size());
        assertEquals(Set.of(1,2,3), tasks.stream().map(MesPqcInspectionTaskDO::getRoundNo).collect(java.util.stream.Collectors.toSet()));
        for (var task : tasks) {
            assertEquals("PATROL", task.getInspectionRuleKey()); assertEquals("PATROL", task.getShiftCode());
            assertEquals("PENDING", task.getTaskStatus());
            assertEquals(Map.of("ratio", 8, "fixed", 5), MesPqcInspectionItemPlan.require(task));
            assertEquals("D", MesPqcInspectionItemPlan.requireSampling(task).get("ratio").sampleCode());
            assertEquals("C", MesPqcInspectionItemPlan.requireSampling(task).get("fixed").sampleCode());
            assertNull(task.getRouteProcessId()); assertNull(task.getProcessId());
        }
        var inOrder = inOrder(dependency(MesProcessPoolActiveOrderMapper.class), dependency(MesPqcInspectionTaskMapper.class));
        inOrder.verify(dependency(MesProcessPoolActiveOrderMapper.class)).selectByIdForUpdate(1L);
        inOrder.verify(dependency(MesPqcInspectionTaskMapper.class)).selectListByActiveOrderIdForUpdate(1L);
    }

    @Test
    void missingMatrixLevelCannotUseFirstInspectionQuantity() {
        var process = process(100L, 101L);
        process.getInspectionItems().get(0).setInspectionLevel(null);
        process.getInspectionItems().get(0).setFirstInspectionQuantity(12);
        doReturn(List.of(process)).when(service).buildProcesses(eq(order), anyList());
        var error = assertThrows(ServiceException.class, () -> service.createPatrolRound(10L, 1L, 0));
        assertTrue(error.getMessage().contains("检验水平"));
        assertTrue(tasks.isEmpty());
    }

    @Test
    void acceptsRoundsBeyondTwoAndKeepsNumbersAcrossBusinessDates() {
        tasks.add(MesPqcInspectionTaskDO.builder().inspectionType("PATROL").roundNo(11).build());
        assertEquals(12, service.createPatrolRound(10L, 1L, 11));
        assertEquals(12, tasks.get(1).getRoundNo());
    }

    @Test
    void rejectsReleasedOrdersAndAbsentPatrolDefinitions() {
        order.setReleaseDecisionId(99L);
        assertThrows(ServiceException.class, () -> service.createPatrolRound(10L, 1L, 0));
        assertTrue(tasks.isEmpty());
        order.setReleaseDecisionId(null);
        doReturn(List.of()).when(service).buildProcesses(eq(order), anyList());
        assertThrows(ServiceException.class, () -> service.createPatrolRound(10L, 1L, 0));
        assertTrue(tasks.isEmpty());
    }

    @Test
    void requiresFrozenSourcesBeforePatrolCreationWithoutDerivingThemFromExistingTasks() {
        assertEquals(List.of(100L,200L), MesPqcQaSources.requireVersionIds(order));
        order.setPqcQaSourceVersionIdsJson(null);
        ServiceException missing = assertThrows(ServiceException.class, () -> MesPqcQaSources.requireVersionIds(order));
        assertEquals(PRO_PQC_QA_SOURCE_SNAPSHOT_REQUIRED.getCode(), missing.getCode());
        assertTrue(missing.getMessage().contains("新建的活跃订单"));
        assertNull(order.getPqcQaSourceVersionIdsJson());
        order.setPqcQaSourceVersionIdsJson("[200]");
        ServiceException invalid = assertThrows(ServiceException.class, () -> MesPqcQaSources.requireVersionIds(order));
        assertEquals(PRO_PQC_QA_SOURCE_SNAPSHOT_INVALID.getCode(), invalid.getCode());
        assertTrue(tasks.isEmpty());
    }

    @Test
    void releaseCoverageIncludesEveryCreatedRoundAndEveryFrozenSource() {
        service.createPatrolRound(10L, 1L, 0);
        service.createPatrolRound(10L, 1L, 1);
        service.createPatrolRound(10L, 1L, 2);
        var completeness = new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesOrderReleaseCompletenessServiceImpl();
        var processMapper = mock(cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper.class);
        var itemMapper = mock(cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper.class);
        var versions = mock(MesQaInspectionRegulationVersionMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(completeness, "qaProcessMapper", processMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(completeness, "qaItemMapper", itemMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(completeness, "regulationVersionMapper", versions);
        for (long version : List.of(100L,200L)) {
            when(versions.selectById(version)).thenReturn(MesQaInspectionRegulationVersionDO.builder()
                    .finalInspectionApplicable(false).finalInspectionNotApplicableReason("不适用")
                    .inspectionTypeRulesJson("[{\"key\":\"FIRST\",\"inspectionType\":\"FIRST\",\"required\":false},"
                        + "{\"key\":\"PATROL\",\"inspectionType\":\"PATROL\",\"required\":true},"
                        + "{\"key\":\"FINAL\",\"inspectionType\":\"FINAL\",\"required\":false}]").build());
            when(processMapper.selectListByVersionIds(List.of(version))).thenReturn(List.of(
                    cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationProcessDO.builder().id(version+1).build()));
            when(itemMapper.selectListByVersionId(version)).thenReturn(List.of(
                    cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO.builder().qaProcessId(version+1).itemCode("ratio").inspectionType("PATROL").build(),
                    cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO.builder().qaProcessId(version+1).itemCode("fixed").inspectionType("PATROL").build()));
        }
        List<String> complete = org.springframework.test.util.ReflectionTestUtils.invokeMethod(completeness,
                "missingExpectedPqcTaskIdentities", order, tasks);
        assertEquals(List.of(), complete);
        tasks.remove(tasks.size()-1);
        List<String> missing = org.springframework.test.util.ReflectionTestUtils.invokeMethod(completeness,
                "missingExpectedPqcTaskIdentities", order, tasks);
        assertTrue(missing.stream().anyMatch(reason -> reason.contains("巡检轮次工序覆盖不完整")));
        tasks.clear();
        List<String> absent = org.springframework.test.util.ReflectionTestUtils.invokeMethod(completeness,
                "missingExpectedPqcTaskIdentities", order, tasks);
        assertEquals(2, absent.size());
    }

    @Test
    void patrolEnabledOrdersRemainSelectableWithoutPendingTasksUntilReleased() {
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectActiveList()).thenReturn(List.of(order));
        when(dependency(MesQaInspectionRegulationVersionMapper.class).selectById(100L)).thenReturn(
                MesQaInspectionRegulationVersionDO.builder().finalInspectionApplicable(false)
                        .finalInspectionNotApplicableReason("不适用")
                        .inspectionTypeRulesJson("[{\"key\":\"FIRST\",\"inspectionType\":\"FIRST\",\"required\":false},"
                            + "{\"key\":\"PATROL\",\"inspectionType\":\"PATROL\",\"required\":true},"
                            + "{\"key\":\"FINAL\",\"inspectionType\":\"FINAL\",\"required\":false}]").build());
        when(dependency(cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationItemMapper.class)
                .selectListByVersionId(100L)).thenReturn(List.of(
                    cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationItemDO.builder()
                            .inspectionType("PATROL").build()));
        stubOrderListSummaries();
        assertEquals(List.of(1L), service.listActiveOrders().stream()
                .map(MesFrontlineActiveOrderCandidate::activeOrderId).toList());
        order.setReleaseDecisionId(99L);
        assertTrue(service.listActiveOrders().isEmpty());
        order.setReleaseDecisionId(null);
        order.setBusinessStatus("COMPLETED");
        assertTrue(service.listActiveOrders().isEmpty());
    }

    private void stubOrderListSummaries() {
        when(dependency(MesProWorkOrderMapper.class).selectListByIds(any())).thenReturn(List.of(
                MesProWorkOrderDO.builder().id(2L).productId(5L).quantity(BigDecimal.TEN).build()));
        when(dependency(cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper.class)
                .selectListByIdsIgnoreDeleted(any())).thenReturn(List.of(
                    cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO.builder().id(3L).build()));
        when(dependency(cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper.class)
                .selectById(4L)).thenReturn(
                    cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO.builder()
                        .id(4L).routeId(3L).active(true).lifecycleStatus("ACTIVE")
                        .routeSnapshotJson("{\"configSnapshots\":{\"products\":[{\"itemId\":5}]}}").build());
        when(dependency(cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService.class)
                .getItemMap(Set.of(5L))).thenReturn(Map.of(5L,
                    cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO.builder().id(5L).name("产品").build()));
    }

    @Test
    void missingSnapshotIsExplicitlyBlockedWithoutBlockingHealthyOrdersOrDerivingSources() {
        stubOrderListSummaries();
        var blocked = MesProcessPoolActiveOrderDO.builder().id(6L).workOrderId(2L).routeId(3L).routeVersionId(4L)
                .qaRegulationVersionId(100L).activeStatus("ACTIVE").businessStatus("ACTIVE").build();
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectActiveList()).thenReturn(List.of(blocked, order));
        for (Set<Long> pending : List.of(Set.<Long>of(), Set.of(6L))) {
            when(dependency(MesPqcInspectionTaskMapper.class).selectActiveOrderIdsByTaskStatus(any(), eq("PENDING")))
                    .thenReturn(pending);
            for (String source : Arrays.asList(null, "[200]")) {
                blocked.setPqcQaSourceVersionIdsJson(source);
                var result = service.listActiveOrders();
                assertEquals(2, result.size());
                var healthy = result.stream().filter(row -> row.activeOrderId().equals(1L)).findFirst().orElseThrow();
                var invalid = result.stream().filter(row -> row.activeOrderId().equals(6L)).findFirst().orElseThrow();
                assertFalse(healthy.readBlocked());
                assertNull(healthy.readBlockReason());
                assertTrue(invalid.readBlocked());
                assertEquals(source == null ? PRO_PQC_QA_SOURCE_SNAPSHOT_REQUIRED.getMsg()
                        : PRO_PQC_QA_SOURCE_SNAPSHOT_INVALID.getMsg(), invalid.readBlockReason());
                assertEquals(source, blocked.getPqcQaSourceVersionIdsJson());
            }
        }
        verify(dependency(MesPqcInspectionTaskMapper.class), never()).selectListByActiveOrderId(anyLong());
    }

    @Test
    void orderListDoesNotHideUnexpectedDataAccessFailures() {
        when(dependency(MesProcessPoolActiveOrderMapper.class).selectActiveList()).thenThrow(new IllegalStateException("database unavailable"));
        var failure = assertThrows(IllegalStateException.class, service::listActiveOrders);
        assertEquals("database unavailable", failure.getMessage());
    }

    @Test
    void rejectsCorruptPatrolRoundBeforeAllocatingNewTasks() {
        tasks.add(MesPqcInspectionTaskDO.builder().inspectionType("PATROL").roundNo(null).build());
        assertThrows(ServiceException.class, () -> service.createPatrolRound(10L, 1L, 0));
        assertEquals(1, tasks.size());
    }

    @Test
    void patrolProjectionKeepsCommonQaSourceIdentityEvenBeforeAnyTasksExist() {
        var source = cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO
                .builder().dccProjectCodeId(19L).regulationId(201L).publishedVersionId(200L)
                .finalInspectionApplicable(false).build();
        var qaProcess = cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationPublishedVersionRespVO.InspectionProcess
                .builder().qaProcessId(201L).processName("包装检验").build();
        MesFrontlinePqcProcessRespVO projection = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service,
                "toPqcProcessRespVO",
                cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO.builder().id(3L).build(),
                order, source, qaProcess, List.of(), List.of(), List.of(), Map.of(), List.of(), Map.of());
        assertEquals(19L, projection.getDccProjectCodeId());
        assertEquals(200L, projection.getRegulationVersionId());
        assertEquals(201L, projection.getQaProcessId());
        assertTrue(projection.getPqcTaskOptions().isEmpty());
    }

    @Test
    void cancelsOnlyEmptyRoundsAndNeverReusesTheirNumber() {
        service.createPatrolRound(10L, 1L, 0);
        var mapper = dependency(MesPqcInspectionTaskMapper.class);
        when(mapper.updateById(any(MesPqcInspectionTaskDO.class))).thenAnswer(call -> {
            MesPqcInspectionTaskDO update = call.getArgument(0);
            var task = tasks.stream().filter(t -> t.getId().equals(update.getId())).findFirst().orElseThrow();
            task.setTaskStatus(update.getTaskStatus()).setCancelReason(update.getCancelReason())
                .setCancelledBy(update.getCancelledBy()).setCancelledAt(update.getCancelledAt());
            return 1;
        });
        service.cancelPatrolRound(10L, 1L, 1);
        assertTrue(tasks.stream().allMatch(t -> "CANCELLED".equals(t.getTaskStatus())
                && "确认撤销第 1 轮巡检".equals(t.getCancelReason()) && Long.valueOf(10).equals(t.getCancelledBy()) && t.getCancelledAt() != null));
        assertEquals(2, service.createPatrolRound(10L, 1L, 1));
        tasks.get(2).setTaskStatus("SUBMITTED").setSubmittedEventId(91L);
        assertThrows(ServiceException.class, () -> service.cancelPatrolRound(10L, 1L, 2));
        assertEquals("PENDING", tasks.get(3).getTaskStatus());
        verify(mapper, times(2)).updateById(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void restoresOriginalTasksWithoutChangingNumbersPlansOrCancellationAudit() {
        service.createPatrolRound(10L, 1L, 0);
        service.createPatrolRound(10L, 1L, 1);
        var cancelledAt = java.time.LocalDateTime.of(2026, 11, 5, 9, 30);
        var createdAt = cancelledAt.minusMinutes(10);
        tasks.subList(0, 2).forEach(task -> {
            task.setTaskStatus("CANCELLED").setCancelledAt(cancelledAt).setCancelledBy(10L)
                    .setCancelReason("确认撤销第 1 轮巡检");
            task.setCreateTime(createdAt);
        });
        var originalIds = tasks.stream().map(MesPqcInspectionTaskDO::getId).toList();
        var plans = tasks.stream().map(MesPqcInspectionTaskDO::getInspectionItemPlanJson).toList();
        var mapper = dependency(MesPqcInspectionTaskMapper.class);
        when(mapper.updateById(any(MesPqcInspectionTaskDO.class))).thenAnswer(call -> {
            MesPqcInspectionTaskDO update = call.getArgument(0);
            assertNull(update.getCancelledAt());
            assertNull(update.getInspectionItemPlanJson());
            assertNull(update.getCreateTime());
            tasks.stream().filter(t -> t.getId().equals(update.getId())).findFirst().orElseThrow()
                    .setTaskStatus(update.getTaskStatus());
            return 1;
        });
        service.restorePatrolRound(10L, 1L, 1);
        assertEquals(originalIds, tasks.stream().map(MesPqcInspectionTaskDO::getId).toList());
        assertEquals(plans, tasks.stream().map(MesPqcInspectionTaskDO::getInspectionItemPlanJson).toList());
        assertTrue(tasks.stream().allMatch(task -> "PENDING".equals(task.getTaskStatus())));
        assertTrue(tasks.subList(0, 2).stream().allMatch(task -> createdAt.equals(task.getCreateTime())
                && cancelledAt.equals(task.getCancelledAt()) && Long.valueOf(10).equals(task.getCancelledBy())
                && "确认撤销第 1 轮巡检".equals(task.getCancelReason())));
        verify(mapper, times(2)).updateById(any(MesPqcInspectionTaskDO.class));
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        assertEquals(3, service.createPatrolRound(10L, 1L, 2));
    }

    @Test
    void refusesRestoreForMissingMixedOrSubmittedRoundsBeforeAnyWrites() {
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, null));
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 0));
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        service.createPatrolRound(10L, 1L, 0);
        tasks.get(0).setTaskStatus("CANCELLED");
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        tasks.get(1).setTaskStatus("CANCELLED").setSubmittedEventId(99L);
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        tasks.get(1).setSubmittedEventId(null).setSubmittedContentHash("submitted");
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        tasks.get(1).setSubmittedContentHash(null).setActualInspectionQuantity(1);
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        verify(dependency(MesPqcInspectionTaskMapper.class), never()).updateById(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void refusesRestoreWhenPiecesExistOrOrderSnapshotNoLongerContainsTheRound() {
        service.createPatrolRound(10L, 1L, 0);
        tasks.forEach(task -> task.setTaskStatus("CANCELLED"));
        var pieceMapper = dependency(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper.class);
        when(pieceMapper.selectListByTaskIdForUpdate(tasks.get(1).getId())).thenReturn(List.of(
                new cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO()));
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        when(pieceMapper.selectListByTaskIdForUpdate(tasks.get(1).getId())).thenReturn(List.of());
        doReturn(List.of(process(100L, 101L))).when(service).buildProcesses(eq(order), anyList());
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        assertTrue(tasks.stream().allMatch(task -> "CANCELLED".equals(task.getTaskStatus())));
        verify(dependency(MesPqcInspectionTaskMapper.class), never()).updateById(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void restoreRespectsReleaseAndNonconformanceLocks() {
        service.createPatrolRound(10L, 1L, 0);
        tasks.forEach(task -> task.setTaskStatus("CANCELLED"));
        order.setReleaseDecisionId(99L);
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        order.setReleaseDecisionId(null);
        var release = (cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService)
                org.springframework.test.util.ReflectionTestUtils.getField(service, "roundReleaseStateService");
        when(release.isReleaseApplicationLockedForUpdate(1L)).thenReturn(true);
        assertThrows(ServiceException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        when(release.isReleaseApplicationLockedForUpdate(1L)).thenReturn(false);
        doThrow(new IllegalStateException("订单冻结")).when(dependency(
                cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService.class))
                .ensurePqcSubmissionNotFrozen(1L, 2L, "恢复巡检轮次");
        assertThrows(IllegalStateException.class, () -> service.restorePatrolRound(10L, 1L, 1));
        verify(dependency(MesPqcInspectionTaskMapper.class), never()).updateById(any(MesPqcInspectionTaskDO.class));
    }

    @Test
    void finalCreationIncludesOnlyEnabledProcessesAndUsesTheirOwnQuantity() {
        var enabled = process(100L, 101L);
        enabled.setFinalInspectionApplicable(true);
        var finalRule = new MesFrontlinePqcProcessRespVO.QaInspectionTypeRule();
        finalRule.setKey("FINAL"); finalRule.setRequired(true);
        enabled.setInspectionTypeRules(List.of(finalRule));
        enabled.getInspectionItems().forEach(item -> {
            item.setApplicableInspectionTypes(List.of("PATROL", "FINAL")); item.setFinalInspectionQuantity(3);
        });
        doReturn(List.of(enabled, process(200L,201L))).when(service).buildProcesses(eq(order), anyList());
        service.createFinalInspection(10L, 1L);
        assertEquals(1, tasks.size());
        assertEquals(101L, tasks.get(0).getQaProcessId());
        assertEquals("FINAL", tasks.get(0).getInspectionType());
        assertEquals(Map.of("ratio",3,"fixed",3), MesPqcInspectionItemPlan.require(tasks.get(0)));
        assertThrows(ServiceException.class, () -> service.createFinalInspection(10L, 1L));
        assertEquals(1, tasks.size());
    }

    @Test
    void finalCreationFailsWhenNoProcessEnabled() {
        assertThrows(ServiceException.class, () -> service.createFinalInspection(10L, 1L));
        assertTrue(tasks.isEmpty());
    }

    private static MesFrontlinePqcProcessRespVO process(long versionId, long processId) {
        var rule = new MesFrontlinePqcProcessRespVO.QaInspectionTypeRule();
        rule.setKey("PATROL"); rule.setRequired(true);
        var ratio = new MesFrontlinePqcProcessRespVO.PqcInspectionItem();
        ratio.setItemCode("ratio"); ratio.setApplicableInspectionTypes(List.of("PATROL")); ratio.setInspectionLevel("I");
        var fixed = new MesFrontlinePqcProcessRespVO.PqcInspectionItem();
        fixed.setItemCode("fixed"); fixed.setApplicableInspectionTypes(List.of("PATROL")); fixed.setInspectionLevel("S3");
        var process = new MesFrontlinePqcProcessRespVO();
        process.setRegulationVersionId(versionId); process.setQaProcessId(processId); process.setRegulationId(versionId + 1);
        process.setDccProjectCodeId(9L); process.setInspectionTypeRules(List.of(rule)); process.setInspectionItems(List.of(ratio, fixed));
        return process;
    }
}
