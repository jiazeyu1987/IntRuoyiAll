package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 分贝通费用报销助手状态 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthFenbeitongAssistantStatusRespVO {

    @Schema(description = "是否正在运行", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean running;

    @Schema(description = "当前配置是否允许启动", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean launchable;

    @Schema(description = "状态说明")
    private String message;

}
