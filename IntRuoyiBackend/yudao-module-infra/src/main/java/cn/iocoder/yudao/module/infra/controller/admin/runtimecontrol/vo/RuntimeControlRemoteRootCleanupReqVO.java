package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台远程根分区临时目录清理 Request VO")
@Data
public class RuntimeControlRemoteRootCleanupReqVO {

    @Schema(description = "目标环境，支持 test/prod/backup", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标环境不能为空")
    private String targetEnvironment;

    @Schema(description = "清理原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "清理原因不能为空")
    private String reason;

    @Schema(description = "高危清理确认文本，正式服或备用服务器必须为 PROD")
    private String prodConfirmText;
}
