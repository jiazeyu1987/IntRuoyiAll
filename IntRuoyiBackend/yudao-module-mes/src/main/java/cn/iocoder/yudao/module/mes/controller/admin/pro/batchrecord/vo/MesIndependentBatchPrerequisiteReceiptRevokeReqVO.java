package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesIndependentBatchPrerequisiteReceiptRevokeReqVO {
    @NotBlank private String receiptId;
    @NotBlank private String reason;
}
