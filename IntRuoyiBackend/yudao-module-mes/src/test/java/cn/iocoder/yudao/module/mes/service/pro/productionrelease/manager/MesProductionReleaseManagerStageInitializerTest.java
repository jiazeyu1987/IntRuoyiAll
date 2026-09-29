package cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerException;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowBlockerType;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowStatus;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReleaseServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationCommand;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseManagerStageInitializationResult;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportNodeEvidence;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.MesProductionReleaseReportSnapshots;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRequiredCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCandidates;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProductionReleaseManagerStageInitializerTest {

    @Mock private MesProcessPoolActiveOrderReleaseApplicationMapper applicationMapper;
    @Mock private MesProEdhrBatchExecutionMapper batchExecutionMapper;
    @Mock private MesProEdhrReleaseTransactionMapper releaseTransactionMapper;
    @Mock private MesProEdhrWorkTaskMapper workTaskMapper;
    @Mock private MesProductionReleaseRequiredCandidateResolver candidateResolver;
    @Mock private MesProductionReleaseBusinessReadinessService businessReadinessService;

    private MesProductionReleaseManagerStageInitializerImpl initializer;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        initializer = new MesProductionReleaseManagerStageInitializerImpl(
                applicationMapper, batchExecutionMapper, releaseTransactionMapper, workTaskMapper,
                candidateResolver, businessReadinessService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createsPendingApprovalTransactionAndFrozenManagementRepresentativeTask() {
        MesProcessPoolActiveOrderReleaseApplicationDO application = application();
        List<MesProductionReleaseReportNodeEvidence> evidences = evidences();
        String reportSnapshotHash = MesProductionReleaseReportSnapshots.hash(application, evidences);
        when(applicationMapper.selectById(701L)).thenReturn(application);
        when(batchExecutionMapper.selectById(901L)).thenReturn(batch());
        when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(
                        77L, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(8101L, 8102L), "manager-candidate-hash"));
        when(businessReadinessService.resolveBusinessReadinessChecks(any()))
                .thenReturn(passReadiness());
        AtomicLong ids = new AtomicLong(1000L);
        when(releaseTransactionMapper.insert(any(MesProEdhrReleaseTransactionDO.class))).thenAnswer(invocation -> {
            MesProEdhrReleaseTransactionDO transaction = invocation.getArgument(0);
            transaction.setId(ids.incrementAndGet());
            return 1;
        });
        when(workTaskMapper.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(invocation -> {
            MesProEdhrWorkTaskDO task = invocation.getArgument(0);
            task.setId(ids.incrementAndGet());
            return 1;
        });

        MesProductionReleaseManagerStageInitializationResult result = initializer.initializeManagerReleaseStage(
                new MesProductionReleaseManagerStageInitializationCommand()
                        .setApplicationId(701L)
                        .setBatchExecutionId(901L)
                        .setReportSnapshotHash(reportSnapshotHash)
                        .setReportEvidences(evidences)
                        .setExpectedApplicationVersion(4));

        assertNotNull(result.getReleaseTransactionId());
        assertNotNull(result.getManagerReleaseWorkTaskId());
        assertEquals("manager-candidate-hash", result.getManagerCandidateSnapshotHash());
        ArgumentCaptor<MesProEdhrReleaseTransactionDO> transactionCaptor =
                ArgumentCaptor.forClass(MesProEdhrReleaseTransactionDO.class);
        verify(releaseTransactionMapper).insert(transactionCaptor.capture());
        assertEquals(MesProEdhrReleaseServiceImpl.STATUS_PENDING_APPROVAL,
                transactionCaptor.getValue().getReleaseStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS, transactionCaptor.getValue().getDhrStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
                transactionCaptor.getValue().getDeviationStatus());
        assertEquals(MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
                transactionCaptor.getValue().getReworkStatus());
        assertEquals(6, transactionCaptor.getValue().getRequiredCheckCount());
        assertEquals(0, transactionCaptor.getValue().getFailedCheckCount());
        assertEquals(0, transactionCaptor.getValue().getBlockingCheckCount());
        org.junit.jupiter.api.Assertions.assertTrue(
                transactionCaptor.getValue().getPrecheckSnapshotJson().contains(reportSnapshotHash));
        org.junit.jupiter.api.Assertions.assertTrue(
                transactionCaptor.getValue().getPrecheckSnapshotJson().contains("business-readiness-hash"));
        ArgumentCaptor<MesProEdhrWorkTaskDO> taskCaptor = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(workTaskMapper).insert(taskCaptor.capture());
        MesProEdhrWorkTaskDO task = taskCaptor.getValue();
        assertEquals("RELEASE_APPROVE", task.getTaskType());
        assertEquals("RELEASE_TRANSACTION", task.getBusinessScopeType());
        assertEquals("ROLE_GROUP", task.getCandidateSourceType());
        assertEquals(77L, task.getCandidateSourceId());
        assertEquals("8101,8102", task.getCandidateUserSnapshot());
        assertEquals("manager-candidate-hash", task.getResponsibilitySourceVersion());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"PRECHECK_REQUIRED", "PRECHECK_FAILED", "PRECHECK_PASSED"})
    void standalonePrecheckDoesNotBlockFormalManagerStage(String precheckStatus) {
        MesProcessPoolActiveOrderReleaseApplicationDO application = application();
        List<MesProductionReleaseReportNodeEvidence> reports = evidences();
        String snapshotHash = MesProductionReleaseReportSnapshots.hash(application, reports);
        when(applicationMapper.selectById(701L)).thenReturn(application);
        when(releaseTransactionMapper.selectCurrentByBatchExecutionId(901L)).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(990L).setBatchExecutionId(901L)
                        .setReleaseCode("STANDALONE-PRECHECK").setReleaseStatus(precheckStatus)
                        .setVersion(1).setPrecheckSnapshotJson("{\"origin\":\"standalone-precheck\"}"));
        org.mockito.Mockito.lenient().when(releaseTransactionMapper.selectByIdForUpdate(990L)).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(990L).setBatchExecutionId(901L)
                        .setReleaseCode("STANDALONE-PRECHECK").setReleaseStatus(precheckStatus)
                        .setVersion(1).setPrecheckSnapshotJson("{\"origin\":\"standalone-precheck\"}"));
        org.mockito.Mockito.lenient().when(releaseTransactionMapper.updateById(any(MesProEdhrReleaseTransactionDO.class)))
                .thenReturn(1);
        org.mockito.Mockito.lenient().when(batchExecutionMapper.selectById(901L)).thenReturn(batch());
        org.mockito.Mockito.lenient().when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(77L,
                        MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(8101L, 8102L), "manager-candidate-hash"));
        org.mockito.Mockito.lenient().when(businessReadinessService.resolveBusinessReadinessChecks(any()))
                .thenReturn(passReadiness());
        org.mockito.Mockito.lenient().when(releaseTransactionMapper.insert(any(MesProEdhrReleaseTransactionDO.class)))
                .thenAnswer(invocation -> {
                    ((MesProEdhrReleaseTransactionDO) invocation.getArgument(0)).setId(1001L);
                    return 1;
                });
        org.mockito.Mockito.lenient().when(workTaskMapper.insert(any(MesProEdhrWorkTaskDO.class)))
                .thenAnswer(invocation -> {
                    ((MesProEdhrWorkTaskDO) invocation.getArgument(0)).setId(1002L);
                    return 1;
                });

        MesProductionReleaseManagerStageInitializationResult result = initializer.initializeManagerReleaseStage(
                new MesProductionReleaseManagerStageInitializationCommand().setApplicationId(701L)
                        .setBatchExecutionId(901L).setReportSnapshotHash(snapshotHash)
                        .setReportEvidences(reports).setExpectedApplicationVersion(4));

        assertEquals(990L, result.getReleaseTransactionId());
        ArgumentCaptor<MesProEdhrReleaseTransactionDO> upgraded = ArgumentCaptor.forClass(MesProEdhrReleaseTransactionDO.class);
        verify(releaseTransactionMapper).updateById(upgraded.capture());
        verify(releaseTransactionMapper, never()).insert(any(MesProEdhrReleaseTransactionDO.class));
        assertEquals(2, upgraded.getValue().getVersion());
        assertEquals("STANDALONE-PRECHECK", upgraded.getValue().getReleaseCode());
        assertEquals("PENDING_APPROVAL", upgraded.getValue().getReleaseStatus());
        assertEquals("{\"origin\":\"standalone-precheck\"}", com.alibaba.fastjson.JSON.parseObject(
                upgraded.getValue().getPrecheckSnapshotJson()).getJSONObject("standalonePrecheck")
                .getString("snapshotJson"));
        ArgumentCaptor<MesProEdhrWorkTaskDO> task = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(workTaskMapper).insert(task.capture());
        assertEquals(result.getReleaseTransactionId(), task.getValue().getBusinessScopeId());
        assertEquals("8101,8102", task.getValue().getCandidateUserSnapshot());
    }

    @Test
    void initializerRejectsTransactionThatBecamePendingAfterDiscovery() {
        var application = application();
        var reports = evidences();
        when(applicationMapper.selectById(701L)).thenReturn(application);
        when(releaseTransactionMapper.selectCurrentByBatchExecutionId(901L)).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(990L).setReleaseStatus("PRECHECK_PASSED"));
        org.mockito.Mockito.lenient().when(releaseTransactionMapper.selectByIdForUpdate(990L)).thenReturn(
                new MesProEdhrReleaseTransactionDO().setId(990L).setBatchExecutionId(901L)
                        .setReleaseStatus("PENDING_APPROVAL").setVersion(2));
        assertThrows(MesReleaseFlowBlockerException.class, () -> initializer.initializeManagerReleaseStage(
                new MesProductionReleaseManagerStageInitializationCommand().setApplicationId(701L)
                        .setBatchExecutionId(901L).setExpectedApplicationVersion(4).setReportEvidences(reports)
                        .setReportSnapshotHash(MesProductionReleaseReportSnapshots.hash(application, reports))));
        verify(releaseTransactionMapper).selectByIdForUpdate(990L);
        verify(releaseTransactionMapper, never()).updateById(any(MesProEdhrReleaseTransactionDO.class));
        verify(workTaskMapper, never()).insert(any(MesProEdhrWorkTaskDO.class));
    }

    @Test
    void createsPendingApprovalTransactionFromActiveOrderFormalFactsAfterPqcRelease() {
        LocalDateTime preciseDecidedAt = LocalDateTime.of(2026, 9, 20, 10, 0, 0, 123456789);
        MesProcessPoolActiveOrderReleaseApplicationDO application = new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(701L)
                .setActiveOrderId(801L)
                .setWorkOrderId(301L)
                .setWorkOrderCode("WO-001")
                .setBatchCode("BATCH-001")
                .setBatchExecutionId(901L)
                .setPqcReleaseWorkTaskId(951L)
                .setPqcDecision("PASS")
                .setPqcDecidedBy(7002L)
                // The database datetime column truncates sub-millisecond precision after PQC approval.
                .setPqcDecidedAt(preciseDecidedAt.withNano(123000000))
                .setSourceSnapshotHash("active-order-source-hash")
                .setApplicationStatus(MesReleaseFlowStatus.MANAGER_RELEASE_PENDING)
                .setVersion(5)
                .setAppliedBy(7001L);
        String formalFactsHash = MesProductionReleaseFormalFactSnapshots.activeOrderFactsSnapshotHash(
                application, "APPROVE", 7002L, preciseDecidedAt);
        application.setReportSnapshotHash(formalFactsHash);
        when(applicationMapper.selectById(701L)).thenReturn(application);
        when(batchExecutionMapper.selectById(901L)).thenReturn(batch());
        when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(
                        77L, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(8101L, 8102L), "manager-candidate-hash"));
        when(businessReadinessService.resolveActiveOrderFormalFactsReadiness(any(), any()))
                .thenReturn(passReadiness());
        AtomicLong ids = new AtomicLong(1000L);
        when(releaseTransactionMapper.insert(any(MesProEdhrReleaseTransactionDO.class))).thenAnswer(invocation -> {
            MesProEdhrReleaseTransactionDO transaction = invocation.getArgument(0);
            transaction.setId(ids.incrementAndGet());
            return 1;
        });
        when(workTaskMapper.insert(any(MesProEdhrWorkTaskDO.class))).thenAnswer(invocation -> {
            MesProEdhrWorkTaskDO task = invocation.getArgument(0);
            task.setId(ids.incrementAndGet());
            return 1;
        });

        MesProductionReleaseManagerStageInitializationResult result = initializer.initializeManagerReleaseStage(
                new MesProductionReleaseManagerStageInitializationCommand()
                        .setApplicationId(701L)
                        .setBatchExecutionId(901L)
                        .setReportSnapshotHash(formalFactsHash)
                        .setExpectedApplicationVersion(5)
                        .setActiveOrderFormalFacts(true));

        assertNotNull(result.getReleaseTransactionId());
        assertNotNull(result.getManagerReleaseWorkTaskId());
        verify(releaseTransactionMapper).insert(any(MesProEdhrReleaseTransactionDO.class));
        verify(workTaskMapper).insert(any(MesProEdhrWorkTaskDO.class));
    }

    @Test
    void blocksManagerStageWhenBusinessReadinessHasUnverifiedFormalCheck() {
        MesProcessPoolActiveOrderReleaseApplicationDO application = application();
        List<MesProductionReleaseReportNodeEvidence> evidences = evidences();
        String reportSnapshotHash = MesProductionReleaseReportSnapshots.hash(application, evidences);
        when(applicationMapper.selectById(701L)).thenReturn(application);
        when(batchExecutionMapper.selectById(901L)).thenReturn(batch());
        when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(
                        77L, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(8101L, 8102L), "manager-candidate-hash"));
        when(businessReadinessService.resolveBusinessReadinessChecks(any()))
                .thenReturn(blockingReadiness());

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> initializer.initializeManagerReleaseStage(
                        new MesProductionReleaseManagerStageInitializationCommand()
                                .setApplicationId(701L)
                                .setBatchExecutionId(901L)
                                .setReportSnapshotHash(reportSnapshotHash)
                                .setReportEvidences(evidences)
                                .setExpectedApplicationVersion(4)));

        assertEquals(MesReleaseFlowBlockerType.RELEASE_TRANSACTION_NOT_PROCESSABLE,
                failure.getFailure().getBlockers().get(0).getBlockerType());
        verify(releaseTransactionMapper, never()).insert((MesProEdhrReleaseTransactionDO)
                any(MesProEdhrReleaseTransactionDO.class));
        verify(workTaskMapper, never()).insert((MesProEdhrWorkTaskDO)
                any(MesProEdhrWorkTaskDO.class));
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

    private MesProductionReleaseBusinessReadiness blockingReadiness() {
        return new MesProductionReleaseBusinessReadiness(
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS,
                MesProEdhrReleaseServiceImpl.STATUS_PRECHECK_REQUIRED,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_NOT_APPLICABLE,
                MesProEdhrReleaseServiceImpl.CHECK_RESULT_PASS,
                6, 1, 1, "{\"items\":[]}", "business-readiness-blocked-hash", List.of());
    }

    private MesProcessPoolActiveOrderReleaseApplicationDO application() {
        return new MesProcessPoolActiveOrderReleaseApplicationDO()
                .setId(701L)
                .setBatchExecutionId(901L)
                .setApplicationStatus(MesReleaseFlowStatus.REPORT_UPLOAD_PENDING)
                .setVersion(4)
                .setAppliedBy(7001L);
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

    private List<MesProductionReleaseReportNodeEvidence> evidences() {
        return List.of(
                evidence(911L, "FINISHED_PRODUCT_INSPECTION_RECORD", null, 1011L, '1'),
                evidence(912L, "FINISHED_PRODUCT_INSPECTION_REPORT", null, 1012L, '2'),
                evidence(913L, "INCOMING_INSPECTION_REPORT", null, 1013L, '3'),
                evidence(914L, "STERILIZATION_REPORT", "STER-001", 1014L, '4'));
    }

    private MesProductionReleaseReportNodeEvidence evidence(
            Long batchTaskId, String nodeType, String sterilizationBatchNo, Long attachmentId, char hashDigit) {
        return new MesProductionReleaseReportNodeEvidence()
                .setBatchExecutionId(901L)
                .setBatchTaskId(batchTaskId)
                .setNodeType(nodeType)
                .setSterilizationBatchNo(sterilizationBatchNo)
                .setActiveAttachmentVersion(1)
                .setAttachmentIds(List.of(attachmentId))
                .setAttachmentHashes(List.of(String.valueOf(hashDigit).repeat(64)));
    }
}
