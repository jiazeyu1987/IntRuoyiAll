package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderVersionUpgradeApplyResult {

    private Long requestId;
    private String requestCode;
    private Long sourceActiveOrderId;
    private Long sourceWorkOrderId;
    private Long targetActiveOrderId;
    private Long targetBatchExecutionId;
    private Long voidedBatchExecutionId;
    private String requestStatus;
    private String approvalStatus;
    private String freezeStatus;
}
