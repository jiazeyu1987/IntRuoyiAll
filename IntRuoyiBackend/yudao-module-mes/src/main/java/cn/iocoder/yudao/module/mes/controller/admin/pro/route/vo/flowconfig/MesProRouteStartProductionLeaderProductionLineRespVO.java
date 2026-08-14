package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线工序开始生产组长可选负责范围 Response VO")
@Data
@Accessors(chain = true)
public class MesProRouteStartProductionLeaderProductionLineRespVO {

    @Schema(description = "负责范围 ID，当前等于工艺路线 ID", example = "7001")
    private Long productionLineId;

    @Schema(description = "负责范围编码，当前等于工艺路线编码", example = "LINE-A")
    private String productionLineCode;

    @Schema(description = "负责范围名称，当前等于工艺路线名称", example = "压力泵一线")
    private String productionLineName;

    @Schema(description = "该负责范围覆盖的路线工序 ID")
    private List<Long> routeProcessIds;

    @Schema(description = "该负责范围覆盖的工序名称")
    private List<String> processNames;
}
