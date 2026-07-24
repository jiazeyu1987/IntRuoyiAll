package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionReexecuteReqVO {

    @NotNull(message = "来源拒收批次执行ID不能为空")
    private Long sourceRejectedBatchExecutionId;

    @NotBlank(message = "同批号重做原因不能为空")
    private String reason;

    private String remark;
}
