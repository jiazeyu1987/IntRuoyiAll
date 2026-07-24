package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditDetailRespVO {

    private Long executionId;
    private String executionCode;
    private MesProBatchRecordExecutionFieldAuditBatchRespVO auditBatch;
    private List<MesProBatchRecordExecutionFieldAuditItemRespVO> items;
    private List<AttachmentSummary> attachmentSummaries;
    private MesProBatchRecordExecutionFieldAuditSignatureRespVO signature;
    private MesProBatchRecordExecutionFieldAuditHashVerificationRespVO hashVerification;

    @Data
    @Accessors(chain = true)
    public static class AttachmentSummary {

        private Long id;
        private Long auditBatchId;
        private Long signatureId;
        private Long executionId;
        private Long workTaskId;
        private Integer rowIndex;
        private Integer columnIndex;
        private String fieldKey;
        private String fieldPath;
        private String fieldLabel;
        private String attachmentType;
        private String attachmentGroupKey;
        private String attachmentAction;
        private Integer versionNo;
        private Long fileId;
        private String fileUrl;
        private Long storageConfigId;
        private String storagePath;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String sha256;
        private String storageRetentionHash;
        private String previousAttachmentHash;
        private String attachmentHash;
        private Long operatorId;
        private String operatorName;
        private LocalDateTime operatedAt;
        private String reasonCategory;
        private String reasonText;
    }
}
