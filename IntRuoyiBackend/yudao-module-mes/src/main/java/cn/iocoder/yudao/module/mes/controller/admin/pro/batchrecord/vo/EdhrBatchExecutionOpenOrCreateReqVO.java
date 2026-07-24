package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionOpenOrCreateReqVO {

    @NotNull(message = "workOrderId 不能为空")
    private Long workOrderId;

    @NotBlank(message = "batchCode 不能为空")
    private String batchCode;

    private Long routeId;

    private String remark;
}
