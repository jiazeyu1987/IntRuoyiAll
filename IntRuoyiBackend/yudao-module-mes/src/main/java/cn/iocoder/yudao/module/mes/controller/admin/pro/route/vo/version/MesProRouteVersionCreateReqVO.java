package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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

}
