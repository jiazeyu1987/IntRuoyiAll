package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 工艺路线 DCC 项目代码绑定 Response VO")
@Data
@Accessors(chain = true)
public class MesRouteDccProjectBindingRespVO {

    @Schema(description = "工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long routeId;

    @Schema(description = "DCC项目代码ID，未绑定时为空", example = "200")
    private Long dccProjectCodeId;

    @Schema(description = "当前关系版本，未绑定且无历史时为0", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long version;

    @Schema(description = "是否存在当前绑定", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean bound;
}
