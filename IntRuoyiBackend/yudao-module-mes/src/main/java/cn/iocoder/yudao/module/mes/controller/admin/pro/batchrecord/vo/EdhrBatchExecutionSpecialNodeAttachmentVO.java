package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionSpecialNodeAttachmentVO {

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
