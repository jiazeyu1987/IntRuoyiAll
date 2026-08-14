package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseProcessInspectionPlanCommand {

    private Long tenantId;

    private Long activeOrderId;

    private Long workOrderId;

    private Long productId;

    private Long routeId;

    private Long routeVersionId;

    private String batchCode;

    private String sourceSnapshotHash;
}
