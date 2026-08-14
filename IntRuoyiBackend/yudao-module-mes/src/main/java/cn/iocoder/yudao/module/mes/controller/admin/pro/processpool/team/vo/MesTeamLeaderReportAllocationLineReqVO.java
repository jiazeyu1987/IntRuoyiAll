package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 班组长报工分配行 Request VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderReportAllocationLineReqVO {

    @Schema(description = "活跃订单记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8101")
    @NotNull
    private Long activeOrderId;

    @Schema(description = "分配数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "80")
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal allocatedQuantity;
}
