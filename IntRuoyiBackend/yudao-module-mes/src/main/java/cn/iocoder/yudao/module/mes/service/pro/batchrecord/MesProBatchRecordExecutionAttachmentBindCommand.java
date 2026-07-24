package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionAttachmentBindCommand {

    private Long executionId;
    private Long workTaskId;
    private Long auditBatchId;
    private Long signatureId;
    private Integer rowIndex;
    private Integer columnIndex;
    private String fieldKey;
    private String fieldPath;
    private String fieldLabel;
    private String attachmentType;
    private String attachmentGroupKey;
    private Long fileId;
    private String fileUrl;
    private Long storageConfigId;
    private String storagePath;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private String storageRetentionJson;
    private String expectedPreviousAttachmentHash;
    private Long operatorId;
    private String operatorName;
    private String reasonCategory;
    private String reasonText;
}
