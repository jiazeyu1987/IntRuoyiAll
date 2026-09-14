package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 分贝通费用报销助手访问票据校验 Request VO")
@Data
public class AuthFenbeitongAssistantTicketValidateReqVO {

    @Schema(description = "短期访问票据", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "票据不能为空")
    private String ticket;

}
