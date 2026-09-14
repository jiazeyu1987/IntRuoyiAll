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

    @Schema(description = "金蝶配置快照")
    private KingdeeConfig kingdeeConfig;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KingdeeConfig {

        @Schema(description = "金蝶基础地址", example = "http://172.30.30.8/K3Cloud")
        private String baseUrl;

        @Schema(description = "账套 ID", example = "6977227150362f")
        private String acctId;

        @Schema(description = "用户名", example = "kingdee-user")
        private String username;

        @Schema(description = "密码", example = "password")
        private String password;

        @Schema(description = "金蝶应用 ID", example = "invoice-print-app")
        private String appId;

        @Schema(description = "金蝶应用密钥", example = "invoice-print-secret")
        private String signedData;

        @Schema(description = "SimPas 签名时间戳", example = "1787795088")
        private String timestamp;

        @Schema(description = "语言 LCID", example = "2052")
        private Integer lcid;

    }

}
