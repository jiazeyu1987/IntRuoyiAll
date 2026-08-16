package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长活跃订单申请放行 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseApplyReqVO {

    @Schema(description = "活跃订单记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    @NotNull
    private Long activeOrderId;

    @Schema(description = "请求幂等键", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "active-order-release-8101-20260808120000")
    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[\\x21-\\x7E]+")
    private String idempotencyKey;

    @Schema(description = "申请说明", example = "生产与检验均已完成，申请负责人放行")
    @Size(max = 500)
    private String applyRemark;
}
