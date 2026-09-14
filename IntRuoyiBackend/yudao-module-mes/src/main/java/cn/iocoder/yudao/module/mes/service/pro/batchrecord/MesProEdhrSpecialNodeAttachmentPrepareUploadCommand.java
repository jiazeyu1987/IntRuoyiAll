package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrSpecialNodeAttachmentPrepareUploadCommand {

    private Long taskId;
    private String idempotencyKey;
    private String fileName;
    private String contentType;
    private byte[] content;
}
