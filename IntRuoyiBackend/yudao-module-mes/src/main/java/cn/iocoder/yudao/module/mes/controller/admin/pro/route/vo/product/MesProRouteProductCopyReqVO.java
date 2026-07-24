package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 工艺路线产品复制 Request VO")
@Data
public class MesProRouteProductCopyReqVO {

    @Schema(description = "路线版本编号；传入 DRAFT 候选版本时仅写候选快照", example = "100")
    private Long routeVersionId;

    @Schema(description = "源工艺路线产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "源工艺路线产品编号不能为空")
    private Long sourceRouteProductId;

    @Schema(description = "目标产品物料编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "目标产品物料编号不能为空")
    private Long targetItemId;

    @Schema(description = "生产数量", example = "100")
    private Integer quantity;

    @Schema(description = "生产用时", example = "60.00")
    private BigDecimal productionTime;

    @Schema(description = "时间单位", example = "MINUTE")
    private String timeUnitType;

    @Schema(description = "备注")
    private String remark;

}
