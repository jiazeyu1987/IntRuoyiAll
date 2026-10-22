package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureEvidenceDTO;
import cn.iocoder.yudao.module.signature.api.dto.ElectronicSignatureVerificationDTO;
import cn.iocoder.yudao.module.signature.api.dto.SignatureSubjectCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Explicit subject contracts; the real writer/listener chain is covered by the service test. */
class MesEdhrFinalApprovalReceiptVerifierTest {
    private final ElectronicSignatureQueryService signatures = mock(ElectronicSignatureQueryService.class);
    private final MesEdhrFinalApprovalReceiptVerifier verifier = new MesEdhrFinalApprovalReceiptVerifier(
            signatures, mock(MesProEdhrWorkTaskMapper.class));
    private final MesProBatchRecordExecutionDO execution = new MesProBatchRecordExecutionDO().setId(9001L)
            .setProcessInstanceId("process-final").setFieldAuditRevision(7L)
            .setFieldAuditHeadHash("head").setCellValuesHash("cells");
    private final MesProEdhrWorkTaskDO task = new MesProEdhrWorkTaskDO().setExecutionId(9001L)
            .setTaskType("APPROVE").setBpmTaskId("task-final").setSignatureCellKey("R1C1").setSignatureRowIndex(1)
            .setSignatureColumnIndex(1).setReviewSourceType("POST").setReviewSourceId(7001L).setReviewSourceName("QA");
    private final LocalDateTime signedAt = LocalDateTime.of(2026, 10, 6, 12, 1, 2);

    @Test
    void acceptsOriginal17AndExplicitUserAndSystem18UsingTheirFullCanonicalContent() {
        String oldSubject = originalSubject();
        for (String subject : List.of(oldSubject,
                MesBatchRecordSignatureSubjectAdapter.withOpinionSource(oldSubject, "APPROVE"),
                MesBatchRecordSignatureSubjectAdapter.withOpinionSource(oldSubject, null))) {
            var evidence = evidence(subject, "APPROVE", canonical(subject, "APPROVE"));
            bind(evidence);
            assertSame(evidence, verifier.requireSignature(execution, task, 2301L, 88L));
        }
    }

    @Test
    void rejectsUnknownSourceInvalidLengthsAndSourceReasonConflicts() {
        String oldSubject = originalSubject();
        String user = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(oldSubject, "APPROVE");
        String system = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(oldSubject, null);
        String userCanonical = canonical(user, "APPROVE");
        for (var evidence : List.of(
                evidence(append(oldSubject, "MES_OPINION_SOURCE_V1:UNKNOWN"), "APPROVE", userCanonical),
                evidence(append(user, "extra"), "APPROVE", userCanonical),
                evidence(user, "", userCanonical),
                evidence(system, "用户意见", userCanonical))) {
            bind(evidence);
            assertThrows(RuntimeException.class, () -> verifier.requireSignature(execution, task, 2301L, 88L));
        }
    }

    @Test
    void rejectsSourceSwapEvenWhenBaseIdentityReasonAndVerificationHashesAgree() {
        String oldSubject = originalSubject();
        String user = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(oldSubject, "APPROVE");
        String system = MesBatchRecordSignatureSubjectAdapter.withOpinionSource(oldSubject, null);
        bind(evidence(system, "APPROVE", canonical(user, "APPROVE")));
        assertThrows(IllegalStateException.class, () -> verifier.requireSignature(execution, task, 2301L, 88L));
    }

    private String originalSubject() {
        return MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(9001L, "APPROVE", "process-final", "task-final",
                "approveNode", "审批", "R1C1", 1, 1, "POST", 7001L, "QA", "APPROVE", 7L, "head", "cells", "");
    }

    private String canonical(String subject, String reason) {
        return canonical(subject, reason, "APPROVE");
    }

