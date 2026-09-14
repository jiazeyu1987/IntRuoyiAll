package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - MES 生产组长最新版本模拟订单复制 Response VO")
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderSimulationCopyRespVO {

    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private String workOrderName;
    private Long routeId;
    private Long routeVersionId;
    private String routeVersionNo;
    private Long qaRegulationVersionId;
    private String simulationRunId;
}
