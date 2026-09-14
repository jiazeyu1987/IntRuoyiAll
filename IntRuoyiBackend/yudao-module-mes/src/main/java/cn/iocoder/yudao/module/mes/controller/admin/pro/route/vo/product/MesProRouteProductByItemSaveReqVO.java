package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES 产品侧工艺路线绑定保存 Request VO")
@Data
public class MesProRouteProductByItemSaveReqVO {

    @Schema(description = "产品物料编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品物料编号不能为空")
    private Long itemId;

    @Schema(description = "工艺路线编号；为空时解除绑定", example = "1")
    private Long routeId;

}