    private String canonical(String subject, String reason, String action) {
        return new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(88L,
                "MES", action, "MES_BATCH_RECORD", subject, MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject),
                reason)).canonicalContentJson();
    }

    private String append(String subject, String extra) {
        String base = new String(Base64.getUrlDecoder().decode(subject), StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((base + "\n" + extra).getBytes(StandardCharsets.UTF_8));
    }

    private ElectronicSignatureEvidenceDTO evidence(String subject, String reason, String canonical) {
        return new ElectronicSignatureEvidenceDTO(2301L, "MES", "APPROVE", "MES_BATCH_RECORD", subject,
                MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), 88L, "APPROVE", "批记录审批通过", reason,
                signedAt, "SERVER_CLOCK:" + signedAt, "SESSION_PLUS_PASSWORD", "content", "evidence", "SHA-256",
                "system-local-v1", "mes-batch-record-signature-v1", "VALID", "process-final", "task-final", "approveNode", 2,
                canonical, null, null, null, "审批人", "Asia/Shanghai");
    }

    private void bind(ElectronicSignatureEvidenceDTO evidence) {
        when(signatures.getById(2301L)).thenReturn(evidence);
        when(signatures.verifyEvidence(2301L)).thenReturn(new ElectronicSignatureVerificationDTO(2301L, "VALID",
                "content", "content", "evidence", "evidence", "SHA-256", "system-local-v1",
                "LOCAL_DIGEST_TIME_AND_CREATION_AUDIT", "CONSISTENT", "CONSISTENT", "SERVER_CLOCK",
                "UNTRUSTED_EXTERNAL_TIME", "CONSISTENT", List.of(), signedAt, "V1"));
    }

    @Test
    void approveEntryRejectsOriginal17RejectReceipt() {
        // A narrow negative contract control, not proof of real signing or audit validity.
        String reason = "批记录需补正";
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(9001L, "REJECT",
                "process-final", "task-final", "approveNode", "审批", "R1C1", 1, 1,
                "POST", 7001L, "QA", "REJECT", 7L, "head", "cells", "");
        org.junit.jupiter.api.Assertions.assertEquals(17,
                new String(java.util.Base64.getUrlDecoder().decode(subject),
                        java.nio.charset.StandardCharsets.UTF_8).split("\\n", -1).length);
        String canonical = new MesBatchRecordSignatureSubjectAdapter().loadAndAuthorize(new SignatureSubjectCommand(
                88L, "MES", "REJECT", "MES_BATCH_RECORD", subject,
                MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), reason)).canonicalContentJson();
        var reject = new ElectronicSignatureEvidenceDTO(2301L, "MES", "REJECT", "MES_BATCH_RECORD", subject,
                MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), 88L, "REJECT", "批记录审批驳回", reason,
                signedAt, "SERVER_CLOCK:" + signedAt, "SESSION_PLUS_PASSWORD", "content", "evidence", "SHA-256",
                "system-local-v1", "mes-batch-record-signature-v1", "VALID", "process-final", "task-final",
                "approveNode", 2, canonical, null, null, null, "审批人", "Asia/Shanghai");
        task.setTaskType("APPROVE");
        when(signatures.getById(2301L)).thenReturn(reject);
        var error = assertThrows(IllegalStateException.class,
                () -> verifier.requireSignature(execution, task, 2301L, 88L));
        org.junit.jupiter.api.Assertions.assertEquals(
                "EDHR_FINAL_APPROVAL_RECEIPT_INVALID: signature action/actor/process/task conflict", error.getMessage());
        org.mockito.Mockito.verify(signatures, org.mockito.Mockito.never()).verifyEvidence(2301L);
    }

    @Test
    void acceptsOriginal17RejectReceiptOnlyAtReviewEntry() {
        String subject = rejectSubject();
        var evidence = rejectEvidence(subject, "批记录需补正", canonical(subject, "批记录需补正", "REJECT"));
        task.setTaskType("REVIEW");
        bind(evidence);
        assertSame(evidence, verifier.requireRejectSignature(execution, task, 2301L, 88L));
    }

    @ParameterizedTest(name = "rejectReceiptRejectsTampering_{0}")
    @EnumSource(RejectReceiptMutation.class)
    void rejectEntryRejectsEveryReceiptBindingMismatch(RejectReceiptMutation mutation) {
        String subject = rejectSubject();
        String reason = "批记录需补正";
        String canonical = canonical(subject, reason, "REJECT");
        var base = rejectEvidence(subject, reason, canonical);
        task.setTaskType(mutation == RejectReceiptMutation.TASK_ROLE ? "APPROVE" : "REVIEW");
        var tampered = switch (mutation) {
            case ID -> copy(base, 2302L, null, null, null, null, null, null, null, null, null, null, null);
            case ACTOR -> copy(base, null, 89L, null, null, null, null, null, null, null, null, null, null);
            case MODULE -> copy(base, null, null, "OTHER", null, null, null, null, null, null, null, null, null);
            case SUBJECT_TYPE -> copy(base, null, null, null, "OTHER", null, null, null, null, null, null, null, null);
            case ACTION -> copy(base, null, null, null, null, "APPROVE", null, null, null, null, null, null, null);
            case STATUS -> copy(base, null, null, null, null, null, "INVALID", null, null, null, null, null, null);
            case POLICY -> copy(base, null, null, null, null, null, null, "policy-v2", null, null, null, null, null);
            case AUTHENTICATION -> copy(base, null, null, null, null, null, null, null, "SESSION", null, null, null, null);
            case SIGNED_AT -> copySignedAt(base);
            case PROCESS -> copy(base, null, null, null, null, null, null, null, null, "other-process", null, null, null);
            case TASK -> copy(base, null, null, null, null, null, null, null, null, null, "other-task", null, null);
            case TASK_ROLE -> base;
            case SUBJECT_EXECUTION -> copySubject(base, subjectPart(subject, 0, "9002"), null, null);
            case SUBJECT_ACTION -> copySubject(base, subjectPart(subject, 1, "APPROVE"), null, null);
            case SUBJECT_PROCESS -> copySubject(base, subjectPart(subject, 2, "other-process"), null, null);
            case SUBJECT_TASK -> copySubject(base, subjectPart(subject, 3, "other-task"), null, null);
            case SUBJECT_CELL -> copySubject(base, subjectPart(subject, 6, "OTHER_CELL"), null, null);
            case SUBJECT_SOURCE -> copySubject(base, subjectPart(subject, 9, "DEPT"), null, null);
            case SUBJECT_REVISION -> copySubject(base, subjectPart(subject, 13, "8"), null, null);
            case SUBJECT_HEAD -> copySubject(base, subjectPart(subject, 14, "other-head"), null, null);
            case SUBJECT_CELLS -> copySubject(base, subjectPart(subject, 15, "other-cells"), null, null);
            case SUBJECT_RESULT -> copySubject(base, subjectPart(subject, 12, "APPROVE"), null, null);
            case SUBJECT_VERSION -> copySubject(base, null, "other-version", null);
            case CANONICAL_CONTENT -> copySubject(base, null, null, "{\"different\":true}");
            case CONTENT_HASH -> copy(base, null, null, null, null, null, null, null, null, null, null, "other-content", null);
            case EVIDENCE_HASH -> copy(base, null, null, null, null, null, null, null, null, null, null, null, "other-evidence");
        };
        bind(tampered);
        assertThrows(RuntimeException.class, () -> verifier.requireRejectSignature(execution, task, 2301L, 88L));
    }

    private String rejectSubject() {
        return MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(9001L, "REJECT", "process-final", "task-final",
                "approveNode", "审批", "R1C1", 1, 1, "POST", 7001L, "QA", "REJECT", 7L, "head", "cells", "");
    }

    private String subjectPart(String subject, int index, String value) {
        String[] parts = new String(Base64.getUrlDecoder().decode(subject), StandardCharsets.UTF_8).split("\n", -1);
        parts[index] = value;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(String.join("\n", parts).getBytes(StandardCharsets.UTF_8));
    }

    private ElectronicSignatureEvidenceDTO copySubject(ElectronicSignatureEvidenceDTO value, String subject,
            String version, String canonical) {
        return new ElectronicSignatureEvidenceDTO(value.id(), value.moduleCode(), value.actionCode(), value.subjectType(),
                subject == null ? value.subjectId() : subject, version == null ? value.subjectVersion() : version,
                value.actorId(), value.meaningCode(), value.meaningLabel(), value.reason(), value.signedAt(),
                value.timeEvidenceId(), value.authenticationMethod(), value.contentHash(), value.evidenceHash(),
                value.algorithm(), value.keyVersion(), value.policyVersion(), value.verificationStatus(),
                value.processInstanceId(), value.taskId(), value.nodeCode(), value.nodeOrder(),
                canonical == null ? value.canonicalContentJson() : canonical, value.beforeContentJson(),
                value.afterContentJson(), value.fieldDiffJson(), value.actorDisplayName(), value.timeZone());
    }

    private ElectronicSignatureEvidenceDTO copySignedAt(ElectronicSignatureEvidenceDTO value) {
        return new ElectronicSignatureEvidenceDTO(value.id(), value.moduleCode(), value.actionCode(), value.subjectType(),
                value.subjectId(), value.subjectVersion(), value.actorId(), value.meaningCode(), value.meaningLabel(),
                value.reason(), null, value.timeEvidenceId(), value.authenticationMethod(), value.contentHash(),
                value.evidenceHash(), value.algorithm(), value.keyVersion(), value.policyVersion(),
                value.verificationStatus(), value.processInstanceId(), value.taskId(), value.nodeCode(),
                value.nodeOrder(), value.canonicalContentJson(), value.beforeContentJson(), value.afterContentJson(),
                value.fieldDiffJson(), value.actorDisplayName(), value.timeZone());
    }

    private ElectronicSignatureEvidenceDTO copy(ElectronicSignatureEvidenceDTO value, Long id, Long actor,
            String module, String subjectType, String action, String status, String policy, String authentication,
            String process, String taskId, String contentHash, String evidenceHash) {
        return new ElectronicSignatureEvidenceDTO(id == null ? value.id() : id, module == null ? value.moduleCode() : module,
                action == null ? value.actionCode() : action, subjectType == null ? value.subjectType() : subjectType,
                value.subjectId(), value.subjectVersion(), actor == null ? value.actorId() : actor, value.meaningCode(),
                value.meaningLabel(), value.reason(), value.signedAt(), value.timeEvidenceId(),
                authentication == null ? value.authenticationMethod() : authentication,
                contentHash == null ? value.contentHash() : contentHash,
                evidenceHash == null ? value.evidenceHash() : evidenceHash, value.algorithm(), value.keyVersion(),
                policy == null ? value.policyVersion() : policy, status == null ? value.verificationStatus() : status,
                process == null ? value.processInstanceId() : process, taskId == null ? value.taskId() : taskId,
                value.nodeCode(), value.nodeOrder(), value.canonicalContentJson(), value.beforeContentJson(),
                value.afterContentJson(), value.fieldDiffJson(), value.actorDisplayName(), value.timeZone());
    }

    private ElectronicSignatureEvidenceDTO rejectEvidence(String subject, String reason, String canonical) {
        return new ElectronicSignatureEvidenceDTO(2301L, "MES", "REJECT", "MES_BATCH_RECORD", subject,
                MesBatchRecordSignatureSubjectAdapter.subjectVersion(subject), 88L, "REJECT", "批记录审批驳回", reason,
                signedAt, "SERVER_CLOCK:" + signedAt, "SESSION_PLUS_PASSWORD", "content", "evidence", "SHA-256",
                "system-local-v1", "mes-batch-record-signature-v1", "VALID", "process-final", "task-final",
                "approveNode", 2, canonical, null, null, null, "审批人", "Asia/Shanghai");
    }

    private enum RejectReceiptMutation {
        ID, ACTOR, MODULE, SUBJECT_TYPE, ACTION, STATUS, POLICY, AUTHENTICATION, SIGNED_AT, PROCESS, TASK, TASK_ROLE,
        SUBJECT_EXECUTION, SUBJECT_ACTION, SUBJECT_PROCESS, SUBJECT_TASK, SUBJECT_CELL, SUBJECT_SOURCE,
        SUBJECT_REVISION, SUBJECT_HEAD, SUBJECT_CELLS, SUBJECT_RESULT, SUBJECT_VERSION, CANONICAL_CONTENT,
        CONTENT_HASH, EVIDENCE_HASH
    }
}
