package cn.iocoder.yudao.module.showroom.workflow.model;

import java.util.List;

public record ShowroomApprovalDetail(ShowroomChangeRequest changeRequest,
                                     List<ShowroomChangeRequestItem> fieldDiffs,
                                     ShowroomApprovalTargetPreview targetPreview,
                                     List<ShowroomVersionAudit> versionDiffs,
                                     List<ShowroomApprovalSignatureRecord> signatureRecords) {

    public ShowroomApprovalDetail {
        fieldDiffs = List.copyOf(fieldDiffs);
        versionDiffs = List.copyOf(versionDiffs);
        signatureRecords = List.copyOf(signatureRecords);
    }

}
