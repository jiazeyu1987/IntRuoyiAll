package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Schema(description = "Admin - DCC controlled file electronic distribution recipient sign request")
@Data
public class DccControlledFileDistributionRecipientSignReqVO {

    @Schema(description = "加签接收人用户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "加签接收人不能为空")
    private List<Long> userIds;

    @Schema(description = "当前登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "加签意见", example = "请同步接收")
    @Size(max = 1000, message = "加签意见不能超过 1000 个字符")
    private String comment;

}
