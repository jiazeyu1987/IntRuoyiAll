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
}
