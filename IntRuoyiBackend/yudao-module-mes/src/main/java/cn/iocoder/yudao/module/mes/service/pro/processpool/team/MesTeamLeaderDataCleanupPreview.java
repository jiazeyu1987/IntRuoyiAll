package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MesTeamLeaderDataCleanupPreview {

    private Long leaderUserId;
    private List<Long> orderIds;
    private List<Integer> orderVersions;
    private List<Long> workOrderIds;
    private List<Long> batchExecutionIds;
    private Integer activeOrderCount;
    private Integer reportEventCount;
    private Integer batchExecutionCount;
    private Integer releaseApplicationCount;
    private Integer releaseTransactionCount;
}
