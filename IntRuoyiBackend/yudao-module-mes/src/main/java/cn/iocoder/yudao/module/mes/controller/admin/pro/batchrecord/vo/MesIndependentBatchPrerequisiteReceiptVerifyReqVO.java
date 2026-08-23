package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesIndependentBatchPrerequisiteReceiptVerifyReqVO {
    @NotBlank private String receiptId;
    @NotBlank private String entryType;
    @NotBlank private String sourceSnapshotHash;
}
