package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRebuildPreview {

    private Long activeOrderId;
    private boolean hasHistoricalRuntimeData;
    private int productionReportCount;
    private int productionProgressCount;
    private int pqcInspectionResultCount;
    private int processSnapshotCount;
    private int pqcTaskCount;
    private int releaseApplicationCount;
    private int eventCount;
}
