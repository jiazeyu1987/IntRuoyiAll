package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesIndependentBatchPrerequisiteReceiptRevokeCommand {
    private String receiptId;
    private String reason;
}
