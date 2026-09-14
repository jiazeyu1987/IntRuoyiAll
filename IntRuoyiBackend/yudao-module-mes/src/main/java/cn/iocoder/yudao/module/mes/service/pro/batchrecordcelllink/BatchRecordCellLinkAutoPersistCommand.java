package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkAutoPersistCommand {

    private Long executionId;

    private Long workTaskId;

    private String trigger;

    private String idempotencyNamespace;
}
