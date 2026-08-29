package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 发票凭证打印助手运行状态 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthInvoiceVoucherPrintAssistantStatusRespVO {

    @Schema(description = "助手是否已运行", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean running;

    @Schema(description = "助手是否允许启动", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean launchable;

    @Schema(description = "状态提示", example = "发票凭证打印助手尚未启动，请点击启动助手。")
    private String message;

}
