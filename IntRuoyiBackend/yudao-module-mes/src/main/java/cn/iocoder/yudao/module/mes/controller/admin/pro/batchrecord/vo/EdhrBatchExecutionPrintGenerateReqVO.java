package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/** Renders a released active-order batch PDF without creating or changing an archive. */
@Data
@Accessors(chain = true)
public class EdhrBatchExecutionPrintGenerateReqVO {
    @NotNull(message = "batchExecutionId 不能为空")
    private Long batchExecutionId;
}
