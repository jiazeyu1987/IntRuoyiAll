package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.productionrelease.core.MesReleaseFlowAuditRecorder;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.report.*;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskSummary;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalTaskTimelineEntry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real domain/signature writers, canonicalizer, hash verifier, actor resolver and projections.
 * Only authentication and persistence/external domain ports are mocks; no final reason mock. */
class MesEdhrOpinionSignatureChainTest {
    private ElectronicSignatureRecordDO stored;
    private cn.iocoder.yudao.framework.security.core.LoginUser signedIn;
    private cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO createdAudit;
    private GxpAuditService actualCreationAudit;
    private cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper auditLedgerRows;
    private final ElectronicSignatureRecordMapper signatureRows = mock(ElectronicSignatureRecordMapper.class);
    private final MesProEdhrWorkTaskMapper tasks = mock(MesProEdhrWorkTaskMapper.class);
    private final MesProcessPoolActiveOrderReleaseApplicationMapper applications = mock(MesProcessPoolActiveOrderReleaseApplicationMapper.class);
    private final MesProEdhrReleaseTransactionMapper transactions = mock(MesProEdhrReleaseTransactionMapper.class);
    private final AdminUserMapper users = mock(AdminUserMapper.class);
    private final AdminUserApi auth = mock(AdminUserApi.class);
    private final GxpAuditService audit = mock(GxpAuditService.class);
    private final MesBatchRecordSignatureSubjectAdapter adapter = new MesBatchRecordSignatureSubjectAdapter();
    private final ElectronicSignatureServiceImpl kernel = new ElectronicSignatureServiceImpl();
    private final ElectronicSignatureQueryServiceImpl queries = new ElectronicSignatureQueryServiceImpl();
    private final MesProBatchRecordExecutionSignatureService writer = new MesProBatchRecordExecutionSignatureService();
    private cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService verifiedFacts;
    private MesManagerReleaseCompletedActorResolver actors;
    private MesEdhrApprovalOutcomeReader outcomes;
    private MesPqcProductionReleaseServiceImpl pqc;
    private MesProcessPoolActiveOrderReleaseApplicationDO application;
    private MesProEdhrWorkTaskDO task;
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 7, 10, 0);

    @BeforeAll
    static void initializeUserMapperMetadata() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), AdminUserDO.class.getName()), AdminUserDO.class);
    }

    @BeforeEach void init() {
        TenantContextHolder.setTenantId(1L);
        var user = new AdminUserDO().setId(188L).setUsername("reviewer").setNickname("真实意见操作者").setStatus(0);
        user.setTenantId(1L);
        var directory = mock(AdminUserService.class);
        when(directory.getUser(188L)).thenReturn(user);
        when(users.selectOne(any())).thenReturn(user);
        var authorization = mock(DccElectronicSignatureAuthorizationService.class);
        when(authorization.isElectronicSignatureEnabled(188L)).thenReturn(true);
        ReflectionTestUtils.setField(writer, "adminUserService", directory);
        ReflectionTestUtils.setField(writer, "authorizationService", authorization);
        ReflectionTestUtils.setField(writer, "adminUserApi", auth);
        ReflectionTestUtils.setField(writer, "electronicSignatureService", kernel);
        ReflectionTestUtils.setField(writer, "permissionService", mock(cn.iocoder.yudao.module.system.service.permission.PermissionService.class));
        ReflectionTestUtils.setField(kernel, "adminUserApi", auth);
        ReflectionTestUtils.setField(kernel, "signatureRecordMapper", signatureRows);
        configureActualCreationAudit();
        ReflectionTestUtils.setField(kernel, "subjectAdapters", List.of(adapter));
        ReflectionTestUtils.setField(queries, "signatureRecordMapper", signatureRows);
        when(signatureRows.insert(any(ElectronicSignatureRecordDO.class))).thenAnswer(call -> {
            stored = call.getArgument(0); stored.setId(88L); return 1;
        });
        when(signatureRows.selectById(88L)).thenAnswer(call -> stored);
        application = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(95L).setActiveOrderId(180L)
                .setWorkOrderId(93L).setWorkOrderCode("WO-93").setBatchCode("B-92").setBatchExecutionId(92L)
                .setPqcReleaseWorkTaskId(177L).setApplicationStatus("PQC_RELEASE_PENDING").setVersion(1)
                .setSourceSnapshotHash("formal-source");
        application.setTenantId(1L);
        task = new MesProEdhrWorkTaskDO().setId(177L).setTaskType("PQC_PRODUCTION_RELEASE")
                .setStatus("TODO").setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(95L)
                .setWorkOrderId(93L).setBatchExecutionId(92L).setCandidateUserSnapshot("188");
        when(tasks.selectById(177L)).thenReturn(task);
        when(tasks.selectOne(any())).thenReturn(task);
        when(applications.selectById(95L)).thenReturn(application);
        when(applications.selectByIdForUpdate(95L)).thenReturn(application);
        when(applications.selectOne(any())).thenAnswer(call -> application);
        when(applications.approveFromPending(eq(95L), eq(1), eq(92L), eq(188L), any(), any(), any()))
                .thenAnswer(call -> {
                    application.setPqcDecision("APPROVE").setPqcDecidedBy(188L).setPqcDecidedAt(call.getArgument(4))
                            .setDossierSummaryJson(call.getArgument(6)).setApplicationStatus("MANAGER_RELEASE_PENDING");
                    return 1;
                });
        when(tasks.completePqcApprovedTask(eq(177L), any(), any())).thenAnswer(call -> {
            task.setStatus("DONE").setCompletedAt(call.getArgument(2)).setReason("APPROVE"); return 1;
        });
        when(applications.handoffReportsToManager(eq(95L), eq(2), any(), eq(91L), eq(178L), eq("candidates"))).thenReturn(1);
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        when(orders.selectByIdForUpdate(180L)).thenReturn(new MesProcessPoolActiveOrderDO().setId(180L)
                .setWorkOrderId(93L).setUdiControlDocumentNo("UDI-93"));
        var workOrders = mock(MesProWorkOrderMapper.class);
        when(workOrders.selectByIdForUpdate(93L)).thenReturn(new MesProWorkOrderDO().setId(93L));
        var manager = mock(MesProductionReleaseManagerStageInitializer.class);
        when(manager.initializeManagerReleaseStage(any())).thenReturn(new MesProductionReleaseManagerStageInitializationResult()
                .setReleaseTransactionId(91L).setManagerReleaseWorkTaskId(178L).setManagerCandidateSnapshotHash("candidates"));
        pqc = new MesPqcProductionReleaseServiceImpl(applications, orders, workOrders, tasks, mock(MesPqcReleaseDossierPort.class),
                mock(MesProductionReleaseBatchExecutionPort.class), manager,
                mock(MesReleaseFlowAuditRecorder.class), writer, mock(MesProEdhrNonconformanceReviewService.class),
                mock(MesProEdhrNonconformanceReviewMapper.class), Clock.fixed(AT.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        configureAuthenticatedPqcWriter(authorization);
        ReflectionTestUtils.setField(pqc, "gxpAuditService", audit);
        ReflectionTestUtils.setField(pqc, "outcomeNotifications", mock(
                cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseOutcomeNotificationService.class));
        ReflectionTestUtils.setField(pqc, "approvalAuthority", mock(cn.iocoder.yudao.module.system.api.permission.ApprovalAuthorityApi.class));
        var batches = mock(MesProEdhrBatchExecutionMapper.class);
        var batch = new MesProEdhrBatchExecutionDO().setId(92L).setWorkOrderId(93L);
        batch.setTenantId(1L);
        when(batches.selectByTenantIdAndIdForUpdate(1L, 92L)).thenReturn(batch);
        ReflectionTestUtils.setField(pqc, "contentBatchMapper", batches);
        ReflectionTestUtils.setField(pqc, "lifecycleGuard", mock(MesEdhrBatchLifecycleGuard.class));
        var affected = mock(cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector.class);
        when(affected.capture(any(), any(), any(), any(), anyBoolean())).thenReturn(java.util.Map.of("formal", "state"));
        ReflectionTestUtils.setField(pqc, "affectedStates", affected);
        actors = new MesManagerReleaseCompletedActorResolver(tasks, transactions);
        ReflectionTestUtils.setField(actors, "applicationMapper", applications);
        ReflectionTestUtils.setField(actors, "users", users);
        ReflectionTestUtils.setField(actors, "signatures", queries);
        ReflectionTestUtils.setField(actors, "verifiedSignatureFacts", verifiedFacts);
        outcomes = new MesEdhrApprovalOutcomeReader(tasks, transactions, mock(MesProEdhrReleaseDecisionMapper.class),
                applications, actors, queries, new MesProductionReleaseSignoffService(queries), mock(MesEdhrExecutionFinalOutcomeReader.class));
    }
    private void replaceWithLegacyPqcSignature() {
        String subject = stored.getSubjectId(); String reason = stored.getReason();
        stored = null; createdAudit = null;
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
            kernel.sign(new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand(
                    "MES", "PQC_RELEASE", "MES_BATCH_RECORD", subject,
                    MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), "test-credential", reason,
                    "legacy-pqc-fixture", null, null));
        }
    }

    private void configureAuthenticatedPqcWriter(DccElectronicSignatureAuthorizationService authorization) {
        var authentication = mock(cn.iocoder.yudao.module.system.service.signature.SystemSignatureAuthenticationService.class);
        when(authentication.identityDomain()).thenReturn("SYSTEM_USER");
        when(authentication.authenticate(any())).thenAnswer(call -> new cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationOutcome(
                new cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner(1L,"SYSTEM_USER",188L,188L,"00000000-0000-0000-0000-000000000001",1L,"request"),null));
        when(authentication.lockAndRevalidate(any(),any())).thenAnswer(call -> call.getArgument(0));
        var tx = mock(org.springframework.transaction.PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenAnswer(call -> new org.springframework.transaction.support.SimpleTransactionStatus());
        var executor = new cn.iocoder.yudao.module.signature.service.ElectronicSignatureTransactionExecutor(
                signatureRows,actualCreationAudit,List.of(authentication),Clock.fixed(AT.toInstant(ZoneOffset.UTC),ZoneOffset.UTC));
        when(auth.getUser(188L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                .setId(188L).setNickname("真实意见操作者"));
        var authenticatedWriter = new MesPqcReleaseAuthenticatedSignatureService(executor,verifiedFacts,auth,authorization);
        doAnswer(call -> {actualCreationAudit.acquireLedgerLock(); return null;}).when(audit).acquireLedgerLock();
        ReflectionTestUtils.setField(pqc,"signatureAuthentication",authentication);
        ReflectionTestUtils.setField(pqc,"transactionManager",tx);
        ReflectionTestUtils.setField(pqc,"authenticatedSignatures",authenticatedWriter);
    }

    @AfterEach void clear() {
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            for (var callback : org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations())
                callback.afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    private void configureActualCreationAudit() {
        var actualAudit = new cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl();
        // Resolve the authenticated actor through the actual audit writer's new directory port.
        // Only this tenant-bound storage lookup is mocked; audit/signature creation and verification stay real.
        var auditUsers = mock(AdminUserMapper.class);
        var currentActor = new AdminUserDO().setId(188L).setUsername("reviewer")
                .setNickname("真实意见操作者").setStatus(0);
        currentActor.setTenantId(1L);
        when(auditUsers.selectGxpAuditActor(1L, 188L)).thenReturn(currentActor);
        ReflectionTestUtils.setField(actualAudit, "adminUserMapper", auditUsers);
        var auditRows = mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper.class);
        var sequenceRows = mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper.class);
        actualCreationAudit = actualAudit;
        auditLedgerRows = sequenceRows;
        var policyRows = mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper.class);
        var activations = mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper.class);
        var relationRows = mock(cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper.class);
        var sequence = new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO();
        sequence.setTenantId(1L); sequence.setNextLedgerSequence(1L);
        when(sequenceRows.selectByTenantIdForUpdate(1L)).thenReturn(sequence);
        when(sequenceRows.updateById(any(cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO.class)))
                .thenAnswer(call -> { sequence.setNextLedgerSequence(((cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO)
                        call.getArgument(0)).getNextLedgerSequence()); return 1; });
        var activation = new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO();
        activation.setTenantId(1L); activation.setPolicyVersion("audit-v1");
        when(activations.selectLatestForUpdate(1L)).thenReturn(activation);
        var policy = new cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO();
        policy.setTenantId(1L); policy.setPolicyVersion("audit-v1"); policy.setOperationId("signature.record.create");
        policy.setDomain("SIGNATURE"); policy.setSubjectType("SIGNATURE_RECORD"); policy.setActionType("CREATE");
        policy.setActive(true); policy.setApplicability("GXP"); policy.setReasonPolicy("USER_REQUIRED");
        when(policyRows.selectByPolicyVersionForUpdate(1L, "audit-v1", "signature.record.create")).thenReturn(policy);
        when(auditRows.insert(any(cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO.class))).thenAnswer(call -> {
            createdAudit = call.getArgument(0); createdAudit.setId(901L); return 1;
        });
        when(auditRows.selectSignatureCreationEvents(1L, "88")).thenAnswer(call -> createdAudit == null ? List.of() : List.of(createdAudit));
        ReflectionTestUtils.setField(actualAudit, "auditEventMapper", auditRows);
        ReflectionTestUtils.setField(actualAudit, "ledgerSequenceMapper", sequenceRows);
        ReflectionTestUtils.setField(actualAudit, "policyOperationMapper", policyRows);
        ReflectionTestUtils.setField(actualAudit, "policyActivationMapper", activations);
        ReflectionTestUtils.setField(actualAudit, "eventRelationMapper", relationRows);
        var actualVerifier = new cn.iocoder.yudao.module.system.service.gxpaudit.SignatureCreationVerificationService();
        ReflectionTestUtils.setField(actualVerifier, "auditEventMapper", auditRows);
        ReflectionTestUtils.setField(queries, "signatureCreationVerificationService", actualVerifier);
        verifiedFacts = new cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService(signatureRows, actualVerifier);
        ReflectionTestUtils.setField(kernel, "gxpAuditService", actualAudit);
        var login = new cn.iocoder.yudao.framework.security.core.LoginUser();
        login.setId(188L); login.setTenantId(1L); login.setUserType(2);
        login.setInfo(java.util.Map.of("username", "reviewer", cn.iocoder.yudao.framework.security.core.LoginUser.INFO_KEY_NICKNAME, "真实意见操作者"));
        signedIn = login;
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login, null, List.of()));
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
    }

    @ParameterizedTest @NullSource
    @ValueSource(strings = {"PQC_RELEASE", "APPROVE", "普通中文", "首行\n次行", "首行\r\n次行\t\"引号\"\\路径"})
    void actualFinalApproveWriterSnapshotAndReaderRetainUserSource(String opinion) {
        var executions = mock(MesProBatchRecordExecutionMapper.class);
        var snapshots = mock(MesProBatchRecordApprovalSnapshotMapper.class);
        var execution = new MesProBatchRecordExecutionDO().setId(301L).setBatchExecutionId(92L).setWorkOrderId(93L)
                .setStatus(1).setProcessInstanceId("process-301").setProcessDefinitionKey("approval-v1")
                .setFieldAuditRevision(7L).setFieldAuditHeadHash("head").setCellValuesJson("{}");
        execution.setCellValuesHash(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues("{}"));
        var snapshot = new MesProBatchRecordApprovalSnapshotDO().setId(302L).setExecutionId(301L)
                .setProcessInstanceId("process-301").setProcessDefinitionKey("approval-v1").setApprovalStatus("SUBMITTED")
                .setCurrentBpmTaskId("bpm-301").setCurrentTaskDefinitionKey("approve-node").setSnapshotHash("snapshot-hash")
                .setSnapshotJson(JsonUtils.toJsonString(java.util.Map.of("cellValuesHash", execution.getCellValuesHash(),
                        "fieldAuditRevision", 7L, "fieldAuditHeadHash", "head", "domainTraceSnapshotId", 305L,
                        "domainTraceHash", "domain-hash", "domainTraceStatus", "VERIFIED")));
        task.setTaskType("APPROVE").setExecutionId(301L).setBpmTaskId("bpm-301")
                .setSignatureCellKey("cell").setSignatureRowIndex(2).setSignatureColumnIndex(3)
                .setReviewSourceType("POST").setReviewSourceId(403L).setReviewSourceName("QA");
        when(executions.selectById(301L)).thenReturn(execution);
        when(executions.selectByIdForUpdate(301L)).thenReturn(execution);
        when(executions.selectOne(any())).thenReturn(execution);
        when(snapshots.selectByExecutionId(301L)).thenReturn(snapshot);
        when(snapshots.selectOne(any())).thenReturn(snapshot);
        when(tasks.selectList(any())).thenReturn(List.of(task));
        var workTasks = mock(MesProEdhrWorkTaskService.class);
        when(workTasks.validateWritableReviewOrApproveTask(177L, 301L)).thenReturn(task);
        when(workTasks.completeApproveTask(177L, 301L)).thenAnswer(call -> {
            task.setStatus("DONE").setCompletedAt(stored.getSignedAt()); return task;
        });
        when(snapshots.approveAndClearCurrentBpmTask(any())).thenAnswer(call -> {
            MesProBatchRecordApprovalSnapshotDO patch = call.getArgument(0);
            snapshot.setApprovalStatus(patch.getApprovalStatus()).setApproveSignatureId(patch.getApproveSignatureId())
                    .setApprovedBy(patch.getApprovedBy()).setApprovedAt(patch.getApprovedAt()).setClosedAt(patch.getClosedAt())
                    .setCurrentBpmTaskId(null).setCurrentTaskDefinitionKey(null); return 1;
        });
        when(executions.updateById(any(MesProBatchRecordExecutionDO.class))).thenAnswer(call -> {
            MesProBatchRecordExecutionDO patch = call.getArgument(0);
            execution.setStatus(patch.getStatus()).setApprovedBy(patch.getApprovedBy()).setApprovedAt(patch.getApprovedAt())
                    .setClosedAt(patch.getClosedAt()); return 1;
        });
        var bpm = mock(cn.iocoder.yudao.module.bpm.service.task.BpmTaskService.class);
        var bpmTask = mock(org.flowable.task.api.Task.class);
        when(bpmTask.getId()).thenReturn("bpm-301"); when(bpmTask.getProcessInstanceId()).thenReturn("process-301");
        when(bpmTask.getTaskDefinitionKey()).thenReturn("approve-node"); when(bpmTask.getName()).thenReturn("正式批准");
        when(bpm.validateTask(188L, "bpm-301")).thenReturn(bpmTask);
        var assignment = mock(cn.iocoder.yudao.module.bpm.approval.service.ownership.EdhrDomainTaskAssignmentService.class);
        var fieldAudit = mock(MesProBatchRecordExecutionFieldAuditService.class);
        when(fieldAudit.verifyChain(301L)).thenReturn(MesProBatchRecordExecutionFieldAuditHashVerification.valid("head", "head", 1, 1));
        var finalWriter = new MesProBatchRecordExecutionServiceImpl();
        ReflectionTestUtils.setField(finalWriter, "executionMapper", executions);
        ReflectionTestUtils.setField(finalWriter, "approvalSnapshotMapper", snapshots);
        ReflectionTestUtils.setField(finalWriter, "workTaskService", workTasks);
        ReflectionTestUtils.setField(finalWriter, "permissionGateService", mock(MesProEdhrPermissionGateService.class));
        ReflectionTestUtils.setField(finalWriter, "domainTraceService", mock(MesProBatchRecordDomainTraceService.class));
        ReflectionTestUtils.setField(finalWriter, "fieldAuditService", fieldAudit);
        ReflectionTestUtils.setField(finalWriter, "bpmTaskService", bpm);
        ReflectionTestUtils.setField(finalWriter, "domainTaskAssignmentService", assignment);
        // Reuse real ledger acquisition/append/verifier; only mapper ports are doubles.
        // This proves call order and evidence construction, not physical locks or DB roundtrips.
        ReflectionTestUtils.setField(finalWriter, "gxpAuditService", actualCreationAudit);
        ReflectionTestUtils.setField(finalWriter, "executionSignatureService", writer);
        ReflectionTestUtils.setField(finalWriter, "finalApprovalReceiptVerifier", new MesEdhrFinalApprovalReceiptVerifier(queries, tasks));
        var finalReader = new MesEdhrExecutionFinalOutcomeReader(executions, snapshots, tasks, queries, users);
        var reader = new MesEdhrApprovalOutcomeReader(tasks, transactions, mock(MesProEdhrReleaseDecisionMapper.class),
                applications, actors, queries, new MesProductionReleaseSignoffService(queries), finalReader);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
            var result = finalWriter.approveBatchRecordExecution(
                    new cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionApproveReqVO()
                            .setExecutionId(301L).setWorkTaskId(177L).setProcessInstanceId("process-301")
                            .setApprovalSnapshotId(302L).setApprovalSnapshotHash("snapshot-hash").setBpmTaskId("bpm-301")
                            .setPassword("test-credential").setComment(opinion));
            assertEquals("FINAL_APPROVED", result.getResultType());
        }
        assertEquals(88L, snapshot.getApproveSignatureId());
        var lockOrder = inOrder(auditLedgerRows, executions);
        lockOrder.verify(auditLedgerRows).selectByTenantIdForUpdate(1L);
        lockOrder.verify(executions).selectByIdForUpdate(301L);
        verify(assignment).assign("bpm-301", 177L);
        assertEquals("VALID", queries.verifyEvidence(88L).verificationStatus());
        assertSameOpinion(reader.readAuthorizedTask(task), opinion);
        assertEquals(opinion == null ? "SYSTEM" : "USER", JsonUtils.parseTree(stored.getCanonicalContentJson()).path("opinionSource").asText());
        var receiptVerifier = new MesEdhrFinalApprovalReceiptVerifier(queries, tasks);
        String originalTask = task.getBpmTaskId();
        task.setBpmTaskId("foreign-task");
        assertThrows(RuntimeException.class, () -> receiptVerifier.requireSignature(execution, task, 88L, 188L));
        task.setBpmTaskId(originalTask);
        String original = stored.getSubjectId();
        String decoded = new String(java.util.Base64.getUrlDecoder().decode(original), java.nio.charset.StandardCharsets.UTF_8);
        stored.setSubjectId(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                decoded.replace("MES_OPINION_SOURCE_V1:", "MES_OPINION_SOURCE_V9:").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThrows(RuntimeException.class, () -> reader.readAuthorizedTask(task));
        stored.setSubjectId(original);
        snapshot.setApprovedBy(189L);
        assertThrows(RuntimeException.class, () -> reader.readAuthorizedTask(task));
        snapshot.setApprovedBy(188L);
        String historicalSubject = MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(original);
        stored = null; createdAudit = null;
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
            kernel.sign(new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand(
                    "MES", "APPROVE", "MES_BATCH_RECORD", historicalSubject,
                    MesBatchRecordSignatureSubjectAdapter.subjectVersion(historicalSubject), "test-credential", "APPROVE",
                    "historical-final-17-fixture", null, null));
        }
        snapshot.setApprovedAt(stored.getSignedAt()).setClosedAt(stored.getSignedAt());
        execution.setApprovedAt(stored.getSignedAt()).setClosedAt(stored.getSignedAt());
        String frozenHistoricalContent = stored.getCanonicalContentJson(), frozenHistoricalHash = stored.getEvidenceHash();
        assertFalse(JsonUtils.parseTree(frozenHistoricalContent).has("opinionSource"));
        assertEquals("VALID", queries.verifyEvidence(88L).verificationStatus());
        assertSameOpinion(reader.readAuthorizedTask(task), null);
        assertEquals("APPROVE", reader.readAuthorizedTask(task).sourceRemark());
        assertEquals(frozenHistoricalContent, stored.getCanonicalContentJson());
        assertEquals(frozenHistoricalHash, stored.getEvidenceHash());
    }

    @Test void sourceContractRejectsSystemOpinionConflictEmptyUserAndUnknownVersionBeforeInsert() {
        String legacy = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(92L, "PQC_RELEASE",
                null, null, null, null, null, null, null, "PQC_RELEASE_APPLICATION", 95L, "PQC生产放行",
                "PQC_RELEASE", null, null, null, null);
        String system = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(legacy, null);
        String user = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(legacy, "PQC_RELEASE");
        String decoded = new String(java.util.Base64.getUrlDecoder().decode(user), java.nio.charset.StandardCharsets.UTF_8);
        String unknown = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                decoded.replace("MES_OPINION_SOURCE_V1:USER", "MES_OPINION_SOURCE_V1:OTHER").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
            for (var input : List.of(new String[]{system, "用户原文"}, new String[]{user, ""}, new String[]{unknown, "PQC_RELEASE"})) {
                assertThrows(RuntimeException.class, () -> kernel.sign(new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand(
                        "MES", "PQC_RELEASE", "MES_BATCH_RECORD", input[0], MesBatchRecordSignatureSubjectAdapter.subjectVersion(input[0]),
                        "test-credential", input[1], "invalid-source-" + input[0], null, null)));
            }
        }
        verify(signatureRows, never()).insert(any(ElectronicSignatureRecordDO.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PQC_RELEASE", "APPROVE", "历史普通意见"})
    void oldSeventeenPartSignatureRemainsDigestValidAndOpinionUnknown(String historicalReason) {
        signPqc("历史原文");
        String subject = MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(stored.getSubjectId());
        stored = null; createdAudit = null;
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
            kernel.sign(new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand("MES", "PQC_RELEASE", "MES_BATCH_RECORD",
                    subject, MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), "test-credential", historicalReason,
                    "historical-17-fixture", null, null));
        }
        String content = stored.getCanonicalContentJson(), hash = stored.getContentHash(), evidenceHash = stored.getEvidenceHash();
        String frozenV1Content = "{\"actionType\":\"PQC_RELEASE\",\"adapter\":\"MES_BATCH_RECORD\",\"approvalResult\":\"PQC_RELEASE\","
                + "\"bpmTaskDefinitionKey\":\"\",\"bpmTaskId\":\"\",\"bpmTaskName\":\"\",\"cellValuesHash\":\"\","
                + "\"executionId\":\"92\",\"fieldAuditHeadHash\":\"\",\"fieldAuditRevision\":\"\",\"processInstanceId\":\"\","
                + "\"reason\":" + JsonUtils.toJsonString(historicalReason) + ",\"reviewSourceId\":\"95\",\"reviewSourceName\":\"PQC生产放行\","
                + "\"reviewSourceType\":\"PQC_RELEASE_APPLICATION\",\"signatureCellKey\":\"\",\"signatureChallengeHash\":\"\","
                + "\"signatureColumnIndex\":\"\",\"signatureRowIndex\":\"\"}";
        assertEquals(frozenV1Content, content);
        assertEquals(cn.hutool.crypto.digest.DigestUtil.sha256Hex(frozenV1Content), hash);
        assertEquals(17, new String(java.util.Base64.getUrlDecoder().decode(subject), java.nio.charset.StandardCharsets.UTF_8).split("\n", -1).length);
        assertEquals("VALID", queries.verifyEvidence(88L).verificationStatus());
        assertSameOpinion(outcomes.readAuthorizedTask(task), null);
        assertEquals(historicalReason, outcomes.readAuthorizedTask(task).sourceRemark());
        assertEquals(content, stored.getCanonicalContentJson()); assertEquals(hash, stored.getContentHash());
        assertEquals(evidenceHash, stored.getEvidenceHash());
    }

    @ParameterizedTest @NullSource
    @ValueSource(strings = {"PQC_RELEASE", "APPROVE", "普通中文", "第一行\n第二行", "第一行\r\n第二行\t制表", "引号\"与路径\\"})
    void formalPqcWriterReceiptRealActorAndBothProjectionsPreserveOpinion(String opinion) {
        signPqc(opinion);
        var verification = queries.verifyEvidence(88L);
        assertEquals("VALID", verification.verificationStatus());
        assertEquals("CONSISTENT", verification.digestStatus());
        assertEquals("CONSISTENT", verification.timeConsistencyStatus());
        assertEquals("CONSISTENT", verification.auditBindingStatus());
        assertEquals(opinion == null ? "SYSTEM" : "USER", JsonUtils.parseTree(stored.getCanonicalContentJson()).path("opinionSource").asText());
        assertEquals(188L, actors.resolvePqc(177L, "RELEASE_APPLICATION", 95L, 92L, 93L, "APPROVE", task.getCompletedAt()));
        assertSameOpinion(outcomes.readAuthorizedTask(task), opinion);
    }

    @ParameterizedTest @NullSource
    @ValueSource(strings = {"PQC_RELEASE", "APPROVE", "正式首行\n次行\r\n制表\t引号\"路径\\"})
    void actualPqcSignatureAndCreationAuditFeedBothFormalConsumers(String opinion) {
        signPqc(opinion);
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(180L).setWorkOrderId(93L);
        var batchRows = mock(MesProEdhrBatchExecutionMapper.class);
        when(batchRows.selectOne(any())).thenReturn(new MesProEdhrBatchExecutionDO().setId(92L).setWorkOrderId(93L).setTenantId(1L));
        var directory = mock(AdminUserService.class);
        when(directory.getUser(188L)).thenReturn(new AdminUserDO().setId(188L).setNickname("真实意见操作者"));
        var summary = new MesTeamLeaderActiveOrderDetailServiceImpl(null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, applications, null, null, transactions, null,
                queries, null, directory);
        ReflectionTestUtils.setField(summary, "workTaskMapper", tasks);
        ReflectionTestUtils.setField(summary, "verifiedSignatureFacts", verifiedFacts);
        ReflectionTestUtils.setField(summary, "batchExecutionMapper", batchRows);
        ReflectionTestUtils.invokeMethod(summary, "attachPqcProductionRelease", detail, application, formalPqcPurpose());
        assertEquals("真实意见操作者", queries.getById(88L).actorDisplayName());
        assertEquals("真实意见操作者", detail.getPqcProductionRelease().getSignature().getSignerName());
        assertEquals(88L, detail.getPqcProductionRelease().getSignature().getSignatureId());
        assertEquals(stored.getSignedAt(), detail.getPqcProductionRelease().getSignature().getSignedAt());
        var authorizedDetail = mock(MesTeamLeaderActiveOrderDetailService.class);
        when(authorizedDetail.getDetail(188L, 180L)).thenReturn(detail);
        var consumer = new MesActiveOrderSignatureEvidenceService(authorizedDetail, null, null, queries,
                null, null, null, null, null, applications, transactions, tasks, batchRows, null);
        ReflectionTestUtils.setField(consumer, "verifiedSignatureFacts", verifiedFacts);
        var result = consumer.getTeam(188L, 180L, 88L);
        assertEquals("VALID", result.verification().verificationStatus());
        assertEquals("CONSISTENT", result.verification().auditBindingStatus());
        assertEquals(opinion, MesBatchRecordSignatureSubjectAdapter.verifiedUserOpinion(result.evidence().subjectId(), "PQC_RELEASE", result.evidence().reason()));
        assertEquals(stored.getCanonicalContentJson(), result.evidence().canonicalContentJson());
        var originalAudit = createdAudit;
        createdAudit = null;
        assertEquals("MISMATCH", consumer.getTeam(188L, 180L, 88L).verification().verificationStatus());
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(summary, "attachPqcProductionRelease", detail, application, formalPqcPurpose()));
        createdAudit = originalAudit;
        String originalTime = stored.getTimeEvidenceId();
        stored.setTimeEvidenceId(null);
        var missingTime = consumer.getTeam(188L, 180L, 88L).verification();
        assertNotEquals("VALID", missingTime.verificationStatus());
        assertEquals("MISSING", missingTime.timeConsistencyStatus());
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(summary, "attachPqcProductionRelease", detail, application, formalPqcPurpose()));
        stored.setTimeEvidenceId(originalTime);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RENAMED", "DELETED", "DISABLED", "UNAVAILABLE"})
    void actualSystemUserV1KernelAuditQueryAndPqcConsumersKeepFrozenIdentity(String directoryState) {
        // Establish the actual PQC application/task/receipt first, then read a legal historical V1 kernel signature.
        signPqc("历史原文");
        String subject = MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(stored.getSubjectId());
        stored = null; createdAudit = null;
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
            kernel.signAuthorized(new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand(
                            "MES", "PQC_RELEASE", "MES_BATCH_RECORD", subject,
                            MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), "test-credential", "历史原文",
                            "historical-system-user-v1", null, null),
                    new cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity("SYSTEM_USER", 188L, "签署时正式姓名"), null);
        }
        String originalCanonical = stored.getCanonicalContentJson(), originalHash = stored.getEvidenceHash();
        assertEquals("SESSION_PLUS_SELECTED_USER_PASSWORD", stored.getAuthenticationMethod());
        assertEquals(5, JsonUtils.parseTree(originalCanonical).path("signatureIdentity").size());
        assertFalse(JsonUtils.parseTree(originalCanonical).has("opinionSource"));
        var verified = queries.verifyEvidence(88L);
        assertEquals("VALID", verified.verificationStatus());
        assertEquals("CONSISTENT", verified.digestStatus());
        assertEquals("CONSISTENT", verified.timeConsistencyStatus());
        assertEquals("CONSISTENT", verified.auditBindingStatus());
        assertEquals("签署时正式姓名", queries.getById(88L).actorDisplayName());
        var directory = mock(AdminUserService.class);
        if (directoryState.equals("DELETED")) when(directory.getUser(188L)).thenReturn(null);
        else if (directoryState.equals("UNAVAILABLE")) when(directory.getUser(188L)).thenThrow(new IllegalStateException("当前目录不可用"));
        else when(directory.getUser(188L)).thenReturn(new AdminUserDO().setId(188L).setNickname("当前新名")
                    .setStatus(directoryState.equals("DISABLED") ? 1 : 0));
        var batchRows = mock(MesProEdhrBatchExecutionMapper.class);
        when(batchRows.selectOne(any())).thenReturn(new MesProEdhrBatchExecutionDO().setId(92L).setWorkOrderId(93L).setTenantId(1L));
        var summary = new MesTeamLeaderActiveOrderDetailServiceImpl(null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, applications, null, null, transactions, null,
                queries, null, directory);
        ReflectionTestUtils.setField(summary, "workTaskMapper", tasks);
        ReflectionTestUtils.setField(summary, "verifiedSignatureFacts", verifiedFacts);
        ReflectionTestUtils.setField(summary, "batchExecutionMapper", batchRows);
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(180L).setWorkOrderId(93L).setWorkOrderCode("WO-93");
        ReflectionTestUtils.invokeMethod(summary, "attachPqcProductionRelease", detail, application, formalPqcPurpose());
        assertEquals("签署时正式姓名", detail.getPqcProductionRelease().getSignature().getSignerName());
        var authorizedDetail = mock(MesTeamLeaderActiveOrderDetailService.class);
        when(authorizedDetail.getDetail(188L, 180L)).thenReturn(detail);
        when(authorizedDetail.getArchivedFormalDetail(180L)).thenReturn(detail);
        var evidenceReader = new MesActiveOrderSignatureEvidenceService(authorizedDetail, null, null, queries,
                null, null, null, null, null, applications, transactions, tasks, batchRows, null);
        ReflectionTestUtils.setField(evidenceReader, "verifiedSignatureFacts", verifiedFacts);
        assertEquals("VALID", evidenceReader.getTeam(188L, 180L, 88L).verification().verificationStatus());
        assertEquals("签署时正式姓名", evidenceReader.getTeam(188L, 180L, 88L).signerName());
        var authorization = mock(MesPqcProductionReleaseService.class);
        when(authorization.get(188L, 95L)).thenReturn(new MesPqcProductionReleaseDecisionResult().setApplicationId(95L)
                .setStatus("MANAGER_RELEASE_PENDING").setDecision("APPROVE").setSignatureId(88L));
        var orderRows = mock(MesProcessPoolActiveOrderMapper.class);
        when(orderRows.selectByIdIgnoreDeleted(180L)).thenReturn(new MesProcessPoolActiveOrderDO().setId(180L)
                .setWorkOrderId(93L).setLeaderUserId(188L));
        var materials = mock(cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService.class);
        when(materials.getPage(any())).thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(), 0L));
        var pqcDetail = new MesPqcReleaseOrderDetailService(authorization, applications, orderRows, queries, directory, authorizedDetail, materials);
        ReflectionTestUtils.setField(pqcDetail, "workTaskMapper", tasks);
        ReflectionTestUtils.setField(pqcDetail, "verifiedSignatureFacts", verifiedFacts);
        ReflectionTestUtils.setField(pqcDetail, "batchMapper", batchRows);
        assertEquals("签署时正式姓名", pqcDetail.get(188L, 95L).detail().getPqcProductionRelease().getSignature().getSignerName());
        assertEquals(originalCanonical, stored.getCanonicalContentJson());
        assertEquals(originalHash, stored.getEvidenceHash());
        var auditEvent = createdAudit;
        createdAudit = null;
        assertEquals("INCOMPLETE", queries.verifyEvidence(88L).verificationStatus());
        assertEquals(queries.verifyEvidence(88L).verificationStatus(), pqcDetail.get(188L, 95L).detail().getPqcProductionRelease().getSignature().getVerification().verificationStatus());
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(summary, "attachPqcProductionRelease", detail, application, formalPqcPurpose()));
        createdAudit = auditEvent;
        String originalTimeEvidence = stored.getTimeEvidenceId();
        stored.setTimeEvidenceId(null);
        assertEquals("MISSING", queries.verifyEvidence(88L).timeConsistencyStatus());
        assertEquals(queries.verifyEvidence(88L).verificationStatus(), pqcDetail.get(188L, 95L).detail().getPqcProductionRelease().getSignature().getVerification().verificationStatus());
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(summary, "attachPqcProductionRelease", detail, application, formalPqcPurpose()));
        stored.setTimeEvidenceId(originalTimeEvidence);
        stored.setActorId(189L);
        assertEquals("MISMATCH", queries.verifyEvidence(88L).verificationStatus());
        assertTrue(queries.verifyEvidence(88L).reasonCodes().contains("IDENTITY_BINDING_MISMATCH"));
        assertThrows(RuntimeException.class, () -> evidenceReader.getTeam(188L, 180L, 88L));
        assertThrows(RuntimeException.class, () -> pqcDetail.get(188L, 95L));
        stored.setActorId(188L);
        stored.setCanonicalContentJson(originalCanonical.replace("签署时正式姓名", "篡改签时姓名"));
        assertEquals("MISMATCH", queries.verifyEvidence(88L).verificationStatus());
        assertEquals("MISMATCH", evidenceReader.getTeam(188L, 180L, 88L).verification().verificationStatus());
        assertEquals("UNVERIFIED_SNAPSHOT", pqcDetail.get(188L, 95L).detail().getPqcProductionRelease().getSignature().getSignerNameStatus());
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(summary, "attachPqcProductionRelease", detail, application, formalPqcPurpose()));
        stored.setCanonicalContentJson(originalCanonical);
        assertEquals("VALID", queries.verifyEvidence(88L).verificationStatus());
        verifyNoInteractions(directory);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VALID", "ORDINARY", "AUDIT_MISSING", "TIME_MISSING", "SIGNED_AT_MISSING", "HASH_MISSING", "HASH_MISMATCH", "AUDIT_MISMATCH"})
    void realInitialDetailsAndBothEvidenceEntriesExposeVerificationWithoutAuthorizingFormalConsumption(String state) {
        signPqc("历史意见");
        if (state.equals("ORDINARY")) replaceWithLegacyPqcSignature();
        if (!state.equals("ORDINARY")) {
            String subject = MesBatchRecordSignatureSubjectAdapter.subjectWithoutOpinionSource(stored.getSubjectId());
            stored = null; createdAudit = null;
            try (var security = mockStatic(SecurityFrameworkUtils.class)) {
                security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
                security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
                kernel.signAuthorized(new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand(
                        "MES", "PQC_RELEASE", "MES_BATCH_RECORD", subject, MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject),
                        "test-credential", "历史意见", "readable-historical-v1", null, null),
                        new cn.iocoder.yudao.module.signature.api.dto.AuthorizedSignatureIdentity("SYSTEM_USER", 188L, "签时原名"), null);
            }
        }
        var chain = realReadChain();
        if (state.equals("AUDIT_MISSING")) createdAudit = null;
        if (state.equals("TIME_MISSING")) stored.setTimeEvidenceId(null);
        if (state.equals("SIGNED_AT_MISSING")) { stored.setSignedAt(null); stored.setTimeEvidenceId(null); }
        if (state.equals("HASH_MISSING")) stored.setContentHash(null);
        if (state.equals("HASH_MISMATCH")) stored.setContentHash("changed-content-hash");
        if (state.equals("AUDIT_MISMATCH")) createdAudit.setReason("changed-audit-reason");
        var verified = queries.verifyEvidence(88L);
        var initialTeam = chain.team().getDetail(188L, 180L);
        var initialPqc = chain.pqc().get(188L, 95L).detail();
        for (var detail : List.of(initialTeam, initialPqc)) {
            var projection = detail.getPqcProductionRelease().getSignature();
            assertEquals(88L, projection.getSignatureId());
            assertEquals(verified.verificationStatus(), projection.getVerification().verificationStatus());
            assertEquals(stored.getSignedAt(), projection.getSignedAt());
            String nameState = state.equals("ORDINARY") ? "NOT_RECORDED" : state.equals("VALID") ? "VERIFIED_SNAPSHOT" : "UNVERIFIED_SNAPSHOT";
            assertEquals(nameState, projection.getSignerNameStatus());
            assertEquals(state.equals("ORDINARY") ? "未记录" : state.equals("VALID") ? "签时原名" : "签时姓名未核验", projection.getSignerName());
            var projected = cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.MesProcessPoolTeamLeaderController
                    .toActiveOrderDetailRespVO(detail).getPqcProductionRelease().getSignature();
            assertEquals(projection.getVerification(), projected.getVerification());
            assertEquals(nameState, projected.getSignerNameStatus());
        }
        for (var result : List.of(chain.evidence().getTeam(188L, 180L, 88L), chain.evidence().getPqc(188L, 95L, 88L))) {
            assertEquals(verified.verificationStatus(), result.verification().verificationStatus());
            assertEquals(verified.digestStatus(), result.verification().digestStatus());
            assertEquals(verified.timeConsistencyStatus(), result.verification().timeConsistencyStatus());
            assertEquals(verified.auditBindingStatus(), result.verification().auditBindingStatus());
            assertEquals(stored.getSignedAt(), result.evidence().signedAt());
            assertEquals(stored.getCanonicalContentJson(), result.evidence().canonicalContentJson());
            assertEquals(initialTeam.getPqcProductionRelease().getSignature().getSignerName(), result.signerName());
        }
        if (!verified.verificationStatus().equals("VALID")) assertThrows(RuntimeException.class, () -> chain.team().getFormalDetail(180L));
        else assertEquals("VALID", chain.team().getFormalDetail(180L).getPqcProductionRelease().getSignature().getVerification().verificationStatus());
        verifyNoInteractions(chain.directory());
        for (Object mapper : List.of(signatureRows, applications, tasks)) {
            assertTrue(mockingDetails(mapper).getInvocations().stream()
                    .allMatch(call -> call.getMethod().getName().startsWith("select")), "read side must not write or re-sign");
        }
    }

    @Test void realReadEntriesRejectForeignRelationsAndPropagateDatabaseFailures() {
        signPqc("正式意见");
        var chain = realReadChain();
        assertThrows(RuntimeException.class, () -> chain.team().getDetail(189L, 180L));
        assertThrows(RuntimeException.class, () -> chain.evidence().getPqc(189L, 95L, 88L));
        String receipt = application.getDossierSummaryJson();
        application.setDossierSummaryJson(receipt.replace("\"signatureId\":88", "\"signatureId\":89"));
        assertThrows(RuntimeException.class, () -> chain.evidence().getTeam(188L, 180L, 88L));
        application.setDossierSummaryJson(receipt);
        task.setBusinessScopeId(96L);
        assertThrows(RuntimeException.class, () -> chain.pqc().get(188L, 95L));
        task.setBusinessScopeId(95L);
        application.setTenantId(2L);
        assertThrows(RuntimeException.class, () -> chain.evidence().getPqc(188L, 95L, 88L));
        application.setTenantId(1L);
        stored.setActorId(189L);
        assertThrows(RuntimeException.class, () -> chain.evidence().getTeam(188L, 180L, 88L));
        stored.setActorId(188L);
        var offline = new IllegalStateException("signature DB offline");
        doThrow(offline).when(signatureRows).selectById(88L);
        assertSame(offline, assertThrows(IllegalStateException.class, () -> chain.team().getDetail(188L, 180L)));
        assertSame(offline, assertThrows(IllegalStateException.class, () -> chain.evidence().getPqc(188L, 95L, 88L)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUBJECT", "VERSION", "SOURCE", "CANONICAL", "TASK_STATUS", "RECEIPT_MISSING", "RECEIPT_ACTOR", "BATCH"})
    void realInitialAndEvidenceEntriesKeepAllBusinessAssociationGuards(String fault) {
        signPqc("正式意见");
        var chain = realReadChain();
        if (fault.equals("SUBJECT")) stored.setSubjectId(MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(92L, "PQC_RELEASE",
                null, null, null, null, null, null, null, "PQC_RELEASE_APPLICATION", 96L, "PQC生产放行", "PQC_RELEASE", null, null, null, null));
        if (fault.equals("VERSION")) stored.setSubjectVersion("foreign-version");
        if (fault.equals("SOURCE")) {
            String decoded = new String(java.util.Base64.getUrlDecoder().decode(stored.getSubjectId()), java.nio.charset.StandardCharsets.UTF_8);
            stored.setSubjectId(java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                    decoded.replace("MES_OPINION_SOURCE_V1:USER", "MES_OPINION_SOURCE_V9:USER").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        if (fault.equals("CANONICAL")) stored.setCanonicalContentJson("{}");
        if (fault.equals("TASK_STATUS")) task.setStatus("TODO");
        if (fault.equals("RECEIPT_MISSING")) application.setDossierSummaryJson(null);
        if (fault.equals("RECEIPT_ACTOR")) {
            var receipt = (com.fasterxml.jackson.databind.node.ObjectNode) JsonUtils.parseTree(application.getDossierSummaryJson());
            receipt.put("decidedBy", 189L); application.setDossierSummaryJson(receipt.toString());
        }
        if (fault.equals("BATCH")) application.setBatchExecutionId(999L);
        assertThrows(RuntimeException.class, () -> chain.team().getDetail(188L, 180L));
        assertThrows(RuntimeException.class, () -> chain.pqc().get(188L, 95L));
        assertThrows(RuntimeException.class, () -> chain.evidence().getTeam(188L, 180L, 88L));
        assertThrows(RuntimeException.class, () -> chain.evidence().getPqc(188L, 95L, 88L));
        for (Object mapper : List.of(signatureRows, applications, tasks)) {
            assertTrue(mockingDetails(mapper).getInvocations().stream().allMatch(call -> call.getMethod().getName().startsWith("select")));
        }
    }

    private ReadChain realReadChain() {
        var orders = mock(MesProcessPoolActiveOrderMapper.class);
        var order = new MesProcessPoolActiveOrderDO().setId(180L).setWorkOrderId(93L).setLeaderUserId(188L).setRouteId(20L).setActiveStatus("ACTIVE");
        when(orders.selectById(180L)).thenReturn(order);
        when(orders.selectByIdIgnoreDeleted(180L)).thenReturn(order);
        var rows = mock(MesProcessPoolActiveOrderDetailReadMapper.class);
        when(rows.selectByActiveOrderId(180L)).thenReturn(List.of(new MesTeamLeaderActiveOrderDetailReadDO()
                .setSnapshotId(40L).setActiveOrderId(180L).setWorkOrderId(93L).setWorkOrderCode("WO-93")
                .setRouteName("Frozen route").setRouteProcessId(30L).setProcessId(31L).setProcessName("Frozen process")
                .setRequiredQuantity(java.math.BigDecimal.ONE)));
        when(applications.selectLatestByActiveOrderId(180L)).thenReturn(application);
        var batches = mock(MesProEdhrBatchExecutionMapper.class);
        when(batches.selectOne(any())).thenReturn(new MesProEdhrBatchExecutionDO().setId(92L).setWorkOrderId(93L).setTenantId(1L));
        var directory = mock(AdminUserService.class);
        var team = new MesTeamLeaderActiveOrderDetailServiceImpl(orders, rows,
                mock(cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialService.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper.class),
                mock(cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper.class),
                mock(cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper.class),
                mock(cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper.class),
                mock(cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper.class),
                mock(MesProcessPoolActiveOrderCompletionBackfillMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper.class),
                mock(cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper.class),
                applications, mock(MesProEdhrNonconformanceReviewMapper.class), mock(MesProEdhrOperationAuditEventMapper.class),
                transactions, mock(MesProcessPoolTeamMaintenanceAuditMapper.class), queries, mock(MesSubmissionSignatureIdentityReader.class), directory);
        ReflectionTestUtils.setField(team, "workTaskMapper", tasks);
        ReflectionTestUtils.setField(team, "marketReleaseAccess", mock(MesMarketReleaseAccess.class));
        ReflectionTestUtils.setField(team, "verifiedSignatureFacts", verifiedFacts);
        ReflectionTestUtils.setField(team, "batchExecutionMapper", batches);
        var materials = mock(cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService.class);
        when(materials.getPage(any())).thenReturn(new cn.iocoder.yudao.framework.common.pojo.PageResult<>(List.of(), 0L));
        var pqcDetail = new MesPqcReleaseOrderDetailService(pqc, applications, orders, queries, directory, team, materials);
        ReflectionTestUtils.setField(pqcDetail, "workTaskMapper", tasks);
        ReflectionTestUtils.setField(pqcDetail, "verifiedSignatureFacts", verifiedFacts);
        ReflectionTestUtils.setField(pqcDetail, "batchMapper", batches);
        var evidence = new MesActiveOrderSignatureEvidenceService(team, pqcDetail, null, queries,
                null, null, null, null, null, applications, transactions, tasks, batches, null);
        ReflectionTestUtils.setField(evidence, "verifiedSignatureFacts", verifiedFacts);
        clearInvocations(signatureRows, applications, tasks);
        return new ReadChain(team, pqcDetail, evidence, directory);
    }

    private static Object formalPqcPurpose() {
        try {
            Class<?> type = Class.forName(MesTeamLeaderActiveOrderDetailServiceImpl.class.getName() + "$PqcSignaturePurpose");
            return java.util.Arrays.stream(type.getEnumConstants()).filter(value -> value.toString().equals("FORMAL_CONSUMPTION"))
                    .findFirst().orElseThrow();
        } catch (ClassNotFoundException ex) { throw new AssertionError(ex); }
    }
    private record ReadChain(MesTeamLeaderActiveOrderDetailServiceImpl team, MesPqcReleaseOrderDetailService pqc,
            MesActiveOrderSignatureEvidenceService evidence, AdminUserService directory) {}

    @Test void actualCreationAuditMissingOrTamperedCannotBeHiddenByMatchingSignatureDigests() {
        signPqc("PQC_RELEASE");
        var original = createdAudit;
        createdAudit = null;
        var missing = queries.verifyEvidence(88L);
        assertEquals("CONSISTENT", missing.digestStatus());
        assertEquals("MISMATCH", missing.verificationStatus());
        assertEquals("MISSING", missing.auditBindingStatus());
        assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task));
        createdAudit = original;
        createdAudit.setReason("篡改来源意见");
        assertEquals("MISMATCH", queries.verifyEvidence(88L).verificationStatus());
        assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task));
    }

    @Test void tenantActorReceiptTaskSignatureVersionAndHashesCannotBeSubstituted() {
        signPqc("PQC_RELEASE");
        String version = stored.getSubjectVersion();
        Long actor = stored.getActorId();
        String canonical = stored.getCanonicalContentJson();
        String evidence = stored.getEvidenceHash();
        String content = stored.getContentHash();
        String receipt = application.getDossierSummaryJson();
        assertSameOpinion(outcomes.readAuthorizedTask(task), "PQC_RELEASE");
        stored.setTenantId(2L); assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task)); stored.setTenantId(1L);
        stored.setActorId(189L); assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task)); stored.setActorId(actor);
        stored.setSubjectVersion("foreign-version"); assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task)); stored.setSubjectVersion(version);
        stored.setCanonicalContentJson(canonical.replace("PQC_RELEASE", "tampered"));
        assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task)); stored.setCanonicalContentJson(canonical);
        stored.setContentHash("wrong-content"); assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task)); stored.setContentHash(content);
        stored.setEvidenceHash("wrong-evidence"); assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task)); stored.setEvidenceHash(evidence);
        application.setDossierSummaryJson(receipt.replace("\"signatureId\":88", "\"signatureId\":89"));
        assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task)); application.setDossierSummaryJson(receipt);
        application.setPqcReleaseWorkTaskId(178L); assertThrows(RuntimeException.class, () -> outcomes.readAuthorizedTask(task));
    }

    @Test void ordinaryDiagnosticReadDoesNotIssueFormalFactAndFrozenNameRemainsAvailable() {
        signPqc("正式意见");
        var fact = verifiedFacts.requireVerifiedFact(88L);
        assertEquals("VERIFIED_SNAPSHOT", MesActiveOrderSignatureEvidenceService
                .pqcReadProjection(queries.getById(88L), fact).getSignerNameStatus());
        createdAudit = null;
        var evidence = queries.getById(88L);
        var diagnostic = queries.verifyEvidence(88L);
        assertEquals("MISMATCH", MesActiveOrderSignatureEvidenceService
                .pqcReadProjection(evidence, diagnostic).getVerification().verificationStatus());
        assertThrows(RuntimeException.class, () -> verifiedFacts.requireVerifiedFact(88L));
    }
    @Test void actualBpmSignatureReadsThroughReleasedOwnerGraphWithoutMesProtocolConversion() {
        // Real signature kernel, creation audit and typed verifier; owner storage ports are bounded doubles.
        var bpmAdapter = new cn.iocoder.yudao.module.bpm.approval.service.signature.BpmApprovalSignatureSubjectAdapter();
        ReflectionTestUtils.setField(kernel, "subjectAdapters", List.of(bpmAdapter));
        String subject = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.join("\n", "EDHR", "EDHR_WORK_TASK", "177", "177", "", "a".repeat(64))
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String version = cn.hutool.crypto.digest.DigestUtil.sha256Hex(subject);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
            kernel.sign(new cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureCommand(
                    "BPM", "APPROVE", "BPM_APPROVAL_TASK", subject, version, "test-credential",
                    "上市意见", "market-chain", null, null));
        }
        assertEquals("VALID", verifiedFacts.requireVerifiedFact(88L).verification().verificationStatus());
        when(signatureRows.selectList(any())).thenAnswer(call -> List.of(stored));
        application.setApplicationStatus("RELEASED").setReleaseTransactionId(91L).setReleaseApprovalWorkTaskId(177L);
        task.setTaskType("RELEASE_APPROVE").setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(91L)
                .setStatus("DONE").setCandidateSourceType("ROLE_GROUP")
                .setResponsibilitySourceKey(cn.iocoder.yudao.module.mes.service.pro.productionrelease.role.MesProductionReleaseRoleCodes.MANAGEMENT_REPRESENTATIVE);
        var transaction = new MesProEdhrReleaseTransactionDO().setId(91L).setBatchExecutionId(92L).setWorkOrderId(93L)
                .setReleaseStatus("RELEASED").setApprovalSignatureId(88L).setApprovedBy(188L)
                .setApprovalSignoffEvidenceHash(stored.getEvidenceHash()).setApprovalOpinion("上市意见");
        when(transactions.selectById(91L)).thenReturn(transaction);
        when(applications.selectListByActiveOrderIds(List.of(180L))).thenReturn(List.of(application));
        var batches = mock(MesProEdhrBatchExecutionMapper.class);
        when(batches.selectById(92L)).thenReturn(new MesProEdhrBatchExecutionDO().setId(92L).setWorkOrderId(93L).setTenantId(1L));
        var detail = new MesTeamLeaderActiveOrderDetail().setActiveOrderId(180L).setOperationFacts(List.of(
                new MesTeamLeaderActiveOrderDetail.OperationFact().setSignatureId(88L).setActorUserId(188L)
                        .setActorName("上市签署人").setOperationType("BATCH_RECORD_RELEASE_APPROVED")));
        var batchDetail = mock(MesProEdhrBatchActiveOrderDetailService.class);
        when(batchDetail.getSignatureDetail(92L, 88L)).thenReturn(detail);
        var reader = new MesActiveOrderSignatureEvidenceService(null, null, batchDetail, queries,
                null, null, null, null, null, applications, transactions, tasks, batches, new MesProductionReleaseSignoffService(queries));
        ReflectionTestUtils.setField(reader, "verifiedSignatureFacts", verifiedFacts);
        assertEquals("BPM", reader.getBatch(92L, null, 88L).evidence().moduleCode());
        assertEquals("VALID", reader.getBatch(92L, null, 88L).verification().verificationStatus());
        transaction.setApprovedBy(189L);
        assertThrows(RuntimeException.class, () -> reader.getBatch(92L, null, 88L));
        transaction.setApprovedBy(188L).setApprovalSignatureId(89L);
        assertThrows(RuntimeException.class, () -> reader.getBatch(92L, null, 88L));
        transaction.setApprovalSignatureId(88L).setApprovalSignoffEvidenceHash("wrong");
        assertThrows(RuntimeException.class, () -> reader.getBatch(92L, null, 88L));
        transaction.setApprovalSignoffEvidenceHash(stored.getEvidenceHash());
        createdAudit = null;
        assertThrows(RuntimeException.class, () -> reader.getBatch(92L, null, 88L));
    }
    private void signPqc(String opinion) {
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(188L);
            security.when(SecurityFrameworkUtils::getLoginUser).thenReturn(signedIn);
            pqc.approve(188L, new MesPqcProductionReleaseApproveCommand().setApplicationId(95L).setPqcReleaseWorkTaskId(177L)
                    .setExpectedVersion(1).setApprovalOpinion(opinion).setSignaturePassword("test-credential")
                    .setIdempotencyKey("opinion-chain").setUdiControlDocumentNo("UDI-93"));
        }
    }
    private static void assertSameOpinion(MesEdhrApprovalOutcome result, String opinion) {
        assertEquals(opinion, result.approvalRemark());
        var list = MesEdhrApprovalOutcomeProjection.apply(new ApprovalTaskSummary(), result);
        var timeline = MesEdhrApprovalOutcomeProjection.apply(new ApprovalTaskTimelineEntry(), result);
        assertEquals(opinion, list.getApprovalRemark());
        assertEquals(opinion, timeline.getApprovalRemark());
        assertEquals(opinion, timeline.getComment());
    }
}
