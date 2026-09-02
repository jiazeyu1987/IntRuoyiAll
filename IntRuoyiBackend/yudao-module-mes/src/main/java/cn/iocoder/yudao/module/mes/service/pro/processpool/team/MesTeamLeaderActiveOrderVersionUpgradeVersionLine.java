package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderVersionUpgradeVersionLine {

    private String objectType;
    private String objectName;
    private Long objectId;
    private Long currentVersionId;
    private String currentVersionNo;
    private Long targetVersionId;
    private String targetVersionNo;
    private Boolean changed;
}
