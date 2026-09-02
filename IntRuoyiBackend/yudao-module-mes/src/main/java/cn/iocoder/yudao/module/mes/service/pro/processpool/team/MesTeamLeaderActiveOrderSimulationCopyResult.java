package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesTeamLeaderActiveOrderSimulationCopyResult {

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
