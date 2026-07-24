package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesProBatchRecordExecutionAttachmentPrepareUploadResult {

    private String uploadToken;

    private Long fileId;

    private String fileUrl;

    private Long storageConfigId;

    private String storagePath;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String sha256;

    private String storageRetentionJson;

    private String storageRetentionHash;
}
