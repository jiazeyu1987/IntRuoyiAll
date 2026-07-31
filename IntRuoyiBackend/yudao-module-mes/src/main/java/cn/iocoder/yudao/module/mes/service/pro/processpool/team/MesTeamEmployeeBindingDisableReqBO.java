package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class MesTeamEmployeeBindingDisableReqBO {

    private Long bindingId;
    private Long leaderUserId;
}
