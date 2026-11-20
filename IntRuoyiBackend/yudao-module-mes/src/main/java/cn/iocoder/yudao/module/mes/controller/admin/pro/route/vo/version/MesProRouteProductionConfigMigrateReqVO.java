package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 只向尚无生产配置快照的草稿迁移正式配置，不改写生效版本。 */
@Data
public class MesProRouteProductionConfigMigrateReqVO {
    @NotNull private Long routeVersionId;
    @NotBlank private String expectedRouteSnapshotSha256;
    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal missingOveragePercent;
}
