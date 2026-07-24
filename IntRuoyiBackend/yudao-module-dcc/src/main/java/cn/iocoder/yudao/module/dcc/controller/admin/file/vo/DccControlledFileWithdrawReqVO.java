package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccControlledFileWithdrawReqVO {

    @NotBlank(message = "撤回原因不能为空")
    private String reason;
}
