package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderCompletionCommand {

    private Long activeOrderId;
    private Integer expectedVersion;
    private String idempotencyKey;
}
