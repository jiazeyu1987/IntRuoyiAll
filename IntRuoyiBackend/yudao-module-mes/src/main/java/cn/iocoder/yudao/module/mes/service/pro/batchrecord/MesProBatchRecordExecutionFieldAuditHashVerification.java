package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditHashVerification {

    private MesProBatchRecordExecutionFieldAuditHashVerificationStatus status;

    private String calculatedHeadHash;

    private String storedHeadHash;

    private Long checkedBatchCount;

    private Long checkedItemCount;

    private Long brokenBatchId;

    private Long brokenItemId;

    private String failedReason;

    private LocalDateTime checkedAt;

    private String attachmentChainStatus;

    private Integer checkedAttachmentCount;

    private String attachmentChainHeadHash;

    private Integer attachmentChainIssueCount;

    private String attachmentChainFailedReason;

    public static MesProBatchRecordExecutionFieldAuditHashVerification valid(String calculatedHeadHash,
                                                                             String storedHeadHash,
                                                                             long checkedBatchCount,
                                                                             long checkedItemCount) {
        return new MesProBatchRecordExecutionFieldAuditHashVerification()
                .setStatus(MesProBatchRecordExecutionFieldAuditHashVerificationStatus.VALID)
                .setCalculatedHeadHash(calculatedHeadHash)
                .setStoredHeadHash(storedHeadHash)
                .setCheckedBatchCount(checkedBatchCount)
                .setCheckedItemCount(checkedItemCount)
                .setCheckedAt(LocalDateTime.now());
    }

    public static MesProBatchRecordExecutionFieldAuditHashVerification failed(
            MesProBatchRecordExecutionFieldAuditHashVerificationStatus status,
            String calculatedHeadHash,
            String storedHeadHash,
            long checkedBatchCount,
            long checkedItemCount,
            Long brokenBatchId,
            Long brokenItemId,
            String failedReason) {
        return new MesProBatchRecordExecutionFieldAuditHashVerification()
                .setStatus(status)
                .setCalculatedHeadHash(calculatedHeadHash)
                .setStoredHeadHash(storedHeadHash)
                .setCheckedBatchCount(checkedBatchCount)
                .setCheckedItemCount(checkedItemCount)
                .setBrokenBatchId(brokenBatchId)
                .setBrokenItemId(brokenItemId)
                .setFailedReason(failedReason)
                .setCheckedAt(LocalDateTime.now());
    }
}
