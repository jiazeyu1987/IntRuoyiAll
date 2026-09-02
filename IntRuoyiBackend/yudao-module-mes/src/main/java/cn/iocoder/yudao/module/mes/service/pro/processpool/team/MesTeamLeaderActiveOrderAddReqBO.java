package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderAddReqBO {

    private Long leaderUserId;
    private Long workOrderId;
    private Long pickListId;
    private String pickListCandidateSnapshotHash;
    private String idempotencyKey;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;
}
