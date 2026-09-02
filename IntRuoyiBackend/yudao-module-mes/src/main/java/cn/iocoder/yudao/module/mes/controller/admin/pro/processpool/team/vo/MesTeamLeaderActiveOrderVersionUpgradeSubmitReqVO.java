package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 班组长活跃订单版本升级重启提交 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderVersionUpgradeSubmitReqVO {

    @Schema(description = "活跃订单记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    @NotNull(message = "活跃订单记录编号不能为空")
    private Long activeOrderId;

    @Schema(description = "提交幂等键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "提交幂等键不能为空")
    private String idempotencyKey;

    @Schema(description = "升级原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "升级原因不能为空")
    private String upgradeReason;

    @Schema(description = "确认审批通过后整单从头执行", requiredMode = Schema.RequiredMode.REQUIRED)
    @AssertTrue(message = "必须确认审批通过后整单从头执行")
    private Boolean confirmRestartFromBeginning;
}
