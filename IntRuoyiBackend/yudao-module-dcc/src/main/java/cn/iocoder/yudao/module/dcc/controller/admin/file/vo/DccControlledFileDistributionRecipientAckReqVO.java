package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "Admin - DCC controlled file electronic distribution receipt request")
@Data
public class DccControlledFileDistributionRecipientAckReqVO {

    @Schema(description = "当前登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "签收意见", example = "已收到")
    @Size(max = 1000, message = "签收意见不能超过 1000 个字符")
    private String comment;

}
