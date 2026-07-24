package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrPrintTaskMarkFailedReqVO {

    @NotNull(message = "打印任务ID不能为空")
    private Long id;

    @NotBlank(message = "打印失败原因不能为空")
    private String failureReason;
}
