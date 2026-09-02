package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderVersionUpgradeSubmitCommand {

    private Long activeOrderId;
    private String idempotencyKey;
    private String upgradeReason;
    private Boolean confirmRestartFromBeginning;
}
