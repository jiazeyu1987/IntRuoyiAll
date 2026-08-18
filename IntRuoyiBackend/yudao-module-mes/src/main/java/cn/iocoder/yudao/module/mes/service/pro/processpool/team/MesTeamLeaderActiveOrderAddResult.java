package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MesTeamLeaderActiveOrderAddResult {

    public static final String ACTION_ADD = "ADD";
    public static final String ACTION_REUSE = "REUSE";
    public static final String ACTION_RECOVER = "RECOVER";

    private Long activeOrderId;
    private String action;
}
