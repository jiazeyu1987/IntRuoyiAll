package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 运行控制台事故关闭 Request VO")
@Data
public class RuntimeControlIncidentCloseReqVO {

    @Schema(description = "责任人门禁结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "PASSED")
    @NotBlank(message = "责任人门禁结果不能为空")
    private String ownerGateResult;

    @Schema(description = "验证结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "PASSED")
    @NotBlank(message = "验证结果不能为空")
    private String verificationResult;

    @Schema(description = "剩余风险", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "剩余风险不能为空")
    private String remainingRisk;

    @Schema(description = "复盘状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "DONE")
    @NotBlank(message = "复盘状态不能为空")
    private String postmortemStatus;

    @Schema(description = "关闭原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关闭原因不能为空")
    private String closeReason;
}
