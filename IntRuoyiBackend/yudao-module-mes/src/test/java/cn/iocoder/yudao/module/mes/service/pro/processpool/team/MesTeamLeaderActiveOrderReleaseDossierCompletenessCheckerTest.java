package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerTest {

    private final MesTeamLeaderActiveOrderReleaseDossierCompletenessChecker checker =
            new MesTeamLeaderActiveOrderReleaseDossierCompletenessCheckerImpl();

    @Test
    void shouldCompleteOnlyWhenThreeFormalDocumentsAuditsSourcesSignaturesAndOwnerAreComplete() {
        MesTeamLeaderActiveOrderReleaseDossierCompletenessResult result = checker.check(completeCommand());

        assertTrue(result.isComplete());
        assertTrue(result.getBlockers().isEmpty());
    }

    @Test
    void shouldValidateEveryFormalDocumentWhenOneTypeHasMultipleProcessReports() {
        MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand command = completeCommand();
        command.getDocuments().add(document("BATCH_RECORD", 9904L, 9914L));

        MesTeamLeaderActiveOrderReleaseDossierCompletenessResult result = checker.check(command);

        assertTrue(result.isComplete());
        assertTrue(result.getBlockers().isEmpty());
    }

    @Test
    void shouldReturnSpecificBlockersWithoutCreatingWorkTaskWhenAnyDocumentOrOwnerIsIncomplete() {
        MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand command = completeCommand();
        MesTeamLeaderActiveOrderReleaseDocumentEvidence loss = command.getDocuments().stream()
                .filter(document -> "LOSS_REPORT".equals(document.getDocumentType()))
                .findFirst()
                .orElseThrow();
        loss.setFieldAuditIds(List.of());
        loss.setAuditedRequiredFieldCount(1);
        loss.setSourceConsistent(false);
        command.setReleaseApprovalRuleId(null);
        command.setReleaseOwnerCandidateUserIds(List.of());

        MesTeamLeaderActiveOrderReleaseDossierCompletenessResult result = checker.check(command);

        assertFalse(result.isComplete());
        assertTrue(result.getBlockers().stream().anyMatch(blocker ->
                "DOSSIER_COMPLETENESS_BLOCKED".equals(blocker.getBlockerType())
                        && "LOSS_REPORT".equals(blocker.getObjectType())));
        assertTrue(result.getBlockers().stream().anyMatch(blocker ->
                "LOSS_SOURCE_REQUIRED".equals(blocker.getBlockerType())
                        && "LOSS_REPORT".equals(blocker.getObjectType())));
        assertTrue(result.getBlockers().stream().anyMatch(blocker ->
                "RELEASE_OWNER_REQUIRED".equals(blocker.getBlockerType())));
    }

    private static MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand completeCommand() {
        List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> documents = new ArrayList<>();
        documents.add(document("BATCH_RECORD", 9901L, 9911L));
        documents.add(document("PROCESS_INSPECTION", 9902L, 9912L));
        documents.add(document("LOSS_REPORT", 9903L, 9913L));
        return new MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand()
                .setBatchExecutionId(9701L)
                .setSourceSnapshotHash("AO_RELEASE_SOURCE_V1:complete")
                .setDocuments(documents)
                .setReleaseApprovalRuleId(8801L)
                .setReleaseOwnerCandidateUserIds(List.of(3001L));
    }

    private static MesTeamLeaderActiveOrderReleaseDocumentEvidence document(String type,
                                                                             Long executionId,
                                                                             Long fieldAuditId) {
        return new MesTeamLeaderActiveOrderReleaseDocumentEvidence()
                .setDocumentType(type)
                .setBatchExecutionId(9701L)
                .setBatchExecutionTaskId(9800L + executionId - 9900L)
                .setBatchRecordExecutionIds(List.of(executionId))
                .setTargetReportIds(List.of("REPORT-" + type))
                .setTargetDefinitionIds(List.of(400L))
                .setTargetVersionIds(List.of(401L))
                .setTargetSnapshotHashes(List.of("snapshot-" + type))
                .setFieldAuditIds(List.of(fieldAuditId))
                .setRequiredFieldCount(2)
                .setAuditedRequiredFieldCount(2)
                .setSourceObjectIds(List.of(1001L, 1002L))
                .setSourceValueHashes(List.of("source-hash-1", "source-hash-2"))
                .setSignatureEvidence(List.of(signature("FILLER", 1101L, 2101L),
                        signature("REVIEWER", 1201L, 3001L)))
                .setSourceSnapshotHash("AO_RELEASE_SOURCE_V1:complete")
                .setSourceConsistent(true);
    }

    private static MesTeamLeaderActiveOrderReleaseSignatureEvidence signature(String role,
                                                                               Long signatureId,
                                                                               Long userId) {
        return new MesTeamLeaderActiveOrderReleaseSignatureEvidence()
                .setRole(role)
                .setSourceType("FORMAL_SOURCE")
                .setSourceId(signatureId + 1000L)
                .setSignatureId(signatureId)
                .setUserId(userId)
                .setSignedAt(LocalDateTime.of(2026, 8, 1, 9, 0))
                .setEvidenceHash("signature-hash-" + signatureId);
    }
}
