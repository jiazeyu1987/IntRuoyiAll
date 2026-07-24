package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 从生产订单补齐工艺路线产品 Request VO")
@Data
public class MesProRouteProductBindFromWorkOrdersReqVO {

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工艺路线编号不能为空")
    private Long routeId;

    @Schema(description = "路线版本编号；传入 DRAFT 候选版本时仅写候选快照", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "路线版本编号不能为空")
    private Long routeVersionId;

}
