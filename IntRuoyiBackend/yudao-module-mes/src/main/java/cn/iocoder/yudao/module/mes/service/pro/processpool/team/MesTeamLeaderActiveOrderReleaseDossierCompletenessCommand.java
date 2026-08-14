package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseDossierCompletenessCommand {

    private Long batchExecutionId;
    private String sourceSnapshotHash;
    private List<MesTeamLeaderActiveOrderReleaseDocumentEvidence> documents;
    private Long releaseApprovalRuleId;
    private List<Long> releaseOwnerCandidateUserIds;
}
