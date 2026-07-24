package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.process;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES 工艺路线工序关系 Response VO")
@Data
public class MesProRouteProcessRelationRespVO {

    @Schema(description = "路线工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long routeProcessId;

    @Schema(description = "工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long processId;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "工序名称")
    private String processName;

}
