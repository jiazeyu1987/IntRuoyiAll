package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesTeamLeaderLossReasonItem {

    private Long id;
    private String reasonCode;
    private String reasonName;
    private Boolean enabled;

}
