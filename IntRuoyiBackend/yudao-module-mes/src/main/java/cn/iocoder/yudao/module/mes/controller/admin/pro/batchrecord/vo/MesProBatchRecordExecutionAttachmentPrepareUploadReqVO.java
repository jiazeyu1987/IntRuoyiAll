package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionAttachmentPrepareUploadReqVO {

    @NotNull(message = "执行记录编号不能为空")
    private Long executionId;

    @NotNull(message = "工单任务编号不能为空")
    private Long workTaskId;
}
