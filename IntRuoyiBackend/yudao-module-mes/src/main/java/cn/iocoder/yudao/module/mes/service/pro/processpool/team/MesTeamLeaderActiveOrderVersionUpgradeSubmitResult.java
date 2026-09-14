package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderVersionUpgradeSubmitResult {

    private Long activeOrderId;
    private String requestCode;
    private String approvalStatus;
    private String freezeStatus;
}
