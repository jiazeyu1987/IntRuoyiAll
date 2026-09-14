package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderReleaseDossierCompletenessResult {

    private boolean complete;
    private List<MesTeamLeaderActiveOrderReleaseBlocker> blockers;
}
