package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionSpecialNodeAttachmentSavePendingReqVO {

    @NotNull(message = "批次执行 ID 不能为空")
    private Long batchExecutionId;
}
