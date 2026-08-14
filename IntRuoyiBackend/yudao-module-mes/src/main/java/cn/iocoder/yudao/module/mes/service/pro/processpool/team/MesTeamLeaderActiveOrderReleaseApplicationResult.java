package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseApplicationResult {

    private Long applicationId;
    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private Long batchExecutionId;
    private Long releaseTransactionId;
    private Long releaseApprovalWorkTaskId;
    private String status;
    private String statusName;
    private MesTeamLeaderActiveOrderReleaseDossierSummary dossierSummary;
    private List<MesTeamLeaderActiveOrderReleaseBlocker> blockers;
    private LocalDateTime appliedAt;
}
