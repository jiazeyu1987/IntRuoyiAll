package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesTeamLeaderDataCleanupResult {

    private Integer activeOrderCount;
    private Integer reportEventCount;
    private Integer batchExecutionCount;
    private Integer batchRecordExecutionCount;
    private Integer releaseApplicationCount;
    private Integer releaseTransactionCount;
}
