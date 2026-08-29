package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 发票凭证打印助手访问票据校验 Request VO")
@Data
public class AuthInvoiceVoucherPrintTicketValidateReqVO {

    @Schema(description = "短期访问票据", requiredMode = Schema.RequiredMode.REQUIRED, example = "c3c4f4f8-...")
    @NotBlank(message = "短期访问票据不能为空")
    private String ticket;

}
