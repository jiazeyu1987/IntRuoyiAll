package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序开始生产组长可选产线 Response VO")
@Data
@Accessors(chain = true)
public class MesProRouteStartProductionLeaderProductionLineRespVO {

    @Schema(description = "产线 ID", example = "7001")
    private Long productionLineId;

    @Schema(description = "产线编码", example = "LINE-A")
    private String productionLineCode;

    @Schema(description = "产线名称", example = "压力泵一线")
    private String productionLineName;

    @Schema(description = "该产线覆盖的路线工序 ID")
    private List<Long> routeProcessIds;

    @Schema(description = "该产线覆盖的工序名称")
    private List<String> processNames;
}
