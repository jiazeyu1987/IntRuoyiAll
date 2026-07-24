package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台责任人矩阵保存 Request VO")
@Data
public class RuntimeControlOwnerMatrixSaveReqVO {

    @Schema(description = "环境", requiredMode = Schema.RequiredMode.REQUIRED, example = "prod")
    @NotBlank(message = "环境不能为空")
    private String environment;

    @Schema(description = "动作", requiredMode = Schema.RequiredMode.REQUIRED, example = "promote-prod")
    @NotBlank(message = "动作不能为空")
    private String action;

    @Schema(description = "角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "release-owner")
    @NotBlank(message = "角色不能为空")
    private String role;

    @Schema(description = "是否必填", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "是否必填不能为空")
    private Boolean required;

    @Schema(description = "责任人用户编号")
    private Long ownerUserId;

    @Schema(description = "责任人姓名")
    private String ownerName;

    @Schema(description = "升级说明")
    private String escalationPath;
}
