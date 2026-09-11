package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 分贝通费用报销助手访问票据 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthFenbeitongAssistantTicketRespVO {

    @Schema(description = "短期访问票据", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ticket;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expiresTime;

}
