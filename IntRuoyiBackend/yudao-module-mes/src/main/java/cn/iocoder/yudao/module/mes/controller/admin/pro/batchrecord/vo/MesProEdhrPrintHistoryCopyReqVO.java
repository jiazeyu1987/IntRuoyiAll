package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrPrintHistoryCopyReqVO {

    @NotNull(message = "来源打印任务ID不能为空")
    private Long sourcePrintTaskId;

    @NotBlank(message = "来源对象类型不能为空")
    private String sourceObjectType;

    @NotBlank(message = "来源对象编码不能为空")
    private String sourceObjectCode;

    @NotBlank(message = "历史副本原因不能为空")
    private String copyReason;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
