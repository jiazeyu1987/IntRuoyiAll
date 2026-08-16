package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 工艺路线候选版本产品复制 Request VO")
@Data
public class MesProRouteProductCandidateCopyReqVO {

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工艺路线编号不能为空")
    private Long routeId;

    @Schema(description = "路线候选版本编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "路线候选版本编号不能为空")
    private Long routeVersionId;

    @Schema(description = "源产品物料编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "源产品物料编号不能为空")
    private Long sourceItemId;

    @Schema(description = "目标产品物料编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "目标产品物料编号不能为空")
    private Long targetItemId;

}
