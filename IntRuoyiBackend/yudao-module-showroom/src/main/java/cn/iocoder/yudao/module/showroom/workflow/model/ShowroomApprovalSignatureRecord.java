package cn.iocoder.yudao.module.showroom.workflow.model;

import java.time.Instant;

public record ShowroomApprovalSignatureRecord(Long id,
                                              Long changeRequestId,
                                              String approvalStage,
                                              String actionType,
                                              Long actorId,
                                              String signatureMode,
                                              Boolean passwordVerified,
                                              String comment,
                                              Instant signedAt) {
}
