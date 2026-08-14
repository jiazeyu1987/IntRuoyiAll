package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长正式负责工艺路线 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderResponsibleRouteRespVO {

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    private Long routeId;

    @Schema(description = "工艺路线编码", example = "R-PUMP")
    private String routeCode;

    @Schema(description = "工艺路线名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "球囊扩张压力泵")
    private String routeName;

}
