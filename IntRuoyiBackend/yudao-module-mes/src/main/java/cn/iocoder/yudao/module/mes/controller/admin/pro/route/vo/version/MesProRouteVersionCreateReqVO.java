package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 工艺路线候选版本创建 Request VO")
@Data
public class MesProRouteVersionCreateReqVO {

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "工艺路线编号不能为空")
    private Long routeId;

    @Schema(description = "来源路线版本编号")
    private Long sourceRouteVersionId;

    @Schema(description = "变更原因")
    private String changeReason;

    @Schema(description = "是否从现有生产组长正式配置迁移历史生产配置")
    private Boolean migrateLegacyProductionConfig;

    @Schema(description = "迁移时对缺失允许超量比例的显式统一补值，范围 0 到 100")
    private BigDecimal missingOveragePercent;

}
