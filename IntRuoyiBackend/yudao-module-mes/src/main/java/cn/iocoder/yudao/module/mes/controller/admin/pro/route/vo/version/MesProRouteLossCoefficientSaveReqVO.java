package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MesProRouteLossCoefficientSaveReqVO {
    @NotNull private Long routeVersionId;
    @NotBlank private String expectedRouteSnapshotSha256;
    @NotNull @DecimalMin(value = "1", message = "损耗系数必须大于等于1")
    private BigDecimal lossCoefficient;
}
