package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesEdhrApprovalOutcomeReaderTest {
    @Mock MesProEdhrWorkTaskMapper tasks;
    @Mock MesProEdhrReleaseTransactionMapper transactions;
    @Mock MesProEdhrReleaseDecisionMapper decisions;
    @Mock MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    @Mock MesManagerReleaseCompletedActorResolver actors;
    @Mock ElectronicSignatureQueryService signatures;
    @Mock MesProductionReleaseSignoffService signoff;
    @Mock AdminUserMapper users;
    @Mock MesEdhrExecutionFinalOutcomeReader executionFinal;
    MesEdhrApprovalOutcomeReader reader;
    static final LocalDateTime AT = LocalDateTime.of(2026, 10, 6, 10, 0);
    static final String OPINION = "original user opinion\n  \"quoted\" \\ kept";

    @BeforeEach void init() {
        TenantContextHolder.setTenantId(1L);
        reader = new MesEdhrApprovalOutcomeReader(tasks, transactions, decisions, applications, actors, signatures, signoff, executionFinal);
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "LEGACY_NEW_STATE"})
    void missingAndUnsupportedTaskCodesKeepRawStateWithoutInventingDecision(String status) {
        var task = task("REVIEW").setStatus(status);
        when(tasks.selectOne(any())).thenReturn(task);
        var result = reader.readAuthorizedTask(task);
        assertEquals(status == null || status.isBlank() ? "UNKNOWN" : "UNSUPPORTED", result.taskOutcome());
        assertEquals(status, result.sourceStatus());
        assertEquals("UNKNOWN", result.decisionState());
        assertNull(result.approvalResult());
        assertNull(result.approvalRemark());
        verifyNoInteractions(transactions, decisions, applications, actors, signatures, signoff);
    }

    @Test void doneReviewAndRejectTextDoNotSelectSignatureOrAssigneeAsActor() {
        var task = task("REVIEW").setReason("REJECT: raw unlinked reason").setAssigneeUserId(999L);
        when(tasks.selectOne(any())).thenReturn(task);
        var result = reader.readAuthorizedTask(task);
        assertEquals("COMPLETED", result.taskOutcome());
        assertEquals("UNKNOWN", result.decisionState());
        assertNull(result.approvalResult());
        assertNull(result.actorUserId());
        assertNull(result.approvalRemark());
        assertEquals(task.getReason(), result.sourceRemark());
        verifyNoInteractions(signatures);
    }

    @Test void cancellationDoesNotTurnUnclassifiedReasonIntoUserOpinionOrSystemTermination() {
        var task = task("APPROVE").setStatus("CANCELED").setReason("peer task closed");
        when(tasks.selectOne(any())).thenReturn(task);
        var result = reader.readAuthorizedTask(task);
        assertEquals("CANCELLED", result.taskOutcome());
        assertNull(result.approvalResult());
        assertNull(result.approvalRemark());
        assertNull(result.terminationReason());
        assertEquals("peer task closed", result.sourceRemark());
    }

    @Test void taskSnapshotConflictFailsBeforeReadingOtherDomains() {
        var projected = task("REVIEW");
        when(tasks.selectOne(any())).thenReturn(task("REVIEW").setId(178L));
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(projected));
        verifyNoInteractions(transactions, applications, signatures);
    }

    @Test void missingTenantFailsBeforeReadingTask() {
        TenantContextHolder.clear();
        assertThrows(NullPointerException.class, () -> reader.readAuthorizedTask(task("REVIEW")));
        verifyNoInteractions(tasks, transactions, decisions, applications, signatures);
    }

    @Test void exactTaskQueriesConstrainCurrentTenantIdAndDeletion() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ac06-work"), MesProEdhrWorkTaskDO.class);
        var task = task("REVIEW");
        when(tasks.selectOne(any())).thenReturn(task);
        reader.readAuthorizedTask(task);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX> query =
                ArgumentCaptor.forClass(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class);
        verify(tasks).selectOne(query.capture());
        String sql = query.getValue().getSqlSegment();
        assertTrue(sql.contains("tenant_id ="), sql);
        assertTrue(sql.contains("id ="), sql);
        assertTrue(sql.contains("deleted ="), sql);
        assertTrue(query.getValue().getParamNameValuePairs().containsValue(1L));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue(177L));
        assertTrue(query.getValue().getParamNameValuePairs().containsValue(false));
    }

    @Test void missingPqcDecisionDoesNotReuseTaskReason() {
        var task = pqcFacts(null);
        var result = reader.readAuthorizedTask(task);
        assertEquals("UNKNOWN", result.decisionState());
        assertNull(result.approvalResult());
        assertNull(result.approvalRemark());
        verifyNoInteractions(actors, signatures);
    }

    @Test void marketExactApplicationDecisionAndVerifiedSignaturePreserveOriginalOpinion() {
        var task = marketFacts();
        var signature = mock(ElectronicSignatureEvidenceDTO.class);
        when(signature.id()).thenReturn(88L);
        when(signature.reason()).thenReturn(OPINION);
        when(signature.subjectId()).thenReturn("existing-signed-subject");
        when(signatures.getById(88L)).thenReturn(signature);
        when(actors.resolve(177L, "RELEASE_TRANSACTION", 91L, 92L, 93L)).thenReturn(188L);
        when(signoff.findVerifiedSignatureId(177L, 188L, "existing-signed-subject", "evidence", OPINION))
                .thenReturn(Optional.of(88L));
        var result = reader.readAuthorizedTask(task);
        assertEquals("COMPLETED", result.taskOutcome());
        assertEquals("RELEASED", result.businessStatus());
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.approvalResult());
        assertEquals(OPINION, result.approvalRemark());
        assertEquals(188L, result.actorUserId());
        assertEquals("94", result.evidenceId());
        assertNull(result.terminationReason());
    }

    @Test void marketOpinionAndActorRunThroughActualResolverAndSignoff() {
        // Manager task completion uses prepared.occurredAt; finalization writes its own
        // occurredAt to transaction and decision. Their exact FKs bind the action.
        var task = marketFacts().setReason("APPROVE").setCompletedAt(AT.minusSeconds(1));
        String imageHash = "a".repeat(64);
        String subject = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("EDHR\nEDHR_WORK_TASK\n177\n177\n\n" + imageHash).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String canonical = cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(java.util.Map.of(
                "adapter", "BPM_APPROVAL", "moduleCode", "EDHR", "sourceTaskType", "EDHR_WORK_TASK",
                "sourceTaskId", "177", "businessKey", "177", "processInstanceId", "",
                "reviewResult", "APPROVE", "reason", OPINION, "signatureImagePayloadHash", imageHash));
        var signature = new ElectronicSignatureEvidenceDTO(88L, "BPM", "APPROVE", "BPM_APPROVAL_TASK", subject,
                cn.hutool.crypto.digest.DigestUtil.sha256Hex(subject), 188L, "APPROVE", "批准", OPINION, AT,
                "time", "SESSION_PLUS_PASSWORD", "content", "evidence", "SHA-256", "key", "v1", "VALID",
                "", "177", "node", 1, canonical, null, null, null, "actor");
        when(signatures.getById(88L)).thenReturn(signature);
        when(signatures.listBySubject("BPM", "BPM_APPROVAL_TASK", subject)).thenReturn(java.util.List.of(signature));
        when(signatures.verifyEvidence(88L)).thenReturn(new ElectronicSignatureVerificationDTO(
                88L, "VALID", "content", "content", "evidence", "evidence", "SHA-256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK",
                "NOT_BOUND", "CONSISTENT", java.util.List.of(), AT, "V1"));
        reader = new MesEdhrApprovalOutcomeReader(tasks, transactions, decisions, applications,
                new MesManagerReleaseCompletedActorResolver(tasks, transactions), signatures,
                new MesProductionReleaseSignoffService(signatures), executionFinal);
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.approvalResult());
        assertEquals(OPINION, result.approvalRemark());
        assertEquals(188L, result.actorUserId());
        assertEquals(AT, result.actedAt());
        verify(signatures).verifyEvidence(88L);
        verifyNoInteractions(actors, signoff);
    }

    @Test void marketApplicationFromAnotherTaskFailsWithoutSelectingClosestFact() {
        var task = marketFacts();
        when(applications.selectOne(any())).thenReturn(application().setReleaseApprovalWorkTaskId(178L));
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
        verifyNoInteractions(actors, signatures, signoff);
    }

    @Test void marketMissingOriginalOpinionRemainsAbsentWithoutGeneratedText() {
        var task = task("RELEASE_APPROVE");
        when(tasks.selectOne(any())).thenReturn(task);
        when(transactions.selectOne(any())).thenReturn(transaction().setApprovalOpinion(null));
        when(decisions.selectOne(any())).thenReturn(decision().setApprovalOpinion(null));
        when(applications.selectOne(any())).thenReturn(application());
        when(actors.resolve(177L, "RELEASE_TRANSACTION", 91L, 92L, 93L)).thenReturn(188L);
        var signature = mock(ElectronicSignatureEvidenceDTO.class);
        when(signature.id()).thenReturn(88L);
        when(signature.subjectId()).thenReturn("existing-signed-subject");
        when(signatures.getById(88L)).thenReturn(signature);
        when(signoff.findVerifiedSignatureId(177L, 188L, "existing-signed-subject", "evidence", null))
                .thenReturn(Optional.of(88L));
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.approvalResult());
        assertNull(result.approvalRemark());
    }

    @Test void applicationFromAnotherTenantFails() {
        var task = task("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION").setBusinessScopeId(95L);
        when(tasks.selectOne(any())).thenReturn(task);
        var app = application();
        app.setTenantId(2L);
        when(applications.selectOne(any())).thenReturn(app);
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
        verifyNoInteractions(actors, signatures);
    }

    @Test void marketMissingExactTaskRelationKeepsBusinessStatusButNoDecision() {
        var task = task("RELEASE_APPROVE");
        when(tasks.selectOne(any())).thenReturn(task);
        when(transactions.selectOne(any())).thenReturn(transaction());
        when(decisions.selectOne(any())).thenReturn(decision().setReleaseApplicationId(null));
        var result = reader.readAuthorizedTask(task);
        assertEquals("RELEASED", result.businessStatus());
        assertEquals("UNKNOWN", result.decisionState());
        assertNull(result.approvalResult());
        assertNull(result.approvalRemark());
        verifyNoInteractions(applications, actors, signatures);
    }

    @Test void unknownMarketDecisionKeepsOriginalBusinessCodeWithoutChangingTaskCompletion() {
        var task = marketFacts();
        when(transactions.selectOne(any())).thenReturn(transaction().setReleaseStatus("FUTURE_DECISION"));
        when(decisions.selectOne(any())).thenReturn(decision().setDecisionStatus("FUTURE_DECISION"));
        var result = reader.readAuthorizedTask(task);
        assertEquals("COMPLETED", result.taskOutcome());
        assertEquals("DONE", result.sourceStatus());
        assertEquals("FUTURE_DECISION", result.businessStatus());
        assertEquals("UNSUPPORTED", result.decisionState());
        assertEquals("FUTURE_DECISION", result.decisionSourceStatus());
        assertNull(result.approvalResult());
        verifyNoInteractions(actors, signatures, signoff);
    }

    @Test void marketSignedOpinionConflictFailsInsteadOfUsingDecisionText() {
        var task = marketFacts();
        when(actors.resolve(177L, "RELEASE_TRANSACTION", 91L, 92L, 93L)).thenReturn(188L);
        var signature = mock(ElectronicSignatureEvidenceDTO.class);
        when(signature.id()).thenReturn(88L);
        when(signature.reason()).thenReturn("other action");
        when(signatures.getById(88L)).thenReturn(signature);
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
        verifyNoInteractions(signoff);
    }

    @Test void pqcRejectedApplicationUsesFormalRejectOpinionAndOriginalActor() {
        var task = pqcFacts("REJECT");
        when(actors.resolvePqc(177L, "RELEASE_APPLICATION", 95L, 92L, 93L, "REJECT", AT)).thenReturn(188L);
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.REJECT, result.approvalResult());
        assertEquals(OPINION, result.approvalRemark());
        assertEquals(188L, result.actorUserId());
        assertEquals("PQC_RELEASE_REJECTED", result.businessStatus());
        verifyNoInteractions(signatures);
    }

    @Test void pqcRejectedReceiptAndTenantActorAreValidatedByActualResolver() {
        var task = task("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(95L).setReason("REJECT");
        var app = application().setActiveOrderId(180L).setPqcReleaseWorkTaskId(177L).setPqcDecision("REJECT")
                .setPqcDecidedBy(188L).setPqcDecidedAt(AT).setPqcRejectReason(OPINION)
                .setApplicationStatus("PQC_RELEASE_REJECTED");
        app.setDossierSummaryJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(
                java.util.Map.of("applicationId", 95L, "pqcReleaseWorkTaskId", 177L,
                        "decidedBy", 188L, "decision", "REJECT", "rejectReason", OPINION)));
        when(tasks.selectOne(any())).thenReturn(task);
        when(applications.selectOne(any())).thenReturn(app);
        var user = new AdminUserDO().setId(188L).setStatus(0);
        user.setTenantId(1L);
        when(users.selectOne(any())).thenReturn(user);
        var actualActors = new MesManagerReleaseCompletedActorResolver(tasks, transactions);
        ReflectionTestUtils.setField(actualActors, "applicationMapper", applications);
        ReflectionTestUtils.setField(actualActors, "users", users);
        ReflectionTestUtils.setField(actualActors, "signatures", signatures);
        reader = new MesEdhrApprovalOutcomeReader(tasks, transactions, decisions, applications, actualActors, signatures, signoff, executionFinal);
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.REJECT, result.approvalResult());
        assertEquals(OPINION, result.approvalRemark());
        assertEquals(188L, result.actorUserId());
        verify(tasks, times(2)).selectOne(any());
        verify(applications, times(2)).selectOne(any());
        verifyNoInteractions(signatures);
        app.setDossierSummaryJson("{\"applicationId\":95,\"pqcReleaseWorkTaskId\":178,\"decidedBy\":188,\"decision\":\"REJECT\"}");
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
    }

    @Test void pqcApprovedOpinionComesFromExactReceiptSignature() {
        var task = pqcFacts("APPROVE");
        when(actors.resolvePqc(177L, "RELEASE_APPLICATION", 95L, 92L, 93L, "APPROVE", AT)).thenReturn(188L);
        var signature = mock(ElectronicSignatureEvidenceDTO.class);
        when(signature.id()).thenReturn(88L);
        when(signature.actorId()).thenReturn(188L);
        when(signature.reason()).thenReturn(OPINION);
        when(signature.subjectId()).thenReturn(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter
                .withOpinionSource(cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(
                        92L, "PQC_RELEASE", null, null, null, null, null, null, null, "PQC_RELEASE_APPLICATION", 95L,
                        "PQC生产放行", "PQC_RELEASE", null, null, null, null), OPINION));
        when(signatures.getById(88L)).thenReturn(signature);
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.approvalResult());
        assertEquals(OPINION, result.approvalRemark());
        assertEquals("MANAGER_RELEASE_PENDING", result.businessStatus());
    }

    @Test void unknownNonemptyPqcDecisionPreservesCodeAndCompletedTaskSeparately() {
        var task = pqcFacts("OTHER_DECISION");
        var result = reader.readAuthorizedTask(task);
        assertEquals("COMPLETED", result.taskOutcome());
        assertEquals("UNSUPPORTED", result.decisionState());
        assertEquals("OTHER_DECISION", result.decisionSourceStatus());
        assertEquals("DONE", result.sourceStatus());
        assertNull(result.approvalResult());
        verifyNoInteractions(actors, signatures);
    }

    @Test void qaClosureDoesNotReuseEarlierPqcOpinion() {
        var task = pqcFacts("REJECT").setReason("NONCONFORMANCE_VOID");
        var result = reader.readAuthorizedTask(task);
        assertEquals("UNKNOWN", result.decisionState());
        assertNull(result.approvalResult());
        assertNull(result.approvalRemark());
        verifyNoInteractions(actors, signatures);
    }

    @Test void handoffCompletionHasItsOwnActorAndSourceButNoApprovalDecision() {
        var task = handoff().setStatus("DONE")
                .setCompletedBy(188L).setCompletionSourceId(91L).setCompletedAt(AT).setReason("frozen source description");
        var result = reader.readAuthorizedHandoff(task);
        assertEquals("COMPLETED", result.taskOutcome());
        assertEquals(188L, result.taskCompletedByUserId());
        assertNull(result.actorUserId());
        assertNull(result.actedAt());
        assertNull(result.approvalResult());
        assertNull(result.approvalRemark());
        assertEquals(task.getReason(), result.sourceRemark());
        verifyNoInteractions(signatures);
    }

    @Test void handoffMissingCompletionIdentityOrForeignTenantFails() {
        var task = handoff().setStatus("DONE").setCompletedAt(AT);
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedHandoff(task));
        task.setTenantId(2L);
        task.setCompletedBy(188L).setCompletionSourceId(91L);
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedHandoff(task));
    }

    private static MesActiveOrderHandoffTaskDO handoff() {
        var task = new MesActiveOrderHandoffTaskDO().setId(177L).setActiveOrderId(180L).setWorkOrderId(93L)
                .setSourceId(180L).setRoundId(182L).setTaskType("PRODUCTION_HANDOFF").setSourceType("ACTIVE_ORDER")
                .setCandidateUserSnapshot("188").setActionUrl("/mes/pro/feedback/edhr-batch-production-fill"
                        + "?activeOrderId=180&cycleId=180&handoffTaskId=177&roundId=182&handoffType=PRODUCTION_HANDOFF");
        task.setTenantId(1L);
        return task;
    }

    private MesProEdhrWorkTaskDO marketFacts() {
        var task = task("RELEASE_APPROVE");
        when(tasks.selectOne(any())).thenReturn(task);
        when(transactions.selectOne(any())).thenReturn(transaction());
        when(decisions.selectOne(any())).thenReturn(decision());
        when(applications.selectOne(any())).thenReturn(application());
        return task;
    }
    private MesProEdhrWorkTaskDO pqcFacts(String decision) {
        var task = task("PQC_PRODUCTION_RELEASE").setBusinessScopeType("RELEASE_APPLICATION")
                .setBusinessScopeId(95L).setReason(decision);
        when(tasks.selectOne(any())).thenReturn(task);
        when(applications.selectOne(any())).thenReturn(application().setPqcReleaseWorkTaskId(177L)
                .setPqcDecision(decision).setPqcDecidedBy(188L).setPqcDecidedAt(AT).setPqcRejectReason(OPINION)
                .setApplicationStatus("REJECT".equals(decision) ? "PQC_RELEASE_REJECTED" : "MANAGER_RELEASE_PENDING")
                .setDossierSummaryJson("{\"signatureId\":88}"));
        return task;
    }
    private static MesProEdhrWorkTaskDO task(String type) {
        return new MesProEdhrWorkTaskDO().setId(177L).setTaskType(type).setStatus("DONE").setCompletedAt(AT)
                .setBusinessScopeType("RELEASE_TRANSACTION").setBusinessScopeId(91L)
                .setBatchExecutionId(92L).setWorkOrderId(93L).setAssigneeUserId(999L).setReason("source code");
    }
    private static MesProEdhrReleaseTransactionDO transaction() {
        return new MesProEdhrReleaseTransactionDO().setId(91L).setBatchExecutionId(92L).setWorkOrderId(93L)
                .setReleaseDecisionId(94L).setReleaseStatus("RELEASED").setApprovedBy(188L).setApprovedAt(AT)
                .setApprovalSignatureId(88L).setApprovalSignoffEvidenceHash("evidence").setApprovalOpinion(OPINION);
    }
    private static MesProEdhrReleaseDecisionDO decision() {
        return new MesProEdhrReleaseDecisionDO().setId(94L).setReleaseTransactionId(91L)
                .setBatchExecutionId(92L).setWorkOrderId(93L).setReleaseApplicationId(95L)
                .setDecisionStatus("RELEASED").setActorUserId(188L).setDecidedAt(AT).setApprovalOpinion(OPINION);
    }
    private static MesProcessPoolActiveOrderReleaseApplicationDO application() {
        var app = new MesProcessPoolActiveOrderReleaseApplicationDO().setId(95L).setActiveOrderId(180L).setReleaseTransactionId(91L)
                .setReleaseApprovalWorkTaskId(177L).setBatchExecutionId(92L).setWorkOrderId(93L);
        app.setTenantId(1L);
        return app;
    }
}
