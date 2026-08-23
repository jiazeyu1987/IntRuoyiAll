package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长活跃订单完成 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderCompletionReqVO {

    @Schema(description = "活跃订单记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    @NotNull
    private Long activeOrderId;

    @Schema(description = "客户端持有的活跃订单版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull
    private Integer expectedVersion;

    @Schema(description = "一次性完成幂等键", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "active-order-complete-8101-20260822090000")
    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[\\x21-\\x7E]+")
    private String idempotencyKey;
}
