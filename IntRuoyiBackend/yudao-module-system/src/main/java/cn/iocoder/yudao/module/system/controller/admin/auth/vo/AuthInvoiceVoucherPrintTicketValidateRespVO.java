package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 发票凭证打印助手访问票据校验 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthInvoiceVoucherPrintTicketValidateRespVO {

    @Schema(description = "是否有效", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean valid;

    @Schema(description = "拒绝原因", example = "expired")
    private String reason;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "权限标识", example = "erp:invoice-voucher-print:query")
    private String permission;

    @Schema(description = "过期时间")
    private LocalDateTime expiresTime;

}
