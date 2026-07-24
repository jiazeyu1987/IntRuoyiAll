package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionTaskOpenReqVO {

    @NotNull(message = "batchExecutionId 不能为空")
    private Long batchExecutionId;

    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    private Long workTaskId;
}
