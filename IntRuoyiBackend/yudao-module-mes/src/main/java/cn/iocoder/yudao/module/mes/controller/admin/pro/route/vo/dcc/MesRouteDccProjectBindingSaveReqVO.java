package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 工艺路线 DCC 项目代码绑定保存 Request VO")
@Data
@Accessors(chain = true)
public class MesRouteDccProjectBindingSaveReqVO {

    @Schema(description = "工艺路线ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "工艺路线ID不能为空")
    private Long routeId;

    @Schema(description = "DCC项目代码ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "DCC项目代码ID不能为空")
    private Long dccProjectCodeId;

    @Schema(description = "前端读取到的关系版本", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "关系版本不能为空")
    private Long expectedVersion;
}
