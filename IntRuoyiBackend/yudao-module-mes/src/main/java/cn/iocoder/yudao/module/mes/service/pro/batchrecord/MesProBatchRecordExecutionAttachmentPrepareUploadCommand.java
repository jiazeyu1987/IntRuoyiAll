package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionAttachmentPrepareUploadCommand {

    private Long executionId;

    private Long workTaskId;

    private Long operatorId;

    private String fileName;

    private String contentType;

    private byte[] content;
}
