package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrSpecialNodeAttachment {

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
