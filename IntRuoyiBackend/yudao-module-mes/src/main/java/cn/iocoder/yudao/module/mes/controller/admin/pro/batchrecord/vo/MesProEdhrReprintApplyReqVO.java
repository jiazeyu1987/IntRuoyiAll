package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrReprintApplyReqVO {

    @NotNull(message = "原打印任务ID不能为空")
    private Long originalPrintTaskId;

    @NotBlank(message = "补打原因编码不能为空")
    private String reprintReasonCode;

    @NotBlank(message = "补打原因不能为空")
    private String reprintReason;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
