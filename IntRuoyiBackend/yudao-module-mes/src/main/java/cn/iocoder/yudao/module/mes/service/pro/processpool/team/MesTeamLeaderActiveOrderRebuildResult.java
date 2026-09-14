package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRebuildResult {

    private Long activeOrderId;
    private boolean historicalRuntimeDataDeleted;
    private int deletedProductionReportCount;
    private int deletedProductionProgressCount;
    private int deletedPqcInspectionResultCount;
    private int deletedProcessSnapshotCount;
    private int deletedPqcTaskCount;
    private int rebuiltProcessSnapshotCount;
    private int rebuiltPqcTaskCount;
}
