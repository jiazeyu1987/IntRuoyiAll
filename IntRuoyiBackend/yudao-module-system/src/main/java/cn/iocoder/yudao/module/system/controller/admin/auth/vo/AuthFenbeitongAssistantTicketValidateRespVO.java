package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "管理后台 - 分贝通费用报销助手访问票据校验 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthFenbeitongAssistantTicketValidateRespVO {

    @Schema(description = "是否有效", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean valid;

    @Schema(description = "拒绝原因")
    private String reason;

    @Schema(description = "用户编号")
    private Long userId;

    @Schema(description = "权限集合")
    private Set<String> permissions;

    @Schema(description = "过期时间")
    private LocalDateTime expiresTime;

    @Schema(description = "金蝶配置快照")
    private KingdeeConfig kingdeeConfig;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KingdeeConfig {

        private String baseUrl;
        private String acctId;
        private String username;
        private String password;
        private String appId;
        private String signedData;
        private String timestamp;
        private Integer lcid;

    }

}
