package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DccFrozenApprovalSignaturesTest {
    private final DccControlledFileDO file = DccControlledFileDO.builder().id(10L).versionNo("A.2").build();
    private final List<String> stages = List.of("DOC_CONTROL_REVIEW", "MATRIX_REVIEW", "MATRIX_APPROVAL", "DOC_CONTROL_APPROVAL");

    private List<DccControlledFileRouteSnapshotDO> roster() {
        return stages.stream().map(stage -> DccControlledFileRouteSnapshotDO.builder().controlledFileId(10L)
                .stageCode(stage).resolvedUserIds("1,2").approveMethod(stage.equals("MATRIX_REVIEW") ? "ALL" : "ANY")
                .requireAllApprovals(stage.equals("MATRIX_REVIEW")).build()).toList();
    }

    private List<DccControlledFileSignatureDO> signed() {
        var result = new ArrayList<DccControlledFileSignatureDO>();
        stages.forEach(stage -> result.add(signature(stage, 1L)));
        result.add(signature("MATRIX_REVIEW", 2L));
        return result;
    }

    private DccControlledFileSignatureDO signature(String stage, long actor) {
        return DccControlledFileSignatureDO.builder().id(actor).controlledFileId(10L).revisionId(10L)
                .versionNo("A.2").actorId(actor).actionType("APPROVE").meaningCode(stage + "_APPROVE")
                .passwordVerified(true).signedAt(LocalDateTime.now()).taskId(stage + actor)
                .evidenceStatus("VALID").evidenceHash("signature-hash").build();
    }

    @Test
    void completeAllAndAnyStagesAreAccepted() {
        assertDoesNotThrow(() -> DccFrozenApprovalSignatures.requireComplete(file, roster(), signed()));
    }

    @Test
    void missingSecondMandatorySignerCannotBeReplacedByDuplicateFirstSignature() {
        var signatures = signed(); signatures.remove(signatures.size() - 1);
        signatures.add(signature("MATRIX_REVIEW", 1L));
        assertThrows(ServiceException.class, () -> DccFrozenApprovalSignatures.requireComplete(file, roster(), signatures));
    }

    @Test
    void wrongVersionUnrelatedUserRejectedActionOrInvalidEvidenceCannotCount() {
        for (int mutation = 0; mutation < 6; mutation++) {
            var signatures = signed(); var required = signatures.get(signatures.size() - 1);
            switch (mutation) {
                case 0 -> required.setVersionNo("A.1");
                case 1 -> required.setRevisionId(11L);
                case 2 -> required.setActorId(3L);
                case 3 -> required.setActionType("REJECT");
                case 4 -> required.setEvidenceStatus("INVALID");
                case 5 -> required.setPasswordVerified(false);
            }
            assertThrows(ServiceException.class, () -> DccFrozenApprovalSignatures.requireComplete(file, roster(), signatures));
        }
    }

    @Test
    void missingStageDuplicateStageOrMalformedRosterFailsClosed() {
        var missing = new ArrayList<>(roster()); missing.remove(0);
        assertThrows(ServiceException.class, () -> DccFrozenApprovalSignatures.requireComplete(file, missing, signed()));
        var duplicate = new ArrayList<>(roster()); duplicate.set(1, duplicate.get(0));
        assertThrows(ServiceException.class, () -> DccFrozenApprovalSignatures.requireComplete(file, duplicate, signed()));
        var malformed = roster(); malformed.get(1).setResolvedUserIds("1,");
        assertThrows(ServiceException.class, () -> DccFrozenApprovalSignatures.requireComplete(file, malformed, signed()));
    }

    @Test
    void previousWorkflowRoundCannotSatisfyCurrentFrozenApprovalRoster() {
        file.setProcessInstanceId("current-round");
        var signatures=signed();
        for(var signature:signatures) {
            signature.setProcessInstanceId("current-round");
            signature.setEvidencePayloadVersion("v4-workflow");
        }
        assertDoesNotThrow(()->DccFrozenApprovalSignatures.requireComplete(file,roster(),signatures));
        signatures.get(signatures.size()-1).setProcessInstanceId("previous-round");
        assertThrows(ServiceException.class,()->DccFrozenApprovalSignatures.requireComplete(file,roster(),signatures));
    }
    @Test void historicalFormatsCannotSatisfyTheNewNativeRoundEvenIfStoredValidAndTaskIdsMatch() {
        var nativeFile=nativeFile();var nativeRoster=nativeRoster();var signatures=nativeSignatures();
        assertDoesNotThrow(()->DccFrozenApprovalSignatures.requireComplete(nativeFile,nativeRoster,signatures,nativeAssignments()));
        for(String format:List.of("v1","v2","v3","")) {
            var changed=nativeSignatures();changed.get(1).setEvidencePayloadVersion(format);changed.get(1).setProcessInstanceId(null);
            assertThrows(ServiceException.class,()->DccFrozenApprovalSignatures.requireComplete(nativeFile,nativeRoster,changed,nativeAssignments()));
        }
    }
    @Test void theNewNativeRoundCannotBeOmittedFromTheFileOrItsAssignmentEvidence() {
        var nativeFile=nativeFile();nativeFile.setProcessInstanceId(null);
        var signed=nativeSignatures();signed.forEach(sig->sig.setProcessInstanceId(null));
        var assigned=nativeAssignments();assigned.forEach(row->row.setProcessInstanceId(null));
        assertThrows(ServiceException.class,()->DccFrozenApprovalSignatures.requireComplete(nativeFile,nativeRoster(),signed,assigned));
    }
    private DccControlledFileDO nativeFile(){return DccControlledFileDO.builder().id(10L).versionNo("A/1")
            .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).processInstanceId("current-round").build();}
    private List<DccControlledFileRouteSnapshotDO> nativeRoster(){return List.of(
            DccControlledFileRouteSnapshotDO.builder().controlledFileId(10L).stageCode("MATRIX_REVIEW").candidateSourceType("DEPT")
                    .candidateSourceIds("51").resolvedUserIds("1").approveMethod("ALL").requireAllApprovals(true).build(),
            DccControlledFileRouteSnapshotDO.builder().controlledFileId(10L).stageCode("MATRIX_APPROVAL").resolvedUserIds("2").approveMethod("ANY").build(),
            DccControlledFileRouteSnapshotDO.builder().controlledFileId(10L).stageCode("DOC_CONTROL_REVIEW").resolvedUserIds("3").approveMethod("ANY").build());}
    private List<DccControlledFileTaskAssigneeSnapshotDO> nativeAssignments(){return List.of(DccControlledFileTaskAssigneeSnapshotDO.builder()
            .controlledFileId(10L).departmentId(51L).stageCode("MATRIX_REVIEW").processInstanceId("current-round").leaderUserId(1L)
            .assigneeUserId(1L).assignmentSignatureId(100L).assignedTime(LocalDateTime.now()).bpmTaskId("review-task").build());}
    private List<DccControlledFileSignatureDO> nativeSignatures(){var assignment=nativeSignature(100L,1L,"review-task","MATRIX_REVIEW");
        assignment.setActionType("ASSIGN");assignment.setMeaningCode("MATRIX_REVIEW_ASSIGN");return List.of(assignment,
            nativeSignature(101L,1L,"review-task","MATRIX_REVIEW"),nativeSignature(102L,2L,"approval-task","MATRIX_APPROVAL"),
            nativeSignature(103L,3L,"control-task","DOC_CONTROL_REVIEW"));}
    private DccControlledFileSignatureDO nativeSignature(long id,long actor,String task,String stage){return DccControlledFileSignatureDO.builder()
            .id(id).controlledFileId(10L).revisionId(10L).versionNo("A/1").actorId(actor).actionType("APPROVE").meaningCode(stage+"_APPROVE")
            .taskId(task).processInstanceId("current-round").evidencePayloadVersion("v4-workflow").evidenceStatus("VALID")
            .passwordVerified(true).signedAt(LocalDateTime.now()).evidenceHash("frozen-hmac").build();}
}
