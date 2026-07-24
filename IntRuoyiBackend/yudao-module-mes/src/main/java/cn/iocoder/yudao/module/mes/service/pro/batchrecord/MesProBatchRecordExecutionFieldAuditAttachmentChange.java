package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditAttachmentChange {

    private Long workTaskId;

    private Integer rowIndex;

    private Integer columnIndex;

    private String fieldKey;

    private String fieldPath;

    private String fieldLabel;

    private String attachmentType;

    private String attachmentGroupKey;

    private String attachmentAction;

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
}
