package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderVersionUpgradePreview {

    private Long activeOrderId;
    private Long workOrderId;
    private String workOrderCode;
    private Boolean allLatestFormalVersions;
    private Boolean perVersionSelectionAllowed;
    private Boolean submittable;
    private List<String> blockers = List.of();
    private List<MesTeamLeaderActiveOrderVersionUpgradeVersionLine> currentVersions = List.of();
    private List<MesTeamLeaderActiveOrderVersionUpgradeVersionLine> targetVersions = List.of();
}
