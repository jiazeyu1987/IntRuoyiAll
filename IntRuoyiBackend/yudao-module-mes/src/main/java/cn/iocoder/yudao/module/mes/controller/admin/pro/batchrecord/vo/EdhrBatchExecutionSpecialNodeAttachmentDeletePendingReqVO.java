package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionSpecialNodeAttachmentDeletePendingReqVO {

    @NotNull(message = "任务 ID 不能为空")
    private Long taskId;

    @Valid
    @NotNull(message = "待提交附件不能为空")
    private EdhrBatchExecutionSpecialNodeAttachmentVO attachment;
}
