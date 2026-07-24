package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线版本发布阻断 Response VO")
@Data
public class MesProRouteVersionBlockerRespVO {

    @Schema(description = "路线版本编号")
    private Long routeVersionId;

    @Schema(description = "是否可发布")
    private Boolean publishable;

    @Schema(description = "阻断原因")
    private List<String> blockers;

}
