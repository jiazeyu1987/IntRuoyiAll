package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseLossReportPlanCommand {

    private Long tenantId;
    private Long activeOrderId;
    private Long workOrderId;
    private Long routeId;
    private Long routeVersionId;
    private Long productId;
    private String batchCode;
    private String sourceSnapshotHash;
    private List<MesProcessPoolActiveOrderProcessSnapshotDO> processSnapshots;
}
