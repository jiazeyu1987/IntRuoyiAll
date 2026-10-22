package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.ApprovalTaskReviewResult;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.manager.MesProductionReleaseSignoffService;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesEdhrExecutionFinalOutcomeReaderTest {
    @Mock MesProEdhrWorkTaskMapper tasks;
    @Mock MesProBatchRecordExecutionMapper executions;
    @Mock MesProBatchRecordApprovalSnapshotMapper snapshots;
    @Mock MesProEdhrReleaseTransactionMapper transactions;
    @Mock MesProEdhrReleaseDecisionMapper decisions;
    @Mock cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper applications;
    @Mock MesManagerReleaseCompletedActorResolver actors;
    @Mock MesProductionReleaseSignoffService signoff;
    @Mock ElectronicSignatureQueryService signatures;
    @Mock AdminUserMapper users;
    MesEdhrApprovalOutcomeReader reader;
    MesProEdhrWorkTaskDO task;
    MesProBatchRecordExecutionDO execution;
    MesProBatchRecordApprovalSnapshotDO snapshot;
    static final LocalDateTime AT = LocalDateTime.of(2026, 10, 7, 10, 0);
    static final String OPINION = "Original \"quoted\" \\ opinion";

    @BeforeEach void init() {
        TenantContextHolder.setTenantId(1L);
        reader = new MesEdhrApprovalOutcomeReader(tasks, transactions, decisions, applications, actors, signatures, signoff,
                new MesEdhrExecutionFinalOutcomeReader(executions, snapshots, tasks, signatures, users));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void finalApprovalReadsExactSnapshotSignatureAndOriginalActor() {
        facts(true, OPINION); validIdentity();
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.approvalResult());
        assertEquals(OPINION, result.approvalRemark());
        assertEquals(188L, result.actorUserId());
        assertEquals(AT, result.actedAt());
        assertEquals("3", result.businessStatus());
        assertEquals("DONE", result.sourceStatus());
        assertEquals("302", result.evidenceId());
        verify(signatures).getById(88L);
        verify(signatures).verifyEvidence(88L);
        verifyNoInteractions(actors, signoff, transactions, decisions, applications);
    }

    @Test void finalReviewRejectionReadsExactSnapshotAndOriginalRejectReason() {
        facts(false, OPINION); validIdentity();
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.REJECT, result.approvalResult());
        assertEquals(OPINION, result.approvalRemark());
        assertEquals("2", result.businessStatus());
        assertEquals(188L, result.actorUserId());
    }

    @Test void disabledHistoricalFinalSignerKeepsValidEvidenceWhileForeignTenantStillFails() {
        facts(true, OPINION); validIdentity();
        var user = new AdminUserDO().setId(188L).setStatus(1); user.setTenantId(1L);
        when(users.selectOne(any())).thenReturn(user);
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.APPROVE,result.approvalResult());
        assertEquals(188L,result.taskCompletedByUserId());
        assertEquals(OPINION,result.approvalRemark());
        user.setTenantId(2L);
        assertThrows(IllegalStateException.class,() -> reader.readAuthorizedTask(task));
    }

    @Test void earlierReviewNeverReceivesFinalApproveDecision() {
        facts(true, OPINION, false);
        task.setTaskType("REVIEW");
        var result = reader.readAuthorizedTask(task);
        assertUnknownWithSnapshot(result);
        verifyNoInteractions(signatures, users);
    }

    @Test void differentBpmTaskRetainsSnapshotEvidenceButNoTaskDecision() {
        facts(true, OPINION);
        task.setBpmTaskId("historical-task");
        assertUnknownWithSnapshot(reader.readAuthorizedTask(task));
        verify(tasks, never()).selectList(any());
        verifyNoInteractions(users);
    }

    @Test void differentCellCannotConsumeOtherTasksFinalSignature() {
        facts(false, OPINION);
        task.setSignatureCellKey("previous-cell");
        assertUnknownWithSnapshot(reader.readAuthorizedTask(task));
        verifyNoInteractions(users);
    }

    @Test void differentReviewSourceRoundCannotConsumeOtherTasksFinalSignature() {
        facts(false, OPINION);
        task.setReviewSourceId(999L);
        assertUnknownWithSnapshot(reader.readAuthorizedTask(task));
        verifyNoInteractions(users);
    }

    @Test void duplicateExactTaskTupleIsUnknownRatherThanFirstCandidateOrAssignee() {
        facts(false, OPINION);
        var duplicate = task("REVIEW").setId(178L).setAssigneeUserId(188L);
        when(tasks.selectList(any())).thenReturn(List.of(task, duplicate));
        assertUnknownWithSnapshot(reader.readAuthorizedTask(task));
        verifyNoInteractions(users);
    }

    @Test void missingBpmTaskIdentityCannotProveUniqueTaskDecision() {
        facts(true, OPINION);
        task.setBpmTaskId(null);
        assertUnknownWithSnapshot(reader.readAuthorizedTask(task));
        verifyNoInteractions(users);
    }

    @Test void missingFinalSignatureIdKeepsFormalSnapshotEvidence() {
        facts(true, OPINION, false);
        snapshot.setApproveSignatureId(null);
        assertUnknownWithSnapshot(reader.readAuthorizedTask(task));
        verifyNoInteractions(signatures, users);
    }

    @Test void unavailableSignatureDoesNotUseSnapshotStatusAsOpinion() {
        facts(true, OPINION, false);
        assertUnknownWithSnapshot(reader.readAuthorizedTask(task));
        verifyNoInteractions(users);
    }

    @Test void oldWriterGeneratedActionReasonDoesNotBecomeUserOpinion() {
        facts(true, "APPROVE", true, false); validIdentity();
        var result = reader.readAuthorizedTask(task);
        assertEquals(ApprovalTaskReviewResult.APPROVE, result.approvalResult());
        assertNull(result.approvalRemark());
        assertEquals("APPROVE", result.sourceRemark());
    }

    @Test void foreignSnapshotExecutionFailsBeforeSignatureRead() {
        facts(true, OPINION, false);
        snapshot.setExecutionId(999L);
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
        verifyNoInteractions(signatures, users);
    }

    @Test void snapshotProcessMustMatchPersistedExecution() {
        facts(true, OPINION, false);
        snapshot.setProcessInstanceId("another-process");
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
        verifyNoInteractions(signatures, users);
    }

    @Test void refetchedTaskIdentityChangesFailBeforeReadingExecution() {
        task = task("APPROVE");
        when(tasks.selectOne(any())).thenReturn(task("APPROVE").setSignatureColumnIndex(77));
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
        verifyNoInteractions(executions, snapshots, signatures);
    }

    @Test void refetchedTaskCandidateSnapshotChangeFailsBeforeReadingExecution() {
        task = task("APPROVE").setCandidateUserSnapshot("188");
        when(tasks.selectOne(any())).thenReturn(task("APPROVE").setCandidateUserSnapshot("999"));
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
        verifyNoInteractions(executions, snapshots, signatures);
    }

    @Test void exactSourceQueriesRequireTenantDeletionAndNeverLatestOrLimit() {
        for (Class<?> type : List.of(MesProBatchRecordExecutionDO.class, MesProBatchRecordApprovalSnapshotDO.class,
                MesProEdhrWorkTaskDO.class, AdminUserDO.class)) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                    new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ac06-final-" + type.getName()), type);
        }
        facts(true, OPINION); validIdentity();
        reader.readAuthorizedTask(task);
        @SuppressWarnings("rawtypes")
        var executionQuery = org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class);
        @SuppressWarnings("rawtypes")
        var snapshotQuery = org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class);
        @SuppressWarnings("rawtypes")
        var siblingQuery = org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX.class);
        verify(executions).selectOne(executionQuery.capture());
        verify(snapshots).selectOne(snapshotQuery.capture());
        verify(tasks).selectList(siblingQuery.capture());
        for (var query : List.of(executionQuery.getValue(), snapshotQuery.getValue(), siblingQuery.getValue())) {
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("tenant_id ="), sql);
            assertTrue(sql.contains("deleted ="), sql);
            assertFalse(sql.toUpperCase().contains("LIMIT"), sql);
            assertFalse(sql.toUpperCase().contains("ORDER BY"), sql);
            assertTrue(query.getParamNameValuePairs().containsValue(1L));
            assertTrue(query.getParamNameValuePairs().containsValue(301L));
        }
        assertTrue(snapshotQuery.getValue().getSqlSegment().contains("execution_id ="));
        assertTrue(siblingQuery.getValue().getSqlSegment().contains("task_type ="));
        assertTrue(siblingQuery.getValue().getParamNameValuePairs().containsValue("APPROVE"));
    }

    @Test void unsupportedSnapshotCodePreservedSeparatelyFromTaskCompletion() {
        facts(true, OPINION, false);
        snapshot.setApprovalStatus("FUTURE_DECISION");
        var result = reader.readAuthorizedTask(task);
        assertEquals("COMPLETED", result.taskOutcome());
        assertEquals("UNSUPPORTED", result.decisionState());
        assertEquals("FUTURE_DECISION", result.decisionSourceStatus());
        assertEquals("DONE", result.sourceStatus());
        assertNull(result.approvalResult());
        verifyNoInteractions(signatures, users);
    }

    @Test void sourceTenantActorMismatchFailsInsteadOfTakingTaskAssignee() {
        facts(true, OPINION);
        when(tasks.selectList(any())).thenReturn(List.of(task));
        var foreign = new AdminUserDO().setId(188L).setStatus(0);
        foreign.setTenantId(2L);
        when(users.selectOne(any())).thenReturn(foreign);
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
        verify(signatures, never()).verifyEvidence(any());
    }

    @Test void rejectOpinionConflictIsNotRepairedWithSnapshotOrTaskText() {
        facts(false, OPINION); validIdentity();
        snapshot.setRejectReason("different original reason");
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
    }

    @Test void tamperedFinalSignatureCannotBecomeApprovalResult() {
        facts(true, OPINION); validIdentity();
        when(signatures.verifyEvidence(88L)).thenReturn(new ElectronicSignatureVerificationDTO(
                88L, "MISMATCH", "content", "changed", "evidence", "evidence", "SHA-256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "MISMATCH", "CONSISTENT", "SERVER_CLOCK",
                "NOT_BOUND", "CONSISTENT", List.of("DIGEST_MISMATCH"), AT, "V1"));
        assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
    }

    @Test void malformedMultilineV1ContentFailsWithoutRepairingHistoricalWriter() {
        // Historical raw-linefeed JSON remains invalid even though the current writer
        // now escapes control characters. Never rewrite stored bytes to repair evidence.
        facts(true, "line one\nline two"); validIdentity();
        var original = signatures.getById(88L);
        when(signatures.getById(88L)).thenReturn(new ElectronicSignatureEvidenceDTO(
                original.id(), original.moduleCode(), original.actionCode(), original.subjectType(),
                original.subjectId(), original.subjectVersion(), original.actorId(), original.meaningCode(),
                original.meaningLabel(), original.reason(), original.signedAt(), original.timeEvidenceId(),
                original.authenticationMethod(), original.contentHash(), original.evidenceHash(), original.algorithm(),
                original.keyVersion(), original.policyVersion(), original.verificationStatus(), original.processInstanceId(),
                original.taskId(), original.nodeCode(), original.nodeOrder(),
                original.canonicalContentJson().replace("\\n", "\n"), original.beforeContentJson(),
                original.afterContentJson(), original.fieldDiffJson(), original.actorDisplayName(), original.timeZone()));
        assertThrows(RuntimeException.class, () -> reader.readAuthorizedTask(task));
    }

    @Test void selfConsistentSignatureCannotApproveDifferentFormalAuditTuple() {
        for (boolean approve : List.of(true, false)) {
            for (String field : List.of("revision", "head", "cells")) {
                facts(approve, OPINION);
                switch (field) {
                    case "revision" -> execution.setFieldAuditRevision(8L);
                    case "head" -> execution.setFieldAuditHeadHash("other-head");
                    case "cells" -> execution.setCellValuesHash("other-cells");
                }
                var error = assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
                assertTrue(error.getMessage().contains("audit/node"));
            }
        }
        verifyNoInteractions(users);
        verify(signatures, never()).verifyEvidence(any());
    }

    @Test void selfConsistentSignatureMustMatchNodeAndFormalDecisionTime() {
        for (boolean approve : List.of(true, false)) {
            for (boolean wrongNode : List.of(true, false)) {
                facts(approve, OPINION);
                var original = signatures.getById(88L);
                when(signatures.getById(88L)).thenReturn(withNodeAndTime(original,
                        wrongNode ? "other-node" : original.nodeCode(), wrongNode ? AT : AT.minusSeconds(1)));
                assertThrows(IllegalStateException.class, () -> reader.readAuthorizedTask(task));
            }
        }
        verifyNoInteractions(users);
        verify(signatures, never()).verifyEvidence(any());
    }

    @Test void missingFormalAuditOrSignatureNodeTimeRemainsUnknown() {
        for (String field : List.of("revision", "head", "cells", "node", "time")) {
            facts(true, OPINION);
            var original = signatures.getById(88L);
            switch (field) {
                case "revision" -> execution.setFieldAuditRevision(null);
                case "head" -> execution.setFieldAuditHeadHash(null);
                case "cells" -> execution.setCellValuesHash(null);
                case "node" -> when(signatures.getById(88L)).thenReturn(withNodeAndTime(original, null, AT));
                case "time" -> when(signatures.getById(88L)).thenReturn(withNodeAndTime(original, "node", null));
            }
            assertUnknownWithSnapshot(reader.readAuthorizedTask(task));
        }
        verifyNoInteractions(users);
        verify(signatures, never()).verifyEvidence(any());
    }

    private static ElectronicSignatureEvidenceDTO withNodeAndTime(ElectronicSignatureEvidenceDTO original,
            String node, LocalDateTime time) {
        return new ElectronicSignatureEvidenceDTO(original.id(), original.moduleCode(), original.actionCode(),
                original.subjectType(), original.subjectId(), original.subjectVersion(), original.actorId(),
                original.meaningCode(), original.meaningLabel(), original.reason(), time, original.timeEvidenceId(),
                original.authenticationMethod(), original.contentHash(), original.evidenceHash(), original.algorithm(),
                original.keyVersion(), original.policyVersion(), original.verificationStatus(), original.processInstanceId(),
                original.taskId(), node, original.nodeOrder(), original.canonicalContentJson(), original.beforeContentJson(),
                original.afterContentJson(), original.fieldDiffJson(), original.actorDisplayName(), original.timeZone());
    }

    private void facts(boolean approve, String opinion) { facts(approve, opinion, true); }
    private void facts(boolean approve, String opinion, boolean withSignature) {
        facts(approve, opinion, withSignature, true);
    }
    private void facts(boolean approve, String opinion, boolean withSignature, boolean sourceRecorded) {
        String action = approve ? "APPROVE" : "REJECT";
        task = task(approve ? "APPROVE" : "REVIEW");
        execution = new MesProBatchRecordExecutionDO().setId(301L).setWorkOrderId(93L).setBatchExecutionId(92L)
                .setFieldAuditRevision(7L).setFieldAuditHeadHash("head").setCellValuesHash("cells")
                .setProcessInstanceId("process-1").setStatus(approve ? 3 : 2)
                .setApprovedBy(188L).setApprovedAt(AT).setRejectedBy(188L).setRejectedAt(AT).setRejectReason(opinion);
        snapshot = new MesProBatchRecordApprovalSnapshotDO().setId(302L).setExecutionId(301L)
                .setProcessInstanceId("process-1").setApprovalStatus(approve ? "APPROVED" : "REJECTED")
                .setApproveSignatureId(88L).setRejectSignatureId(88L).setApprovedBy(188L).setApprovedAt(AT)
                .setRejectedBy(188L).setRejectedAt(AT).setRejectReason(opinion);
        when(tasks.selectOne(any())).thenReturn(task);
        when(executions.selectOne(any())).thenReturn(execution);
        when(snapshots.selectOne(any())).thenReturn(snapshot);
        if (!withSignature) return;
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(301L, action, "process-1", "bpm-401",
                "node", "approval", "cell", 2, 3, "SOURCE", 403L, "source", action, 7L, "head", "cells", null);
        if (approve && sourceRecorded) subject = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(subject, opinion);
        String version = cn.hutool.crypto.digest.DigestUtil.sha256Hex(subject);
        var canonical = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(
                new SignatureSubjectCommand(188L, "MES", action, "MES_BATCH_RECORD", subject, version, opinion));
        var signature = new ElectronicSignatureEvidenceDTO(88L, "MES", action, "MES_BATCH_RECORD", subject, version,
                188L, action, action, opinion, AT, "time", "SESSION_PLUS_PASSWORD", "content", "evidence",
                "SHA-256", "key", "mes-batch-record-signature-v1", "VALID", "process-1", "bpm-401", "node", 1,
                canonical.canonicalContentJson(), null, null, null, "actor");
        when(signatures.getById(88L)).thenReturn(signature);
    }
    private void validIdentity() {
        when(tasks.selectList(any())).thenReturn(List.of(task));
        var user = new AdminUserDO().setId(188L).setStatus(0);
        user.setTenantId(1L);
        when(users.selectOne(any())).thenReturn(user);
        when(signatures.verifyEvidence(88L)).thenReturn(new ElectronicSignatureVerificationDTO(
                88L, "VALID", "content", "content", "evidence", "evidence", "SHA-256", "v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK",
                "NOT_BOUND", "CONSISTENT", List.of(), AT, "V1"));
    }
    private static MesProEdhrWorkTaskDO task(String type) {
        return new MesProEdhrWorkTaskDO().setId(177L).setTaskType(type).setStatus("DONE").setCompletedAt(AT.minusSeconds(1))
                .setExecutionId(301L).setBatchExecutionId(92L).setWorkOrderId(93L).setBpmTaskId("bpm-401")
                .setSignatureCellKey("cell").setSignatureRowIndex(2).setSignatureColumnIndex(3)
                .setReviewSourceType("SOURCE").setReviewSourceId(403L).setReviewSourceName("source").setAssigneeUserId(999L);
    }
    private static void assertUnknownWithSnapshot(MesEdhrApprovalOutcome result) {
        assertEquals("UNKNOWN", result.decisionState());
        assertNull(result.approvalResult());
        assertNull(result.approvalRemark());
        assertNull(result.actorUserId());
        assertEquals("EXISTING_EXECUTION_FINAL_SNAPSHOT", result.evidenceType());
        assertEquals("302", result.evidenceId());
    }
}
