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
import org.springframework.test.util.ReflectionTestUtils;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationService;

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
    @Mock private MesEdhrActiveOrderReleaseFactService activeOrderFacts;
    private MesProEdhrReleaseServiceImpl facts;

    private MesProductionReleaseManagerStageInitializerImpl initializer;

    private MesProductionReleaseManagerStageInitializationCommand command() {
        return new MesProductionReleaseManagerStageInitializationCommand()
            .setSourceMethod("MANAGED_REPORT_COMPLETE").setSourceWorkTaskId(99L).setSourceActorId(8101L)
            .setSourceExpectedApplicationVersion(3).setSourceIdempotencyKey("report-complete-test")
            .setSourcePayloadHash("a".repeat(64)).setReportEvidences(evidences());
    }
    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(1L);
        var actor=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(8101L).setTenantId(1L);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor,null,List.of()));
        initializer = new MesProductionReleaseManagerStageInitializerImpl(
                applicationMapper, batchExecutionMapper, releaseTransactionMapper, workTaskMapper,
                candidateResolver, businessReadinessService);
        ReflectionTestUtils.setField(initializer, "audit", org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class));
        ReflectionTestUtils.setField(initializer, "approvalAuthority", org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi.class));
        org.mockito.Mockito.lenient().when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(1L, 901L))
            .thenAnswer(invocation -> { var row = batch(); row.setTenantId(1L); return row; });
        ReflectionTestUtils.setField(initializer, "lifecycleGuard", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrBatchLifecycleGuard.class));
        ReflectionTestUtils.setField(initializer, "notificationService",
                org.mockito.Mockito.mock(MesReleaseTaskNotificationService.class));
        facts = org.mockito.Mockito.mock(MesProEdhrReleaseServiceImpl.class);
        ReflectionTestUtils.setField(initializer, "releaseFacts", facts);
        ReflectionTestUtils.setField(initializer, "activeOrderFacts", activeOrderFacts);
        org.mockito.Mockito.lenient().when(facts.produceManagerPrecheck(any(MesProEdhrReleaseTransactionDO.class), any(MesProEdhrBatchExecutionDO.class)))
            .thenAnswer(invocation -> {
                MesProEdhrReleaseTransactionDO tx = invocation.getArgument(0);
                tx.setRequiredCheckCount(10).setFailedCheckCount(0).setBlockingCheckCount(0);
                tx.setPrecheckSnapshotJson("{\"requiredCheckCount\":10}");
                return tx;
            });
        org.mockito.Mockito.lenient().when(facts.captureManagerPriorPrecheck(any(MesProEdhrReleaseTransactionDO.class))).thenReturn("{}");
        var material = new cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseMaterialGateReceipt()
            .setReceiptId("MATERIALS-901-1").setBatchExecutionId(901L).setVersion(1).setReceiptHash("b".repeat(64))
            .setManifestHash("c".repeat(64)).setSourceSnapshotHash("d".repeat(64));
        var proof = new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationEvidence(
            "MATERIAL_GATE_V1", 12L, material.getReceiptId(), "1", material.getReceiptHash(), 13L, "e".repeat(64));
        org.mockito.Mockito.lenient().when(facts.requireManagerReadyMaterialFact(any(MesProEdhrBatchExecutionDO.class)))
            .thenReturn(new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrVerifiedMaterialGateFact(material, proof));
        var audit = org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class);
        org.mockito.Mockito.lenient().when(audit.append(any(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class)))
            .thenReturn(new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditAppendResult(21L, 1L, "f".repeat(64), false));
        ReflectionTestUtils.setField(initializer, "audit", audit);
        ReflectionTestUtils.setField(initializer, "handoffAssociations", org.mockito.Mockito.mock(MesEdhrManagedHandoffMaterialAssociationStore.class));
        org.mockito.Mockito.lenient().when(releaseTransactionMapper.updateById(any(MesProEdhrReleaseTransactionDO.class))).thenReturn(1);
        // Domain precheck and source proof are explicit ports; actual appendHandoff codec still executes.
        org.mockito.Mockito.lenient().when(workTaskMapper.updateById(any(MesProEdhrWorkTaskDO.class))).thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test void pendingVoidStopsManagerStageBeforeAnyApplicationOrTaskWrite() {
        var guard = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrBatchLifecycleGuard.class);
        ReflectionTestUtils.setField(initializer, "audit", org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService.class));
        ReflectionTestUtils.setField(initializer, "approvalAuthority", org.mockito.Mockito.mock(cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi.class));
        org.mockito.Mockito.lenient().when(batchExecutionMapper.selectByTenantIdAndIdForUpdate(1L, 901L))
            .thenAnswer(invocation -> { var row = batch(); row.setTenantId(1L); return row; });
        ReflectionTestUtils.setField(initializer, "lifecycleGuard", guard);
        org.mockito.Mockito.doThrow(new IllegalStateException("pending formal VOID")).when(guard).requireReleaseAllowed(901L);
        assertThrows(IllegalStateException.class, () -> initializer.initializeManagerReleaseStage(
                command().setApplicationId(701L)
                        .setBatchExecutionId(901L).setReportSnapshotHash("2".repeat(64))
                        .setReportEvidences(evidences()).setExpectedApplicationVersion(4)));
        org.mockito.Mockito.verify(batchExecutionMapper).selectByTenantIdAndIdForUpdate(1L, 901L);
        org.mockito.Mockito.verifyNoInteractions(applicationMapper, releaseTransactionMapper, workTaskMapper);
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
                        List.of(8101L, 8102L), "1".repeat(64)));
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
                command()
                        .setApplicationId(701L)
                        .setBatchExecutionId(901L)
                        .setReportSnapshotHash(reportSnapshotHash)
                        .setReportEvidences(evidences)
                        .setExpectedApplicationVersion(4));

        assertNotNull(result.getReleaseTransactionId());
        assertNotNull(result.getManagerReleaseWorkTaskId());
        assertEquals("1".repeat(64), result.getManagerCandidateSnapshotHash());
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
        assertEquals(10, transactionCaptor.getValue().getRequiredCheckCount());
        assertEquals(0, transactionCaptor.getValue().getFailedCheckCount());
        assertEquals(0, transactionCaptor.getValue().getBlockingCheckCount());
        var handoffs=(MesEdhrManagedHandoffMaterialAssociationStore)ReflectionTestUtils.getField(initializer,"handoffAssociations");
        var association=ArgumentCaptor.forClass(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationDO.class);
        verify(handoffs).insert(association.capture(),any());
        var payload=MesEdhrManagedHandoffMaterialAssociationStore.parse(association.getValue().getAssociationPayloadJson());
        assertEquals(reportSnapshotHash,payload.path("reportSnapshotHash").asText());
        assertEquals("MANAGED_HANDOFF_MATERIAL_ASSOCIATION_V1",payload.path("associationProtocol").asText());
        assertEquals("MATERIALS-901-1",payload.path("materialReceiptId").asText());
        var audit=(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService)ReflectionTestUtils.getField(initializer,"audit");
        var auditCommand=ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(audit).append(auditCommand.capture());
        var applicationLink=auditCommand.getValue().getLinks().stream().filter(link -> "RELEASE_APPLICATION".equals(link.objectType())).findFirst().orElseThrow();
        assertEquals(reportSnapshotHash,applicationLink.objectHash());
        ArgumentCaptor<MesProEdhrWorkTaskDO> taskCaptor = ArgumentCaptor.forClass(MesProEdhrWorkTaskDO.class);
        verify(workTaskMapper).insert(taskCaptor.capture());
        MesProEdhrWorkTaskDO task = taskCaptor.getValue();
        assertEquals("RELEASE_APPROVE", task.getTaskType());
        assertEquals("RELEASE_TRANSACTION", task.getBusinessScopeType());
        assertEquals("ROLE_GROUP", task.getCandidateSourceType());
        assertEquals(77L, task.getCandidateSourceId());
        assertEquals("8101,8102", task.getCandidateUserSnapshot());
        assertEquals("1".repeat(64), task.getResponsibilitySourceVersion());
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
                        List.of(8101L, 8102L), "1".repeat(64)));
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
                command().setApplicationId(701L)
                        .setBatchExecutionId(901L).setReportSnapshotHash(snapshotHash)
                        .setReportEvidences(reports).setExpectedApplicationVersion(4));

        assertEquals(990L, result.getReleaseTransactionId());
        ArgumentCaptor<MesProEdhrReleaseTransactionDO> upgraded = ArgumentCaptor.forClass(MesProEdhrReleaseTransactionDO.class);
        verify(releaseTransactionMapper,org.mockito.Mockito.times(2)).updateById(upgraded.capture());
        verify(releaseTransactionMapper, never()).insert(any(MesProEdhrReleaseTransactionDO.class));
        assertEquals(2, upgraded.getValue().getVersion());
        assertEquals("STANDALONE-PRECHECK", upgraded.getValue().getReleaseCode());
        assertEquals("PENDING_APPROVAL", upgraded.getValue().getReleaseStatus());
        verify(facts).captureManagerPriorPrecheck(any(MesProEdhrReleaseTransactionDO.class));
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
                command().setApplicationId(701L)
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
        var completionProof=new cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationEvidence(
                "COMPLETION_MAIN_V1",12L,"12","1","d".repeat(64),13L,"e".repeat(64));
        var activeFact=new MesEdhrVerifiedActiveOrderReleaseFact(701L,901L,formalFactsHash,"d".repeat(64),
                completionProof,14L,"f".repeat(64),15L);
        when(activeOrderFacts.require(701L,901L,formalFactsHash)).thenReturn(activeFact);
        when(facts.produceActiveOrderManagerPrecheck(any(),any(),any(),any())).thenAnswer(invocation -> {
            MesProEdhrReleaseTransactionDO tx=invocation.getArgument(0);
            return tx.setRequiredCheckCount(6).setFailedCheckCount(0).setBlockingCheckCount(0);
        });
        when(applicationMapper.selectById(701L)).thenReturn(application);
        when(batchExecutionMapper.selectById(901L)).thenReturn(batch());
        when(candidateResolver.resolveRequiredCandidates(1L,
                MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE))
                .thenReturn(new MesProductionReleaseRoleCandidates(
                        77L, MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE,
                        List.of(8101L, 8102L), "1".repeat(64)));
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
                command()
                        .setApplicationId(701L)
                        .setBatchExecutionId(901L)
                        .setReportSnapshotHash(formalFactsHash)
                        .setExpectedApplicationVersion(5)
                        .setActiveOrderFormalFacts(true).setSourceMethod("MANAGED_PQC_APPROVE").setReportEvidences(List.of()));

        assertNotNull(result.getReleaseTransactionId());
        assertNotNull(result.getManagerReleaseWorkTaskId());
        verify(releaseTransactionMapper).insert(any(MesProEdhrReleaseTransactionDO.class));
        verify(workTaskMapper).insert(any(MesProEdhrWorkTaskDO.class));
        verify(facts,never()).produceManagerPrecheck(any(),any());
        verify(facts,never()).requireManagerReadyMaterialFact(any());
        var handoffs=(MesEdhrManagedHandoffMaterialAssociationStore)ReflectionTestUtils.getField(initializer,"handoffAssociations");
        var association=ArgumentCaptor.forClass(cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationDO.class);
        verify(handoffs).insert(association.capture(),any());
        var payload=MesEdhrManagedHandoffMaterialAssociationStore.parse(association.getValue().getAssociationPayloadJson());
        assertEquals("MANAGED_ACTIVE_ORDER_ASSOCIATION_V1",payload.path("associationProtocol").asText());
        org.junit.jupiter.api.Assertions.assertFalse(payload.has("materialReceiptId"));
        assertEquals(14L,payload.path("pqcSignatureId").longValue());
        assertEquals(formalFactsHash,payload.path("reportSnapshotHash").asText());
        var audit=(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService)ReflectionTestUtils.getField(initializer,"audit");
        var auditCommand=ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(audit).append(auditCommand.capture());
        var links=auditCommand.getValue().getLinks();
        org.junit.jupiter.api.Assertions.assertTrue(links.stream().allMatch(link -> link.objectHash()==null || link.objectHash().matches("[0-9a-f]{64}")));
        var applicationLink=links.stream().filter(link -> "RELEASE_APPLICATION".equals(link.objectType())).findFirst().orElseThrow();
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(formalFactsHash),applicationLink.objectHash());
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
                        List.of(8101L, 8102L), "1".repeat(64)));
        when(businessReadinessService.resolveBusinessReadinessChecks(any()))
                .thenReturn(blockingReadiness());

        MesReleaseFlowBlockerException failure = assertThrows(MesReleaseFlowBlockerException.class,
                () -> initializer.initializeManagerReleaseStage(
                        command()
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
