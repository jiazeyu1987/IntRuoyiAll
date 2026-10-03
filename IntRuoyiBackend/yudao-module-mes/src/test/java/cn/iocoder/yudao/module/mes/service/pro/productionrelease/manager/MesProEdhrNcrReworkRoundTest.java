package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskStatus;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrNcrReworkRoundTest {

    @Mock
    private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock
    private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock
    private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock
    private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock
    private MesProductionReleaseRequiredCandidateResolver candidateResolver;
    @Mock
    private MesProductionReleaseBusinessReadinessService businessReadinessService;

    private MesProductionReleaseManagerStageInitializerImpl initializer;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        initializer = new MesProductionReleaseManagerStageInitializerImpl(
                applicationMapper, batchExecutionMapper, releaseTransactionMapper, workTaskMapper,
                candidateResolver, businessReadinessService);
        ReflectionTestUtils.setField(initializer, "notificationService",
                org.mockito.Mockito.mock(MesReleaseTaskNotificationService.class));
        org.mockito.Mockito.lenient().when(workTaskMapper.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createsNewTransactionAndTaskForEachReworkRoundWithoutMutatingHistory() {
        MesProEdhrBatchExecutionDO batch = batch();
        MesProcessPoolActiveOrderReleaseApplicationDO first = formalApplication(701L, 951L,
                "active-order-source-1", LocalDateTime.of(2026, 9, 20, 10, 0), 5);
        MesProcessPoolActiveOrderReleaseApplicationDO second = formalApplication(702L, 952L,
                "active-order-source-2", LocalDateTime.of(2026, 9, 21, 10, 0), 5);
        MesProcessPoolActiveOrderReleaseApplicationDO third = formalApplication(703L, 953L,
                "active-order-source-3", LocalDateTime.of(2026, 9, 22, 10, 0), 5);
        when(applicationMapper.selectById(701L)).thenReturn(first);
        when(applicationMapper.selectById(702L)).thenReturn(second);
        when(applicationMapper.selectById(703L)).thenReturn(third);
        when(batchExecutionMapper.selectById(901L)).thenReturn(batch);
        when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(77L,
                        MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(8101L, 8102L), "manager-candidate-hash"));
        when(businessReadinessService.resolveActiveOrderFormalFactsReadiness(any(), any()))
                .thenReturn(passReadiness());

        AtomicLong ids = new AtomicLong(1100L);
        List<MesProEdhrReleaseTransactionDO> transactions = new ArrayList<>();
        List<MesProEdhrWorkTaskDO> tasks = new ArrayList<>();
        List<MesProEdhrWorkTaskDO> urlBindings = new ArrayList<>();
        when(releaseTransactionMapper.insert(any(MesProEdhrReleaseTransactionDO.class))).thenAnswer(invocation -> {
            MesProEdhrReleaseTransactionDO transaction = invocation.getArgument(0);
            transaction.setId(ids.incrementAndGet());
            transactions.add(transaction);
            return 1;
        });
        when(workTaskMapper.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(invocation -> {
            MesProEdhrWorkTaskDO task = invocation.getArgument(0);
            task.setId(ids.incrementAndGet());
            tasks.add(task);
            return 1;
        });
        when(workTaskMapper.updateById(any(MesProEdhrWorkTaskDO.class))).thenAnswer(invocation -> {
            MesProEdhrWorkTaskDO binding = invocation.getArgument(0);
            // At each round only the row just inserted may receive its generated-ID URL binding.
            // A call targeting an earlier (now historical) row or carrying business fields fails here.
            assertEquals(tasks.get(tasks.size() - 1).getId(), binding.getId());
            Map<?, ?> values = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(binding), Map.class);
            assertEquals(Set.of("id", "actionUrl"), values.entrySet().stream()
                    .filter(entry -> entry.getValue() != null).map(entry -> entry.getKey().toString())
                    .collect(java.util.stream.Collectors.toSet()));
            assertNull(binding.getStatus()); assertNull(binding.getReason());
            assertNull(binding.getCandidateUserSnapshot()); assertNull(binding.getBusinessScopeId());
            urlBindings.add(binding);
            return 1;
        });
        when(releaseTransactionMapper.selectCurrentByBatchExecutionId(901L)).thenAnswer(invocation -> transactions.stream()
                .filter(transaction -> !MesProEdhrReleaseServiceImpl.STATUS_REJECTED
                        .equals(transaction.getReleaseStatus())
                        || !"NONCONFORMANCE_REWORK".equals(transaction.getRejectReason()))
                .max(java.util.Comparator.comparing(MesProEdhrReleaseTransactionDO::getId))
                .orElse(null));

        String firstHash = first.getReportSnapshotHash();
        String secondHash = second.getReportSnapshotHash();
        String thirdHash = third.getReportSnapshotHash();
        initializer.initializeManagerReleaseStage(command(701L, firstHash));
        MesProEdhrReleaseTransactionDO firstTransaction = transactions.get(0);
        MesProEdhrWorkTaskDO firstTask = tasks.get(0);
        firstTransaction.setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_REJECTED)
                .setRejectReason("NONCONFORMANCE_REWORK").setVersion(2);
        firstTask.setStatus(MesProEdhrWorkTaskStatus.CANCELED).setReason("NONCONFORMANCE_REWORK");

        initializer.initializeManagerReleaseStage(command(702L, secondHash));
        MesProEdhrReleaseTransactionDO secondTransaction = transactions.get(1);
        MesProEdhrWorkTaskDO secondTask = tasks.get(1);
        secondTransaction.setReleaseStatus(MesProEdhrReleaseServiceImpl.STATUS_REJECTED)
                .setRejectReason("NONCONFORMANCE_REWORK").setVersion(2);
        secondTask.setStatus(MesProEdhrWorkTaskStatus.CANCELED).setReason("NONCONFORMANCE_REWORK");

        initializer.initializeManagerReleaseStage(command(703L, thirdHash));

        assertEquals(3, transactions.size());
        assertEquals(3, tasks.size());
        assertNotEquals(firstTransaction.getId(), secondTransaction.getId());
        assertNotEquals(secondTransaction.getId(), transactions.get(2).getId());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_REJECTED, firstTransaction.getReleaseStatus());
        assertEquals("NONCONFORMANCE_REWORK", firstTransaction.getRejectReason());
        assertEquals(2, firstTransaction.getVersion());
        assertEquals(MesProEdhrWorkTaskStatus.CANCELED, firstTask.getStatus());
        assertEquals("NONCONFORMANCE_REWORK", firstTask.getReason());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_REJECTED, secondTransaction.getReleaseStatus());
        assertEquals("NONCONFORMANCE_REWORK", secondTransaction.getRejectReason());
        assertEquals(MesProEdhrWorkTaskStatus.CANCELED, secondTask.getStatus());
        MesProEdhrReleaseTransactionDO latestTransaction = transactions.get(2);
        MesProEdhrWorkTaskDO latestTask = tasks.get(2);
        assertEquals(3, new HashSet<>(transactions.stream()
                .map(MesProEdhrReleaseTransactionDO::getReleaseCode).toList()).size());
        assertEquals(3, new HashSet<>(tasks.stream()
                .map(MesProEdhrWorkTaskDO::getTaskCode).toList()).size());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL, latestTransaction.getReleaseStatus());
        assertEquals(1, latestTransaction.getVersion());
        assertEquals(MesProEdhrWorkTaskStatus.TODO, latestTask.getStatus());
        assertEquals(latestTransaction.getId(), latestTask.getBusinessScopeId());
        assertEquals(901L, latestTask.getBatchExecutionId());
        assertEquals("RELEASE_APPROVE", latestTask.getTaskType());
        assertEquals("RELEASE_TRANSACTION", latestTask.getBusinessScopeType());
        assertEquals(latestTransaction.getId(), latestTask.getBusinessScopeId());
        assertEquals(901L, latestTask.getBatchExecutionId());
        verify(releaseTransactionMapper, org.mockito.Mockito.never())
                .updateById(any(MesProEdhrReleaseTransactionDO.class));
        verify(workTaskMapper, org.mockito.Mockito.times(3))
                .updateById(any(MesProEdhrWorkTaskDO.class));
        assertEquals(3, urlBindings.size());
        for (int index = 0; index < urlBindings.size(); index++) {
            assertEquals(tasks.get(index).getId(), urlBindings.get(index).getId());
            assertEquals("/mes/pro/feedback/edhr-batch-execution?batchExecutionId=901&releaseTransactionId="
                    + transactions.get(index).getId() + "&workTaskId=" + tasks.get(index).getId()
                    + "&action=marketRelease", urlBindings.get(index).getActionUrl());
        }
        verify(releaseTransactionMapper, org.mockito.Mockito.times(3))
                .insert(any(MesProEdhrReleaseTransactionDO.class));
        verify(workTaskMapper, org.mockito.Mockito.times(3)).insert(any(MesProEdhrWorkTaskDO.class));
    }

    private MesProductionReleaseManagerStageInitializationCommand command(Long applicationId, String snapshotHash) {
        return new MesProductionReleaseManagerStageInitializationCommand()
                .setApplicationId(applicationId)
                .setBatchExecutionId(901L)
                .setReportSnapshotHash(snapshotHash)
                .setExpectedApplicationVersion(5)
                .setActiveOrderFormalFacts(true);
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO formalApplication(
            Long id, Long pqcTaskId, String sourceSnapshotHash, LocalDateTime pqcDecidedAt, Integer version) {
        MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(id)
                .setActiveOrderId(801L)
                .setWorkOrderId(301L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setBatchExecutionId(901L)
                .setPqcReleaseWorkTaskId(pqcTaskId)
                .setPqcDecision("APPROVE")
                .setPqcDecidedBy(7002L)
                .setPqcDecidedAt(pqcDecidedAt)
                .setSourceSnapshotHash(sourceSnapshotHash)
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                .setVersion(version)
                .setAppliedBy(7001L);
        application.setReportSnapshotHash(MesProductionReleaseFormalFactSnapshots.activeOrderFactsSnapshotHash(
                application, "APPROVE", 7002L, pqcDecidedAt));
        return application;
    }

    private MesProductionReleaseBusinessReadiness passReadiness() {
        return new MesProductionReleaseBusinessReadiness(
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS,
                6, 0, 0, "{\"items\":[]}", "business-readiness-hash", List.of());
    }

    private MesProEdhrBatchExecutionDO batch() {
        return new MesProEdhrBatchExecutionDO()
                .setId(901L)
                .setBatchExecutionCode("BE-901")
                .setWorkOrderId(301L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setProductId(401L)
                .setProductCode("P-001")
                .setProductName("Product")
                .setRouteId(501L)
                .setRouteCode("R-001")
                .setRouteName("Route");
    }
}
