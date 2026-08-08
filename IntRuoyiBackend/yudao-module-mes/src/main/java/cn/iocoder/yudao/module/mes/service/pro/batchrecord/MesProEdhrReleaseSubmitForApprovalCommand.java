package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrReleaseSubmitForApprovalCommand {

    private Long releaseTransactionId;
    private String idempotencyKey;
    private String submitReason;
}
